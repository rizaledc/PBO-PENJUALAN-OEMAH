package com.penjualanrumah.repository;

import com.penjualanrumah.model.Order;
import com.penjualanrumah.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer(User customers);
}
