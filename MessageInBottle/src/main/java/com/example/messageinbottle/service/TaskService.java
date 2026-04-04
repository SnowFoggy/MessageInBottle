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

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, WalletRepository walletRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
    }

    public List<String> getCategories() {
        return List.of("全部", "校园", "跑腿", "学习", "生活");
    }

    public List<HomeTaskResponse> getHomeTasks() {
        return taskRepository.findByStatusOrderByCreatedAtDesc("OPEN")
                .stream()
                .map(this::toHomeTaskResponse)
                .toList();
    }

    public HomeTaskResponse publishTask(PublishTaskRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

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
        task.setCreatedAt(System.currentTimeMillis());
        task = taskRepository.save(task);
        return toHomeTaskResponse(task);
    }

    public AcceptedTaskResponse acceptTask(Long taskId, Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        if (!"OPEN".equals(task.getStatus())) {
            throw new IllegalArgumentException("该任务已被接取");
        }

        task.setStatus("ACCEPTED");
        task.setAccepterId(userId);
        task.setReviewStatus("进行中");
        task.setCompleted(false);
        task = taskRepository.save(task);
        return toAcceptedTaskResponse(task);
    }

    public AcceptedTaskResponse completeAcceptedTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        if (!userId.equals(task.getAccepterId())) {
            throw new IllegalArgumentException("只能操作自己接取的任务");
        }
        if (Boolean.TRUE.equals(task.getCompleted())) {
            throw new IllegalArgumentException("任务已完成提交");
        }

        task.setCompleted(true);
        task.setReviewStatus("待审核");
        task = taskRepository.save(task);
        return toAcceptedTaskResponse(task);
    }

    public List<AdminReviewItemResponse> getPendingReviewTasks() {
        return taskRepository.findByReviewStatusOrderByCreatedAtDesc("待审核")
                .stream()
                .map(this::toAdminReviewItemResponse)
                .toList();
    }

    public AdminReviewItemResponse approveTask(Long taskId) {
        Task task = getPendingReviewTask(taskId);
        task.setReviewStatus("通过");
        task.setStatus("DONE");
        task = taskRepository.save(task);
        return toAdminReviewItemResponse(task);
    }

    public AdminReviewItemResponse rejectTask(Long taskId) {
        Task task = getPendingReviewTask(taskId);
        task.setReviewStatus("驳回");
        task.setStatus("ACCEPTED");
        task.setCompleted(false);
        task = taskRepository.save(task);
        return toAdminReviewItemResponse(task);
    }

    public WalletResponse getWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("钱包不存在"));
        return new WalletResponse(wallet.getId(), wallet.getUserId(), wallet.getBalance(), wallet.getUpdatedAt());
    }

    public List<PublishedTaskResponse> getPublishedTasks(Long userId) {
        return taskRepository.findByPublisherIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toPublishedTaskResponse)
                .toList();
    }

    public List<PublishedTaskResponse> getCompletedTasks(Long userId) {
        return taskRepository.findByPublisherIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(task -> "DONE".equals(task.getStatus()))
                .map(this::toPublishedTaskResponse)
                .toList();
    }

    public List<AcceptedTaskResponse> getAcceptedTasks(Long userId) {
        return taskRepository.findByAccepterIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toAcceptedTaskResponse)
                .toList();
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
                task.getPublisherName()
        );
    }

    private AcceptedTaskResponse toAcceptedTaskResponse(Task task) {
        return new AcceptedTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getAmount(),
                task.getDeadline(),
                task.getReviewStatus(),
                Boolean.TRUE.equals(task.getCompleted())
        );
    }

    private PublishedTaskResponse toPublishedTaskResponse(Task task) {
        return new PublishedTaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getAmount(),
                task.getDeadline(),
                mapProgress(task)
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
            return "进行中";
        }
        return "已完成";
    }
}
