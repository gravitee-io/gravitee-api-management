package io.gravitee.apim.core.portal_page.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class PortalNavigationItem {

    @Nonnull
    private final PortalPageNavigationId id;

    @Setter
    @Nonnull
    private String organizationId;

    @Setter
    @Nonnull
    private String environmentId;

    @Setter
    @Nonnull
    private String title;

    @Setter
    @Nonnull
    private PortalArea area;

    @Setter
    @Nullable
    private Integer order;

    protected PortalNavigationItem(
        @Nonnull PortalPageNavigationId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String title,
        @Nonnull PortalArea area
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.environmentId = environmentId;
        this.title = title;
        this.area = area;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalNavigationItem that = (PortalNavigationItem) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "PortalNavigationItem[id=" + id + ", title=" + title + "]";
    }
}
