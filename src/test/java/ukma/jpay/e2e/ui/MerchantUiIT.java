package ukma.jpay.e2e.ui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.web.server.LocalServerPort;
import ukma.jpay.e2e.ui.pages.LoginPage;
import ukma.jpay.e2e.ui.pages.MerchantFormPage;
import ukma.jpay.e2e.ui.pages.MerchantListPage;
import ukma.jpay.support.IntegrationTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@IntegrationTest
class MerchantUiIT {

    private static final Path OUTPUT = Paths.get("target", "playwright");

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    private int port;

    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    @BeforeEach
    void openPage() {
        context = browser.newContext();
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
        page = context.newPage();
    }

    @AfterEach
    void savePage(TestInfo testInfo) {
        String name = testInfo.getTestMethod().orElseThrow().getName();
        page.screenshot(new Page.ScreenshotOptions().setPath(OUTPUT.resolve(name + ".png")).setFullPage(true));
        context.tracing().stop(new Tracing.StopOptions().setPath(OUTPUT.resolve(name + "-trace.zip")));
        context.close();
    }

    @Test
    void adminCreatesMerchant() {
        String email = "ui-" + UUID.randomUUID() + "@example.com";

        MerchantListPage list = loginPage().loginAs("admin", "admin123");
        MerchantListPage result = list.clickNewMerchant()
                .fillName("Playwright Shop")
                .fillEmail(email)
                .fillCurrency("USD")
                .selectProvider("Stripe")
                .submit();

        assertThat(result.successMessage()).containsText("Playwright Shop");
        assertThat(result.rowWith(email)).isVisible();
    }

    @Test
    void invalidFormShowsValidationErrors() {
        MerchantFormPage form = loginPage().loginAs("admin", "admin123")
                .clickNewMerchant()
                .fillName("")
                .fillEmail("not-an-email")
                .fillCurrency("usd")
                .submitExpectingErrors();

        assertThat(form.errors()).hasCount(4);
        assertThat(page).hasURL(baseUrl() + "/merchants");
    }

    @Test
    void userCannotCreateMerchant() {
        MerchantListPage list = loginPage().loginAs("user", "user123");

        assertThat(list.heading()).hasText("Merchants");
        assertThat(list.newMerchantButton()).hasCount(0);

        list.openUrl("/merchants/new");

        assertThat(list.heading()).containsText("403");
    }

    private LoginPage loginPage() {
        return new LoginPage(page, baseUrl()).open();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
