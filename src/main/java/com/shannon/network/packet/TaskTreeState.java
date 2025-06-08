package com.shannon.network.packet;

import java.util.List;

public class TaskTreeState {
    public String goal;
    public String strategy;
    public String status;
    public String error;
    public List<SubTask> subTasks;

    public static class SubTask {
        public String subTaskGoal;
        public String subTaskStrategy;
        public String subTaskStatus;
        public String subTaskResult;
    }

    @Override
    public String toString() {
        return "TaskTreeState{" +
                "goal='" + goal + '\'' +
                ", strategy='" + strategy + '\'' +
                ", status='" + status + '\'' +
                ", error='" + error + '\'' +
                ", subTasks=" + subTasks +
                '}';
    }
}