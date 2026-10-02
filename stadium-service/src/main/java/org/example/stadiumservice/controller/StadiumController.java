package org.example.stadiumservice.controller;

import org.example.stadiumservice.entity.Stadium;
import org.example.stadiumservice.repository.StadiumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fields")
@CrossOrigin(origins = "*") // Cho phép mọi nguồn (bao gồm frontend port 8082) truy cập API
public class StadiumController {

    @Autowired
    private StadiumRepository stadiumRepository;

    @GetMapping
    public List<Stadium> getAllStadiums(@RequestParam(required = false) String type) {
        if (type != null && !type.trim().isEmpty()) {
            // Nếu người dùng bấm "Tất cả" hoặc truyền rỗng thì trả về hết
            if (type.equalsIgnoreCase("all") || type.equalsIgnoreCase("Tất cả sân")) {
                return stadiumRepository.findAll();
            }
            // Lọc theo cột type trong database
            return stadiumRepository.findByTypeIgnoreCase(type.trim());
        }
        return stadiumRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public void deleteStadium(@PathVariable Long id) {
        stadiumRepository.deleteById(id);
    }
}