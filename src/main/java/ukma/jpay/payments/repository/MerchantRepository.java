package ukma.jpay.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ukma.jpay.payments.persistence.MerchantEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<MerchantEntity, UUID> {

    Optional<MerchantEntity> findByContactEmailIgnoreCase(String contactEmail);

    boolean existsByContactEmailIgnoreCase(String contactEmail);

    boolean existsByContactEmailIgnoreCaseAndIdNot(String contactEmail, UUID id);

    boolean existsByProvidersId(String providerId);

    @Query("""
            select distinct m from MerchantEntity m
            left join fetch m.providers
            order by m.name, m.id
            """)
    List<MerchantEntity> findAllWithProviders();

    @Query("""
            select distinct m from MerchantEntity m
            left join fetch m.providers
            where m.id = :id
            """)
    Optional<MerchantEntity> findWithProvidersById(@Param("id") UUID id);
}
