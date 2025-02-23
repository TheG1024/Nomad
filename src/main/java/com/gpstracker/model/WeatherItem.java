package com.gpstracker.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WeatherItem {
    private LocalDateTime dt_txt;
    private Main main;
    private List<Weather> weather;
    private Wind wind;
}
