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
import type { FoundationTokens, PortalThemeDocument } from '../types';
import { toKebab } from '../registry/var-names';

interface ParsedCssVars {
    light: Map<string, string>;
    dark: Map<string, string>;
}

function parseCssCustomProperties(css: string): ParsedCssVars {
    const light = new Map<string, string>();
    const dark = new Map<string, string>();

    const varPattern = /--(portal-[a-z0-9-]+)\s*:\s*([^;]+);/g;
    const rootBlockRegex = /:root[^{]*\{([^}]+)\}/g;
    let match: RegExpExecArray | null;

    // eslint-disable-next-line no-cond-assign
    while ((match = rootBlockRegex.exec(css)) !== null) {
        const block = match[0];
        const content = match[1];
        const isDark = /\.dark/.test(block);

        let varMatch: RegExpExecArray | null;
        const localPattern = new RegExp(varPattern.source, 'g');
        // eslint-disable-next-line no-cond-assign
        while ((varMatch = localPattern.exec(content)) !== null) {
            const target = isDark ? dark : light;
            target.set(varMatch[1], varMatch[2].trim());
        }
    }

    return { light, dark };
}

function toCamel(kebab: string): string {
    return kebab.replace(/-([a-z])/g, (_, c: string) => c.toUpperCase());
}

const FOUNDATION_VAR_MAP: Record<string, keyof FoundationTokens> = {
    'portal-color-primary': 'primary',
    'portal-color-primary-foreground': 'primaryForeground',
    'portal-color-secondary': 'secondary',
    'portal-color-background': 'background',
    'portal-color-surface': 'surface',
    'portal-color-text': 'text',
    'portal-color-muted': 'muted',
    'portal-color-muted-foreground': 'mutedForeground',
    'portal-color-accent': 'accent',
    'portal-color-border': 'border',
    'portal-color-ring': 'ring',
    'portal-color-destructive': 'destructive',
    'portal-color-link': 'link',
    'portal-font-family': 'fontFamily',
    'portal-font-heading-font-family': 'headingFontFamily',
    'portal-font-font-size': 'fontSize',
    'portal-font-line-height': 'lineHeight',
    'portal-spacing-border-radius': 'borderRadius',
    'portal-spacing-border-width': 'borderWidth',
    'portal-spacing-padding': 'padding',
    'portal-layout-max-width': 'maxWidth',
    'portal-layout-sidebar-width': 'sidebarWidth',
    'portal-layout-header-height': 'headerHeight',
    'portal-layout-footer-height': 'footerHeight',
};

function extractFoundationFromVars(vars: Map<string, string>): Partial<FoundationTokens> {
    const foundation: Partial<FoundationTokens> = {};
    for (const [key, value] of vars) {
        const tokenKey = FOUNDATION_VAR_MAP[key];
        if (tokenKey) {
            (foundation as Record<string, string>)[tokenKey] = value;
        }
    }
    return foundation;
}

function extractCustomVariables(lightVars: Map<string, string>, darkVars: Map<string, string>) {
    const customNames = new Set<string>();
    for (const key of lightVars.keys()) {
        if (key.startsWith('portal-custom-')) customNames.add(key.replace('portal-custom-', ''));
    }
    for (const key of darkVars.keys()) {
        if (key.startsWith('portal-custom-')) customNames.add(key.replace('portal-custom-', ''));
    }

    return Array.from(customNames).map(name => ({
        name,
        lightValue: lightVars.get(`portal-custom-${name}`) ?? '',
        darkValue: darkVars.get(`portal-custom-${name}`) ?? '',
    }));
}

export interface ImportResult {
    readonly success: boolean;
    readonly theme?: Partial<PortalThemeDocument>;
    readonly error?: string;
}

function extractInstanceOverrides(css: string): Record<string, Record<string, string>> {
    const match = css.match(/portal-instance-overrides:\s*(\{[\s\S]*?\})\s*\*\//);
    if (!match) {
        return {};
    }
    try {
        const parsed = JSON.parse(match[1]) as Record<string, Record<string, string>>;
        return parsed && typeof parsed === 'object' ? parsed : {};
    } catch {
        return {};
    }
}

export function importThemeFromCss(css: string): ImportResult {
    try {
        const parsed = parseCssCustomProperties(css);

        if (parsed.light.size === 0 && parsed.dark.size === 0) {
            return { success: false, error: 'No portal theme variables found in the CSS file.' };
        }

        const lightFoundation = parsed.light.size > 0
            ? extractFoundationFromVars(parsed.light)
            : {};

        const darkFoundation = parsed.dark.size > 0
            ? extractFoundationFromVars(parsed.dark)
            : {};

        const customVariables = extractCustomVariables(parsed.light, parsed.dark);
        const instanceOverrides = extractInstanceOverrides(css);

        return {
            success: true,
            theme: {
                foundation: { light: lightFoundation, dark: darkFoundation },
                customVariables,
                instanceOverrides,
            },
        };
    } catch (error) {
        return {
            success: false,
            error: error instanceof Error ? error.message : 'Failed to parse CSS file.',
        };
    }
}

export function readFileAsText(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = () => reject(reader.error);
        reader.readAsText(file);
    });
}
