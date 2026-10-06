package org.example.bookingservice.dto;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourtGridDTO {
    private Long subCourtId;
    private String courtName;
    private List<TimeSlotDTO> slots;
}