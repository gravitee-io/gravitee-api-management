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
import {
    buildHtmlPageContent,
    htmlPageFollowsLayoutWidth,
    isBlockPageContent,
    isHtmlPage,
    isHtmlPageContent,
    isOpenApiPageContent,
} from './page-content-type';

describe('page-content-type', () => {
    it('should identify html page content', () => {
        const content = {
            id: 'content-1',
            portalId: 'portal-1',
            navigationItemId: 'page-1',
            contentType: 'HTML' as const,
            html: '<p>Hi</p>',
        };

        expect(isHtmlPageContent(content)).toBe(true);
        expect(isBlockPageContent(content)).toBe(false);
        expect(isOpenApiPageContent(content)).toBe(false);
    });

    it('should identify html navigation pages', () => {
        expect(
            isHtmlPage({
                id: 'page-1',
                portalId: 'portal-1',
                title: 'Custom',
                type: 'PAGE',
                contentType: 'HTML',
                parentId: null,
                order: 0,
                slug: 'custom',
            }),
        ).toBe(true);
    });

    it('should default html pages to full width', () => {
        const content = {
            id: 'content-1',
            portalId: 'portal-1',
            navigationItemId: 'page-1',
            contentType: 'HTML' as const,
            html: '<p>Hi</p>',
        };

        expect(htmlPageFollowsLayoutWidth(content)).toBe(false);
    });

    it('should respect followLayoutWidth when set to true', () => {
        const content = {
            id: 'content-1',
            portalId: 'portal-1',
            navigationItemId: 'page-1',
            contentType: 'HTML' as const,
            html: '<p>Hi</p>',
            followLayoutWidth: true,
        };

        expect(htmlPageFollowsLayoutWidth(content)).toBe(true);
    });

    it('should clear followLayoutWidth when building content with full width', () => {
        const content = {
            id: 'content-1',
            portalId: 'portal-1',
            navigationItemId: 'page-1',
            contentType: 'HTML' as const,
            html: '<p>Hi</p>',
            followLayoutWidth: true,
        };

        const updated = buildHtmlPageContent(content, {
            html: '<p>Updated</p>',
            css: '.page {}',
            followLayoutWidth: false,
        });

        expect(updated.html).toBe('<p>Updated</p>');
        expect(updated.css).toBe('.page {}');
        expect(updated.followLayoutWidth).toBeUndefined();
        expect(htmlPageFollowsLayoutWidth(updated)).toBe(false);
    });
});
