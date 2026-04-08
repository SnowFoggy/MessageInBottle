package com.example.messageinbottle.service;

import com.example.messageinbottle.config.NotificationWebSocketHandler;
import com.example.messageinbottle.dto.MessageBoxResponse;
import com.example.messageinbottle.dto.NotificationEnvelope;
import com.example.messageinbottle.entity.MessageBox;
import com.example.messageinbottle.repository.MessageBoxRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageBoxService {

    private final MessageBoxRepository messageBoxRepository;
    private final NotificationWebSocketHandler notificationWebSocketHandler;

    public MessageBoxService(MessageBoxRepository messageBoxRepository,
                             NotificationWebSocketHandler notificationWebSocketHandler) {
        this.messageBoxRepository = messageBoxRepository;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
    }

    public List<MessageBoxResponse> getMessages(Long userId) {
        return messageBoxRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MessageBoxResponse createAndPush(Long userId, Long taskId, String type, String title, String content) {
        MessageBox messageBox = new MessageBox();
        messageBox.setUserId(userId);
        messageBox.setTaskId(taskId);
        messageBox.setType(type);
        messageBox.setTitle(title);
        messageBox.setContent(content);
        messageBox.setCreatedAt(System.currentTimeMillis());
        MessageBox saved = messageBoxRepository.save(messageBox);
        MessageBoxResponse response = toResponse(saved);
        notificationWebSocketHandler.push(userId, new NotificationEnvelope("message", response));
        return response;
    }

    private MessageBoxResponse toResponse(MessageBox messageBox) {
        return new MessageBoxResponse(
                messageBox.getId(),
                messageBox.getUserId(),
                messageBox.getTaskId(),
                messageBox.getType(),
                messageBox.getTitle(),
                messageBox.getContent(),
                messageBox.getCreatedAt()
        );
    }
}

