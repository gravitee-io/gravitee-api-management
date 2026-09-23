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
import { attachDevManifests, parseDevModuleEntries, withLocalDevModuleFallbacks } from './dev-module-entries';
import type { GammaModuleResponse } from './modules.types';

describe('parseDevModuleEntries', () => {
    it('parses comma-separated id=url pairs', () => {
        expect(parseDevModuleEntries('apim=http://localhost:3001/mf-manifest.json,platform=http://localhost:3002/mf-manifest.json')).toEqual({
            apim: 'http://localhost:3001/mf-manifest.json',
            platform: 'http://localhost:3002/mf-manifest.json',
        });
    });

    it('returns an empty map for a blank string', () => {
        expect(parseDevModuleEntries('')).toEqual({});
    });
});

describe('withLocalDevModuleFallbacks', () => {
    it('leaves an empty map unchanged so production builds do not fetch localhost', () => {
        expect(withLocalDevModuleFallbacks({})).toEqual({});
    });

    it('adds Platform when only APIM is overridden', () => {
        expect(withLocalDevModuleFallbacks({ apim: 'http://localhost:3001/mf-manifest.json' })).toEqual({
            apim: 'http://localhost:3001/mf-manifest.json',
            platform: 'http://localhost:3002/mf-manifest.json',
        });
    });

    it('does not replace an explicit Platform override', () => {
        expect(
            withLocalDevModuleFallbacks({
                apim: 'http://localhost:3001/mf-manifest.json',
                platform: 'http://127.0.0.1:4002/mf-manifest.json',
            }),
        ).toEqual({
            apim: 'http://localhost:3001/mf-manifest.json',
            platform: 'http://127.0.0.1:4002/mf-manifest.json',
        });
    });
});

describe('attachDevManifests', () => {
    const apim: GammaModuleResponse = { id: 'apim', name: 'APIM', version: '1.0.0' };
    const localManifest = { name: 'gravitee_gamma_module_apim', exposes: [{ name: './App' }] };

    it('fetches a local manifest when the backend module has no UI', async () => {
        const fetchImpl = jest.fn().mockResolvedValue({
            ok: true,
            json: async () => localManifest,
        });

        const result = await attachDevManifests([apim], { apim: 'http://localhost:3001/mf-manifest.json' }, fetchImpl as unknown as typeof fetch);

        expect(fetchImpl).toHaveBeenCalledWith('http://localhost:3001/mf-manifest.json', expect.objectContaining({ credentials: 'omit' }));
        expect(result[0]?.mfManifest).toEqual(localManifest);
    });

    it('does not fetch when the backend already shipped a manifest', async () => {
        const fetchImpl = jest.fn();
        const withUi: GammaModuleResponse = { ...apim, mfManifest: { name: 'apim', exposes: [{ name: './Module' }] } };

        const result = await attachDevManifests([withUi], { apim: 'http://localhost:3001/mf-manifest.json' }, fetchImpl as unknown as typeof fetch);

        expect(fetchImpl).not.toHaveBeenCalled();
        expect(result[0]).toBe(withUi);
    });

    it('leaves backend-only modules unchanged when there is no override', async () => {
        const result = await attachDevManifests([apim], {});
        expect(result).toEqual([apim]);
    });
});
