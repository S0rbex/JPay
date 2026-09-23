package ukma.jpay.payments.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcPaymentRepository implements PaymentRepository {

    private final JdbcClient jdbcClient;

    JdbcPaymentRepository(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public Payment save(Payment payment) {
        jdbcClient.sql("""
                        MERGE INTO payments (
                            id, status, amount, currency, merchant_reference, provider_id, created_at
                        ) KEY (id) VALUES (
                            :id, :status, :amount, :currency, :merchantReference, :providerId, :createdAt
                        )
                        """)
                .param("id", payment.id())
                .param("status", payment.status().name())
                .param("amount", payment.amount())
                .param("currency", payment.currency())
                .param("merchantReference", payment.merchantReference())
                .param("providerId", payment.providerId())
                .param("createdAt", Timestamp.from(payment.createdAt()))
                .update();
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return jdbcClient.sql("""
                        SELECT id, status, amount, currency, merchant_reference, provider_id, created_at
                        FROM payments
                        WHERE id = :id
                        """)
                .param("id", paymentId)
                .query((resultSet, rowNumber) -> new Payment(
                        resultSet.getObject("id", UUID.class),
                        TransactionStatus.valueOf(resultSet.getString("status")),
                        resultSet.getBigDecimal("amount"),
                        resultSet.getString("currency"),
                        resultSet.getString("merchant_reference"),
                        resultSet.getString("provider_id"),
                        resultSet.getTimestamp("created_at").toInstant()))
                .optional();
    }

    @Override
    public boolean updateStatus(UUID paymentId, TransactionStatus from, TransactionStatus to) {
        int updated = jdbcClient.sql("""
                        UPDATE payments SET status = :to WHERE id = :id AND status = :from
                        """)
                .param("to", to.name())
                .param("id", paymentId)
                .param("from", from.name())
                .update();
        return updated == 1;
    }
}
