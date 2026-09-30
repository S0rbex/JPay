package ukma.jpay.payments.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "payment_providers")
public class PaymentProviderEntity {

    @Id
    @NotBlank
    @Size(max = 64)
    @Column(length = 64)
    private String id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Min(1)
    @Column(nullable = false)
    private int priority;

    protected PaymentProviderEntity() {
    }

    public PaymentProviderEntity(String id, String name, int priority) {
        this.id = id;
        update(name, priority);
    }

    public void update(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }
}
