package ukma.jpay.e2e.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class MerchantFormPage {

    private final Page page;
    private final String baseUrl;

    public MerchantFormPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public MerchantFormPage fillName(String name) {
        page.locator("#name").fill(name);
        return this;
    }

    public MerchantFormPage fillEmail(String email) {
        page.locator("#contactEmail").fill(email);
        return this;
    }

    public MerchantFormPage fillCurrency(String currency) {
        page.locator("#defaultCurrency").fill(currency);
        return this;
    }

    public MerchantFormPage selectProvider(String providerName) {
        page.locator("label.checkbox", new Page.LocatorOptions().setHasText(providerName))
                .locator("input[type=checkbox]")
                .check();
        return this;
    }

    public MerchantListPage submit() {
        page.locator("form.form button[type=submit]").click();
        return new MerchantListPage(page, baseUrl);
    }

    public MerchantFormPage submitExpectingErrors() {
        page.locator("form.form button[type=submit]").click();
        return this;
    }

    public Locator errors() {
        return page.locator(".error");
    }
}
