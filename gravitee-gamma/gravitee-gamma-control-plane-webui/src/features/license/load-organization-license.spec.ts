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
import { licenseService } from '@gravitee/gamma-modules-sdk';
import type { License } from '@gravitee/gamma-modules-sdk/types';

import { loadOrganizationLicense } from './load-organization-license';
import { TEST_MANAGEMENT_V2_ORGANIZATION_BASE } from '../../testing/factories';
import { resetAllStores, respondWith, respondWithError, seedBootstrap } from '../../testing/helpers';

const LICENSE: License = {
    tier: 'enterprise',
    packs: ['apim-kafka'],
    features: ['apim-native-kafka-reactor'],
    scope: 'ORGANIZATION',
    isExpired: false,
};

describe('loadOrganizationLicense', () => {
    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
        licenseService.setLicense(null);
    });

    it('should fetch the organization license and push it to licenseService', async () => {
        respondWith('get', `${TEST_MANAGEMENT_V2_ORGANIZATION_BASE}/license`, LICENSE);

        await loadOrganizationLicense();

        expect(licenseService.getLicense()).toEqual(LICENSE);
        expect(licenseService.hasFeature('apim-native-kafka-reactor')).toBe(true);
        expect(licenseService.hasPack('apim-kafka')).toBe(true);
        expect(licenseService.isExpired()).toBe(false);
    });

    it('should reject and leave the license unset when the request fails', async () => {
        respondWithError('get', `${TEST_MANAGEMENT_V2_ORGANIZATION_BASE}/license`, 500);

        await expect(loadOrganizationLicense()).rejects.toThrow();
        expect(licenseService.getLicense()).toBeNull();
    });
});
