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
import type { PortalTheme, ThemeTokens, CustomVariable } from '../types';

function toKebab(str: string): string {
    return str.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
}

function tokensToCssBlock(tokens: ThemeTokens, customVariables: readonly CustomVariable[], isDark: boolean): string {
    const lines: string[] = [];

    lines.push('  /* Colors */');
    for (const [key, value] of Object.entries(tokens.colors)) {
        lines.push(`  --portal-color-${toKebab(key)}: ${value};`);
    }

    lines.push('');
    lines.push('  /* Typography */');
    for (const [key, value] of Object.entries(tokens.typography)) {
        lines.push(`  --portal-font-${toKebab(key)}: ${value};`);
    }

    lines.push('');
    lines.push('  /* Spacing */');
    for (const [key, value] of Object.entries(tokens.spacing)) {
        lines.push(`  --portal-spacing-${toKebab(key)}: ${value};`);
    }

    lines.push('');
    lines.push('  /* Layout */');
    for (const [key, value] of Object.entries(tokens.layout)) {
        lines.push(`  --portal-layout-${toKebab(key)}: ${value};`);
    }

    if (customVariables.length > 0) {
        lines.push('');
        lines.push('  /* Custom Variables */');
        for (const cv of customVariables) {
            const safeName = cv.name.replace(/[^a-zA-Z0-9-_]/g, '-').toLowerCase();
            lines.push(`  --portal-custom-${safeName}: ${isDark ? cv.darkValue : cv.lightValue};`);
        }
    }

    return lines.join('\n');
}

export function exportThemeToCss(theme: PortalTheme, portalName?: string): string {
    const header = [
        '/**',
        ` * Portal Theme: ${portalName ?? theme.portalId}`,
        ` * Exported: ${new Date().toISOString()}`,
        ` * Theme ID: ${theme.id}`,
        ' */',
    ].join('\n');

    const lightBlock = `:root {\n${tokensToCssBlock(theme.tokens.light, theme.customVariables, false)}\n}`;
    const darkBlock = `:root.dark {\n${tokensToCssBlock(theme.tokens.dark, theme.customVariables, true)}\n}`;
    const mediaDarkBlock = `@media (prefers-color-scheme: dark) {\n  :root:not(.light) {\n${tokensToCssBlock(theme.tokens.dark, theme.customVariables, true).replace(/^  /gm, '    ')}\n  }\n}`;

    return [header, '', lightBlock, '', darkBlock, '', mediaDarkBlock, ''].join('\n');
}

export function downloadThemeCss(theme: PortalTheme, portalName?: string): void {
    const css = exportThemeToCss(theme, portalName);
    const blob = new Blob([css], { type: 'text/css' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `portal-theme-${theme.portalId}.css`;
    a.click();
    URL.revokeObjectURL(url);
}
