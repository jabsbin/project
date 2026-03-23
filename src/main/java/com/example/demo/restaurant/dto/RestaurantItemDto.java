package com.example.demo.restaurant.dto;

public class RestaurantItemDto {
    public String id;
    public String name;
    public Double lat;
    public Double lng;

    public String category;
    public String address;
    public String phone;

    public Double distance;

    public RestaurantItemDto(String id, String name, Double lat, Double lng,
                             String category, String address, String phone) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.category = category;
        this.address = address;
        this.phone = phone;
    }

    public RestaurantItemDto withDistance(Double d){ this.distance = d; return this; }
}