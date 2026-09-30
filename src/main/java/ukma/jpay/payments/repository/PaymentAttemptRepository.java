package ukma.jpay.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ukma.jpay.payments.persistence.PaymentAttemptEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttemptEntity, UUID> {

    long countByPaymentId(UUID paymentId);

    boolean existsByPaymentIdAndAttemptNumber(UUID paymentId, int attemptNumber);

    boolean existsByProviderId(String providerId);

    Optional<PaymentAttemptEntity> findByIdAndPaymentId(UUID id, UUID paymentId);

    @Query("select coalesce(max(a.attemptNumber), 0) from PaymentAttemptEntity a where a.payment.id = :paymentId")
    int findMaxAttemptNumber(@Param("paymentId") UUID paymentId);

    @Query("""
            select a from PaymentAttemptEntity a
            left join fetch a.provider
            left join fetch a.payment
            where a.payment.id = :paymentId
            order by a.attemptNumber
            """)
    List<PaymentAttemptEntity> findWithDetailsByPaymentId(@Param("paymentId") UUID paymentId);
}
