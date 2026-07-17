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

export function encodeBase64(value: string): string {
    if (typeof btoa === 'function') {
        return btoa(unescape(encodeURIComponent(value)));
    }
    return Buffer.from(value, 'utf-8').toString('base64');
}

export function decodeBase64(value: string): string {
    if (typeof atob === 'function') {
        return decodeURIComponent(escape(atob(value)));
    }
    return Buffer.from(value, 'base64').toString('utf-8');
}

export function escapeHtmlAttribute(value: string): string {
    return value
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;');
}

export function attrsToString(attrs: Record<string, string>): string {
    return Object.entries(attrs)
        .filter(([, value]) => value !== '')
        .map(([key, value]) => `${key}="${escapeHtmlAttribute(value)}"`)
        .join(' ');
}

export function getElementAttributes(el: Element): Record<string, string> {
    const attrs: Record<string, string> = {};
    for (const attr of Array.from(el.attributes)) {
        attrs[attr.name] = attr.value;
    }
    return attrs;
}

export function mapBackgroundColorToCardColor(backgroundColor?: string | null): string {
    if (!backgroundColor || backgroundColor === 'none') {
        return 'white';
    }
    const normalized = backgroundColor.toLowerCase();
    if (normalized.includes('blue')) return 'blue';
    if (normalized.includes('purple')) return 'purple';
    if (normalized.includes('green')) return 'green';
    if (normalized.includes('orange')) return 'orange';
    return 'white';
}

const HTML_CONTAINER_TAGS = new Set([
    'div',
    'section',
    'article',
    'main',
    'header',
    'footer',
    'nav',
    'aside',
    'span',
]);

export function isHtmlContainerTag(tagName: string): boolean {
    return HTML_CONTAINER_TAGS.has(tagName.toLowerCase());
}

export function isGmdTagName(tagName: string): boolean {
    return tagName.toLowerCase().startsWith('gmd-');
}

export function elementContainsGmdDescendant(el: Element): boolean {
    return Array.from(el.querySelectorAll('*')).some(child => isGmdTagName(child.tagName));
}

/**
 * Normalizes markdown stored inside a gmd-md element back to a parseable string.
 */
export function getGmdMarkdownContent(el: HTMLElement): string {
    return (el.textContent ?? '')
        .replace(/^\s+|\s+$/g, '')
        .replace(/\n[ \t]+/g, '\n');
}

export function parseFirstElement(outerHtml: string): HTMLElement | null {
    const doc = new DOMParser().parseFromString(
        `<div data-gmd-wrapper="true">${outerHtml}</div>`,
        'text/html',
    );
    const wrapper = doc.body.querySelector('[data-gmd-wrapper]');
    return wrapper?.firstElementChild as HTMLElement | null;
}

/**
 * Returns true when plain text likely contains GMD structure worth parsing.
 */
export function looksLikeGmd(text: string): boolean {
    const trimmed = text.trim();
    if (!trimmed) {
        return false;
    }

    return /<gmd-[a-z0-9-]+\b/i.test(trimmed)
        || /<style\b[^>]*>[\s\S]*?<\/style>/i.test(trimmed)
        || (/^<(?:div|section|article)\b/i.test(trimmed) && /<\/style>\s*$/i.test(trimmed));
}

export function isStyleOnlyHtmlBlock(block: { readonly type?: string; readonly props?: Record<string, unknown> }): boolean {
    if (block.type !== 'graviteeHtml' || !block.props) {
        return false;
    }

    const html = typeof block.props.html === 'string' ? block.props.html : '';
    const css = typeof block.props.css === 'string' ? block.props.css : '';
    return !html.trim() && Boolean(css.trim());
}

export function isHtmlOnlyHtmlBlock(block: { readonly type?: string; readonly props?: Record<string, unknown> }): boolean {
    if (block.type !== 'graviteeHtml' || !block.props) {
        return false;
    }

    const html = typeof block.props.html === 'string' ? block.props.html : '';
    const css = typeof block.props.css === 'string' ? block.props.css : '';
    return Boolean(html.trim()) && !css.trim();
}

/**
 * Merges trailing style-only graviteeHtml blocks into preceding HTML graviteeHtml blocks
 * so pasted HTML documents render with their stylesheet in a single iframe preview.
 */
export function mergeTrailingStyleBlocks<T extends { readonly type?: string; readonly props?: Record<string, unknown> }>(
    blocks: readonly T[],
): T[] {
    if (blocks.length < 2) {
        return [...blocks];
    }

    const trailingStyles: string[] = [];
    let end = blocks.length;
    while (end > 0 && isStyleOnlyHtmlBlock(blocks[end - 1])) {
        end--;
        const css = String(blocks[end]?.props?.css ?? '').trim();
        if (css) {
            trailingStyles.unshift(css);
        }
    }

    if (trailingStyles.length === 0) {
        return [...blocks];
    }

    const prefix = blocks.slice(0, end);
    if (prefix.length === 0) {
        return [...blocks];
    }

    const combinedCss = trailingStyles.join('\n\n');

    let htmlRunStart = prefix.length;
    while (htmlRunStart > 0 && isHtmlOnlyHtmlBlock(prefix[htmlRunStart - 1])) {
        htmlRunStart--;
    }

    if (htmlRunStart < prefix.length) {
        const mergedHtml = prefix
            .slice(htmlRunStart)
            .map(block => String(block.props?.html ?? '').trim())
            .filter(Boolean)
            .join('\n\n');

        const merged = {
            ...prefix[prefix.length - 1],
            type: 'graviteeHtml',
            props: {
                ...(prefix[prefix.length - 1].props ?? {}),
                html: mergedHtml,
                css: combinedCss,
            },
        } as T;

        return [...prefix.slice(0, htmlRunStart), merged];
    }

    for (let index = prefix.length - 1; index >= 0; index--) {
        const block = prefix[index];
        if (block.type !== 'graviteeHtml' || !block.props) {
            continue;
        }

        const html = String(block.props.html ?? '').trim();
        if (!html) {
            continue;
        }

        const existingCss = String(block.props.css ?? '').trim();
        const merged = {
            ...block,
            props: {
                ...block.props,
                css: existingCss ? `${existingCss}\n\n${combinedCss}` : combinedCss,
            },
        } as T;

        return [...prefix.slice(0, index), merged, ...prefix.slice(index + 1)];
    }

    return [...blocks];
}
