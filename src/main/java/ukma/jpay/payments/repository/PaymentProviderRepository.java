package ukma.jpay.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ukma.jpay.payments.persistence.PaymentProviderEntity;

import java.util.List;
import java.util.UUID;

public interface PaymentProviderRepository extends JpaRepository<PaymentProviderEntity, String> {

    List<PaymentProviderEntity> findByNameContainingIgnoreCaseOrderByIdAsc(String name);

    @Query("""
            select p from MerchantEntity m join m.providers p
            where m.id = :merchantId
            order by p.priority, p.id
            """)
    List<PaymentProviderEntity> findEnabledForMerchant(@Param("merchantId") UUID merchantId);
}
