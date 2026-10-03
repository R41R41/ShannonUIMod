package com.shannon.model;

import java.util.ArrayList;
import java.util.List;

/** Developer log lines posted by the backend to {@code /task_logs}. */
public class DetailedLogsState {
    public List<LogEntry> logs = new ArrayList<>();

    public static class LogEntry {
        /** ISO 8601 time. */
        public String timestamp;
        /** thinking, tool_call, tool_result, reflection, planning, understanding. */
        public String phase;
        /** info, success, warning, error. */
        public String level;
        public String source;
        public String content;
    }
}
