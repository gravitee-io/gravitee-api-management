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
import { markdownToSafeHtml } from './markdown';

describe('markdownToSafeHtml', () => {
    it('should render headings and paragraphs', () => {
        expect(markdownToSafeHtml('# Getting started\n\nSubscribe to a plan.')).toBe(
            '<h1>Getting started</h1><p>Subscribe to a plan.</p>',
        );
    });

    it('should escape HTML in the source', () => {
        expect(markdownToSafeHtml('# Hello <script>alert(1)</script>')).toBe(
            '<h1>Hello &lt;script&gt;alert(1)&lt;/script&gt;</h1>',
        );
    });
});
