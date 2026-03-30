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
import { GammaModuleResponse, parseModule } from './gamma-module';

describe('parseModule', () => {
    const baseResponse: GammaModuleResponse = {
        id: 'mod-1',
        name: 'Delta',
        version: '1.0.0',
        mfManifest: {
            name: 'delta',
            exposes: [{ name: './App' }],
        },
    };

    it('should extract the exposed module name from mfManifest.exposes', () => {
        const result = parseModule(baseResponse);

        expect(result.exposedModule).toBe('App');
    });

    it('should strip the ./ prefix from the exposed module name', () => {
        const response: GammaModuleResponse = {
            ...baseResponse,
            mfManifest: { name: 'delta', exposes: [{ name: './MyComponent' }] },
        };

        const result = parseModule(response);

        expect(result.exposedModule).toBe('MyComponent');
    });

    it('should fallback to "Module" when exposes is missing', () => {
        const response: GammaModuleResponse = {
            ...baseResponse,
            mfManifest: { name: 'delta' },
        };

        const result = parseModule(response);

        expect(result.exposedModule).toBe('Module');
    });

    it('should fallback to "Module" when exposes is empty', () => {
        const response: GammaModuleResponse = {
            ...baseResponse,
            mfManifest: { name: 'delta', exposes: [] },
        };

        const result = parseModule(response);

        expect(result.exposedModule).toBe('Module');
    });

    it('should use the first exposed module when multiple are present', () => {
        const response: GammaModuleResponse = {
            ...baseResponse,
            mfManifest: { name: 'delta', exposes: [{ name: './Primary' }, { name: './Secondary' }] },
        };

        const result = parseModule(response);

        expect(result.exposedModule).toBe('Primary');
    });

    it('should map all other fields correctly', () => {
        const result = parseModule(baseResponse);

        expect(result.id).toBe('mod-1');
        expect(result.name).toBe('Delta');
        expect(result.version).toBe('1.0.0');
        expect(result.remoteName).toBe('delta');
    });
});
