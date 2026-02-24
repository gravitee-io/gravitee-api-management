/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.core.portal_page.model;

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownContainer;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownContent;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;

@Getter
public final class GraviteeMarkdownPageContent extends PortalPageContent<GraviteeMarkdownContent> implements GraviteeMarkdownContainer {

    private static final PortalPageContentType TYPE = PortalPageContentType.GRAVITEE_MARKDOWN;

    @Setter
    @Nonnull
    private GraviteeMarkdownContent content;

    public GraviteeMarkdownPageContent(
        @Nonnull PortalPageContentId id,
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull GraviteeMarkdownContent content
    ) {
        super(id, organizationId, environmentId);
        this.content = content;
    }

    public static GraviteeMarkdownPageContent create(
        @Nonnull String organizationId,
        @Nonnull String environmentId,
        @Nonnull String content
    ) {
        return new GraviteeMarkdownPageContent(
            PortalPageContentId.random(),
            organizationId,
            environmentId,
            new GraviteeMarkdownContent(content)
        );
    }

    public PortalPageContentType getType() {
        return TYPE;
    }

    @Override
    public void update(@Nonnull UpdatePortalPageContent updateGraviteeMarkdownPageContent) {
        this.content = new GraviteeMarkdownContent(updateGraviteeMarkdownPageContent.getContent());
    }

    @Override
    public String getGmdContent() {
        return content.raw();
    }

    @Override
    public String toString() {
        return "GraviteeMarkdown[id=" + getId() + ", content=" + content + "]";
    }
}
