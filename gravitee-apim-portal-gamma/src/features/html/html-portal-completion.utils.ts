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
const HREF_DOUBLE_QUOTE_PATTERN = /href\s*=\s*"[^"]*$/;
const HREF_SINGLE_QUOTE_PATTERN = /href\s*=\s*'[^']*$/;

export function isInsideHrefAttribute(textBeforeCursor: string): boolean {
    return HREF_DOUBLE_QUOTE_PATTERN.test(textBeforeCursor) || HREF_SINGLE_QUOTE_PATTERN.test(textBeforeCursor);
}

export function buildPortalPageHrefSlug(slug: string): string {
    return `./${slug}`;
}

export function buildPortalPageLinkSnippet(slug: string, title: string): string {
    return `<a href="${buildPortalPageHrefSlug(slug)}">${title}</a>`;
}
