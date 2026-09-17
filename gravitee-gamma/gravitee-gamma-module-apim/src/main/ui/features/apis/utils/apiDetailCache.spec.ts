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

import { mergeApiDetailCache } from './apiDetailCache';
import type { ApiDetailDto } from '../types';

describe('mergeApiDetailCache', () => {
    it('returns the updated API when there is no previous cache entry', () => {
        const updated = { id: 'api-1', name: 'New' } as ApiDetailDto;
        expect(mergeApiDetailCache(undefined, updated)).toBe(updated);
    });

    it('preserves deploymentState when the write response omits it', () => {
        const prev = { id: 'api-1', name: 'Old', deploymentState: 'NEED_REDEPLOY' } as ApiDetailDto;
        const updated = { id: 'api-1', name: 'New', responseTemplates: {} } as ApiDetailDto;
        expect(mergeApiDetailCache(prev, updated)).toEqual({
            id: 'api-1',
            name: 'New',
            responseTemplates: {},
            deploymentState: 'NEED_REDEPLOY',
        });
    });

    it('keeps responseTemplates from the write response (does not fall back to prev)', () => {
        const prev = {
            id: 'api-1',
            responseTemplates: {
                DEFAULT: { '*/*': { statusCode: 400 } },
                KEEP: { '*/*': { statusCode: 401 } },
            },
            deploymentState: 'DEPLOYED',
        } as ApiDetailDto;
        const updated = {
            id: 'api-1',
            responseTemplates: { KEEP: { '*/*': { statusCode: 401 } } },
        } as ApiDetailDto;
        expect(mergeApiDetailCache(prev, updated).responseTemplates).toEqual(updated.responseTemplates);
    });

    it('applies an empty responseTemplates map from the write response', () => {
        const prev = {
            id: 'api-1',
            responseTemplates: { DEFAULT: { '*/*': { statusCode: 400 } } },
        } as ApiDetailDto;
        const updated = { id: 'api-1', responseTemplates: {} } as ApiDetailDto;
        expect(mergeApiDetailCache(prev, updated).responseTemplates).toEqual({});
    });
});
