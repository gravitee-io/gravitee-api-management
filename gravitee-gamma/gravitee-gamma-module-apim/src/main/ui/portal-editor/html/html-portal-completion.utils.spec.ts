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
    buildPortalPageHrefSlug,
    buildPortalPageLinkSnippet,
    isInsideHrefAttribute,
} from './html-portal-completion.utils';

describe('html-portal-completion.utils', () => {
    describe('isInsideHrefAttribute', () => {
        it('should return true inside an unclosed double-quoted href', () => {
            expect(isInsideHrefAttribute('<a href="')).toBe(true);
            expect(isInsideHrefAttribute('<a href="./get')).toBe(true);
        });

        it('should return true inside an unclosed single-quoted href', () => {
            expect(isInsideHrefAttribute("<a href='")).toBe(true);
            expect(isInsideHrefAttribute("<a href='./get")).toBe(true);
        });

        it('should return false outside href attributes', () => {
            expect(isInsideHrefAttribute('<a class="link" href="./done">')).toBe(false);
            expect(isInsideHrefAttribute('<div>Hello</div>')).toBe(false);
        });
    });

    describe('buildPortalPageHrefSlug', () => {
        it('should prefix slug with ./', () => {
            expect(buildPortalPageHrefSlug('getting-started-nav001')).toBe('./getting-started-nav001');
        });
    });

    describe('buildPortalPageLinkSnippet', () => {
        it('should build a full anchor tag', () => {
            expect(buildPortalPageLinkSnippet('getting-started-nav001', 'Getting Started')).toBe(
                '<a href="./getting-started-nav001">Getting Started</a>',
            );
        });
    });
});
