// CourtGridDTO.java
package org.example.bookingservice.dto;

import java.util.List;

public class CourtGridDTO {
    private Long courtId;
    private String courtName;
    private List<TimeSlotDTO> slots;

    public CourtGridDTO() {}

    public Long getCourtId() { return courtId; }
    public void setCourtId(Long courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public List<TimeSlotDTO> getSlots() { return slots; }
    public void setSlots(List<TimeSlotDTO> slots) { this.slots = slots; }
}