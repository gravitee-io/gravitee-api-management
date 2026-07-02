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

import { managementV2OrganizationApi } from '../../shared/api/api-client';

/**
 * Fetches the current organization license and pushes it to the SDK licenseService
 * singleton so federated modules can gate features via useHasFeature() / useHasPack().
 *
 * The organization is fixed for the lifetime of the control plane session, so this is
 * loaded once at bootstrap.
 */
export async function loadOrganizationLicense(): Promise<void> {
    const license = await managementV2OrganizationApi.get<License>('/license');
    licenseService.setLicense(license);
}
