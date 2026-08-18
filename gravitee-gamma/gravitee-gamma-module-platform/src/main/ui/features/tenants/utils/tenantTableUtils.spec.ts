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

import { filterTenants } from './tenantTableUtils';
import type { Tenant } from '../types/tenant';

const STUB_TENANTS: Tenant[] = [
    { id: 't-1', key: 'us-east', name: 'US East', description: 'Virginia gateway cluster' },
    { id: 't-2', key: 'eu-west', name: 'EU West', description: 'Frankfurt gateway cluster' },
];

describe('filterTenants', () => {
    it('filters by key, name, or description', () => {
        expect(filterTenants(STUB_TENANTS, 'east').map(row => row.key)).toEqual(['us-east']);
        expect(filterTenants(STUB_TENANTS, 'frankfurt').map(row => row.key)).toEqual(['eu-west']);
        expect(filterTenants(STUB_TENANTS, '  ').map(row => row.key)).toEqual(['us-east', 'eu-west']);
    });
});
