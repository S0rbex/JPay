package ukma.jpay.payments.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcPaymentRepositoryTest {

    private JdbcPaymentRepository repository;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        repository = new JdbcPaymentRepository(dataSource);
    }

    @Test
    void savesFindsAndUpdatesPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment processing = new Payment(
                paymentId,
                TransactionStatus.PROCESSING,
                new BigDecimal("19.99"),
                "USD",
                "order-1",
                "stripe",
                Instant.parse("2026-01-01T00:00:00Z"));

        repository.save(processing);

        assertThat(repository.findById(paymentId)).contains(processing);

        Payment succeeded = processing.transitionTo(TransactionStatus.SUCCEEDED);
        repository.save(succeeded);

        assertThat(repository.findById(paymentId)).contains(succeeded);
    }

    @Test
    void missingPaymentReturnsEmptyOptional() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }
}
