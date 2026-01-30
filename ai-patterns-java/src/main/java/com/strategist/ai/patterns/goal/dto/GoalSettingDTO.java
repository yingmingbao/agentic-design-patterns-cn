package com.strategist.ai.patterns.goal.dto;


import com.alibaba.fastjson.JSON;
import lombok.Data;

@Data
public class GoalSettingDTO {
    String useCase;
    String goalsInput;
    int maxIterations;

    public static void main(String[] args) {
        GoalSettingDTO dto = new GoalSettingDTO();
        dto.goalsInput="Write code to find BinaryGap of a given positive integer";
        dto.useCase="Code simple to understand, Functionally correct, Handles comprehensive edge cases, Takes positive integer input only, prints the results with few examples";
        dto.maxIterations = 5;
        System.out.println(JSON.toJSONString(dto));

        dto.goalsInput="Write code to count the number of files in current directory and all its nested sub directories, and print the total count";
        dto.useCase="Code simple to understand, Functionally correct, Handles comprehensive edge cases, Ignore recommendations for performance, Ignore recommendations for test suite use like unittest or pytest";
        System.out.println(JSON.toJSONString(dto));

        dto.goalsInput="Write code which takes a command line input of a word doc or docx file and opens it and counts the number of words, and characters in it and prints all";
        dto.useCase="Code simple to understand, Functionally correct, Handles edge cases";

        System.out.println(JSON.toJSONString(dto));

    }
}
