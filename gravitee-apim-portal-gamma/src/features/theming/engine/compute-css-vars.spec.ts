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
import { computeCssVars } from '../engine/compute-css-vars';
import { createDefaultThemeDocument } from '../storage/migrate-legacy-theme';

describe('computeCssVars', () => {
    it('should emit foundation and custom variables', () => {
        const doc = {
            ...createDefaultThemeDocument('p1'),
            foundation: {
                light: { primary: '#6366f1' },
                dark: { primary: '#818cf8' },
            },
            customVariables: [{ name: 'brand', lightValue: '#ff6600', darkValue: '#ff9944' }],
        };

        const lightVars = computeCssVars(doc, false);
        expect(lightVars.get('--portal-color-primary')).toBe('#6366f1');
        expect(lightVars.get('--portal-custom-brand')).toBe('#ff6600');
        expect(lightVars.has('--portal-spacing-padding')).toBe(false);

        const darkVars = computeCssVars(doc, true);
        expect(darkVars.get('--portal-color-primary')).toBe('#818cf8');
        expect(darkVars.get('--portal-custom-brand')).toBe('#ff9944');
    });

    it('should resolve size presets for foundation tokens', () => {
        const doc = {
            ...createDefaultThemeDocument('p1'),
            foundation: {
                light: { padding: 'md' },
                dark: { padding: 'lg' },
            },
        };

        expect(computeCssVars(doc, false).get('--portal-spacing-padding')).toBe('1rem');
        expect(computeCssVars(doc, true).get('--portal-spacing-padding')).toBe('1.5rem');
    });

    it('should resolve size presets for element tokens', () => {
        const doc = {
            ...createDefaultThemeDocument('p1'),
            elements: {
                button: {
                    filled: {
                        light: { borderRadius: 'lg' },
                        dark: {},
                    },
                },
            },
        };

        const vars = computeCssVars(doc, false);
        expect(vars.get('--portal-button-filled-border-radius')).toBe('8px');
    });

    it('should resolve nav-item part tokens', () => {
        const doc = {
            ...createDefaultThemeDocument('p1'),
            elements: {
                'nav-item': {
                    default: {
                        light: { background: '#ffffff', padding: 'sm' },
                        dark: {},
                    },
                    selected: {
                        light: { background: '#eef2ff', fontWeight: '600' },
                        dark: {},
                    },
                },
            },
        };

        const vars = computeCssVars(doc, false);
        expect(vars.get('--portal-nav-item-background')).toBe('#ffffff');
        expect(vars.get('--portal-nav-item-padding')).toBe('0.5rem');
        expect(vars.get('--portal-nav-item-selected-background')).toBe('#eef2ff');
        expect(vars.get('--portal-nav-item-selected-font-weight')).toBe('600');
    });
});
