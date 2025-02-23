package com.gpstracker.model;

import lombok.Data;

import java.util.List;

@Data
public class WeatherForecast {
    private List<WeatherItem> list;
    private City city;
}
