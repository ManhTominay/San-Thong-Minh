package org.example.bookingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stadiums")
public class StadiumRecord {
    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    protected StadiumRecord() {
    }

    public StadiumRecord(Long id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
