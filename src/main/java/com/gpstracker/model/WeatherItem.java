package com.gpstracker.model;

import java.time.LocalDateTime;
import java.util.List;

public class WeatherItem {
    private LocalDateTime dt_txt;
    private Main main;
    private List<Weather> weather;
    private Wind wind;

    public LocalDateTime getDt_txt() {
        return dt_txt;
    }

    public void setDt_txt(LocalDateTime dt_txt) {
        this.dt_txt = dt_txt;
    }

    public Main getMain() {
        return main;
    }

    public void setMain(Main main) {
        this.main = main;
    }

    public List<Weather> getWeather() {
        return weather;
    }

    public void setWeather(List<Weather> weather) {
        this.weather = weather;
    }

    public Wind getWind() {
        return wind;
    }

    public void setWind(Wind wind) {
        this.wind = wind;
    }
}
