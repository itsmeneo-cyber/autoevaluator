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




//
//package com.autoevaluator.util;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.*;
//        import java.util.concurrent.ConcurrentHashMap;

//@Component
//public class GlobalRateLimiterFilter extends OncePerRequestFilter {
//
//    // Allow max 100 requests per IP per 1 minute
//    private static final int MAX_REQUESTS_PER_MINUTE = 100;
//    private static final long WINDOW_MS = 60 * 1000;
//
//    private final Map<String, List<Long>> requestTimestamps = new ConcurrentHashMap<>();
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String ip = request.getRemoteAddr(); // or use X-Forwarded-For if behind proxy
//        long now = System.currentTimeMillis();
//
//        requestTimestamps.putIfAbsent(ip, new ArrayList<>());
//        List<Long> timestamps = requestTimestamps.get(ip);
//
//        // Clean old timestamps
//        timestamps.removeIf(time -> (now - time) > WINDOW_MS);
//
//        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
//            response.setStatus(429);
//            response.setContentType("application/json");
//            response.getWriter().write("{\"message\":\"Too many requests. Please slow down.\"}");
//            return;
//        }
//
//        timestamps.add(now);
//        filterChain.doFilter(request, response);
//    }
//}
