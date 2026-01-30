package com.strategist.ai.service.enums;


/**
 * case "doubao":
 * return aiServices.get("doubaoAIComponent");
 * case "bailian":
 * return aiServices.get("baiLianComponent");
 * case "deepsearch":
 * return aiServices.get("deepSearchComponent");
 * case "deepseek":
 * default:
 * return aiServices.get("deepSeekComponent");
 *
 */
public enum AIChatEnum {

    doubao("doubao"),
    bailian("bailian"),
    deepseek("deepseek"),
    deepsearch("deepsearch");

    private String name;

    AIChatEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static AIChatEnum getByName(String name) {
        for (AIChatEnum value : values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return deepseek;
    }

}
