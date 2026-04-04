package com.example.messageinbottle.controller;

import com.example.messageinbottle.dto.AcceptTaskRequest;
import com.example.messageinbottle.dto.AcceptedTaskResponse;
import com.example.messageinbottle.dto.ApiResponse;
import com.example.messageinbottle.dto.HomeTaskResponse;
import com.example.messageinbottle.dto.PublishTaskRequest;
import com.example.messageinbottle.dto.PublishedTaskResponse;
import com.example.messageinbottle.dto.WalletResponse;
import com.example.messageinbottle.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/home/tasks")
    public ApiResponse<List<HomeTaskResponse>> getHomeTasks() {
        return ApiResponse.success("获取成功", taskService.getHomeTasks());
    }

    @GetMapping("/home/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success("获取成功", taskService.getCategories());
    }

    @PostMapping("/tasks/publish")
    public ApiResponse<HomeTaskResponse> publishTask(@Valid @RequestBody PublishTaskRequest request) {
        return ApiResponse.success("发布成功", taskService.publishTask(request));
    }

    @PostMapping("/tasks/{taskId}/accept")
    public ApiResponse<AcceptedTaskResponse> acceptTask(@PathVariable Long taskId, @Valid @RequestBody AcceptTaskRequest request) {
        return ApiResponse.success("接取成功", taskService.acceptTask(taskId, request.getUserId()));
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ApiResponse<AcceptedTaskResponse> completeTask(@PathVariable Long taskId, @Valid @RequestBody AcceptTaskRequest request) {
        return ApiResponse.success("任务已提交", taskService.completeAcceptedTask(taskId, request.getUserId()));
    }

    @PostMapping("/tasks/{taskId}/cancel")
    public ApiResponse<PublishedTaskResponse> cancelTask(@PathVariable Long taskId, @Valid @RequestBody AcceptTaskRequest request) {
        return ApiResponse.success("取消成功", taskService.cancelPublishedTask(taskId, request.getUserId()));
    }

    @GetMapping("/wallet")
    public ApiResponse<WalletResponse> getWallet(@RequestParam Long userId) {
        return ApiResponse.success("获取成功", taskService.getWallet(userId));
    }

    @GetMapping("/mine/published")
    public ApiResponse<List<PublishedTaskResponse>> getPublished(@RequestParam Long userId) {
        return ApiResponse.success("获取成功", taskService.getPublishedTasks(userId));
    }

    @GetMapping("/mine/completed")
    public ApiResponse<List<PublishedTaskResponse>> getCompleted(@RequestParam Long userId) {
        return ApiResponse.success("获取成功", taskService.getCompletedTasks(userId));
    }

    @GetMapping("/mine/accepted")
    public ApiResponse<List<AcceptedTaskResponse>> getAccepted(@RequestParam Long userId) {
        return ApiResponse.success("获取成功", taskService.getAcceptedTasks(userId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }
}
