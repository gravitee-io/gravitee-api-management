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
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { IntegrationsResponse } from '../types/integration';

export async function listIntegrations(environmentId: string, params: { page: number; perPage: number }): Promise<IntegrationsResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page));
    searchParams.set('perPage', String(params.perPage));
    return apimFetchJsonV2<IntegrationsResponse>(environmentId, `/integrations?${searchParams.toString()}`);
}
