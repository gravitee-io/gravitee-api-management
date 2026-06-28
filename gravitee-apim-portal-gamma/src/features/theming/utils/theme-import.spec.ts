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
import { importThemeFromCss } from './theme-import';

describe('theme-import', () => {
    it('should round-trip export and import of default theme', () => {
        const theme = createDefaultTheme('test-portal');
        const css = exportThemeToCss(theme);
        const result = importThemeFromCss(css);

        expect(result.success).toBe(true);
        expect(result.theme?.tokens?.light.colors.primary).toBe(theme.tokens.light.colors.primary);
        expect(result.theme?.tokens?.dark.colors.primary).toBe(theme.tokens.dark.colors.primary);
    });

    it('should return error for empty CSS', () => {
        const result = importThemeFromCss('');

        expect(result.success).toBe(false);
        expect(result.error).toBe('No portal theme variables found in the CSS file.');
    });

    it('should return error for CSS without portal variables', () => {
        const result = importThemeFromCss(':root { --some-other-var: red; }');

        expect(result.success).toBe(false);
    });

    it('should import custom variables from both light and dark sections', () => {
        const theme = {
            ...createDefaultTheme('test-portal'),
            customVariables: [
                { id: 'cv-1', name: 'brand-color', lightValue: '#ff0000', darkValue: '#cc0000' },
            ],
        };
        const css = exportThemeToCss(theme);
        const result = importThemeFromCss(css);

        expect(result.success).toBe(true);
        expect(result.theme?.customVariables).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ name: 'brand-color' }),
            ]),
        );
    });

    it('should preserve default values for tokens not present in CSS', () => {
        const css = ':root { --portal-color-primary: #123456; }';
        const result = importThemeFromCss(css);

        expect(result.success).toBe(true);
        expect(result.theme?.tokens?.light.colors.primary).toBe('#123456');
        expect(result.theme?.tokens?.light.colors.background).toBeDefined();
    });
});
