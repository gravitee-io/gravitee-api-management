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

import {
    applyReadonlyPrimaryOwnerModes,
    buildPrimaryOwnerModeFormState,
    getPrimaryOwnerModeReadonly,
    isPrimaryOwnerMode,
    isPrimaryOwnerModeDirty,
    parsePrimaryOwnerMode,
    primaryOwnerModeOptions,
} from './primaryOwnerMode';
import type { PortalSettings } from '../../security-plan-types/services/portalSettings';

describe('primaryOwnerMode', () => {
    it('parses USER, GROUP, and HYBRID case-insensitively', () => {
        expect(parsePrimaryOwnerMode('user')).toBe('USER');
        expect(parsePrimaryOwnerMode(' Group ')).toBe('GROUP');
        expect(parsePrimaryOwnerMode('HYBRID')).toBe('HYBRID');
    });

    it('accepts only HYBRID, USER, and GROUP as live radio values', () => {
        expect(isPrimaryOwnerMode('USER')).toBe(true);
        expect(isPrimaryOwnerMode('')).toBe(false);
        expect(isPrimaryOwnerMode('TEAM')).toBe(false);
    });

    it('defaults missing or unknown values to HYBRID', () => {
        expect(parsePrimaryOwnerMode(undefined)).toBe('HYBRID');
        expect(parsePrimaryOwnerMode('')).toBe('HYBRID');
        expect(parsePrimaryOwnerMode('TEAM')).toBe('HYBRID');
    });

    it('reads both resource modes from portal settings', () => {
        const settings: PortalSettings = {
            api: { primaryOwnerMode: 'USER' },
            apiProduct: { primaryOwnerMode: 'group' },
        };

        expect(buildPrimaryOwnerModeFormState(settings)).toEqual({ api: 'USER', apiProduct: 'GROUP' });
    });

    it('defaults both resources to HYBRID when settings are missing', () => {
        expect(buildPrimaryOwnerModeFormState(undefined)).toEqual({ api: 'HYBRID', apiProduct: 'HYBRID' });
        expect(buildPrimaryOwnerModeFormState({})).toEqual({ api: 'HYBRID', apiProduct: 'HYBRID' });
    });

    it('marks system-provided fields from metadata.readonly', () => {
        const settings: PortalSettings = {
            metadata: { readonly: ['api.primaryOwnerMode'] },
        };

        expect(getPrimaryOwnerModeReadonly(settings)).toEqual({ api: true, apiProduct: false });
    });

    it('keeps saved values for readonly fields when applying a local draft', () => {
        expect(
            applyReadonlyPrimaryOwnerModes(
                { api: 'GROUP', apiProduct: 'USER' },
                { api: 'HYBRID', apiProduct: 'HYBRID' },
                { api: true, apiProduct: false },
            ),
        ).toEqual({ api: 'HYBRID', apiProduct: 'USER' });
    });

    it('ignores readonly fields when computing dirty state', () => {
        const saved = { api: 'HYBRID' as const, apiProduct: 'HYBRID' as const };
        expect(isPrimaryOwnerModeDirty({ api: 'USER', apiProduct: 'HYBRID' }, saved, { api: true, apiProduct: false })).toBe(false);
        expect(isPrimaryOwnerModeDirty({ api: 'HYBRID', apiProduct: 'USER' }, saved, { api: true, apiProduct: false })).toBe(true);
    });

    it('explains each mode for APIs and API Products', () => {
        const api = primaryOwnerModeOptions('API');
        const product = primaryOwnerModeOptions('API Product');

        expect(api.map(option => option.value)).toEqual(['HYBRID', 'USER', 'GROUP']);
        expect(api[0]?.summary).toContain('an API primary owner can be either a user or a group');
        expect(api[1]?.implication).toContain('Groups cannot take that role');
        expect(api[2]?.implication).toContain('Ownership stays with the team');
        expect(product[0]?.summary).toContain('an API Product primary owner can be either a user or a group');
        expect(product[1]?.summary).toContain('an API Product primary owner can only be a user');
    });
});
