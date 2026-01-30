package com.strategist.ai.patterns.routing.component.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class BookingDetail {

    @JsonPropertyDescription("目的地城市，例如：伦敦")
    String destination;

    @JsonPropertyDescription("出发日期，格式：YYYY-MM-DD")
    String date;

    @JsonPropertyDescription("预订类型：FLIGHT（机票）或 HOTEL（酒店）")
    public String type;

    @JsonPropertyDescription("用户的额外要求，如：靠窗、含早餐")
    String specialRequests;
}
