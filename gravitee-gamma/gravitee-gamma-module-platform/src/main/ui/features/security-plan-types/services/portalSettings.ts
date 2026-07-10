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

export interface PlanSecurityEnabled {
    enabled?: boolean;
}

export interface PlanSecuritySettings {
    apikey?: PlanSecurityEnabled;
    customApiKey?: PlanSecurityEnabled;
    customApiKeyReuse?: PlanSecurityEnabled;
    sharedApiKey?: PlanSecurityEnabled;
    oauth2?: PlanSecurityEnabled;
    keyless?: PlanSecurityEnabled;
    jwt?: PlanSecurityEnabled;
    push?: PlanSecurityEnabled;
    mtls?: PlanSecurityEnabled;
}

export interface PortalSettingsMetadata {
    readonly?: string[];
}

export interface PortalSettings {
    metadata?: PortalSettingsMetadata;
    plan?: {
        security?: PlanSecuritySettings;
        [key: string]: unknown;
    };
    [key: string]: unknown;
}

export async function getPortalSettings(environmentId: string): Promise<PortalSettings> {
    return apimFetchJsonV1Env<PortalSettings>(environmentId, '/settings');
}

export async function savePortalSettings(environmentId: string, settings: PortalSettings): Promise<PortalSettings> {
    return apimFetchJsonV1Env<PortalSettings>(environmentId, '/settings', {
        method: 'POST',
        body: JSON.stringify(settings),
    });
}
