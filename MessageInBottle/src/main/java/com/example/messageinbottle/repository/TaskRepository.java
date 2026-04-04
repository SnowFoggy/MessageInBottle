package com.example.messageinbottle.repository;

import com.example.messageinbottle.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatusOrderByCreatedAtDesc(String status);

    List<Task> findByPublisherIdOrderByCreatedAtDesc(Long publisherId);

    List<Task> findByAccepterIdOrderByCreatedAtDesc(Long accepterId);

    List<Task> findByReviewStatusOrderByCreatedAtDesc(String reviewStatus);
}
