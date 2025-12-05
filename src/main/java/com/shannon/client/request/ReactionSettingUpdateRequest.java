package com.shannon.client.request;

public class ReactionSettingUpdateRequest {
    public String eventType;
    public boolean enabled;
    public int probability;

    public ReactionSettingUpdateRequest(String eventType, boolean enabled, int probability) {
        this.eventType = eventType;
        this.enabled = enabled;
        this.probability = probability;
    }
}
