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
import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type { ApplicationSubscriptionApiKeyEntity } from '../types/applicationSubscription';
import { applicationApiKeyRenewBody } from '../utils/sharedCustomApiKey';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

function applicationApiKeysPath(applicationId: string): string {
    return `/applications/${encodeURIComponent(applicationId)}/apikeys`;
}

export async function listApplicationApiKeys(environmentId: string, applicationId: string): Promise<ApplicationSubscriptionApiKeyEntity[]> {
    return apimFetchJsonV1Env<ApplicationSubscriptionApiKeyEntity[]>(environmentId, applicationApiKeysPath(applicationId));
}

export async function renewApplicationApiKey(environmentId: string, applicationId: string, customApiKey?: string): Promise<void> {
    await apimFetchJsonV1Env<void>(environmentId, `${applicationApiKeysPath(applicationId)}/_renew`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(applicationApiKeyRenewBody(customApiKey)),
    });
}

export async function revokeApplicationApiKey(environmentId: string, applicationId: string, apiKeyId: string): Promise<void> {
    await apimFetchJsonV1Env<void>(environmentId, `${applicationApiKeysPath(applicationId)}/${encodeURIComponent(apiKeyId)}`, {
        method: 'DELETE',
    });
}
