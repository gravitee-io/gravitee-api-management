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
import { gammaFetchJson, resolveOrganizationId } from '../../../shared/api/apimClient';
import type {
    CreateCredentialPayload,
    Credential,
    CredentialConsumer,
    CredentialsPagedResponse,
    RotateCredentialPayload,
    UpdateCredentialPayload,
} from '../types/credential';

async function credentialsPath(environmentId: string): Promise<string> {
    const organizationId = await resolveOrganizationId();
    return `/organizations/${encodeURIComponent(organizationId)}/environments/${encodeURIComponent(environmentId)}/credentials`;
}

async function credentialPath(environmentId: string, credentialId: string): Promise<string> {
    return `${await credentialsPath(environmentId)}/${encodeURIComponent(credentialId)}`;
}

export async function listCredentials(
    environmentId: string,
    params: { query: string; page: number; perPage: number },
): Promise<CredentialsPagedResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page));
    searchParams.set('perPage', String(params.perPage));
    const query = params.query.trim();
    if (query) {
        searchParams.set('q', query);
    }
    return gammaFetchJson<CredentialsPagedResponse>(`${await credentialsPath(environmentId)}?${searchParams.toString()}`);
}

export async function getCredential(environmentId: string, credentialId: string): Promise<Credential> {
    return gammaFetchJson<Credential>(await credentialPath(environmentId, credentialId));
}

export async function createCredential(environmentId: string, payload: CreateCredentialPayload): Promise<Credential> {
    return gammaFetchJson<Credential>(await credentialsPath(environmentId), { method: 'POST', body: JSON.stringify(payload) });
}

export async function updateCredential(environmentId: string, credentialId: string, payload: UpdateCredentialPayload): Promise<Credential> {
    return gammaFetchJson<Credential>(await credentialPath(environmentId, credentialId), { method: 'PUT', body: JSON.stringify(payload) });
}

export async function rotateCredential(environmentId: string, credentialId: string, payload: RotateCredentialPayload): Promise<Credential> {
    return gammaFetchJson<Credential>(`${await credentialPath(environmentId, credentialId)}/_rotate`, {
        method: 'POST',
        body: JSON.stringify(payload),
    });
}

export async function deleteCredential(environmentId: string, credentialId: string): Promise<void> {
    return gammaFetchJson<void>(await credentialPath(environmentId, credentialId), { method: 'DELETE' });
}

export async function listCredentialConsumers(environmentId: string, credentialId: string): Promise<CredentialConsumer[]> {
    const response = await gammaFetchJson<{ data?: CredentialConsumer[] }>(
        `${await credentialPath(environmentId, credentialId)}/consumers`,
    );
    return response.data ?? [];
}
