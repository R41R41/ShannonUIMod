package com.shannon.client.request;

/**
 * スキル切り替えリクエスト
 */
public class SkillSwitchRequest {
    private String skillName;
    private String status;

    public SkillSwitchRequest(String skillName, boolean status) {
        this.skillName = skillName;
        this.status = String.valueOf(status);
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
