package io.gravitee.apim.core.portal_page.model;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;

@Getter
public class GraviteeMarkdown extends PortalPageContent {

    @Setter
    @Nonnull
    private String content;

    public GraviteeMarkdown(@Nonnull PortalPageContentId id, @Nonnull String content) {
        super(id);
        this.content = content;
    }

    public static GraviteeMarkdown create(@Nonnull String content) {
        return new GraviteeMarkdown(PortalPageContentId.random(), content);
    }

    @Override
    public String toString() {
        return "GraviteeMarkdown[id=" + getId() + ", content=" + content + "]";
    }
}
