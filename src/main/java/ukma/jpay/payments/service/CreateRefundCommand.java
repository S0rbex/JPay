package ukma.jpay.payments.service;

import java.math.BigDecimal;

public record CreateRefundCommand(BigDecimal amount, String currency, String reason) {
}
