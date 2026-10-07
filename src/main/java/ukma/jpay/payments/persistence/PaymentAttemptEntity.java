package ukma.jpay.payments.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts", uniqueConstraints = @UniqueConstraint(
        name = "uk_attempt_payment_number", columnNames = {"payment_id", "attempt_number"}))
public class PaymentAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentEntity payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProviderEntity provider;

    @Min(1)
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AttemptResult result;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentAttemptEntity() {
    }

    public PaymentAttemptEntity(PaymentProviderEntity provider, int attemptNumber,
                                AttemptResult result, Instant createdAt) {
        this.provider = Objects.requireNonNull(provider);
        this.attemptNumber = attemptNumber;
        this.result = Objects.requireNonNull(result);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    void setPayment(PaymentEntity payment) {
        this.payment = payment;
    }

    public void setResult(AttemptResult result) {
        this.result = Objects.requireNonNull(result);
    }

    public UUID getId() {
        return id;
    }

    public PaymentEntity getPayment() {
        return payment;
    }

    public PaymentProviderEntity getProvider() {
        return provider;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public AttemptResult getResult() {
        return result;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
