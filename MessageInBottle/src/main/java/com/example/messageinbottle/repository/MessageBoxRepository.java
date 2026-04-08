package com.example.messageinbottle.repository;

import com.example.messageinbottle.entity.MessageBox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageBoxRepository extends JpaRepository<MessageBox, Long> {

    List<MessageBox> findByUserIdOrderByCreatedAtDesc(Long userId);
}

