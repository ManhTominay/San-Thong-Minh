package org.example.stadiumservice.repository;

import org.example.stadiumservice.entity.Stadium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StadiumRepository extends JpaRepository<Stadium, Long> {

    // Tìm kiếm chính xác theo cột type trong database
    List<Stadium> findByTypeIgnoreCase(String type);

    // Giữ lại tìm kiếm theo tên nếu cần dùng cho ô thanh tìm kiếm (search input)
    List<Stadium> findByNameContainingIgnoreCase(String name);
}