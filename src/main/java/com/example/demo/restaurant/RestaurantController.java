package com.example.demo.restaurant;

import com.example.demo.restaurant.dto.RestaurantListDto;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/restaurants", produces = MediaType.APPLICATION_JSON_VALUE)
public class RestaurantController {
    private final RestaurantService service;
    public RestaurantController(RestaurantService service) { this.service = service; }

    @GetMapping("/ping")
    public Map<String, String> ping() { return Map.of("status", "ok"); }

    @GetMapping
    public ResponseEntity<?> getRestaurants(
            @RequestParam("bbox") String bbox,
            @RequestParam(value = "order", defaultValue = "distance") String order,
            @RequestParam(value = "limit", defaultValue = "20") Integer limit,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lng", required = false) Double lng
    ) {
        try {
            String[] p = bbox.split(",");
            if (p.length != 4) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "bad_request",
                        "message", "bbox 형식은 'west,south,east,north' 입니다."
                ));
            }
            double west  = Double.parseDouble(p[0].trim());
            double south = Double.parseDouble(p[1].trim());
            double east  = Double.parseDouble(p[2].trim());
            double north = Double.parseDouble(p[3].trim());

            if (west > east || south > north) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "bad_request",
                        "message", "bbox 값이 올바르지 않습니다."
                ));
            }

            if ("distance".equalsIgnoreCase(order) && (lat == null || lng == null)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "bad_request",
                        "message", "order=distance 사용 시 lat,lng 파라미터가 필요합니다."
                ));
            }

            RestaurantListDto dto = service.searchInBBox(
                    west, south, east, north,
                    order, limit, page, q, category, lat, lng
            );
            return ResponseEntity.ok(dto);

        } catch (NumberFormatException nfe) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "bad_request",
                    "message", "bbox 숫자 파싱 오류: " + nfe.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "server_error",
                    "message", e.getMessage()
            ));
        }
    }
}