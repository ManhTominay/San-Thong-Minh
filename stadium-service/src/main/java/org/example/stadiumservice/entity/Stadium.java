package org.example.stadiumservice.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "stadiums") // Hoặc bảng fields tương ứng
@Data
public class Stadium {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;
    private String description;
}