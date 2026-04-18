package com.example.messageinbottle.service;

import com.example.messageinbottle.dto.AcceptedTaskResponse;
import com.example.messageinbottle.dto.AdminReviewItemResponse;
import com.example.messageinbottle.dto.HomeTaskResponse;
import com.example.messageinbottle.dto.PublishTaskRequest;
import com.example.messageinbottle.dto.PublishedTaskResponse;
import com.example.messageinbottle.dto.WalletResponse;
import com.example.messageinbottle.entity.Task;
import com.example.messageinbottle.entity.User;
import com.example.messageinbottle.entity.Wallet;
import com.example.messageinbottle.repository.TaskRepository;
import com.example.messageinbottle.repository.UserRepository;
import com.example.messageinbottle.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class TaskService {

    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final UploadService uploadService;
    private final MessageBoxService messageBoxService;

    public TaskService(TaskRepository taskRepository,
                       UserRepository userRepository,
                       WalletRepository walletRepository,
                       UploadService uploadService,
                       MessageBoxService messageBoxService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.uploadService = uploadService;
        this.messageBoxService = messageBoxService;
    }

    public List<String> getCategories() {
        return List.of("全部", "校园", "跑腿", "学习", "生活");
    }

    public List<HomeTaskResponse> getHomeTasks() {
        processExpiredAcceptedTasks();
        return taskRepository.findByStatusOrderByCreatedAtDesc("OPEN")
                .stream()
                .map(this::toHomeTaskResponse)
                .toList();
    }

    public HomeTaskResponse publishTask(PublishTaskRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("钱包不存在"));

        validatePublishAmount(request.getAmount());
        if (wallet.getBalance() < request.getAmount()) {
            throw new IllegalArgumentException("钱包余额不足，无法支付该任务金额");
        }

        long now = System.currentTimeMillis();
        wallet.setBalance(wallet.getBalance() - request.getAmount());
        wallet.setUpdatedAt(now);
        walletRepository.save(wallet);

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setCategory(request.getCategory());
        task.setDescription(request.getDescription());
        task.setAmount(request.getAmount());
        task.setDeadline(request.getDeadline());
        task.setPublisherId(user.getId());
        task.setPublisherName(user.getNickname());
        task.setPublishTimeText("刚刚");
        task.setStatus("OPEN");
        task.setReviewStatus("进行中");
        task.setCompleted(false);
        task.setTaskImageUrl(request.getImageUrl());
        task.setCompletionProofUrl(null);
        task.setCreatedAt(now);
        task = taskRepository.save(task);
        messageBoxService.createAndPush(
                user.getId(),
                task.getId(),
                "publish_success",
                "发布提醒",
                "您发布的任务《" + task.getTitle() + "》已发布成功"
        );
        return toHomeTaskResponse(task);
    }

    public AcceptedTaskResponse acceptTask(Long taskId, Long userId) {
        processExpiredAcceptedTasks();
        userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        if (!"OPEN".equals(task.getStatus())) {
            throw new IllegalArgumentException("该任务已被接取");
        }
        if (userId.equals(task.getPublisherId())) {
            throw new IllegalArgumentException("不能接取自己发布的任务");
        }
        if (isTaskExpired(task)) {
            refundPublisher(task);
            task.setStatus("CANCELLED");
            task.setReviewStatus("已超时关闭");
            taskRepository.save(task);
            throw new IllegalArgumentException("任务已超时，无法接取");
        }

        task.setStatus("ACCEPTED");
        task.setAccepterId(userId);
        task.setReviewStatus("进行中");
        task.setCompleted(false);
        task.setCompletionProofUrl(null);
        task = taskRepository.save(task);
        messageBoxService.createAndPush(
                userId,
                task.getId(),
                "accept_success",
                "接取成功",
                "您已成功接取任务《" + task.getTitle() + "》"
        );
        messageBoxService.createAndPush(
                task.getPublisherId(),
                task.getId(),
                "task_accepted",
                "任务接取提醒",
                "您发布的任务《" + task.getTitle() + "》已被接取"
        );
        return toAcceptedTaskResponse(task);
    }

    public AcceptedTaskResponse completeAcceptedTask(Long taskId, Long userId, MultipartFile proofImage) {
        processExpiredAcceptedTasks();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        if (!userId.equals(task.getAccepterId())) {
            throw new IllegalArgumentException("只能操作自己接取的任务");
        }
        if (!"ACCEPTED".equals(task.getStatus())) {
            throw new IllegalArgumentException("当前任务状态不可提交");
        }
        if (isTaskExpired(task)) {
            refundPublisher(task);
            task.setStatus("CANCELLED");
            task.setReviewStatus("已超时关闭");
            task.setCompleted(false);
            task.setCompletionProofUrl(null);
            taskRepository.save(task);
            throw new IllegalArgumentException("任务已超时，金额已退回发布者");
        }
        if (Boolean.TRUE.equals(task.getCompleted())) {
            throw new IllegalArgumentException("任务已完成提交");
        }

        String proofUrl = uploadService.uploadTaskProof(proofImage, taskId, userId);
        task.setCompleted(true);
        task.setReviewStatus("待审核");
        task.setCompletionProofUrl(proofUrl);
        task = taskRepository.save(task);
        messageBoxService.createAndPush(
                task.getPublisherId(),
                task.getId(),
                "task_pending_review",
                "任务待审核提醒",
                "您发布的任务《" + task.getTitle() + "》已被完成，等待管理员审核"
        );
        return toAcceptedTaskResponse(task);
    }

    public List<AdminReviewItemResponse> getPendingReviewTasks() {
        processExpiredAcceptedTasks();
        return taskRepository.findByReviewStatusOrderByCreatedAtDesc("待审核")
                .stream()
                .map(this::toAdminReviewItemResponse)
                .toList();
    }

    public AdminReviewItemResponse approveTask(Long taskId) {
        Task task = getPendingReviewTask(taskId);
        Long accepterId = task.getAccepterId();
        if (accepterId == null) {
            throw new IllegalArgumentException("任务缺少接单用户");
        }

        Wallet wallet = walletRepository.findByUserId(accepterId)
                .orElseThrow(() -> new IllegalArgumentException("接单用户钱包不存在"));
        wallet.setBalance(wallet.getBalance() + task.getAmount());
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletRepository.save(wallet);

        task.setReviewStatus("审核通过");
        task.setStatus("DONE");
        task = taskRepository.save(task);
        messageBoxService.createAndPush(
                task.getPublisherId(),
                task.getId(),
                "task_completed",
                "任务完成提醒",
                "您发布的任务《" + task.getTitle() + "》已完成"
        );
        messageBoxService.createAndPush(
                accepterId,
                task.getId(),
                "reward_granted",
                "审核通过",
                "您接取的任务《" + task.getTitle() + "》审核通过，金额已发放到您的账户"
        );
        return toAdminReviewItemResponse(task);
    }

    public AdminReviewItemResponse rejectTask(Long taskId) {
        Task task = getPendingReviewTask(taskId);
        Long accepterId = task.getAccepterId();
        refundPublisher(task);
        task.setReviewStatus("驳回");
        task.setStatus("CANCELLED");
        task.setCompleted(false);
        task.setCompletionProofUrl(null);
        task = taskRepository.save(task);
        messageBoxService.createAndPush(
                task.getPublisherId(),
                task.getId(),
                "task_rejected_publisher",
                "审核驳回",
                "您发布的任务《" + task.getTitle() + "》审核未通过，金额已退回"
        );
        if (accepterId != null) {
            messageBoxService.createAndPush(
                    accepterId,
                    task.getId(),
                    "task_rejected_accepter",
                    "任务完成失败",
                    "您接取的任务《" + task.getTitle() + "》审核驳回，任务完成失败"
            );
        }
        return toAdminReviewItemResponse(task);
    }

    public AcceptedTaskResponse cancelAcceptedTask(Long taskId, Long userId) {
        processExpiredAcceptedTasks();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        if (!userId.equals(task.getAccepterId())) {
            throw new IllegalArgumentException("只能取消自己接取的任务");
        }
        if (!"ACCEPTED".equals(task.getStatus()) || Boolean.TRUE.equals(task.getCompleted())) {
            throw new IllegalArgumentException("当前任务不可取消接取");
        }

        refundPublisher(task);
        task.setStatus("CANCELLED");
        task.setReviewStatus("已取消接取");
        task.setCompleted(false);
        task.setCompletionProofUrl(null);
        task = taskRepository.save(task);
        messageBoxService.createAndPush(
                task.getPublisherId(),
                task.getId(),
                "task_accept_cancelled_publisher",
                "任务已取消",
                "您发布的任务《" + task.getTitle() + "》已被接取者取消，金额已退回"
        );
        messageBoxService.createAndPush(
                userId,
                task.getId(),
                "task_accept_cancelled_accepter",
                "取消接取",
                "您已取消接取任务《" + task.getTitle() + "》，任务已结束"
        );
        return toAcceptedTaskResponse(task);
    }

    public PublishedTaskResponse cancelPublishedTask(Long taskId, Long userId) {
        processExpiredAcceptedTasks();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        if (!userId.equals(task.getPublisherId())) {
            throw new IllegalArgumentException("只能取消自己发布的任务");
        }
        if (!"OPEN".equals(task.getStatus())) {
            throw new IllegalArgumentException("当前任务不可取消");
        }

        refundPublisher(task);
        task.setStatus("CANCELLED");
        task.setReviewStatus("已取消");
        task.setCompleted(false);
        task.setCompletionProofUrl(null);
        task = taskRepository.save(task);
        return toPublishedTaskResponse(task);
    }

    public WalletResponse getWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("钱包不存在"));
        return new WalletResponse(wallet.getId(), wallet.getUserId(), wallet.getBalance(), wallet.getUpdatedAt());
    }

    public List<PublishedTaskResponse> getPublishedTasks(Long userId) {
        processExpiredAcceptedTasks();
        return taskRepository.findByPublisherIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toPublishedTaskResponse)
                .toList();
    }

    public List<PublishedTaskResponse> getCompletedTasks(Long userId) {
        processExpiredAcceptedTasks();
        return taskRepository.findByPublisherIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(task -> "DONE".equals(task.getStatus()))
                .map(this::toPublishedTaskResponse)
                .toList();
    }

    public List<AcceptedTaskResponse> getAcceptedTasks(Long userId) {
        processExpiredAcceptedTasks();
        return taskRepository.findByAccepterIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toAcceptedTaskResponse)
                .toList();
    }

    private void validatePublishAmount(Double amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("任务金额必须大于0");
        }
    }

    private void processExpiredAcceptedTasks() {
        long now = System.currentTimeMillis();
        List<Task> acceptedTasks = taskRepository.findByStatusOrderByCreatedAtDesc("ACCEPTED");
        for (Task task : acceptedTasks) {
            if (!Boolean.TRUE.equals(task.getCompleted()) && parseDeadlineMillis(task.getDeadline()) < now) {
                refundPublisher(task);
                task.setStatus("CANCELLED");
                task.setReviewStatus("已超时关闭");
                task.setCompleted(false);
                task.setCompletionProofUrl(null);
                taskRepository.save(task);
                if (task.getPublisherId() != null) {
                    messageBoxService.createAndPush(
                            task.getPublisherId(),
                            task.getId(),
                            "task_timeout_publisher",
                            "任务超时关闭",
                            "您发布的任务《" + task.getTitle() + "》因超时未完成，金额已退回"
                    );
                }
                if (task.getAccepterId() != null) {
                    messageBoxService.createAndPush(
                            task.getAccepterId(),
                            task.getId(),
                            "task_timeout_accepter",
                            "任务已失败",
                            "您接取的任务《" + task.getTitle() + "》已超时，任务完成失败"
                    );
                }
            }
        }
    }

    private boolean isTaskExpired(Task task) {
        return parseDeadlineMillis(task.getDeadline()) < System.currentTimeMillis();
    }

    private long parseDeadlineMillis(String deadline) {
        LocalDateTime dateTime = LocalDateTime.parse(deadline, DEADLINE_FORMATTER);
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void refundPublisher(Task task) {
        Wallet wallet = walletRepository.findByUserId(task.getPublisherId())
                .orElseThrow(() -> new IllegalArgumentException("发布者钱包不存在"));
        if (hasRefunded(task)) {
            return;
        }
        wallet.setBalance(wallet.getBalance() + task.getAmount());
        wallet.setUpdatedAt(System.currentTimeMillis());
        walletRepository.save(wallet);
    }

    private boolean hasRefunded(Task task) {
        String reviewStatus = task.getReviewStatus();
        return "已取消".equals(reviewStatus)
                || "已超时关闭".equals(reviewStatus)
                || "驳回".equals(reviewStatus)
                || "已取消接取".equals(reviewStatus);
    }

    private Task getPendingReviewTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        if (!Boolean.TRUE.equals(task.getCompleted()) || !"待审核".equals(task.getReviewStatus())) {
            throw new IllegalArgumentException("当前任务不在待审核状态");
        }
        return task;
    }

    private HomeTaskResponse toHomeTaskResponse(Task task) {
        return new HomeTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getAmount(),
                task.getPublishTimeText(),
                task.getCategory(),
                task.getDescription(),
                task.getDeadline(),
                task.getPublisherId(),
                task.getPublisherName(),
                task.getTaskImageUrl()
        );
    }

    private AcceptedTaskResponse toAcceptedTaskResponse(Task task) {
        return new AcceptedTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getAmount(),
                task.getDeadline(),
                task.getReviewStatus(),
                Boolean.TRUE.equals(task.getCompleted()),
                task.getCompletionProofUrl()
        );
    }

    private PublishedTaskResponse toPublishedTaskResponse(Task task) {
        return new PublishedTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getAmount(),
                task.getDeadline(),
                mapProgress(task),
                task.getTaskImageUrl(),
                task.getCompletionProofUrl()
        );
    }

    private AdminReviewItemResponse toAdminReviewItemResponse(Task task) {
        return new AdminReviewItemResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCategory(),
                task.getAmount(),
                task.getDeadline(),
                task.getPublisherId(),
                task.getPublisherName(),
                task.getAccepterId(),
                task.getReviewStatus(),
                Boolean.TRUE.equals(task.getCompleted()),
                task.getCompletionProofUrl(),
                task.getCreatedAt()
        );
    }

    private String mapProgress(Task task) {
        if ("OPEN".equals(task.getStatus())) {
            return "已发布";
        }
        if ("ACCEPTED".equals(task.getStatus())) {
            if ("待审核".equals(task.getReviewStatus())) {
                return "待审核";
            }
            if ("驳回".equals(task.getReviewStatus())) {
                return "审核驳回";
            }
            return "已接取";
        }
        if ("CANCELLED".equals(task.getStatus())) {
            return task.getReviewStatus();
        }
        return "已完成";
    }
}
