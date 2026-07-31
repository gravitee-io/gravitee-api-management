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

export function toKebab(str: string): string {
    return str.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
}

export function sanitizeCustomVarName(name: string): string {
    return name.replace(/[^a-zA-Z0-9-_]/g, '-').toLowerCase();
}

const FOUNDATION_COLOR_KEYS = new Set([
    'primary', 'primaryForeground', 'secondary', 'background', 'surface', 'text',
    'muted', 'mutedForeground', 'accent', 'border', 'ring', 'destructive', 'link',
]);

const FOUNDATION_FONT_KEYS = new Set([
    'fontFamily', 'headingFontFamily', 'fontSize', 'lineHeight',
]);

const FOUNDATION_SPACING_KEYS = new Set(['borderRadius', 'borderWidth', 'padding']);

const FOUNDATION_LAYOUT_KEYS = new Set(['maxWidth', 'sidebarWidth', 'headerHeight', 'footerHeight']);

export function foundationTokenToCssVar(key: string): string {
    if (FOUNDATION_COLOR_KEYS.has(key)) {
        return `--portal-color-${toKebab(key)}`;
    }
    if (FOUNDATION_FONT_KEYS.has(key)) {
        return `--portal-font-${toKebab(key)}`;
    }
    if (FOUNDATION_SPACING_KEYS.has(key)) {
        return `--portal-spacing-${toKebab(key)}`;
    }
    if (FOUNDATION_LAYOUT_KEYS.has(key)) {
        return `--portal-layout-${toKebab(key)}`;
    }
    return `--portal-${toKebab(key)}`;
}

export function buildElementVarName(elementId: string, variant: string | undefined, property: string): string {
    const propKebab = toKebab(property);
    if (variant) {
        return `--portal-${elementId}-${variant}-${propKebab}`;
    }
    return `--portal-${elementId}-${propKebab}`;
}

export function customVarCssName(name: string): string {
    return `--portal-custom-${sanitizeCustomVarName(name)}`;
}

const BARE_CUSTOM_VAR_NAME = /^[a-zA-Z_][\w-]*$/;
const CUSTOM_VAR_CSS_VALUE = /^var\(\s*--portal-custom-([^)]+)\s*\)$/i;

/** Expand a bare custom-var name typed in the UI to a stored CSS value. */
export function expandColorValueIfCustomVarName(value: string): string {
    const trimmed = value.trim();
    if (
        !trimmed
        || trimmed.startsWith('#')
        || trimmed.startsWith('var(')
        || trimmed.toLowerCase() === 'transparent'
    ) {
        return trimmed;
    }
    if (BARE_CUSTOM_VAR_NAME.test(trimmed)) {
        return `var(${customVarCssName(trimmed)})`;
    }
    return trimmed;
}

/** Collapse a stored custom-var CSS value back to the bare name for display. */
export function collapseCustomVarCssValue(value: string): string {
    const match = value.trim().match(CUSTOM_VAR_CSS_VALUE);
    return match ? match[1].trim() : value;
}
