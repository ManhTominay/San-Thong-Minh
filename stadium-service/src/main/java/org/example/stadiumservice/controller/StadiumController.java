package org.example.stadiumservice.controller;

import org.example.stadiumservice.entity.Stadium;
import org.example.stadiumservice.repository.StadiumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fields")
@CrossOrigin(origins = "*")
public class StadiumController {

    @Autowired
    private StadiumRepository stadiumRepository;

    // Lấy toàn bộ danh sách sân từ cơ sở dữ liệu
    @GetMapping
    public List<Stadium> getAllStadiums() {
        return stadiumRepository.findAll();
    }

    // Thêm mới một sân vào cơ sở dữ liệu
    @PostMapping
    public ResponseEntity<Stadium> createStadium(@RequestBody Stadium stadium) {
        Stadium savedStadium = stadiumRepository.save(stadium);
        return ResponseEntity.ok(savedStadium);
    }
}