package ukma.jpay.payments.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private UUID id;

    @Version
    private Long version;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionStatus status;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 12, fraction = 2)
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    @Column(nullable = false, length = 3)
    private String currency;

    @Size(max = 64)
    @Column(name = "merchant_reference", length = 64)
    private String merchantReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private MerchantEntity merchant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProviderEntity provider;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("attemptNumber ASC")
    private List<PaymentAttemptEntity> attempts = new ArrayList<>();

    protected PaymentEntity() {
    }

    public PaymentEntity(Payment payment, PaymentProviderEntity provider) {
        this.id = Objects.requireNonNull(payment.id());
        this.createdAt = payment.createdAt();
        updateFrom(payment, provider);
    }

    public void updateFrom(Payment payment, PaymentProviderEntity provider) {
        if (!id.equals(payment.id()) || !provider.getId().equals(payment.providerId())) {
            throw new IllegalArgumentException("Payment and provider identifiers must match");
        }
        this.status = payment.status();
        this.amount = payment.amount();
        this.currency = payment.currency();
        this.merchantReference = payment.merchantReference();
        this.provider = provider;
    }

    public void setMerchant(MerchantEntity merchant) {
        this.merchant = merchant;
    }

    public void addAttempt(PaymentAttemptEntity attempt) {
        Objects.requireNonNull(attempt);
        if (attempt.getPayment() != null && attempt.getPayment() != this) {
            throw new IllegalArgumentException("An attempt cannot be moved to another payment");
        }
        if (!attempts.contains(attempt)) {
            attempts.add(attempt);
            attempt.setPayment(this);
        }
    }

    public void removeAttempt(PaymentAttemptEntity attempt) {
        if (attempts.remove(attempt)) {
            attempt.setPayment(null);
        }
    }

    public Payment toPayment() {
        return new Payment(id, status, amount, currency, merchantReference, provider.getId(), createdAt);
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public MerchantEntity getMerchant() {
        return merchant;
    }

    public PaymentProviderEntity getProvider() {
        return provider;
    }

    public List<PaymentAttemptEntity> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }
}
