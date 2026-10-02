package org.example.stadiumservice.controller;

import org.example.stadiumservice.entity.Stadium;
import org.example.stadiumservice.repository.StadiumRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/fields")
@CrossOrigin(origins = "*")
public class StadiumController {

    private final StadiumRepository stadiumRepository;

    public StadiumController(StadiumRepository stadiumRepository) {
        this.stadiumRepository = stadiumRepository;
    }

    // =========================
    // 1. LẤY DANH SÁCH SÂN
    // =========================
    @GetMapping
    public List<Stadium> getAllStadiums(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {

        // Nếu có từ khóa tìm kiếm
        if (search != null && !search.trim().isEmpty()) {

            String keyword = search.trim();

            return stadiumRepository
                    .findByNameContainingIgnoreCaseOrAddressContainingIgnoreCase(
                            keyword,
                            keyword
                    );
        }

        // Nếu lọc theo loại môn
        if (type != null
                && !type.trim().isEmpty()
                && !type.equalsIgnoreCase("all")
                && !type.equalsIgnoreCase("Tất cả sân")) {

            return stadiumRepository.findByTypeIgnoreCase(type.trim());
        }

        // Không có điều kiện → trả toàn bộ
        return stadiumRepository.findAll();
    }


    // =========================
    // 2. LẤY 1 SÂN THEO ID
    // =========================
    @GetMapping("/{id}")
    public Stadium getStadiumById(@PathVariable Long id) {

        return stadiumRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy sân có ID " + id
                        )
                );
    }


    // =========================
    // 3. THÊM SÂN
    // =========================
    @PostMapping
    public ResponseEntity<Stadium> createStadium(
            @RequestBody Stadium stadium) {

        if (stadium.getName() == null
                || stadium.getName().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên sân không được để trống"
            );
        }

        if (stadium.getAddress() == null
                || stadium.getAddress().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Địa chỉ sân không được để trống"
            );
        }

        // ID tự tăng
        stadium.setId(null);

        stadium.setName(stadium.getName().trim());
        stadium.setAddress(stadium.getAddress().trim());

        Stadium savedStadium = stadiumRepository.save(stadium);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedStadium);
    }


    // =========================
    // 4. SỬA SÂN
    // =========================
    @PutMapping("/{id}")
    public Stadium updateStadium(
            @PathVariable Long id,
            @RequestBody Stadium request) {

        Stadium stadium = stadiumRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy sân có ID " + id
                        )
                );

        if (request.getName() == null
                || request.getName().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên sân không được để trống"
            );
        }

        if (request.getAddress() == null
                || request.getAddress().trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Địa chỉ sân không được để trống"
            );
        }

        stadium.setName(request.getName().trim());
        stadium.setAddress(request.getAddress().trim());
        stadium.setType(request.getType());
        stadium.setDescription(request.getDescription());
        stadium.setLocation(request.getLocation());
        stadium.setOwnerId(request.getOwnerId());

        return stadiumRepository.save(stadium);
    }


    // =========================
    // 5. XÓA SÂN
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStadium(
            @PathVariable Long id) {

        if (!stadiumRepository.existsById(id)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy sân có ID " + id
            );
        }

        stadiumRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}