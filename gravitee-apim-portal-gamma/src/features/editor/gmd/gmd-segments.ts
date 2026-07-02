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

type SourceElementRange = {
    readonly outerHtml: string;
    readonly endIndex: number;
};

const SPECIAL_START_PATTERN =
    /<(?:gmd-[a-z0-9-]+\b|style\b|img\b|div\b|span\b|section\b|article\b|table\b|ul\b|ol\b)/i;

function escapeRegExp(value: string): string {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Finds a complete HTML/GMD element using source-string boundaries.
 * DOMParser serialization lengths can differ from the source text and must not
 * be used to advance parse position.
 */
export function findBalancedElementRange(source: string, startIndex: number): SourceElementRange | undefined {
    const slice = source.slice(startIndex);
    const opening = slice.match(/^<([a-z0-9-]+)\b[^>]*>/i);
    if (!opening) {
        return undefined;
    }

    const tagName = opening[1].toLowerCase();
    const openingTag = opening[0];

    if (tagName === 'img') {
        const imgMatch = slice.match(/^<img\b[^>]*\/?>/i);
        if (imgMatch) {
            return { outerHtml: imgMatch[0], endIndex: startIndex + imgMatch[0].length };
        }
    }

    if (tagName === 'style') {
        const styleMatch = slice.match(/^<style\b[^>]*>[\s\S]*?<\/style>/i);
        if (styleMatch) {
            return { outerHtml: styleMatch[0], endIndex: startIndex + styleMatch[0].length };
        }
        return undefined;
    }

    if (/\/\s*>$/.test(openingTag)) {
        return { outerHtml: openingTag, endIndex: startIndex + openingTag.length };
    }

    const openPattern = new RegExp(`<${escapeRegExp(tagName)}(?=[\\s>/])`, 'gi');
    const closePattern = new RegExp(`</${escapeRegExp(tagName)}>`, 'gi');

    let depth = 1;
    let cursor = startIndex + openingTag.length;

    while (cursor < source.length && depth > 0) {
        const tail = source.slice(cursor);
        openPattern.lastIndex = 0;
        closePattern.lastIndex = 0;

        const openMatch = openPattern.exec(tail);
        const closeMatch = closePattern.exec(tail);

        if (!closeMatch) {
            return undefined;
        }

        const openAt = openMatch?.index ?? Number.POSITIVE_INFINITY;
        const closeAt = closeMatch.index;

        if (openAt < closeAt) {
            depth++;
            const nestedOpenTag = tail.slice(openAt).match(/^<[a-z0-9-]+\b[^>]*>/i)?.[0] ?? openMatch![0];
            cursor += openAt + nestedOpenTag.length;
            continue;
        }

        depth--;
        cursor += closeAt + closeMatch[0].length;
    }

    if (depth !== 0) {
        return undefined;
    }

    return { outerHtml: source.slice(startIndex, cursor), endIndex: cursor };
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
        const range = findBalancedElementRange(gmd, atSpecial);

        if (!range) {
            const markdown = gmd.slice(atSpecial).trim();
            if (markdown) {
                segments.push({ type: 'markdown', content: markdown });
            }
            break;
        }

        segments.push({ type: 'element', outerHtml: range.outerHtml });
        position = range.endIndex;
    }

    return segments;
}
