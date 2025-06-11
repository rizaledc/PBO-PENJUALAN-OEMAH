package com.penjualanrumah.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_status")
public class OrderStatus extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "seller_id")
    private User seller;
    
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(nullable = false)
    private LocalDateTime responseDate;

    public OrderStatus() {
        this.responseDate = LocalDateTime.now();
    }

    public enum Status {
        APPROVED, REJECTED, PENDING
    }

    // Getter dan Setter untuk responseDate
    public LocalDateTime getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(LocalDateTime responseDate) {
        this.responseDate = responseDate;
    }
}
