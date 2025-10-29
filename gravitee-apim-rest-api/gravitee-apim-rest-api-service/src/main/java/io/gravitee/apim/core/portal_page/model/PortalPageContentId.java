package io.gravitee.apim.core.portal_page.model;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nonnull;
import java.util.UUID;

public class PortalPageContentId {

    @Nonnull
    private final UUID id;

    private PortalPageContentId(@Nonnull UUID id) {
        this.id = id;
    }

    public static PortalPageContentId random() {
        return new PortalPageContentId(UUID.randomUUID());
    }

    public static PortalPageContentId of(String value) {
        return new PortalPageContentId(UUID.fromString(value));
    }

    public UUID id() {
        return id;
    }

    @JsonValue
    public String json() {
        return this.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortalPageContentId that = (PortalPageContentId) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
