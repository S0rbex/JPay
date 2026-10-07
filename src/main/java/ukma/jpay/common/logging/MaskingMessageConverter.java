package ukma.jpay.common.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MaskingMessageConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return SensitiveDataMasker.mask(super.convert(event));
    }
}
