package io.gravitee.apim.core.portal_page.model;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PortalNavigationLink extends PortalNavigationItem {

    @Setter
    @Nonnull
    private String href;

    public PortalNavigationLink(
        @Nonnull PortalPageNavigationId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String title,
        @Nonnull PortalArea area,
        @Nonnull String href
    ) {
        super(id, organizationId, environmentId, title, area);
        this.href = href;
    }
}
