package ukma.jpay.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @Test
    void masksCardNumberKeepingLastFourDigits() {
        assertThat(SensitiveDataMasker.mask("card 4111 1111 1111 1111 charged"))
                .isEqualTo("card ****1111 charged");
        assertThat(SensitiveDataMasker.mask("pan=4111-1111-1111-1111"))
                .isEqualTo("pan=****");
        assertThat(SensitiveDataMasker.mask("paid with 5500005555555559"))
                .isEqualTo("paid with ****5559");
    }

    @Test
    void leavesNonCardDigitSequencesUntouched() {
        assertThat(SensitiveDataMasker.mask("payment 20000000-0000-0000-0000-000000000001 amount 19.99"))
                .isEqualTo("payment 20000000-0000-0000-0000-000000000001 amount 19.99");
        assertThat(SensitiveDataMasker.mask("id 1234567890123456")).isEqualTo("id 1234567890123456");
    }

    @Test
    void masksSecretsInKeyValueAndJsonForms() {
        assertThat(SensitiveDataMasker.mask("password=hunter2 user=bob"))
                .isEqualTo("password=**** user=bob");
        assertThat(SensitiveDataMasker.mask("{\"cvv\":\"123\",\"amount\":10}"))
                .isEqualTo("{\"cvv\":\"****\",\"amount\":10}");
        assertThat(SensitiveDataMasker.mask("apiKey: sk_live_abc, next"))
                .isEqualTo("apiKey: ****, next");
        assertThat(SensitiveDataMasker.mask("cardNumber=4111111111111111"))
                .isEqualTo("cardNumber=****");
    }

    @Test
    void masksIbanKeepingCountryAndLastFour() {
        assertThat(SensitiveDataMasker.mask("iban UA213223130000026007233566001 set"))
                .isEqualTo("iban UA21****6001 set");
    }

    @Test
    void doesNotMaskWordsContainingPan() {
        assertThat(SensitiveDataMasker.mask("company=Acme")).isEqualTo("company=Acme");
    }

    @Test
    void handlesNullAndEmpty() {
        assertThat(SensitiveDataMasker.mask(null)).isNull();
        assertThat(SensitiveDataMasker.mask("")).isEmpty();
    }

    @Test
    void messageConverterMasksFormattedMessage() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger("masking-test");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);

        logger.info("charging card {} password={}", "4111111111111111", "s3cret");

        MaskingMessageConverter converter = new MaskingMessageConverter();
        converter.start();
        assertThat(converter.convert(appender.list.getFirst()))
                .isEqualTo("charging card ****1111 password=****");
        logger.detachAppender(appender);
    }

    @Test
    void throwableConverterMasksExceptionMessage() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger("masking-throwable-test");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        logger.addAppender(appender);
        logger.setAdditive(false);

        logger.error("failed", new IllegalStateException("bad card 4111111111111111 token=abc123"));

        MaskingThrowableConverter converter = new MaskingThrowableConverter();
        converter.setContext(context);
        converter.start();
        String rendered = converter.convert(appender.list.getFirst());
        assertThat(rendered).contains("****1111").contains("token=****")
                .doesNotContain("4111111111111111").doesNotContain("abc123");
        logger.detachAppender(appender);
    }
}
