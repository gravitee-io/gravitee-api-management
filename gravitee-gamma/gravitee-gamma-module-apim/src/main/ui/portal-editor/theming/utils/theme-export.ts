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
import type { PortalThemeDocument } from '../types';
import { computeCssVars } from '../engine/compute-css-vars';

export function exportThemeToCss(theme: PortalThemeDocument, portalName?: string): string {
    const header = [
        '/**',
        ` * Portal Theme: ${portalName ?? theme.portalId}`,
        ` * Exported: ${new Date().toISOString()}`,
        ` * Theme ID: ${theme.id}`,
        ' */',
    ].join('\n');

    const instanceOverridesBlock = Object.keys(theme.instanceOverrides).length > 0
        ? `\n/* portal-instance-overrides: ${JSON.stringify(theme.instanceOverrides)} */\n`
        : '';

    const lightVars = computeCssVars(theme, false);
    const darkVars = computeCssVars(theme, true);

    const toBlock = (vars: Map<string, string>) =>
        Array.from(vars.entries()).map(([k, v]) => `  ${k}: ${v};`).join('\n');

    const lightBlock = `:root {\n${toBlock(lightVars)}\n}`;
    const darkBlock = `:root.dark {\n${toBlock(darkVars)}\n}`;

    return [header, instanceOverridesBlock, '', lightBlock, '', darkBlock, ''].join('\n');
}

export function downloadThemeCss(theme: PortalThemeDocument, portalName?: string): void {
    const css = exportThemeToCss(theme, portalName);
    const blob = new Blob([css], { type: 'text/css' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `portal-theme-${theme.portalId}.css`;
    a.click();
    URL.revokeObjectURL(url);
}
