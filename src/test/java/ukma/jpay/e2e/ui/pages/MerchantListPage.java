package ukma.jpay.e2e.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class MerchantListPage {

    private final Page page;
    private final String baseUrl;

    public MerchantListPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public Locator heading() {
        return page.locator("main h1");
    }

    public Locator newMerchantButton() {
        return page.locator(".page-title a.btn");
    }

    public Locator successMessage() {
        return page.locator(".alert-success");
    }

    public Locator rowWith(String text) {
        return page.locator("tbody tr", new Page.LocatorOptions().setHasText(text));
    }

    public MerchantFormPage clickNewMerchant() {
        newMerchantButton().click();
        return new MerchantFormPage(page, baseUrl);
    }

    public void openUrl(String path) {
        page.navigate(baseUrl + path);
    }
}
