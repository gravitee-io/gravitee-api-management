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

import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { useEffect, useMemo, useState } from 'react';

import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import {
    isSmtpFormValid,
    parseSmtpPort,
    SmtpSection,
    type SmtpFieldReadonly,
    type SmtpFormState,
} from '../features/organization-settings/components/SmtpSection';
import { useOrgConsoleSettings } from '../features/organization-settings/hooks/useOrgConsoleSettings';
import { useSaveOrgConsoleSettings } from '../features/organization-settings/hooks/useSaveOrgConsoleSettings';
import { PASSWORD_SENTINEL, type ConsoleSettings } from '../features/organization-settings/types/consoleSettings';
import { buildConsoleSettingsSavePayload } from '../features/organization-settings/utils/buildConsoleSettingsSavePayload';
import { isConsoleSettingReadonly } from '../features/organization-settings/utils/isConsoleSettingReadonly';

function buildState(settings: ConsoleSettings | undefined): SmtpFormState {
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

export function SmtpSettingsPage() {
    const canEdit = useHasPermission({ anyOf: ['organization-settings-u'] });
    const { data: settings, isLoading, isError } = useOrgConsoleSettings();
    const saveMutation = useSaveOrgConsoleSettings();
    const [localState, setLocalState] = useState<SmtpFormState>(() => buildState(settings));
    const [savedState, setSavedState] = useState<SmtpFormState>(() => buildState(settings));

    useEffect(() => {
        if (!settings) return;
        const next = buildState(settings);
        setLocalState(next);
        setSavedState(next);
    }, [settings]);

    const trialHidesSmtp = Boolean(settings?.trialInstance?.enabled);
    const readonly = useMemo<SmtpFieldReadonly>(
        () => ({
            enabled: isConsoleSettingReadonly(settings, 'email.enabled'),
            host: isConsoleSettingReadonly(settings, 'email.host'),
            port: isConsoleSettingReadonly(settings, 'email.port'),
            username: isConsoleSettingReadonly(settings, 'email.username'),
            password: isConsoleSettingReadonly(settings, 'email.password'),
            protocol: isConsoleSettingReadonly(settings, 'email.protocol'),
            subject: isConsoleSettingReadonly(settings, 'email.subject'),
            from: isConsoleSettingReadonly(settings, 'email.from'),
            auth: isConsoleSettingReadonly(settings, 'email.properties.auth'),
            startTlsEnable: isConsoleSettingReadonly(settings, 'email.properties.startTlsEnable'),
            sslTrust: isConsoleSettingReadonly(settings, 'email.properties.sslTrust'),
            brandedSenders: isConsoleSettingReadonly(settings, 'email.branded_senders'),
        }),
        [settings],
    );
    const isDirty = JSON.stringify(localState) !== JSON.stringify(savedState);
    const isValid = isSmtpFormValid(localState);

    function handleSave() {
        if (!settings || !isDirty || !isValid || saveMutation.isPending) return;
        const payload = buildConsoleSettingsSavePayload(settings, 'email', {
            email: {
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
            },
        });
        saveMutation.mutate(payload, { onSuccess: () => setSavedState(localState) });
    }

    return (
        <OrgSettingsFormShell
            title="SMTP"
            description="Configure the mail server this organization uses for notifications, invitations, and other emails."
            canEdit={canEdit}
            isDirty={!trialHidesSmtp && isDirty}
            isValid={isValid}
            isSaving={saveMutation.isPending}
            isLoading={isLoading}
            isError={isError}
            onSave={handleSave}
            onDiscard={() => setLocalState(savedState)}
        >
            {trialHidesSmtp ? (
                <p className="text-sm text-muted-foreground">SMTP is not available on trial instances.</p>
            ) : (
                <SmtpSection value={localState} disabled={!canEdit} readonly={readonly} onChange={setLocalState} />
            )}
        </OrgSettingsFormShell>
    );
}
