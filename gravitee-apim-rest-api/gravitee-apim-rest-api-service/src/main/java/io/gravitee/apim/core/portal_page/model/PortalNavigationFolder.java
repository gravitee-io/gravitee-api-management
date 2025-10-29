package io.gravitee.apim.core.portal_page.model;

import jakarta.annotation.Nonnull;

public class PortalNavigationFolder extends PortalNavigationItem {

    public PortalNavigationFolder(
        @Nonnull PortalPageNavigationId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String title,
        @Nonnull PortalArea area
    ) {
        super(id, organizationId, environmentId, title, area);
    }
}
