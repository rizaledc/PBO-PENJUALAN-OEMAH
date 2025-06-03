package com.penjualanrumah.repository;

import com.penjualanrumah.model.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Integer> {
}
