package com.shannon.error.exceptions;

/**
 * 状態更新エラー
 * StateManagerでの状態更新失敗を表現
 */
public class StateUpdateException extends ModException {

    public StateUpdateException(String stateType, Throwable cause) {
        super(
                "状態更新失敗: " + stateType,
                "STATE_UPDATE_ERROR",
                cause);
        addMetadata("stateType", stateType);
    }

    public StateUpdateException(String stateType, String reason) {
        super(
                "状態更新失敗: " + stateType + " - " + reason,
                "STATE_UPDATE_ERROR");
        addMetadata("stateType", stateType);
        addMetadata("reason", reason);
    }
}
