/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { type AmConfig, loadAmConfig, moduleBaseUrl, saveAmConfig } from './amConfig';

describe('amConfig', () => {
    beforeEach(() => localStorage.clear());

    describe('load/save round-trip', () => {
        it('returns an empty config when nothing is stored', () => {
            expect(loadAmConfig()).toEqual({ organizationId: '', environmentId: '', domainId: '', graviteeEnvironmentId: '' });
        });

        it('round-trips a saved config', () => {
            const cfg: AmConfig = {
                organizationId: 'DEFAULT',
                environmentId: 'env-1',
                domainId: 'dom-1',
                graviteeEnvironmentId: 'gv-env-1',
            };
            saveAmConfig(cfg);
            expect(loadAmConfig()).toEqual(cfg);
        });

        it('fills missing keys with empty defaults', () => {
            localStorage.setItem('platform.am-config.v2', JSON.stringify({ organizationId: 'DEFAULT' }));
            expect(loadAmConfig()).toEqual({ organizationId: 'DEFAULT', environmentId: '', domainId: '', graviteeEnvironmentId: '' });
        });

        it('falls back to empty config on malformed JSON', () => {
            localStorage.setItem('platform.am-config.v2', '{not json');
            expect(loadAmConfig()).toEqual({ organizationId: '', environmentId: '', domainId: '', graviteeEnvironmentId: '' });
        });
    });

    describe('moduleBaseUrl', () => {
        it('builds the env-scoped module path', () => {
            expect(moduleBaseUrl({ organizationId: 'DEFAULT', environmentId: '', domainId: '', graviteeEnvironmentId: 'gv-env-1' })).toBe(
                '/organizations/DEFAULT/environments/gv-env-1/modules/platform/am',
            );
        });

        it('encodes the organization and environment ids', () => {
            expect(moduleBaseUrl({ organizationId: 'acme/org', environmentId: '', domainId: '', graviteeEnvironmentId: 'gv/env' })).toBe(
                '/organizations/acme%2Forg/environments/gv%2Fenv/modules/platform/am',
            );
        });
    });
});
