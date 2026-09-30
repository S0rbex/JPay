package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;

public class ResourceInUseException extends BusinessException {

    private final String resource;
    private final String resourceId;

    public ResourceInUseException(String resource, String resourceId) {
        super(ProblemType.RESOURCE_IN_USE,
                "%s %s is still referenced and cannot be deleted".formatted(resource, resourceId));
        this.resource = resource;
        this.resourceId = resourceId;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("resource", resource, "resourceId", resourceId);
    }
}
