package com.shannon.network.packet;

import java.util.HashSet;
import java.util.Set;

/**
 * どのログが展開されているかを管理
 */
public class LogToggleState {
    // 展開されているログのインデックス
    public Set<Integer> expandedLogs = new HashSet<>();

    // セクション全体の展開状態
    public boolean logsExpanded = true; // デフォルトは展開

    public boolean isLogExpanded(int index) {
        return expandedLogs.contains(index);
    }

    public void toggleLog(int index) {
        if (expandedLogs.contains(index)) {
            expandedLogs.remove(index);
        } else {
            expandedLogs.add(index);
        }
    }

    public void toggleAllLogs() {
        logsExpanded = !logsExpanded;
    }
}
