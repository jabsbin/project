package com.example.demo.restaurant;

import com.example.demo.restaurant.dto.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantService {
    private final RestaurantRepository repo;
    public RestaurantService(RestaurantRepository repo) { this.repo = repo; }

    public RestaurantListDto searchInBBox(
            double west, double south, double east, double north,
            String order, int limit, Integer page,
            String q, String category,
            Double lat, Double lng
    ) {
        int p = (page == null || page < 0) ? 0 : page;
        int size = Math.min(Math.max(limit, 1), 500);
        String query = (q == null) ? "" : q.trim();
        String cat   = (category == null) ? "" : category.trim();

        long total = repo.countInBBox(west, south, east, north, query, cat);

        if ("distance".equalsIgnoreCase(order) && lat != null && lng != null) {
            Pageable pageable = PageRequest.of(p, size);
            Page<Object[]> rows = repo.findInBBoxOrderByDistance(
                    west, south, east, north, query, cat, lat, lng, pageable
            );

            List<RestaurantItemDto> items = rows.getContent().stream().map(o -> {
                RestaurantItemDto d = new RestaurantItemDto(
                        "R_" + ((Number) o[0]).longValue(),
                        (String) o[1],
                        o[2] == null ? null : ((Number) o[2]).doubleValue(),
                        o[3] == null ? null : ((Number) o[3]).doubleValue(),
                        (String) o[4],
                        (String) o[5],
                        (String) o[6]
                );
                d.distance = o[7] == null ? null : ((Number) o[7]).doubleValue();
                return d;
            }).toList();

            return new RestaurantListDto(total, items);
        }

        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        Pageable pageable = PageRequest.of(p, size, sort);

        List<RestaurantItemDto> items = repo.findInBBox(west, south, east, north, query, cat, pageable)
                .stream()
                .map(r -> new RestaurantItemDto(
                        "R_" + r.getId(),
                        r.getName(),
                        r.getLat(),
                        r.getLng(),
                        r.getCategory(),
                        r.getAddress(),
                        r.getPhone()
                ))
                .toList();

        return new RestaurantListDto(total, items);
    }
}