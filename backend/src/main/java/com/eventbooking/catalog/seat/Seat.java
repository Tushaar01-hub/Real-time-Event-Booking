package com.eventbooking.catalog.seat;

import com.eventbooking.catalog.show.Show;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Seat entity representing a single seat in a show.
 * Status is stored as AVAILABLE/BOOKED/BLOCKED. HELD is derived from Redis.
 */
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @NotBlank
    @Size(max = 5)
    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false)
    private String section;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false)
    private String status = "AVAILABLE";  // AVAILABLE | BOOKED | BLOCKED

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    // ---------------------------------------------------------------- constructors

    protected Seat() {
        // JPA requires a no-arg constructor
    }

    public Seat(Show show, String rowLabel, Integer seatNumber, String section, BigDecimal price) {
        this.show = show;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.section = section;
        this.price = price;
        this.status = "AVAILABLE";
    }

    // ---------------------------------------------------------------- getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
