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
import type { ApplicationListItem } from '../types/application';
import type {
    ClientCertificate,
    ClientCertificatePagedResult,
    CreateClientCertificate,
    UpdateClientCertificate,
    ValidateCertificateResponse,
} from '../types/applicationCertificate';
import type { ApplicationTypeConfig } from '../types/applicationCreate';

export async function getApplication(environmentId: string, applicationId: string): Promise<ApplicationListItem> {
    return apimFetchJsonV1Env<ApplicationListItem>(environmentId, `/applications/${applicationId}`);
}

export async function getApplicationTypeConfiguration(environmentId: string, applicationId: string): Promise<ApplicationTypeConfig> {
    return apimFetchJsonV1Env<ApplicationTypeConfig>(environmentId, `/applications/${applicationId}/configuration`);
}

export interface UpdateApplicationPayload {
    name: string;
    description: string;
    domain?: string;
    groups?: string[];
    settings?: ApplicationListItem['settings'];
    picture?: string | null;
    background?: string | null;
    disable_membership_notifications?: boolean;
    api_key_mode?: ApplicationListItem['api_key_mode'];
}

export async function updateApplication(
    environmentId: string,
    application: ApplicationListItem,
    payload: UpdateApplicationPayload,
): Promise<ApplicationListItem> {
    const body: UpdateApplicationPayload = {
        name: payload.name,
        description: payload.description,
        domain: payload.domain,
        groups: payload.groups ?? application.groups,
        settings: payload.settings,
        disable_membership_notifications: payload.disable_membership_notifications ?? application.disable_membership_notifications,
        api_key_mode: application.api_key_mode,
    };
    if (payload.picture !== undefined) {
        body.picture = payload.picture;
    }
    if (payload.background !== undefined) {
        body.background = payload.background;
    }
    return apimFetchJsonV1Env<ApplicationListItem>(environmentId, `/applications/${application.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
}

/** Trailing slash matches console `ApplicationService.delete` and management API usage. */
export async function deleteApplication(environmentId: string, applicationId: string): Promise<ApplicationListItem> {
    return apimFetchJsonV1Env<ApplicationListItem>(environmentId, `/applications/${applicationId}/`, {
        method: 'DELETE',
    });
}

export async function listApplicationCertificates(
    environmentId: string,
    applicationId: string,
    page = 1,
    size = 100,
): Promise<ClientCertificatePagedResult> {
    return apimFetchJsonV1Env<ClientCertificatePagedResult>(
        environmentId,
        `/applications/${applicationId}/certificates?page=${page}&size=${size}`,
    );
}

export async function createApplicationCertificate(
    environmentId: string,
    applicationId: string,
    certificate: CreateClientCertificate,
): Promise<ClientCertificate> {
    return apimFetchJsonV1Env<ClientCertificate>(environmentId, `/applications/${applicationId}/certificates`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(certificate),
    });
}

export async function updateApplicationCertificate(
    environmentId: string,
    applicationId: string,
    certificateId: string,
    update: UpdateClientCertificate,
): Promise<ClientCertificate> {
    return apimFetchJsonV1Env<ClientCertificate>(environmentId, `/applications/${applicationId}/certificates/${certificateId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(update),
    });
}

export async function validateApplicationCertificate(
    environmentId: string,
    applicationId: string,
    certificate: string,
): Promise<ValidateCertificateResponse> {
    return apimFetchJsonV1Env<ValidateCertificateResponse>(environmentId, `/applications/${applicationId}/certificates/_validate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ certificate }),
    });
}
