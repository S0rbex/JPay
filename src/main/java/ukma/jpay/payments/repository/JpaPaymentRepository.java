package ukma.jpay.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.persistence.PaymentEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    List<PaymentEntity> findByStatusAndCurrencyOrderByCreatedAtAscIdAsc(
            TransactionStatus status, String currency);

    @Query("""
            select distinct p from PaymentEntity p
            left join fetch p.merchant
            left join fetch p.provider
            left join fetch p.attempts a
            left join fetch a.provider
            order by p.createdAt, p.id
            """)
    List<PaymentEntity> findAllWithDetails();

    @Query("""
            select distinct p from PaymentEntity p
            left join fetch p.merchant
            left join fetch p.provider
            left join fetch p.attempts a
            left join fetch a.provider
            where p.id = :id
            """)
    Optional<PaymentEntity> findWithDetailsById(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PaymentEntity p
            set p.status = :target, p.version = p.version + 1
            where p.id = :id and p.status = :expected
            """)
    int updateStatusIfCurrent(@Param("id") UUID id,
                              @Param("expected") TransactionStatus expected,
                              @Param("target") TransactionStatus target);
}
