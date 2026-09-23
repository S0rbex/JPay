package ukma.jpay;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    @Test
    void modulesAreConsistent() {
        ApplicationModules.of(JPayApplication.class).verify();
    }
}
