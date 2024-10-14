package io.gravitee.gateway.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import io.reactiverse.contextual.logging.ContextualData;
import java.util.Map;

public class ContextualLoggingJsonLayout extends JsonLayout {

    @Override
    protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
        map.put("orgId", ContextualData.get("orgId"));
        map.put("envId", ContextualData.get("envId"));
    }
}
