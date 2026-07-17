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
import DOMPurify from 'dompurify';

const ALLOWED_TAGS = [
    'div', 'span', 'section', 'article', 'header', 'footer', 'nav', 'main', 'aside',
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p', 'ul', 'ol', 'li', 'a', 'img', 'table',
    'thead', 'tbody', 'tr', 'th', 'td', 'strong', 'em', 'br', 'hr', 'style', 'video', 'audio', 'svg',
];

export function sanitizePortalHtml(html: string): string {
    return DOMPurify.sanitize(html, {
        ALLOWED_TAGS,
        ALLOWED_ATTR: ['class', 'id', 'href', 'src', 'alt', 'target', 'rel', 'style', 'data-gravitee-component', 'data-*'],
        ALLOW_DATA_ATTR: true,
    });
}
