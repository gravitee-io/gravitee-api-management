package io.gravitee.apim.core.portal_page.query_service;

import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalPageNavigationId;
import java.util.Collection;

public interface PortalNavigationItemsQueryService {
    PortalNavigationItem findByIdAndEnvironmentId(String environmentId, PortalPageNavigationId id);

    Collection<PortalNavigationItem> findByParentIdAndEnvironmentId(String environmentId, PortalPageNavigationId id);

    Collection<PortalNavigationItem> findTopLevelItemsByEnvironmentId(String environmentId, PortalArea portalArea);
}
