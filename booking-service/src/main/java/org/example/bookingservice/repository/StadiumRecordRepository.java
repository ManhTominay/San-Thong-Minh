package org.example.bookingservice.repository;

import org.example.bookingservice.entity.StadiumRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StadiumRecordRepository extends JpaRepository<StadiumRecord, Long> {
}
