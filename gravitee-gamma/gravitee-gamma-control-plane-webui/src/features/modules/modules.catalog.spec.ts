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
import { excludeHiddenModules, getModuleLabel, orderByCatalog } from './modules.catalog';
import type { GammaModule } from './modules.types';

function moduleWithId(id: string): GammaModule {
    return { id, name: `${id} plugin`, version: '1.0.0', remoteName: id, exposedModule: 'App' };
}

describe('orderByCatalog', () => {
    it('should order known modules by product priority, whatever order the backend returns them in', () => {
        const backendOrder = ['edge', 'act', 'aim', 'portals', 'apim', 'esm', 'platform', 'authz'].map(moduleWithId);

        expect(orderByCatalog(backendOrder).map(m => m.id)).toEqual(['aim', 'apim', 'esm', 'authz', 'act', 'portals', 'edge', 'platform']);
    });

    it('should keep modules missing from the catalog, after the known ones, sorted by id', () => {
        const backendOrder = ['zeta', 'apim', 'beta'].map(moduleWithId);

        expect(orderByCatalog(backendOrder).map(m => m.id)).toEqual(['apim', 'beta', 'zeta']);
    });
});

describe('excludeHiddenModules', () => {
    it('should omit catalog products marked hidden, and keep the rest in the order they arrived', () => {
        const backendOrder = ['edge', 'act', 'aim', 'portals', 'apim', 'esm', 'platform', 'authz'].map(moduleWithId);

        expect(excludeHiddenModules(backendOrder).map(m => m.id)).toEqual(['edge', 'act', 'aim', 'apim', 'esm', 'platform', 'authz']);
    });

    it('should keep modules missing from the catalog', () => {
        const backendOrder = ['zeta', 'portals', 'apim'].map(moduleWithId);

        expect(excludeHiddenModules(backendOrder).map(m => m.id)).toEqual(['zeta', 'apim']);
    });
});

describe('getModuleLabel', () => {
    it('should return the product label of every catalog module', () => {
        expect(getModuleLabel('edge')).toBe('Edge Management');
        expect(getModuleLabel('apim')).toBe('API Management');
        expect(getModuleLabel('act')).toBe('Guardian Agent');
    });

    it('should fall back to the backend name, then to the id, for a module missing from the catalog', () => {
        expect(getModuleLabel('zeta', 'Zeta plugin')).toBe('Zeta plugin');
        expect(getModuleLabel('zeta')).toBe('zeta');
    });
});
