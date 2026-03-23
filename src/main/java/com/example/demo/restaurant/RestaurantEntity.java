package com.example.demo.restaurant;

import jakarta.persistence.*;

@Entity
@Table(name = "restaurants")
public class RestaurantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double lat;
    private Double lng;

    private String category;
    private String address;
    private String phone;

    public Long getId() { return id; }
    public String getName() { return name; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public String getCategory() { return category; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLat(Double lat) { this.lat = lat; }
    public void setLng(Double lng) { this.lng = lng; }
    public void setCategory(String category) { this.category = category; }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
}