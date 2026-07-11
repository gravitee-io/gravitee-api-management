/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type {
    BlockPageContent,
    HtmlPageContent,
    OpenApiPageContent,
    PageContent,
    PageContentType,
    PortalNavigationAsyncApiPage,
    PortalNavigationHtmlPage,
    PortalNavigationOpenApiPage,
    PortalNavigationPage,
} from '../types';

export function getPageContentType(page: PortalNavigationPage): PageContentType {
    return page.contentType ?? 'BLOCK';
}

export function isOpenApiPage(page: PortalNavigationPage): page is PortalNavigationOpenApiPage {
    return getPageContentType(page) === 'OPENAPI';
}

export function isHtmlPage(page: PortalNavigationPage): page is PortalNavigationHtmlPage {
    return getPageContentType(page) === 'HTML';
}

export function isAsyncApiPage(page: PortalNavigationPage): page is PortalNavigationAsyncApiPage {
    return getPageContentType(page) === 'ASYNCAPI';
}

export function isBlockPageContent(content: PageContent): content is BlockPageContent {
    return !content.contentType || content.contentType === 'BLOCK';
}

export function isOpenApiPageContent(content: PageContent): content is OpenApiPageContent {
    return content.contentType === 'OPENAPI';
}

export function isHtmlPageContent(content: PageContent): content is HtmlPageContent {
    return content.contentType === 'HTML';
}

export function htmlPageFollowsLayoutWidth(content: HtmlPageContent): boolean {
    return content.followLayoutWidth === true;
}

export function buildHtmlPageContent(
    content: HtmlPageContent,
    updates: { readonly html: string; readonly css: string; readonly followLayoutWidth: boolean },
): HtmlPageContent {
    const { followLayoutWidth: _removed, ...rest } = content;
    return {
        ...rest,
        html: updates.html,
        css: updates.css,
        ...(updates.followLayoutWidth ? { followLayoutWidth: true } : {}),
    };
}

export function normalizePageContent(content: PageContent): PageContent {
    if (!content.contentType || content.contentType === 'BLOCK') {
        if ('document' in content) {
            return { ...content, contentType: 'BLOCK' };
        }
    }

    return content;
}
