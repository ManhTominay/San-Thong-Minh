// TimeSlotDTO.java
package org.example.bookingservice.dto;

public class TimeSlotDTO {
    private String startTime;
    private String endTime;
    private Double price;
    private String status; // "AVAILABLE" hoặc "BOOKED"

    public TimeSlotDTO() {}

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}