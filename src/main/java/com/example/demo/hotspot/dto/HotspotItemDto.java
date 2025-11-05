package com.example.demo.hotspot.dto;

public class HotspotItemDto {
    public String id;
    public String name;
    public Double lat;
    public Double lng;
    public Integer accidents;
    public Integer casualties;
    public Integer year;
    public Double distance;
    public Double riskScore;

    public HotspotItemDto(String id, String name, Double lat, Double lng,
                          Integer accidents, Integer casualties, Integer year) {
        this.id=id; this.name=name; this.lat=lat; this.lng=lng;
        this.accidents=accidents; this.casualties=casualties; this.year=year;
    }
    public HotspotItemDto withDistance(Double d){ this.distance=d; return this; }
    public HotspotItemDto withRisk(Double r){ this.riskScore=r; return this; }
}
