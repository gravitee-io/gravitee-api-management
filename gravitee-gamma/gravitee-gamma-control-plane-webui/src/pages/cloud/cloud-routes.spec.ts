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
import { cloudSettingsNavPath } from './cloud-settings-navigation';
import { resolveCloudRoute, cloudNavPath } from './cloud-routes';

describe('cloud-routes', () => {
    it('resolves dashboard route', () => {
        const result = resolveCloudRoute('/environments/default/cloud/dashboard', 'default');
        expect(result.activeNavKey).toBe('dashboard');
        expect(result.breadcrumbSegments).toEqual([
            { label: 'Cloud', to: cloudNavPath('dashboard', 'default') },
            { label: 'Dashboard' },
        ]);
    });

    it('resolves settings general route', () => {
        const result = resolveCloudRoute('/environments/default/cloud/settings/general', 'default');
        expect(result.activeNavKey).toBe('settings');
        expect(result.breadcrumbSegments).toEqual([
            { label: 'Cloud', to: cloudNavPath('dashboard', 'default') },
            { label: 'Settings', to: cloudSettingsNavPath('default', 'general') },
            { label: 'General' },
        ]);
    });

    it('resolves settings members route', () => {
        const result = resolveCloudRoute('/environments/default/cloud/settings/members', 'default');
        expect(result.breadcrumbSegments[2]?.label).toBe('Members');
    });
});
