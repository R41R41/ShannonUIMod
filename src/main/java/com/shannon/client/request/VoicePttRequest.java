package com.shannon.client.request;

public class VoicePttRequest {
    private String mcUsername;
    private String action;

    public VoicePttRequest(String mcUsername, String action) {
        this.mcUsername = mcUsername;
        this.action = action;
    }
}
