package io.gravitee.apim.core.portal_page.model;

import jakarta.annotation.Nonnull;
import lombok.Getter;

@Getter
public abstract class PortalPageContent {

    @Nonnull
    private final PortalPageContentId id;

    protected PortalPageContent(@Nonnull PortalPageContentId id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalPageContent that = (PortalPageContent) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
