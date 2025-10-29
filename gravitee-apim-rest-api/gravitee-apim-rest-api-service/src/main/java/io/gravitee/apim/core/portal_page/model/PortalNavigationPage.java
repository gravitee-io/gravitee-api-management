package io.gravitee.apim.core.portal_page.model;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PortalNavigationPage extends PortalNavigationItem {

    @Setter
    @Nonnull
    private PortalPageContentId contentId;

    public PortalNavigationPage(
        @Nonnull PortalPageNavigationId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String title,
        @Nonnull PortalArea area,
        @Nonnull PortalPageContentId contentId
    ) {
        super(id, organizationId, environmentId, title, area);
        this.contentId = contentId;
    }
}
