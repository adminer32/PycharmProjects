package com.system.service.ai.service;

public interface AiService {

    String generateHomework(String subject, String difficulty, int studentCount, String deadline);

    String generateTrainingPlan(Long studentId, int planDuration);
}
