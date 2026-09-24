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
import type { ApiListItem } from '../types';
import { isFederatedApiListItem } from './federatedApi';

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
