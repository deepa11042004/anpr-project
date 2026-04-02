package com.anpr.service;

import com.anpr.dto.SystemLogEntry;
import com.anpr.dto.SystemLogResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SystemLogService {

    private static final Pattern LEVEL_PATTERN = Pattern.compile("\\[\\s*(TRACE|DEBUG|INFO|WARN|ERROR)\\s*\\]");

    @Value("${logging.file.name:logs/anpr-access-control.log}")
    private String logFilePath;

    public SystemLogResponse readLogs(Long cursor, Integer maxLines, Integer tailLines) {
        int safeMaxLines = maxLines == null || maxLines <= 0 ? 200 : Math.min(maxLines, 1000);
        int safeTailLines = tailLines == null || tailLines <= 0 ? 200 : Math.min(tailLines, 2000);

        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            return SystemLogResponse.builder()
                    .success(false)
                    .message("Log file not found: " + logFilePath)
                    .cursor(0)
                    .fileSize(0)
                    .entries(List.of())
                    .build();
        }

        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            long fileSize = raf.length();
            long startOffset;

            if (cursor == null || cursor < 0) {
                startOffset = findStartOffsetForTail(raf, safeTailLines);
            } else {
                startOffset = Math.min(cursor, fileSize);
            }

            raf.seek(startOffset);
            List<SystemLogEntry> entries = new ArrayList<>();

            while (entries.size() < safeMaxLines) {
                long lineOffset = raf.getFilePointer();
                String rawLine = raf.readLine();
                if (rawLine == null) {
                    break;
                }

                String line = new String(rawLine.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                entries.add(SystemLogEntry.builder()
                        .offset(lineOffset)
                        .level(extractLevel(line))
                        .line(line)
                        .build());
            }

            return SystemLogResponse.builder()
                    .success(true)
                    .message("OK")
                    .cursor(raf.getFilePointer())
                    .fileSize(fileSize)
                    .entries(entries)
                    .build();
        } catch (Exception ex) {
            log.error("Failed to read system logs", ex);
            return SystemLogResponse.builder()
                    .success(false)
                    .message("Failed to read logs: " + ex.getMessage())
                    .cursor(0)
                    .fileSize(logFile.length())
                    .entries(List.of())
                    .build();
        }
    }

    private long findStartOffsetForTail(RandomAccessFile raf, int tailLines) throws Exception {
        long fileLength = raf.length();
        if (fileLength == 0) {
            return 0;
        }

        long pointer = fileLength - 1;
        int lines = 0;

        while (pointer >= 0) {
            raf.seek(pointer);
            int read = raf.read();
            if (read == '\n') {
                lines++;
                if (lines > tailLines) {
                    return pointer + 1;
                }
            }
            pointer--;
        }

        return 0;
    }

    private String extractLevel(String line) {
        Matcher matcher = LEVEL_PATTERN.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }

        String upper = line.toUpperCase();
        if (upper.contains(" EXCEPTION") || upper.contains(" ERROR ")) {
            return "ERROR";
        }
        if (upper.contains("WARN")) {
            return "WARN";
        }
        if (upper.contains("DEBUG")) {
            return "DEBUG";
        }
        if (upper.contains("TRACE")) {
            return "TRACE";
        }
        return "INFO";
    }
}
