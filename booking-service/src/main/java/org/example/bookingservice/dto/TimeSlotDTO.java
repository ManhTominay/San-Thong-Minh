package org.example.bookingservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimeSlotDTO {
    private String time;   // Ví dụ: "15:00"
    private String status; // "TRONG", "DA_DAT", "KHOA", "SU_KIEN"
}