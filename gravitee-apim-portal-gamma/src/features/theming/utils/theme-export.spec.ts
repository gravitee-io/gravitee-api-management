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
import { createDefaultTheme } from '../storage/default-theme';
import { exportThemeToCss } from './theme-export';

describe('theme-export', () => {
    it('should generate valid CSS with :root and :root.dark sections', () => {
        const theme = createDefaultTheme('test-portal');
        const css = exportThemeToCss(theme, 'Test Portal');

        expect(css).toContain(':root {');
        expect(css).toContain(':root.dark {');
        expect(css).toContain('@media (prefers-color-scheme: dark)');
    });

    it('should include all color tokens as CSS variables', () => {
        const theme = createDefaultTheme('test-portal');
        const css = exportThemeToCss(theme);

        expect(css).toContain('--portal-color-primary:');
        expect(css).toContain('--portal-color-secondary:');
        expect(css).toContain('--portal-color-background:');
        expect(css).toContain('--portal-color-text:');
        expect(css).toContain('--portal-color-border:');
    });

    it('should include typography tokens', () => {
        const theme = createDefaultTheme('test-portal');
        const css = exportThemeToCss(theme);

        expect(css).toContain('--portal-font-font-family:');
        expect(css).toContain('--portal-font-font-size:');
        expect(css).toContain('--portal-font-line-height:');
    });

    it('should include spacing and layout tokens', () => {
        const theme = createDefaultTheme('test-portal');
        const css = exportThemeToCss(theme);

        expect(css).toContain('--portal-spacing-border-radius:');
        expect(css).toContain('--portal-layout-max-width:');
        expect(css).toContain('--portal-layout-sidebar-width:');
    });

    it('should include custom variables', () => {
        const theme = {
            ...createDefaultTheme('test-portal'),
            customVariables: [
                { id: 'cv-1', name: 'brand-accent', lightValue: '#00ff00', darkValue: '#00cc00' },
            ],
        };
        const css = exportThemeToCss(theme);

        expect(css).toContain('--portal-custom-brand-accent: #00ff00;');
        expect(css).toMatch(/root\.dark[\s\S]*--portal-custom-brand-accent: #00cc00;/);
    });

    it('should include portal name in header comment', () => {
        const theme = createDefaultTheme('test-portal');
        const css = exportThemeToCss(theme, 'My Portal');

        expect(css).toContain('Portal Theme: My Portal');
    });
});
