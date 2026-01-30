package com.strategist.ai.patterns.planning.dto;

import lombok.Data;

import javax.annotation.processing.SupportedAnnotationTypes;

@Data
public class Annotation {
    String title = "";
    String url = "";
    int start_index = 0;
    int end_index = 0;
}
