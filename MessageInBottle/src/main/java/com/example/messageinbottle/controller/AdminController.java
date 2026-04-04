package com.example.messageinbottle.controller;

import com.example.messageinbottle.dto.AdminLoginRequest;
import com.example.messageinbottle.dto.AdminLoginResponse;
import com.example.messageinbottle.dto.AdminReviewItemResponse;
import com.example.messageinbottle.dto.ApiResponse;
import com.example.messageinbottle.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final TaskService taskService;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:123456}")
    private String adminPassword;

    public AdminController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        if (request == null || !adminUsername.equals(request.getUsername()) || !adminPassword.equals(request.getPassword())) {
            throw new IllegalArgumentException("管理员账号或密码错误");
        }
        return ApiResponse.success("登录成功", new AdminLoginResponse(adminUsername, "ADMIN"));
    }

    @GetMapping("/tasks/pending")
    public ApiResponse<List<AdminReviewItemResponse>> getPendingTasks() {
        return ApiResponse.success("获取成功", taskService.getPendingReviewTasks());
    }

    @PostMapping("/tasks/{taskId}/approve")
    public ApiResponse<AdminReviewItemResponse> approve(@PathVariable Long taskId) {
        return ApiResponse.success("审核通过", taskService.approveTask(taskId));
    }

    @PostMapping("/tasks/{taskId}/reject")
    public ApiResponse<AdminReviewItemResponse> reject(@PathVariable Long taskId) {
        return ApiResponse.success("审核驳回", taskService.rejectTask(taskId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }
}


