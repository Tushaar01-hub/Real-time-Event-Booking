package com.eventbooking.catalog.show;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Represents a section of seats in a show venue.
 * Used when creating a show to specify the seat layout.
 */
public class SeatLayoutDto {

    @NotBlank(message = "Section name is required")
    @Size(max = 50, message = "Section name must not exceed 50 characters")
    private String section;

    @Min(value = 1, message = "Rows must be at least 1")
    private int rows;

    @Min(value = 1, message = "Seats per row must be at least 1")
    private int seatsPerRow;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    // ---------------------------------------------------------------- constructors

    public SeatLayoutDto() {
    }

    public SeatLayoutDto(String section, int rows, int seatsPerRow, BigDecimal price) {
        this.section = section;
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
        this.price = price;
    }

    // ---------------------------------------------------------------- getters/setters

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getSeatsPerRow() {
        return seatsPerRow;
    }

    public void setSeatsPerRow(int seatsPerRow) {
        this.seatsPerRow = seatsPerRow;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
