package io.gravitee.rest.api.service.exceptions;

import java.util.Map;
import java.util.HashMap;

public class PortalPageNotFoundException extends AbstractNotFoundException {
    private final String pageId;

    public PortalPageNotFoundException(String pageId) {
        this.pageId = pageId;
    }

    @Override
    public String getMessage() {
        return "Portal page not found: " + pageId;
    }

    @Override
    public String getTechnicalCode() {
        return "portalPage.notFound";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("pageId", pageId);
        return params;
    }
}
