package ukma.jpay.common.error;

import java.util.Map;

public abstract class BusinessException extends RuntimeException {

    private final ProblemType type;

    protected BusinessException(ProblemType type, String message) {
        super(message);
        this.type = type;
    }

    public ProblemType type() {
        return type;
    }

    public Map<String, Object> properties() {
        return Map.of();
    }
}
