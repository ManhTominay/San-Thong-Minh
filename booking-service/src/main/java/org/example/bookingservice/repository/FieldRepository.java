package org.example.bookingservice.repository;

import org.example.bookingservice.entity.Field;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FieldRepository extends JpaRepository<Field, Long> {
    List<Field> findByStadiumId(Long stadiumId);
}