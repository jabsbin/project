package com.example.demo.geocode;

public class GeocodeResult {
    private String name;   // 장소/주소 이름
    private double lat;    // 위도 (y)
    private double lng;    // 경도 (x)
    private String addr;   // 대표 주소 문자열

    public GeocodeResult() {}
    public GeocodeResult(String name, double lat, double lng, String addr) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.addr = addr;
    }

    public String getName() { return name; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public String getAddr() { return addr; }

    public void setName(String name) { this.name = name; }
    public void setLat(double lat) { this.lat = lat; }
    public void setLng(double lng) { this.lng = lng; }
    public void setAddr(String addr) { this.addr = addr; }
}
