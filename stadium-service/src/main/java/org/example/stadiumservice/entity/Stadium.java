package org.example.stadiumservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "stadiums")
@Data
public class Stadium {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;
    private String description;

    @Column(name = "owner_id")
    private Long ownerId;

    private String location;

    private String type; // Thêm trường này để quản lý loại sân chuẩn xác
}