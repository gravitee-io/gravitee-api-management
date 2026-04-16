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
import { normalizeCrudMapRecord, permissionService } from '@gravitee/gamma-modules-sdk';

import { managementApi } from '../../shared/api/api-client';

/**
 * Loads environment-scoped permissions for the current user and merges them into {@link permissionService}.
 */
export async function loadEnvironmentPermissions(envId: string): Promise<void> {
    const raw = await managementApi.get<Record<string, string[] | string>>(`/environments/${envId}/permissions`);
    const normalized = normalizeCrudMapRecord('environment', raw);
    permissionService.load('environment', normalized);
}
