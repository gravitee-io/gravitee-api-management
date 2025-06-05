package io.gravitee.rest.api.model.log;

import lombok.Builder;
import lombok.Data;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@Builder
public class DiagnoticItem {

    private String componentType;
    private String componentName;
    private String key;
    private String message;
}
