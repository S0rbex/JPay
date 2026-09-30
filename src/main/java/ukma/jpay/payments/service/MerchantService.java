package ukma.jpay.payments.service;

import ukma.jpay.payments.domain.Merchant;

import java.util.List;
import java.util.UUID;

public interface MerchantService {

    Merchant create(String name, String contactEmail, String defaultCurrency, List<String> providerIds);

    List<Merchant> list();

    Merchant get(UUID merchantId);

    Merchant update(UUID merchantId, String name, String contactEmail, String defaultCurrency,
                    List<String> providerIds);

    void delete(UUID merchantId);
}
