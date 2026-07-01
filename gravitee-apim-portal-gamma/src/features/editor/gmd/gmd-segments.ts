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

export type GmdSegment =
    | { readonly type: 'markdown'; readonly content: string }
    | { readonly type: 'element'; readonly outerHtml: string };

const SPECIAL_START_PATTERN =
    /<(?:gmd-[a-z0-9-]+\b|style\b|img\b|div\b|span\b|section\b|article\b|table\b|ul\b|ol\b)/i;

function findCompleteGmdElement(source: string, startIndex: number): string | undefined {
    const openingMatch = source.slice(startIndex).match(/^<(gmd-[a-z0-9-]+)\b[^>]*>/i);
    if (!openingMatch) {
        return undefined;
    }

    const tagName = openingMatch[1].toLowerCase();
    const doc = new DOMParser().parseFromString(source.slice(startIndex), 'text/html');
    const element = doc.body.querySelector(tagName);
    return element?.outerHTML;
}

function findCompleteStyleElement(source: string, startIndex: number): string | undefined {
    const match = source.slice(startIndex).match(/^<style\b[^>]*>[\s\S]*?<\/style>/i);
    return match?.[0];
}

function findCompleteHtmlElement(source: string, startIndex: number): string | undefined {
    const openingMatch = source.slice(startIndex).match(/^<([a-z0-9-]+)\b[^>]*>/i);
    if (!openingMatch) {
        return undefined;
    }

    const tagName = openingMatch[1].toLowerCase();
    if (tagName === 'gmd-md' || tagName.startsWith('gmd-')) {
        return findCompleteGmdElement(source, startIndex);
    }

    if (tagName === 'img') {
        const selfClosing = source.slice(startIndex).match(/^<img\b[^>]*\/?>/i);
        return selfClosing?.[0];
    }

    const doc = new DOMParser().parseFromString(source.slice(startIndex), 'text/html');
    const element = doc.body.firstElementChild;
    if (!element || element.tagName.toLowerCase() !== tagName) {
        return undefined;
    }
    return element.outerHTML;
}

export function splitGmdDocument(gmd: string): GmdSegment[] {
    const segments: GmdSegment[] = [];
    let position = 0;

    while (position < gmd.length) {
        const remaining = gmd.slice(position);
        const nextMatch = remaining.match(SPECIAL_START_PATTERN);
        const nextIndex = nextMatch?.index ?? remaining.length;

        if (nextIndex > 0) {
            const markdown = remaining.slice(0, nextIndex).trim();
            if (markdown) {
                segments.push({ type: 'markdown', content: markdown });
            }
            position += nextIndex;
            continue;
        }

        const atSpecial = position;
        const slice = gmd.slice(atSpecial);
        const lowerSlice = slice.toLowerCase();

        let outerHtml: string | undefined;
        if (lowerSlice.startsWith('<style')) {
            outerHtml = findCompleteStyleElement(gmd, atSpecial);
        } else if (lowerSlice.startsWith('<gmd-')) {
            outerHtml = findCompleteGmdElement(gmd, atSpecial);
        } else {
            outerHtml = findCompleteHtmlElement(gmd, atSpecial);
        }

        if (!outerHtml) {
            const markdown = gmd.slice(atSpecial).trim();
            if (markdown) {
                segments.push({ type: 'markdown', content: markdown });
            }
            break;
        }

        segments.push({ type: 'element', outerHtml });
        position = atSpecial + outerHtml.length;
    }

    return segments;
}
