package ukma.jpay.e2e.ui.pages;

import com.microsoft.playwright.Page;

public class LoginPage {

    private final Page page;
    private final String baseUrl;

    public LoginPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    public LoginPage open() {
        page.navigate(baseUrl + "/login");
        return this;
    }

    public MerchantListPage loginAs(String username, String password) {
        page.locator("input[name=username]").fill(username);
        page.locator("input[name=password]").fill(password);
        page.locator("main button[type=submit]").click();
        return new MerchantListPage(page, baseUrl);
    }
}
