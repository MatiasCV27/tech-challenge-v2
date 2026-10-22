package com.challange.order_service.repository;

import com.challange.order_service.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity,Integer> {

    boolean existsByOrderNumber(String orderNumber);

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    @Query(value = "SELECT * FROM t_order WHERE " +
            "(:orderNumber IS NULL OR :orderNumber = '' OR order_number ILIKE CONCAT('%', :orderNumber, '%')) AND " +
            "(:clientId IS NULL OR :clientId = '' OR client_id ILIKE CONCAT('%', :clientId, '%')) AND " +
            "(:status IS NULL OR :status = '' OR status ILIKE CONCAT('%', :status, '%')) AND " +
            "(CAST(:fromDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR created_date >= CAST(:fromDate AS TIMESTAMP WITH TIME ZONE)) AND " +
            "(CAST(:toDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR created_date <= CAST(:toDate AS TIMESTAMP WITH TIME ZONE))",
            nativeQuery = true)
    List<OrderEntity> searchOrder(@Param("orderNumber") String orderNumber, @Param("clientId") String clientId,
            @Param("status") String status, @Param("fromDate") OffsetDateTime fromDate, @Param("toDate") OffsetDateTime toDate);
}
