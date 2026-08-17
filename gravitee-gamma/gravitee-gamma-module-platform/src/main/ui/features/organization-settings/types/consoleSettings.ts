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

/** GET returns this sentinel for `email.password`; POST it again to leave the secret unchanged. */
export const PASSWORD_SENTINEL = '********';

export interface DisableableFeature {
    enabled?: boolean;
}

export interface BrandedSender {
    domains: string[];
    from: string;
    subject: string;
}

export interface ConsoleSettingsEmail extends DisableableFeature {
    host?: string;
    port?: number;
    username?: string;
    password?: string;
    protocol?: string;
    subject?: string;
    from?: string;
    brandedSenders?: BrandedSender[];
    properties?: {
        auth?: boolean;
        startTlsEnable?: boolean;
        sslTrust?: string;
    };
}

export interface ConsoleSettingsCors {
    allowOrigin?: string[];
    allowHeaders?: string[];
    allowMethods?: string[];
    exposedHeaders?: string[];
    maxAge?: number;
}

export interface ConsoleSettingsScheduler {
    tasks?: number;
    notifications?: number;
}

export interface ConsoleSettingsManagement {
    support?: DisableableFeature;
    title?: string;
    url?: string;
    userCreation?: DisableableFeature;
    automaticValidation?: DisableableFeature;
}

export interface ConsoleSettingsMetadata {
    readonly?: string[];
}

/**
 * Org console settings from GET/POST `/organizations/{orgId}/settings`.
 * Extra backend fields are preserved via index signature so a section save cannot drop them.
 */
export interface ConsoleSettings {
    email?: ConsoleSettingsEmail;
    metadata?: ConsoleSettingsMetadata;
    cors?: ConsoleSettingsCors;
    scheduler?: ConsoleSettingsScheduler;
    management?: ConsoleSettingsManagement;
    trialInstance?: DisableableFeature;
    [key: string]: unknown;
}
