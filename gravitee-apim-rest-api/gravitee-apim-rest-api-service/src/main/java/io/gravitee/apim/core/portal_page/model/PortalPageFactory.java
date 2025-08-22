package io.gravitee.apim.core.portal_page.model;

public class PortalPageFactory {
    public static PortalPage createGraviteeMarkdownPage(String id, String content) {
        return new PortalPage(PageId.of(id), new GraviteeMarkdown(content));
    }
}
