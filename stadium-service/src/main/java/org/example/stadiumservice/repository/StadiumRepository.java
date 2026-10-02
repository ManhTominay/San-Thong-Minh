package org.example.stadiumservice.repository;

import org.example.stadiumservice.entity.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StadiumRepository extends JpaRepository<Stadium, Long> {

    // Lọc theo loại sân
    List<Stadium> findByTypeIgnoreCase(String type);

    // Tìm theo tên
    List<Stadium> findByNameContainingIgnoreCase(String name);

    // Tìm theo tên hoặc địa chỉ
    List<Stadium> findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(
            String name,
            String address
    );
}