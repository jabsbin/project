package com.example.demo.hotspot;

import com.example.demo.hotspot.dto.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

@Service
public class HotspotService {
    private final HotspotRepository repo;
    public HotspotService(HotspotRepository repo) { this.repo = repo; }

    private double riskScore(Integer accidents, Integer casualties, Integer year) {
        double a = accidents == null ? 0 : accidents;
        double c = casualties == null ? 0 : casualties;
        int cur = Year.now().getValue();
        double rec = (year == null) ? 0 : Math.max(0, 1.0 - Math.min(5, cur - year) * 0.2);
        return a * 0.6 + c * 0.3 + rec * 10 * 0.1;
    }

    public HotspotListDto searchInBBox(
            double west, double south, double east, double north,
            Integer year, String order, int limit,
            Integer page, String q, Double lat, Double lng
    ) {
        int p = (page == null || page < 0) ? 0 : page;
        int size = Math.min(Math.max(limit, 1), 500);
        String query = (q == null) ? "" : q.trim();

        long total = repo.countInBBox(west, south, east, north, year, query);

        if ("distance".equalsIgnoreCase(order) && lat != null && lng != null) {
            Pageable pageable = PageRequest.of(p, size);
            Page<Object[]> rows = repo.findInBBoxOrderByDistance(
                    west, south, east, north, year, query, lat, lng, pageable
            );
            List<HotspotItemDto> items = rows.getContent().stream().map(o -> {
                HotspotItemDto d = new HotspotItemDto(
                        "HS_" + ((Number) o[0]).longValue(),
                        (String) o[1],
                        o[2] == null ? null : ((Number) o[2]).doubleValue(),
                        o[3] == null ? null : ((Number) o[3]).doubleValue(),
                        o[4] == null ? null : ((Number) o[4]).intValue(),
                        o[5] == null ? null : ((Number) o[5]).intValue(),
                        o[6] == null ? null : ((Number) o[6]).intValue()
                );
                d.distance = o[7] == null ? null : ((Number) o[7]).doubleValue();
                d.riskScore = riskScore(d.accidents, d.casualties, d.year);
                return d;
            }).toList();
            return new HotspotListDto(total, items);
        }

        Sort sort = "casualties".equalsIgnoreCase(order)
                ? Sort.by(Sort.Direction.DESC, "casualties")
                : Sort.by(Sort.Direction.DESC, "accidents");

        Pageable pageable = PageRequest.of(p, size, sort);
        List<HotspotItemDto> items = repo.findInBBox(west, south, east, north, year, query, pageable)
                .stream()
                .map(h -> {
                    HotspotItemDto d = new HotspotItemDto(
                            "HS_" + h.getId(),
                            h.getName(),
                            h.getLat(),
                            h.getLng(),
                            h.getAccidents(),
                            h.getCasualties(),
                            h.getStatYear()
                    );
                    d.riskScore = riskScore(d.accidents, d.casualties, d.year);
                    return d;
                }).toList();

        return new HotspotListDto(total, items);
    }
}
