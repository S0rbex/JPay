package ukma.jpay.payments.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ukma.jpay.payments.domain.PaymentProvider;
import ukma.jpay.payments.error.DuplicateProviderException;
import ukma.jpay.payments.error.ProviderNotFoundException;
import ukma.jpay.payments.error.ResourceInUseException;
import ukma.jpay.payments.persistence.PaymentProviderEntity;
import ukma.jpay.payments.repository.JpaPaymentRepository;
import ukma.jpay.payments.repository.MerchantRepository;
import ukma.jpay.payments.repository.PaymentAttemptRepository;
import ukma.jpay.payments.repository.PaymentProviderRepository;

import java.util.List;

@Service
class ProviderServiceImpl implements ProviderService {

    private final PaymentProviderRepository providers;
    private final JpaPaymentRepository payments;
    private final PaymentAttemptRepository attempts;
    private final MerchantRepository merchants;

    ProviderServiceImpl(PaymentProviderRepository providers, JpaPaymentRepository payments,
                        PaymentAttemptRepository attempts, MerchantRepository merchants) {
        this.providers = providers;
        this.payments = payments;
        this.attempts = attempts;
        this.merchants = merchants;
    }

    @Override
    @Transactional
    public PaymentProvider create(String id, String name, int priority) {
        if (providers.existsById(id)) {
            throw new DuplicateProviderException(id);
        }
        return toDomain(providers.save(new PaymentProviderEntity(id, name, priority)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentProvider> list() {
        return providers.findAll(Sort.by("priority", "id")).stream()
                .map(ProviderServiceImpl::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentProvider get(String providerId) {
        return toDomain(find(providerId));
    }

    @Override
    @Transactional
    public PaymentProvider update(String providerId, String name, int priority) {
        PaymentProviderEntity provider = find(providerId);
        provider.update(name, priority);
        return toDomain(provider);
    }

    @Override
    @Transactional
    public void delete(String providerId) {
        PaymentProviderEntity provider = find(providerId);
        if (payments.existsByProviderId(providerId)
                || attempts.existsByProviderId(providerId)
                || merchants.existsByProvidersId(providerId)) {
            throw new ResourceInUseException("Provider", providerId);
        }
        providers.delete(provider);
    }

    private PaymentProviderEntity find(String providerId) {
        return providers.findById(providerId)
                .orElseThrow(() -> new ProviderNotFoundException(providerId));
    }

    private static PaymentProvider toDomain(PaymentProviderEntity entity) {
        return new PaymentProvider(entity.getId(), entity.getName(), entity.getPriority());
    }
}
