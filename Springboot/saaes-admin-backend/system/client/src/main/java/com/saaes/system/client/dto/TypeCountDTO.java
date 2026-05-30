package com.saaes.system.client.dto;

import lombok.Data;

@Data
public class TypeCountDTO {
    private String type;
    private Integer totalCount;
    private Integer last30DaysCount;
}
