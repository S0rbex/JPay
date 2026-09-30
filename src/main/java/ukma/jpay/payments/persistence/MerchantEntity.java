package ukma.jpay.payments.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class MerchantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(name = "contact_email", nullable = false, unique = true, length = 254)
    private String contactEmail;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "merchant_providers",
            joinColumns = @JoinColumn(name = "merchant_id"),
            inverseJoinColumns = @JoinColumn(name = "provider_id"))
    private Set<PaymentProviderEntity> providers = new LinkedHashSet<>();

    protected MerchantEntity() {
    }

    public MerchantEntity(String name, String contactEmail, String defaultCurrency) {
        update(name, contactEmail, defaultCurrency);
    }

    public void update(String name, String contactEmail, String defaultCurrency) {
        this.name = name;
        this.contactEmail = contactEmail == null ? null : contactEmail.trim().toLowerCase(Locale.ROOT);
        this.defaultCurrency = defaultCurrency;
    }

    public void enableProvider(PaymentProviderEntity provider) {
        providers.add(Objects.requireNonNull(provider));
    }

    public void disableProvider(PaymentProviderEntity provider) {
        providers.remove(provider);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public Set<PaymentProviderEntity> getProviders() {
        return Collections.unmodifiableSet(providers);
    }
}
