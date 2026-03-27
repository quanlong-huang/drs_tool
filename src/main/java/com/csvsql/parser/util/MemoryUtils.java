package com.csvsql.parser.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Utility class for memory monitoring and management.
 *
 * <p>MemoryUtils provides static methods for monitoring JVM memory usage
 * including:</p>
 * <ul>
 *   <li>Current heap memory usage</li>
 *   <li>Maximum available heap memory</li>
 *   <li>Memory usage percentage</li>
 *   <li>Human-readable memory formatting</li>
 * </ul>
 *
 * <p>This utility is useful for monitoring memory during large file processing
 * and triggering garbage collection when needed.</p>
 */
public class MemoryUtils {

    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    /**
     * Get current heap memory usage in bytes.
     */
    public static long getUsedMemory() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getUsed();
    }

    /**
     * Get maximum heap memory in bytes.
     */
    public static long getMaxMemory() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getMax();
    }

    /**
     * Get available heap memory in bytes.
     */
    public static long getAvailableMemory() {
        return getMaxMemory() - getUsedMemory();
    }

    /**
     * Get memory usage percentage.
     */
    public static double getMemoryUsagePercent() {
        long max = getMaxMemory();
        if (max <= 0) return 0;
        return (double) getUsedMemory() / max * 100;
    }

    /**
     * Format memory size for display.
     */
    public static String formatMemory(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * Get memory status string.
     */
    public static String getMemoryStatus() {
        return String.format("Memory: %s / %s (%.1f%%)",
            formatMemory(getUsedMemory()),
            formatMemory(getMaxMemory()),
            getMemoryUsagePercent());
    }

    /**
     * Check if memory usage is above threshold.
     */
    public static boolean isMemoryHigh(double thresholdPercent) {
        return getMemoryUsagePercent() > thresholdPercent;
    }

    /**
     * Suggest garbage collection.
     */
    public static void suggestGC() {
        System.gc();
    }

    /**
     * Get memory info for logging.
     */
    public static String getMemoryInfo() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return String.format("Heap: used=%s, committed=%s, max=%s",
            formatMemory(heapUsage.getUsed()),
            formatMemory(heapUsage.getCommitted()),
            formatMemory(heapUsage.getMax()));
    }
}