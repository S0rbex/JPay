package ukma.jpay.payments.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ukma.jpay.payments.domain.Merchant;
import ukma.jpay.payments.error.DuplicateMerchantEmailException;
import ukma.jpay.payments.error.MerchantNotFoundException;
import ukma.jpay.payments.error.ProviderNotFoundException;
import ukma.jpay.payments.error.ResourceInUseException;
import ukma.jpay.payments.persistence.MerchantEntity;
import ukma.jpay.payments.persistence.PaymentProviderEntity;
import ukma.jpay.payments.repository.JpaPaymentRepository;
import ukma.jpay.payments.repository.MerchantRepository;
import ukma.jpay.payments.repository.PaymentProviderRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchants;
    private final PaymentProviderRepository providers;
    private final JpaPaymentRepository payments;

    MerchantServiceImpl(MerchantRepository merchants, PaymentProviderRepository providers,
                        JpaPaymentRepository payments) {
        this.merchants = merchants;
        this.providers = providers;
        this.payments = payments;
    }

    @Override
    @Transactional
    public Merchant create(String name, String contactEmail, String defaultCurrency,
                           List<String> providerIds) {
        String email = normalize(contactEmail);
        if (merchants.existsByContactEmailIgnoreCase(email)) {
            throw new DuplicateMerchantEmailException(email);
        }
        var merchant = new MerchantEntity(name, email, defaultCurrency);
        resolveProviders(providerIds).forEach(merchant::enableProvider);
        return toDomain(merchants.save(merchant));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Merchant> list() {
        return merchants.findAllWithProviders().stream().map(MerchantServiceImpl::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Merchant get(UUID merchantId) {
        return toDomain(find(merchantId));
    }

    @Override
    @Transactional
    public Merchant update(UUID merchantId, String name, String contactEmail, String defaultCurrency,
                           List<String> providerIds) {
        MerchantEntity merchant = find(merchantId);
        String email = normalize(contactEmail);
        if (merchants.existsByContactEmailIgnoreCaseAndIdNot(email, merchantId)) {
            throw new DuplicateMerchantEmailException(email);
        }
        Map<String, PaymentProviderEntity> desired = resolveProviders(providerIds).stream()
                .collect(Collectors.toMap(PaymentProviderEntity::getId, Function.identity()));

        merchant.update(name, email, defaultCurrency);
        new ArrayList<>(merchant.getProviders()).stream()
                .filter(provider -> !desired.containsKey(provider.getId()))
                .forEach(merchant::disableProvider);
        desired.values().forEach(merchant::enableProvider);
        return toDomain(merchant);
    }

    @Override
    @Transactional
    public void delete(UUID merchantId) {
        MerchantEntity merchant = find(merchantId);
        if (payments.existsByMerchantId(merchantId)) {
            throw new ResourceInUseException("Merchant", merchantId.toString());
        }
        merchants.delete(merchant);
    }

    private MerchantEntity find(UUID merchantId) {
        return merchants.findWithProvidersById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));
    }

    private List<PaymentProviderEntity> resolveProviders(List<String> providerIds) {
        var ids = new LinkedHashSet<>(providerIds);
        List<PaymentProviderEntity> found = providers.findAllById(ids);
        Map<String, PaymentProviderEntity> byId = found.stream()
                .collect(Collectors.toMap(PaymentProviderEntity::getId, Function.identity()));
        ids.stream()
                .filter(id -> !byId.containsKey(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new ProviderNotFoundException(id);
                });
        return found;
    }

    private static String normalize(String contactEmail) {
        return contactEmail.trim().toLowerCase(Locale.ROOT);
    }

    private static Merchant toDomain(MerchantEntity entity) {
        List<String> providerIds = entity.getProviders().stream()
                .map(PaymentProviderEntity::getId)
                .sorted()
                .toList();
        return new Merchant(entity.getId(), entity.getName(), entity.getContactEmail(),
                entity.getDefaultCurrency(), providerIds);
    }
}
