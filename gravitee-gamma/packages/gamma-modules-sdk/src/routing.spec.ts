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
import { type ModuleRouteConfig, buildModuleNavPath, buildModuleRootPath, isRouteKey, resolveModulePath } from './routing';

const SAMPLE_CONFIG: ModuleRouteConfig<'apis' | 'settings'> = {
    routeKeys: ['apis', 'settings'],
    routes: {
        apis: { path: 'apis', label: 'APIs' },
        settings: { path: 'settings', label: 'Settings' },
    },
    defaultRouteKey: 'apis',
};

describe('isRouteKey', () => {
    it('should return true for a known key', () => {
        expect(isRouteKey('apis', SAMPLE_CONFIG.routeKeys)).toBe(true);
        expect(isRouteKey('settings', SAMPLE_CONFIG.routeKeys)).toBe(true);
    });

    it('should return false for an unknown key', () => {
        expect(isRouteKey('unknown', SAMPLE_CONFIG.routeKeys)).toBe(false);
        expect(isRouteKey('', SAMPLE_CONFIG.routeKeys)).toBe(false);
    });
});

describe('resolveModulePath', () => {
    it('should return defaults for an empty pathname', () => {
        expect(resolveModulePath('/', SAMPLE_CONFIG)).toEqual({ modulePrefix: '', activeNavKey: 'apis' });
    });

    it('should resolve federated path /environments/:envHrid/:moduleId/<key>', () => {
        expect(resolveModulePath('/environments/my-env/apim/settings', SAMPLE_CONFIG)).toEqual({
            modulePrefix: 'apim',
            activeNavKey: 'settings',
        });
    });

    it('should default activeNavKey when the sub-segment is not a known key', () => {
        expect(resolveModulePath('/environments/my-env/apim/unknown-page', SAMPLE_CONFIG)).toEqual({
            modulePrefix: 'apim',
            activeNavKey: 'apis',
        });
    });

    it('should default activeNavKey when no sub-segment exists under /environments/:env/:module', () => {
        expect(resolveModulePath('/environments/my-env/apim', SAMPLE_CONFIG)).toEqual({
            modulePrefix: 'apim',
            activeNavKey: 'apis',
        });
    });

    it('should handle legacy standalone path where first segment is a route key', () => {
        expect(resolveModulePath('/settings', SAMPLE_CONFIG)).toEqual({
            modulePrefix: '',
            activeNavKey: 'settings',
        });
    });

    it('should handle standalone with module prefix', () => {
        expect(resolveModulePath('/apim/settings', SAMPLE_CONFIG)).toEqual({
            modulePrefix: 'apim',
            activeNavKey: 'settings',
        });
    });

    it('should handle deep paths preserving the active key', () => {
        expect(resolveModulePath('/environments/e1/apim/apis/some/nested', SAMPLE_CONFIG)).toEqual({
            modulePrefix: 'apim',
            activeNavKey: 'apis',
        });
    });
});

describe('buildModuleNavPath', () => {
    it('should build federated path when environment prefix is present', () => {
        expect(buildModuleNavPath('apim', 'settings', '/environments/my-env/apim/apis')).toBe('/environments/my-env/apim/settings');
    });

    it('should build standalone prefixed path when no environment prefix', () => {
        expect(buildModuleNavPath('apim', 'settings', '/apim/apis')).toBe('/apim/settings');
    });

    it('should build root-level path when no module prefix', () => {
        expect(buildModuleNavPath('', 'settings')).toBe('/settings');
    });
});

describe('buildModuleRootPath', () => {
    it('should build root path under federated environment', () => {
        expect(buildModuleRootPath('/environments/my-env/apim/settings', 'apim', 'apis')).toBe('/environments/my-env/apim/apis');
    });

    it('should build root path under standalone prefix', () => {
        expect(buildModuleRootPath('/apim/settings', 'apim', 'apis')).toBe('/apim/apis');
    });

    it('should return / when no module prefix', () => {
        expect(buildModuleRootPath('/settings', '', 'apis')).toBe('/');
    });
});
