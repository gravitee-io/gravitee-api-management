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

function escapeHtml(text: string): string {
    return text.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
}

function inline(text: string): string {
    return escapeHtml(text)
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/`([^`]+)`/g, '<code>$1</code>');
}

export function markdownToSafeHtml(markdown: string): string {
    const lines = markdown.replaceAll('\r\n', '\n').split('\n');
    const html: string[] = [];
    let paragraph: string[] = [];

    const flushParagraph = () => {
        if (paragraph.length === 0) {
            return;
        }
        html.push(`<p>${inline(paragraph.join(' '))}</p>`);
        paragraph = [];
    };

    for (const line of lines) {
        const heading = /^(#{1,3})\s+(.+)$/.exec(line);
        if (heading?.[1] && heading[2]) {
            flushParagraph();
            const level = heading[1].length;
            html.push(`<h${level}>${inline(heading[2])}</h${level}>`);
            continue;
        }
        if (line.trim() === '') {
            flushParagraph();
            continue;
        }
        paragraph.push(line);
    }
    flushParagraph();
    return html.join('');
}
