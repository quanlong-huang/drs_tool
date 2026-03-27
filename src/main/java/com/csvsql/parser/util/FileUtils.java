package com.csvsql.parser.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for file operations.
 *
 * <p>FileUtils provides static methods for common file system operations
 * including:</p>
 * <ul>
 *   <li>File existence and type checking</li>
 *   <li>Path manipulation (extension, base name, parent)</li>
 *   <li>File searching and listing</li>
 *   <li>File size formatting</li>
 * </ul>
 *
 * <p>All methods are null-safe and handle edge cases gracefully.</p>
 */
public class FileUtils {

    /**
     * Check if a file exists.
     */
    public static boolean exists(String filePath) {
        return new File(filePath).exists();
    }

    /**
     * Check if a path is a file.
     */
    public static boolean isFile(String filePath) {
        return new File(filePath).isFile();
    }

    /**
     * Check if a path is a directory.
     */
    public static boolean isDirectory(String filePath) {
        return new File(filePath).isDirectory();
    }

    /**
     * Get the file extension.
     */
    public static String getExtension(String filePath) {
        if (filePath == null) return "";
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filePath.length() - 1) {
            return filePath.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Get the file name without extension.
     */
    public static String getBaseName(String filePath) {
        if (filePath == null) return "";
        String fileName = new File(filePath).getName();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    /**
     * Get the file name.
     */
    public static String getFileName(String filePath) {
        if (filePath == null) return "";
        return new File(filePath).getName();
    }

    /**
     * Get the parent directory.
     */
    public static String getParent(String filePath) {
        if (filePath == null) return "";
        File parent = new File(filePath).getParentFile();
        return parent != null ? parent.getPath() : "";
    }

    /**
     * Resolve a relative path against a base directory.
     */
    public static String resolvePath(String baseDir, String relativePath) {
        if (relativePath == null) return baseDir;
        if (new File(relativePath).isAbsolute()) {
            return relativePath;
        }
        return Paths.get(baseDir, relativePath).toString();
    }

    /**
     * Find files matching a pattern in a directory.
     */
    public static List<String> findFiles(String directory, String extension) {
        List<String> files = new ArrayList<>();
        File dir = new File(directory);

        if (dir.exists() && dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isFile()) {
                        if (extension == null || extension.isEmpty() ||
                            child.getName().toLowerCase().endsWith("." + extension.toLowerCase())) {
                            files.add(child.getPath());
                        }
                    }
                }
            }
        }

        return files;
    }

    /**
     * Get file size in bytes.
     */
    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }

    /**
     * Format file size for display.
     */
    public static String formatFileSize(long bytes) {
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
     * Ensure parent directories exist.
     */
    public static boolean ensureParentExists(String filePath) {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            return parent.mkdirs();
        }
        return true;
    }
}