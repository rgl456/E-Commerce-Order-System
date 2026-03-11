package com.ragul.InventoryService.repository;

import com.ragul.InventoryService.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    boolean existsByProductId(Long productId);
    Optional<Inventory> findByProductId(Long productId);

}
