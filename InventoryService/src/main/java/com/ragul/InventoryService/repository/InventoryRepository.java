package com.ragul.InventoryService.repository;

import com.ragul.InventoryService.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByProductId(Long productId);

    // to avoid Race Condition we are using pessimistic lock,
    // in this only one thread can access the row at a time,
    // another thread has to wait until the first thread finishes
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findByProductId(Long productId);

}
