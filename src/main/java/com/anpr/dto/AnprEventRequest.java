package com.anpr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnprEventRequest {
    
    private String ipAddress;
    private Integer channelID;
    private String dateTime;
    private String eventType;
    
    @JsonProperty("ANPR")
    private AnprData anpr;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnprData {
        private String licensePlate;
        private Double confidence;
        private String country;
        private String vehicleType;
        private String direction;
        private ImageData image;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageData {
        private String plateImage;
        private String vehicleImage;
    }
}
