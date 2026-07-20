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
    getActivePortalsNavKey,
    resolvePortalsHomePath,
    resolvePortalsRoutePath,
} from './navigation';

describe('portals navigation helpers', () => {
    const embeddedContext = {
        embeddedInConsole: true,
        pathname: '/environments/default/portals/tenants',
    };

    const embeddedPortalTenantsContext = {
        embeddedInConsole: true,
        pathname: '/environments/default/portals/portals/p1/tenants',
    };

    const standaloneContext = {
        embeddedInConsole: false,
        pathname: '/tenants',
    };

    it('should keep absolute paths in standalone mode', () => {
        expect(resolvePortalsRoutePath('portals/p1/tenants/t1', standaloneContext)).toBe('/portals/p1/tenants/t1');
        expect(resolvePortalsRoutePath('portals/p1/settings', standaloneContext)).toBe('/portals/p1/settings');
        expect(resolvePortalsRoutePath('portals/p1/settings/general', standaloneContext)).toBe(
            '/portals/p1/settings/general',
        );
        expect(resolvePortalsHomePath(standaloneContext)).toBe('/');
    });

    it('should build stable console paths regardless of current depth', () => {
        expect(resolvePortalsRoutePath('portals/p1/tenants/t1', embeddedContext)).toBe(
            '/environments/default/portals/portals/p1/tenants/t1',
        );
        expect(resolvePortalsRoutePath('portals/p1/tenants/t1', embeddedPortalTenantsContext)).toBe(
            '/environments/default/portals/portals/p1/tenants/t1',
        );
        expect(resolvePortalsRoutePath('portals/p1/settings/general', embeddedContext)).toBe(
            '/environments/default/portals/portals/p1/settings/general',
        );
        expect(resolvePortalsRoutePath('tenants', embeddedContext)).toBe('/environments/default/portals/tenants');
        expect(resolvePortalsHomePath(embeddedContext)).toBe('/environments/default/portals/');
        expect(resolvePortalsHomePath(embeddedPortalTenantsContext)).toBe('/environments/default/portals/');
    });

    it('should resolve active sidebar keys from host URLs', () => {
        expect(getActivePortalsNavKey('/environments/default/portals/')).toBe('portals');
        expect(getActivePortalsNavKey('/environments/default/portals/tenants')).toBe('tenants');
        expect(getActivePortalsNavKey('/environments/default/portals/portals/p1/tenants/t1')).toBe('tenants');
    });
});
