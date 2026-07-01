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

export function parseFirstElement(outerHtml: string): HTMLElement | null {
    const doc = new DOMParser().parseFromString(
        `<div data-gmd-wrapper="true">${outerHtml}</div>`,
        'text/html',
    );
    const wrapper = doc.body.querySelector('[data-gmd-wrapper]');
    return wrapper?.firstElementChild as HTMLElement | null;
}
