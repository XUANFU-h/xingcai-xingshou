package com.music.scoring_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stress")
@CrossOrigin
public class StressTestController {

    // 极轻量级监控探针，只读底层内存，不查数据库，零侵入无压力
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getServerMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        try {
            // 1. 获取内存信息
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memUsagePercent = (double) usedMemory / totalMemory * 100.0;

            // 2. 获取 CPU 信息 (近期系统负载)
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage();
            int processors = osBean.getAvailableProcessors();
            // 简单估算 CPU 使用率
            double cpuUsagePercent = (cpuLoad / processors) * 100.0;
            if (cpuUsagePercent < 0) cpuUsagePercent = 0.1; // Windows 系统可能拿不到，给个保底值

            // 3. 获取并发线程信息 (用于评估并发压力和带宽堵塞情况)
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            int activeThreads = threadBean.getThreadCount();
            int peakThreads = threadBean.getPeakThreadCount();

            metrics.put("cpuUsage", Math.min(cpuUsagePercent, 100.0));
            metrics.put("memoryUsage", memUsagePercent);
            metrics.put("usedMemoryMB", usedMemory / (1024 * 1024));
            metrics.put("totalMemoryMB", totalMemory / (1024 * 1024));
            metrics.put("activeThreads", activeThreads);
            metrics.put("peakThreads", peakThreads);

        } catch (Exception e) {
            metrics.put("error", "获取指标失败");
        }
        return ResponseEntity.ok(metrics);
    }
}