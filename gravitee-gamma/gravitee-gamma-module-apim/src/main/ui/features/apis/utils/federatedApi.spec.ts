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
import type { ApiDetailDto, ApiListItem } from '../types';
import { getSourceIntegration, isFederatedApiListItem, type SourceIntegration } from './federatedApi';

describe('getSourceIntegration', () => {
    const integrationApi = (overrides: Partial<ApiDetailDto>): ApiDetailDto => ({ id: 'api-1', name: 'API', ...overrides });

    it.each<[string, Partial<ApiDetailDto>, SourceIntegration]>([
        [
            'the integration id and name of a federated API sourced from an integration',
            {
                definitionVersion: 'FEDERATED',
                originContext: { origin: 'INTEGRATION', integrationId: 'int-1', integrationName: 'My Solace env' },
            },
            { integrationId: 'int-1', integrationName: 'My Solace env' },
        ],
        [
            'the integration id without a name when the name is unavailable',
            { definitionVersion: 'FEDERATED', originContext: { origin: 'INTEGRATION', integrationId: 'int-1' } },
            { integrationId: 'int-1', integrationName: undefined },
        ],
        [
            'the integration of a federated agent API sourced from an integration',
            {
                definitionVersion: 'FEDERATED_AGENT',
                originContext: { origin: 'INTEGRATION', integrationId: 'int-1', integrationName: 'My env' },
            },
            { integrationId: 'int-1', integrationName: 'My env' },
        ],
    ])('returns %s', (_label, overrides, expected) => {
        expect(getSourceIntegration(integrationApi(overrides))).toEqual(expected);
    });

    it.each<[string, Partial<ApiDetailDto>]>([
        ['an API created in the platform', { definitionVersion: 'V4', originContext: { origin: 'MANAGEMENT' } }],
        ['an API discovered through Kubernetes', { definitionVersion: 'V4', originContext: { origin: 'KUBERNETES' } }],
        ['a federated API with no origin context', { definitionVersion: 'FEDERATED' }],
        ['a federated API whose origin is not an integration', { definitionVersion: 'FEDERATED', originContext: { origin: 'MANAGEMENT' } }],
        ['a federated API missing its integration id', { definitionVersion: 'FEDERATED', originContext: { origin: 'INTEGRATION' } }],
        [
            'a federated API with an empty integration id',
            { definitionVersion: 'FEDERATED', originContext: { origin: 'INTEGRATION', integrationId: '' } },
        ],
        [
            'a non-federated API that carries an integration origin',
            { definitionVersion: 'V4', originContext: { origin: 'INTEGRATION', integrationId: 'int-1' } },
        ],
    ])('returns null for %s', (_label, overrides) => {
        expect(getSourceIntegration(integrationApi(overrides))).toBeNull();
    });

    it('returns null when there is no API', () => {
        expect(getSourceIntegration(null)).toBeNull();
        expect(getSourceIntegration(undefined)).toBeNull();
    });
});

describe('isFederatedApiListItem', () => {
    let warn: jest.SpyInstance;

    beforeEach(() => {
        warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
    });

    afterEach(() => {
        warn.mockRestore();
    });

    it.each<[ApiListItem['definitionVersion'], boolean]>([
        ['FEDERATED', true],
        ['FEDERATED_AGENT', false],
        ['V4', false],
        ['V2', false],
    ])('classifies a %s row as federated=%s without warning', (definitionVersion, expected) => {
        const row = { definitionVersion } as ApiListItem;

        expect(isFederatedApiListItem(row)).toBe(expected);
        expect(warn).not.toHaveBeenCalled();
    });

    it('treats a row with an unrecognized definition version as non-federated and warns', () => {
        const row = { definitionVersion: 'V1' } as unknown as ApiListItem;

        expect(isFederatedApiListItem(row)).toBe(false);
        expect(warn).toHaveBeenCalledTimes(1);
        expect(warn).toHaveBeenCalledWith(expect.any(String), 'V1');
    });
});
