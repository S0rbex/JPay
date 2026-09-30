package ukma.jpay.payments.web.dto;

import ukma.jpay.payments.domain.Merchant;

import java.util.List;
import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String name,
        String contactEmail,
        String defaultCurrency,
        List<String> providerIds) {

    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(merchant.id(), merchant.name(), merchant.contactEmail(),
                merchant.defaultCurrency(), merchant.providerIds());
    }
}
