package com.example.demo.restaurant.dto;

import java.util.List;

public class RestaurantListDto {
    public long total;
    public List<RestaurantItemDto> items;

    public RestaurantListDto(long total, List<RestaurantItemDto> items) {
        this.total = total;
        this.items = items;
    }
}