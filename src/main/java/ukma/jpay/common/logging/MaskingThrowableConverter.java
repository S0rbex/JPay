package ukma.jpay.common.logging;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;

public class MaskingThrowableConverter extends ThrowableProxyConverter {

    @Override
    protected String throwableProxyToString(IThrowableProxy proxy) {
        return SensitiveDataMasker.mask(super.throwableProxyToString(proxy));
    }
}
