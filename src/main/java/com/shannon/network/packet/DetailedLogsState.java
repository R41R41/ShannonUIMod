package com.shannon.network.packet;

import java.util.ArrayList;
import java.util.List;

public class DetailedLogsState {
    public List<LogEntry> logs = new ArrayList<>();

    public static class LogEntry {
        public String timestamp; // ISO 8601 string
        public String phase; // thinking, tool_call, tool_result, reflection, planning, understanding
        public String level; // info, success, warning, error
        public String source; // ノード名やツール名
        public String content;
        public LogMetadata metadata;

        public static class LogMetadata {
            public String skillName;
            public String toolName;
            public String parameters;
            public String result;
            public Integer duration;
            public String error;
            
            // Planning用フィールド
            public String goal;
            public String strategy;
            public String status;
            public Boolean emergencyResolved;
            public Object actionSequence;  // JSON array
            public Object subTasks;  // JSON array
            public Integer actionCount;
            public Integer subTaskCount;
        }
    }

    @Override
    public String toString() {
        return "DetailedLogsState{" +
                "logs=" + (logs != null ? logs.size() : 0) + " entries" +
                '}';
    }
}
