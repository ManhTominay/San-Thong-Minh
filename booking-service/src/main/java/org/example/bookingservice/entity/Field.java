package org.example.bookingservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "fields")
public class Field {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String type;

    @Column(name = "price_per_hour", nullable = false)
    private Double pricePerHour;

    @Column(name = "stadium_id", nullable = false)
    private Long stadiumId;

    public Field() {
    }

    public Field(Long id, String name, String type, Double pricePerHour, Long stadiumId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.stadiumId = stadiumId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(Double pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public Long getStadiumId() {
        return stadiumId;
    }

    public void setStadiumId(Long stadiumId) {
        this.stadiumId = stadiumId;
    }
}