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
import {
    resolvePortalSettingsSectionLabel,
    truncatePortalBreadcrumbLabel,
} from './portalDetailNavigation';

describe('portalDetailNavigation', () => {
    it('should resolve section labels from settings pathnames', () => {
        expect(resolvePortalSettingsSectionLabel('/portals/p1/settings/general')).toBe('General');
        expect(resolvePortalSettingsSectionLabel('/environments/default/portals/portals/p1/settings/categories')).toBe(
            'Categories',
        );
        expect(
            resolvePortalSettingsSectionLabel('/portals/p1/settings/subscription-forms/form-1'),
        ).toBe('Subscription Forms');
        expect(resolvePortalSettingsSectionLabel('/portals/p1/settings/unknown-section')).toBe('unknown-section');
        expect(resolvePortalSettingsSectionLabel('/portals/p1')).toBe('General');
    });

    it('should truncate long breadcrumb labels', () => {
        expect(truncatePortalBreadcrumbLabel('Short')).toBe('Short');
        expect(truncatePortalBreadcrumbLabel('A'.repeat(50))).toBe(`${'A'.repeat(39)}…`);
    });
});
