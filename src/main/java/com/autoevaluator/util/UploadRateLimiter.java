package com.autoevaluator.util;



import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UploadRateLimiter {

    private static final long UPLOAD_WINDOW_MS = 1000 * 1000; // 1 minute
    private final Map<String, Long> lastUploadTimes = new ConcurrentHashMap<>();

    public boolean canUpload(String studentUsername, String courseName, String type, String assignmentNumber) {
        String key = buildKey(studentUsername, courseName, type, assignmentNumber);
        Long lastUpload = lastUploadTimes.get(key);
        return lastUpload == null || (Instant.now().toEpochMilli() - lastUpload) > UPLOAD_WINDOW_MS;
    }

    public void recordUpload(String studentUsername, String courseName, String type, String assignmentNumber) {
        String key = buildKey(studentUsername, courseName, type, assignmentNumber);
        lastUploadTimes.put(key, Instant.now().toEpochMilli());
    }

    private String buildKey(String studentUsername, String courseName, String type, String assignmentNumber) {
        return studentUsername + ":" + courseName + ":" + type + ":" + (assignmentNumber != null ? assignmentNumber : "NA");
    }
}
