package com.autoevaluator.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskRateLimiter {

    private static final long WINDOW_MS = 60 * 1000; // 1 minute window
    private final Map<String, Long> lastExecutionTimes = new ConcurrentHashMap<>();

    public boolean canExecute(String studentUsername, String courseName, String taskType, String extraKey) {
        String key = buildKey(studentUsername, courseName, taskType, extraKey);
        Long lastTime = lastExecutionTimes.get(key);
        return lastTime == null || (Instant.now().toEpochMilli() - lastTime) > WINDOW_MS;
    }

    public void recordExecution(String studentUsername, String courseName, String taskType, String extraKey) {
        String key = buildKey(studentUsername, courseName, taskType, extraKey);
        lastExecutionTimes.put(key, Instant.now().toEpochMilli());
    }

    private String buildKey(String studentUsername, String courseName, String taskType, String extraKey) {
        return studentUsername + ":" + courseName + ":" + taskType + ":" + (extraKey != null ? extraKey : "NA");
    }
}
