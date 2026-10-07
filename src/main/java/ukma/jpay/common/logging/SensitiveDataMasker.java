package ukma.jpay.common.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    private static final String MASK = "****";

    private static final Pattern KEY_VALUE = Pattern.compile(
            "(?i)(\\b(?:[\\w-]*(?:password|passwd|pwd|secret|token|api[_-]?key|cvv2?|cvc2?|card[_-]?number|iban)[\\w-]*|pan)\"?\\s*[:=]\\s*)(\"[^\"]*\"|[^\\s,;&}\\]]+)");

    private static final Pattern CARD_NUMBER = Pattern.compile("(?<![\\w-])(?:\\d[ -]?){12,18}\\d(?![\\w-])");

    private static final Pattern IBAN = Pattern.compile("\\b([A-Z]{2}\\d{2})[A-Z0-9]{7,26}([A-Z0-9]{4})\\b");

    private SensitiveDataMasker() {
    }

    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = maskKeyValues(text);
        result = maskCardNumbers(result);
        return maskIbans(result);
    }

    private static String maskKeyValues(String text) {
        return KEY_VALUE.matcher(text).replaceAll(match -> {
            String value = match.group(2);
            String masked = value.startsWith("\"") ? "\"" + MASK + "\"" : MASK;
            return Matcher.quoteReplacement(match.group(1) + masked);
        });
    }

    private static String maskCardNumbers(String text) {
        return CARD_NUMBER.matcher(text).replaceAll(match -> {
            String candidate = match.group();
            String digits = candidate.replaceAll("[ -]", "");
            if (digits.length() < 13 || digits.length() > 19 || !passesLuhn(digits)) {
                return Matcher.quoteReplacement(candidate);
            }
            return Matcher.quoteReplacement(MASK + digits.substring(digits.length() - 4));
        });
    }

    private static String maskIbans(String text) {
        return IBAN.matcher(text).replaceAll(match ->
                Matcher.quoteReplacement(match.group(1) + MASK + match.group(2)));
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
