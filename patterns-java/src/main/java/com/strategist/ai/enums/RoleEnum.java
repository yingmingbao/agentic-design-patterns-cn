package com.strategist.ai.enums;

public enum RoleEnum {

    USER("user"),
    ASSISTANT("assistant");

    private String role;

    RoleEnum(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

}
