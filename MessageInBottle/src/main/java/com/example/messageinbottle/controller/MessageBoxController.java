package com.example.messageinbottle.controller;

import com.example.messageinbottle.dto.ApiResponse;
import com.example.messageinbottle.dto.MessageBoxResponse;
import com.example.messageinbottle.service.MessageBoxService;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageBoxController {

    private final MessageBoxService messageBoxService;

    public MessageBoxController(MessageBoxService messageBoxService) {
        this.messageBoxService = messageBoxService;
    }

    @GetMapping
    public ApiResponse<List<MessageBoxResponse>> getMessages(@RequestParam Long userId) {
        return ApiResponse.success("获取成功", messageBoxService.getMessages(userId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.fail(exception.getMessage());
    }
}

