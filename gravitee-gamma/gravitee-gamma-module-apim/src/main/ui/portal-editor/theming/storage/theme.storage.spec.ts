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
import { clearPortalsDatabase } from '../../portals/storage/portals.storage.test-utils';
import { createDefaultThemeDocument } from './migrate-legacy-theme';
import { deleteTheme, getTheme, saveTheme } from './theme.storage';

describe('theme.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should return default theme when none exists', async () => {
        const theme = await getTheme('test-portal');

        expect(theme.portalId).toBe('test-portal');
        expect(theme.schemaVersion).toBe(1);
        expect(theme.foundation.light).toEqual({});
        expect(theme.customVariables).toEqual([]);
        expect(theme.instanceOverrides).toEqual({});
    });

    it('should save and load a theme', async () => {
        const theme = createDefaultThemeDocument('test-portal');
        const modified = {
            ...theme,
            foundation: {
                ...theme.foundation,
                light: { primary: '#ff0000' },
            },
        };

        await saveTheme(modified);
        const loaded = await getTheme('test-portal');

        expect(loaded.foundation.light.primary).toBe('#ff0000');
    });

    it('should delete a theme', async () => {
        const theme = createDefaultThemeDocument('test-portal');
        await saveTheme(theme);
        await deleteTheme('test-portal');

        const loaded = await getTheme('test-portal');
        expect(loaded.id).toBe('theme-test-portal');
    });

    it('should persist custom variables', async () => {
        const theme = {
            ...createDefaultThemeDocument('test-portal'),
            customVariables: [
                { name: 'brand-accent', lightValue: '#00ff00', darkValue: '#00cc00' },
            ],
        };

        await saveTheme(theme);
        const loaded = await getTheme('test-portal');

        expect(loaded.customVariables).toHaveLength(1);
        expect(loaded.customVariables[0].name).toBe('brand-accent');
    });

    it('should migrate legacy theme on load', async () => {
        const legacy = {
            id: 'theme-test-portal',
            portalId: 'test-portal',
            activeMode: 'system' as const,
            tokens: {
                light: {
                    colors: { primary: '#111111', secondary: '', background: '', surface: '', text: '', muted: '', mutedForeground: '', accent: '', border: '', ring: '', destructive: '', link: '' },
                    typography: { fontFamily: 'Inter', headingFontFamily: 'Inter', fontSize: '14px', lineHeight: '1.6', headingScale: 1.25 },
                    spacing: { borderRadius: '6px', borderWidth: '1px', padding: '16px' },
                    layout: { maxWidth: '1200px', sidebarWidth: '240px', headerHeight: '48px', footerHeight: '40px' },
                },
                dark: {
                    colors: { primary: '#222222', secondary: '', background: '', surface: '', text: '', muted: '', mutedForeground: '', accent: '', border: '', ring: '', destructive: '', link: '' },
                    typography: { fontFamily: 'Inter', headingFontFamily: 'Inter', fontSize: '14px', lineHeight: '1.6', headingScale: 1.25 },
                    spacing: { borderRadius: '6px', borderWidth: '1px', padding: '16px' },
                    layout: { maxWidth: '1200px', sidebarWidth: '240px', headerHeight: '48px', footerHeight: '40px' },
                },
            },
            customVariables: [],
        };

        await saveTheme(legacy as never);
        const loaded = await getTheme('test-portal');

        expect(loaded.schemaVersion).toBe(1);
        expect(loaded.foundation.light.primary).toBe('#111111');
        expect(loaded.foundation.dark.primary).toBe('#222222');
    });
});
