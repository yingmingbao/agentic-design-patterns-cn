package com.strategist.ai.patterns.planning.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContentItem {

    String text;
    List<Annotation> annotations;
}
