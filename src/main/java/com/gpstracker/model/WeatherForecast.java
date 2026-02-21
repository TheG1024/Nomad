package com.gpstracker.model;

import java.util.List;

public class WeatherForecast {
    private List<WeatherItem> list;
    private City city;

    public List<WeatherItem> getList() {
        return list;
    }

    public void setList(List<WeatherItem> list) {
        this.list = list;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }
}
