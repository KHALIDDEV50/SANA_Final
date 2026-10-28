package com.example.sana.Repository;
import com.example.sana.Model.BillingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BillingOrderRepository extends JpaRepository<BillingOrder, String> {
    @org.springframework.data.jpa.repository.Query("select o.user.id from BillingOrder o where o.id = :id")
    java.util.Optional<Integer> findOwnerId(@org.springframework.data.repository.query.Param("id") String id);
    List<BillingOrder> findTop10ByUser_IdOrderByCreatedAtDesc(Integer userId);
}
