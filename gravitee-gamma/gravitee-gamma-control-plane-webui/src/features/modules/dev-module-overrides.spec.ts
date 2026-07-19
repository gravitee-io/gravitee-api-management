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
import { resolveGammaModules } from './dev-module-overrides';
import type { GammaModule } from './modules.types';

const API_MODULE: GammaModule = {
    id: 'apim',
    name: 'APIM',
    version: '1.0.0',
    remoteName: 'apim',
    exposedModule: 'App',
};

const GAMMA_BASE = 'http://gamma.test';
const ORG_ID = 'org-1';

describe('resolveGammaModules', () => {
    it('should inject portals from the local dev server when injectUnlistedDevModules is enabled', () => {
        const { modules, remotes } = resolveGammaModules([API_MODULE], {
            devEntries: {},
            gammaBaseURL: GAMMA_BASE,
            organizationId: ORG_ID,
            injectUnlistedDevModules: true,
        });

        expect(modules).toEqual([
            {
                id: 'apim',
                name: 'API Management',
                version: 'dev',
                remoteName: 'gravitee-gamma-module-apim',
                exposedModule: 'App',
            },
            {
                id: 'portals',
                name: 'Developer Portals',
                version: 'dev',
                remoteName: 'portal_gamma',
                exposedModule: 'App',
            },
        ]);
        expect(remotes).toEqual([
            { name: 'gravitee-gamma-module-apim', entry: 'http://localhost:3001/mf-manifest.json' },
            { name: 'portal_gamma', entry: 'http://localhost:4103/portal-editor/mf-manifest.json' },
        ]);
    });

    it('should not inject dev-only modules in production mode', () => {
        const { modules } = resolveGammaModules([API_MODULE], {
            devEntries: {},
            gammaBaseURL: GAMMA_BASE,
            organizationId: ORG_ID,
            injectUnlistedDevModules: false,
        });

        expect(modules).toEqual([API_MODULE]);
    });

    it('should prefer DEV_MODULE_ENTRIES manifest URL for injected modules', () => {
        const { remotes } = resolveGammaModules([], {
            devEntries: { portals: 'http://localhost:9999/mf-manifest.json' },
            gammaBaseURL: GAMMA_BASE,
            organizationId: ORG_ID,
            injectUnlistedDevModules: true,
        });

        expect(remotes).toEqual([
            { name: 'portal_gamma', entry: 'http://localhost:9999/mf-manifest.json' },
            { name: 'gravitee-gamma-module-apim', entry: 'http://localhost:3001/mf-manifest.json' },
        ]);
    });

    it('should not duplicate modules already returned by the API', () => {
        const apiPortals: GammaModule = {
            id: 'portals',
            name: 'Developer Portals',
            version: '1.0.0',
            remoteName: 'portal_gamma',
            exposedModule: 'App',
        };

        const { modules, remotes } = resolveGammaModules([apiPortals], {
            devEntries: { portals: 'http://localhost:4103/portal-editor/mf-manifest.json' },
            gammaBaseURL: GAMMA_BASE,
            organizationId: ORG_ID,
            injectUnlistedDevModules: true,
        });

        expect(modules).toEqual([
            {
                id: 'portals',
                name: 'Developer Portals',
                version: 'dev',
                remoteName: 'portal_gamma',
                exposedModule: 'App',
            },
            {
                id: 'apim',
                name: 'API Management',
                version: 'dev',
                remoteName: 'gravitee-gamma-module-apim',
                exposedModule: 'App',
            },
        ]);
        expect(remotes).toEqual([
            { name: 'portal_gamma', entry: 'http://localhost:4103/portal-editor/mf-manifest.json' },
            { name: 'gravitee-gamma-module-apim', entry: 'http://localhost:3001/mf-manifest.json' },
        ]);
    });

    it('should replace backend stub portals module with local portal-gamma dev remote in development', () => {
        const backendStubPortals: GammaModule = {
            id: 'portals',
            name: 'Developer Portals',
            version: '4.13.0-SNAPSHOT',
            remoteName: 'gravitee_gamma_module_portals',
            exposedModule: 'App',
        };

        const { modules, remotes } = resolveGammaModules([API_MODULE, backendStubPortals], {
            devEntries: {},
            gammaBaseURL: GAMMA_BASE,
            organizationId: ORG_ID,
            injectUnlistedDevModules: true,
        });

        expect(modules).toEqual([
            {
                id: 'apim',
                name: 'API Management',
                version: 'dev',
                remoteName: 'gravitee-gamma-module-apim',
                exposedModule: 'App',
            },
            {
                id: 'portals',
                name: 'Developer Portals',
                version: 'dev',
                remoteName: 'portal_gamma',
                exposedModule: 'App',
            },
        ]);
        expect(remotes).toEqual([
            { name: 'gravitee-gamma-module-apim', entry: 'http://localhost:3001/mf-manifest.json' },
            { name: 'portal_gamma', entry: 'http://localhost:4103/portal-editor/mf-manifest.json' },
        ]);
    });
});
