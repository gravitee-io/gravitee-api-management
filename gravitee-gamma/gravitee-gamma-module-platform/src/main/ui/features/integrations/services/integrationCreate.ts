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
import type { CreateIntegrationRequest, Integration } from '../types/integration';

export async function createIntegration(environmentId: string, request: CreateIntegrationRequest): Promise<Integration> {
    const body: CreateIntegrationRequest = { name: request.name, provider: request.provider };
    if (request.description) {
        body.description = request.description;
    }
    return apimFetchJsonV2<Integration>(environmentId, '/integrations', {
        method: 'POST',
        body: JSON.stringify(body),
    });
}
