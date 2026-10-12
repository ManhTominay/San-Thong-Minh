package org.example.bookingservice.repository;

import jakarta.persistence.LockModeType;
import org.example.bookingservice.entity.CancellationRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CancellationRequestRepository extends JpaRepository<CancellationRequest, Long> {

    boolean existsByBooking_IdAndStatus(Long bookingId, String status);

    @EntityGraph(attributePaths = "booking")
    List<CancellationRequest> findByBooking_IdOrderByCreatedAtDescIdDesc(Long bookingId);

    @EntityGraph(attributePaths = "booking")
    List<CancellationRequest> findByRequesterUserIdOrderByCreatedAtDescIdDesc(Long requesterUserId);

    @EntityGraph(attributePaths = "booking")
    List<CancellationRequest> findAllByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = "booking")
    List<CancellationRequest> findByStatusOrderByCreatedAtDescIdDesc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "booking")
    @Query("select r from CancellationRequest r where r.id = :requestId")
    Optional<CancellationRequest> findByIdForUpdate(@Param("requestId") Long requestId);
}
