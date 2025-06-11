package com.penjualanrumah.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "order_history")
public class OrderHistory extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(nullable = false)
    private LocalDateTime orderDate;

    public OrderHistory() {
        this.orderDate = LocalDateTime.now();
    }

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    // Getter dan Setter
    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
}
