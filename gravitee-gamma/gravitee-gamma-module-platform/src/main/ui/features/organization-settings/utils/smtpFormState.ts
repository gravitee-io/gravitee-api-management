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

import { isConsoleSettingReadonly } from './isConsoleSettingReadonly';
import { parseSmtpPort, type SmtpFieldReadonly, type SmtpFormState } from '../components/SmtpSection';
import { PASSWORD_SENTINEL, type ConsoleSettings, type ConsoleSettingsEmail } from '../types/consoleSettings';

export function buildSmtpFormState(settings: ConsoleSettings | undefined): SmtpFormState {
    return {
        enabled: settings?.email?.enabled ?? false,
        host: settings?.email?.host ?? '',
        port: settings?.email?.port !== undefined && settings?.email?.port !== null ? String(settings.email.port) : '',
        username: settings?.email?.username ?? '',
        password: settings?.email?.password ?? PASSWORD_SENTINEL,
        protocol: settings?.email?.protocol ?? '',
        subject: settings?.email?.subject ?? '',
        from: settings?.email?.from ?? '',
        auth: settings?.email?.properties?.auth ?? false,
        startTlsEnable: settings?.email?.properties?.startTlsEnable ?? false,
        sslTrust: settings?.email?.properties?.sslTrust ?? '',
        brandedSenders: settings?.email?.brandedSenders ?? [],
    };
}

export function buildSmtpFieldReadonly(settings: ConsoleSettings | undefined): SmtpFieldReadonly {
    return {
        enabled: isConsoleSettingReadonly(settings, 'email.enabled'),
        host: isConsoleSettingReadonly(settings, 'email.host'),
        port: isConsoleSettingReadonly(settings, 'email.port'),
        username: isConsoleSettingReadonly(settings, 'email.username'),
        password: isConsoleSettingReadonly(settings, 'email.password'),
        protocol: isConsoleSettingReadonly(settings, 'email.protocol'),
        subject: isConsoleSettingReadonly(settings, 'email.subject'),
        from: isConsoleSettingReadonly(settings, 'email.from'),
        auth: isConsoleSettingReadonly(settings, 'email.properties.auth'),
        startTlsEnable: isConsoleSettingReadonly(settings, 'email.properties.starttls.enable'),
        sslTrust: isConsoleSettingReadonly(settings, 'email.properties.ssl.trust'),
        brandedSenders: isConsoleSettingReadonly(settings, 'email.branded_senders'),
    };
}

export function buildSmtpEmailPatch(localState: SmtpFormState): ConsoleSettingsEmail {
    return {
        enabled: localState.enabled,
        host: localState.host,
        port: parseSmtpPort(localState.port) ?? undefined,
        username: localState.username,
        password: localState.password,
        protocol: localState.protocol,
        subject: localState.subject,
        from: localState.from,
        brandedSenders: localState.brandedSenders,
        properties: {
            auth: localState.auth,
            startTlsEnable: localState.startTlsEnable,
            sslTrust: localState.sslTrust,
        },
    };
}

/**
 * Environment portal settings only — mirrors Classic `portal-settings.component.ts` `canResetBrandedSenders`.
 */
export function canEnvironmentResetBrandedSenders({
    canEdit,
    localState,
    readonly,
    settings,
}: {
    canEdit: boolean;
    localState: SmtpFormState;
    readonly: SmtpFieldReadonly;
    settings: ConsoleSettings | undefined;
}): boolean {
    return canEdit && localState.enabled && !readonly.brandedSenders && settings?.email?.brandedSendersInherited === false;
}
