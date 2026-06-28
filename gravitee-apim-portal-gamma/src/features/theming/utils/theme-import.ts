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
import type { PortalTheme, ThemeTokens, ColorTokens, TypographyTokens, SpacingTokens, LayoutTokens, CustomVariable } from '../types';
import { DEFAULT_LIGHT_TOKENS, DEFAULT_DARK_TOKENS } from '../storage/default-theme';

interface ParsedCssVars {
    light: Map<string, string>;
    dark: Map<string, string>;
}

function parseCssCustomProperties(css: string): ParsedCssVars {
    const light = new Map<string, string>();
    const dark = new Map<string, string>();

    const varPattern = /--(portal-[a-z0-9-]+)\s*:\s*([^;]+);/g;

    const rootBlockRegex = /:root\s*\{([^}]+)\}/g;
    let match: RegExpExecArray | null;

    // eslint-disable-next-line no-cond-assign
    while ((match = rootBlockRegex.exec(css)) !== null) {
        const block = match[0];
        const content = match[1];
        const isDark = /\.dark/.test(block) || /prefers-color-scheme:\s*dark/.test(css.slice(Math.max(0, css.lastIndexOf('@media', match.index)), match.index));

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

function extractTokensFromVars(vars: Map<string, string>, defaults: ThemeTokens): ThemeTokens {
    const colors = { ...defaults.colors } as Record<string, string>;
    const typography = { ...defaults.typography } as Record<string, string | number>;
    const spacing = { ...defaults.spacing } as Record<string, string>;
    const layout = { ...defaults.layout } as Record<string, string>;

    for (const [key, value] of vars) {
        if (key.startsWith('portal-color-')) {
            const tokenKey = toCamel(key.replace('portal-color-', ''));
            if (tokenKey in colors) {
                colors[tokenKey] = value;
            }
        } else if (key.startsWith('portal-font-')) {
            const tokenKey = toCamel(key.replace('portal-font-', ''));
            if (tokenKey in typography) {
                const numVal = Number(value);
                typography[tokenKey] = !isNaN(numVal) && tokenKey === 'headingScale' ? numVal : value;
            }
        } else if (key.startsWith('portal-spacing-')) {
            const tokenKey = toCamel(key.replace('portal-spacing-', ''));
            if (tokenKey in spacing) {
                spacing[tokenKey] = value;
            }
        } else if (key.startsWith('portal-layout-')) {
            const tokenKey = toCamel(key.replace('portal-layout-', ''));
            if (tokenKey in layout) {
                layout[tokenKey] = value;
            }
        }
    }

    return {
        colors: colors as unknown as ColorTokens,
        typography: typography as unknown as TypographyTokens,
        spacing: spacing as unknown as SpacingTokens,
        layout: layout as unknown as LayoutTokens,
    };
}

function extractCustomVariables(lightVars: Map<string, string>, darkVars: Map<string, string>): CustomVariable[] {
    const customNames = new Set<string>();
    for (const key of lightVars.keys()) {
        if (key.startsWith('portal-custom-')) customNames.add(key.replace('portal-custom-', ''));
    }
    for (const key of darkVars.keys()) {
        if (key.startsWith('portal-custom-')) customNames.add(key.replace('portal-custom-', ''));
    }

    return Array.from(customNames).map(name => ({
        id: `custom-${name}`,
        name,
        lightValue: lightVars.get(`portal-custom-${name}`) ?? '',
        darkValue: darkVars.get(`portal-custom-${name}`) ?? '',
    }));
}

export interface ImportResult {
    readonly success: boolean;
    readonly theme?: Partial<PortalTheme>;
    readonly error?: string;
}

export function importThemeFromCss(css: string): ImportResult {
    try {
        const parsed = parseCssCustomProperties(css);

        if (parsed.light.size === 0 && parsed.dark.size === 0) {
            return { success: false, error: 'No portal theme variables found in the CSS file.' };
        }

        const lightTokens = parsed.light.size > 0
            ? extractTokensFromVars(parsed.light, DEFAULT_LIGHT_TOKENS)
            : DEFAULT_LIGHT_TOKENS;

        const darkTokens = parsed.dark.size > 0
            ? extractTokensFromVars(parsed.dark, DEFAULT_DARK_TOKENS)
            : DEFAULT_DARK_TOKENS;

        const customVariables = extractCustomVariables(parsed.light, parsed.dark);

        return {
            success: true,
            theme: {
                tokens: { light: lightTokens, dark: darkTokens },
                customVariables,
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
