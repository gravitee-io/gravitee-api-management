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
import { createDefaultTheme } from './default-theme';
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
        expect(theme.tokens.light.colors.primary).toBeDefined();
        expect(theme.tokens.dark.colors.primary).toBeDefined();
        expect(theme.customVariables).toEqual([]);
    });

    it('should save and load a theme', async () => {
        const theme = createDefaultTheme('test-portal');
        const modified = {
            ...theme,
            tokens: {
                ...theme.tokens,
                light: {
                    ...theme.tokens.light,
                    colors: { ...theme.tokens.light.colors, primary: '#ff0000' },
                },
            },
        };

        await saveTheme(modified);
        const loaded = await getTheme('test-portal');

        expect(loaded.tokens.light.colors.primary).toBe('#ff0000');
    });

    it('should delete a theme', async () => {
        const theme = createDefaultTheme('test-portal');
        await saveTheme(theme);
        await deleteTheme('test-portal');

        const loaded = await getTheme('test-portal');
        expect(loaded.id).toBe('theme-test-portal');
        expect(loaded.tokens.light.colors.primary).toBe(createDefaultTheme('test-portal').tokens.light.colors.primary);
    });

    it('should persist custom variables', async () => {
        const theme = {
            ...createDefaultTheme('test-portal'),
            customVariables: [
                { id: 'cv-1', name: 'brand-accent', lightValue: '#00ff00', darkValue: '#00cc00' },
            ],
        };

        await saveTheme(theme);
        const loaded = await getTheme('test-portal');

        expect(loaded.customVariables).toHaveLength(1);
        expect(loaded.customVariables[0].name).toBe('brand-accent');
    });
});
