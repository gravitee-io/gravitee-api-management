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
import { migrateLegacyTheme } from './migrate-legacy-theme';

describe('migrateLegacyTheme', () => {
    it('should convert legacy tokens to foundation overrides', () => {
        const legacy = {
            id: 'theme-1',
            portalId: 'portal-1',
            activeMode: 'light' as const,
            tokens: {
                light: {
                    colors: {
                        primary: '#111', secondary: '#222', background: '#fff', surface: '#f8f', text: '#000',
                        muted: '#eee', mutedForeground: '#666', accent: '#333', border: '#ccc', ring: '#111',
                        destructive: '#f00', link: '#00f',
                    },
                    typography: {
                        fontFamily: 'Inter', headingFontFamily: 'Inter', fontSize: '14px', lineHeight: '1.5', headingScale: 1.2,
                    },
                    spacing: { borderRadius: '4px', borderWidth: '1px', padding: '8px' },
                    layout: { maxWidth: '1000px', sidebarWidth: '200px', headerHeight: '40px', footerHeight: '32px' },
                },
                dark: {
                    colors: {
                        primary: '#aaa', secondary: '#bbb', background: '#000', surface: '#111', text: '#fff',
                        muted: '#222', mutedForeground: '#999', accent: '#ccc', border: '#333', ring: '#aaa',
                        destructive: '#f55', link: '#55f',
                    },
                    typography: {
                        fontFamily: 'Inter', headingFontFamily: 'Inter', fontSize: '14px', lineHeight: '1.5', headingScale: 1.2,
                    },
                    spacing: { borderRadius: '4px', borderWidth: '1px', padding: '8px' },
                    layout: { maxWidth: '1000px', sidebarWidth: '200px', headerHeight: '40px', footerHeight: '32px' },
                },
            },
            customVariables: [{ id: '1', name: 'accent', lightValue: '#f00', darkValue: '#a00' }],
        };

        const doc = migrateLegacyTheme(legacy);

        expect(doc.schemaVersion).toBe(1);
        expect(doc.foundation.light.primary).toBe('#111');
        expect(doc.foundation.dark.primary).toBe('#aaa');
        expect(doc.foundation.light.headerHeight).toBe('40px');
        expect(doc.customVariables[0].name).toBe('accent');
    });
});
