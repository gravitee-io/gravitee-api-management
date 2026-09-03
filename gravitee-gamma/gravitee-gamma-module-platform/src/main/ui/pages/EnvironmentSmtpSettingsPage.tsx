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
import { Button } from '@gravitee/graphene-core';
import { useMemo, useState } from 'react';

import { useBoundPortalSettingsForm } from '../features/environment-settings/hooks/useBoundPortalSettingsForm';
import { useResetPortalBrandedSenders } from '../features/environment-settings/hooks/useResetPortalBrandedSenders';
import { useSaveEnvironmentPortalSettings } from '../features/environment-settings/hooks/useSaveEnvironmentPortalSettings';
import { buildPortalSettingsSectionPayload } from '../features/environment-settings/utils/buildPortalSettingsSectionPayload';
import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import {
    isSmtpFormValid,
    parseSmtpPort,
    SmtpSection,
    type SmtpFieldReadonly,
    type SmtpFormState,
} from '../features/organization-settings/components/SmtpSection';
import { PASSWORD_SENTINEL } from '../features/organization-settings/types/consoleSettings';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import type { PortalSettings } from '../features/security-plan-types/services/portalSettings';
import { isPortalSettingReadonly } from '../features/security-plan-types/utils/isPortalSettingReadonly';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';

function buildState(settings: PortalSettings | undefined): SmtpFormState {
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

export function EnvironmentSmtpSettingsPage() {
    const canEdit = useHasPermission({ anyOf: ['environment-settings-u'] });
    const { data: settings, isLoading, isError } = usePortalSettings();
    const saveMutation = useSaveEnvironmentPortalSettings();
    const resetMutation = useResetPortalBrandedSenders();
    const [brandedSendersInherited, setBrandedSendersInherited] = useState<boolean | undefined>(
        () => settings?.email?.brandedSendersInherited,
    );
    const [resetOpen, setResetOpen] = useState(false);
    const { localState, setLocalState, savedState, setSavedState, isDirty } = useBoundPortalSettingsForm(
        settings,
        buildState,
        next => setBrandedSendersInherited(next.email?.brandedSendersInherited),
    );

    const readonly = useMemo<SmtpFieldReadonly>(
        () => ({
            enabled: isPortalSettingReadonly(settings, 'email.enabled'),
            host: isPortalSettingReadonly(settings, 'email.host'),
            port: isPortalSettingReadonly(settings, 'email.port'),
            username: isPortalSettingReadonly(settings, 'email.username'),
            password: isPortalSettingReadonly(settings, 'email.password'),
            protocol: isPortalSettingReadonly(settings, 'email.protocol'),
            subject: isPortalSettingReadonly(settings, 'email.subject'),
            from: isPortalSettingReadonly(settings, 'email.from'),
            auth: isPortalSettingReadonly(settings, 'email.properties.auth'),
            startTlsEnable: isPortalSettingReadonly(settings, 'email.properties.starttls.enable'),
            sslTrust: isPortalSettingReadonly(settings, 'email.properties.ssl.trust'),
            brandedSenders: isPortalSettingReadonly(settings, 'email.branded_senders'),
        }),
        [settings],
    );
    const isValid = isSmtpFormValid(localState);
    const resetLocked = brandedSendersInherited !== false || Boolean(readonly.brandedSenders);

    function applySettings(nextSettings: PortalSettings) {
        const next = buildState(nextSettings);
        setLocalState(next);
        setSavedState(next);
        setBrandedSendersInherited(nextSettings.email?.brandedSendersInherited);
    }

    function handleSave() {
        if (!settings || !isDirty || !isValid || saveMutation.isPending) return;
        const payload = buildPortalSettingsSectionPayload(settings, 'email', {
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
        saveMutation.mutate(payload, { onSuccess: applySettings });
    }

    function handleResetToOrg() {
        if (!canEdit || resetLocked || resetMutation.isPending) return;
        resetMutation.mutate(undefined, {
            onSuccess: nextSettings => {
                applySettings(nextSettings);
                setResetOpen(false);
            },
        });
    }

    return (
        <OrgSettingsFormShell
            title="SMTP"
            description="Configure the mail server this environment uses for notifications, invitations, and other emails."
            canEdit={canEdit}
            isDirty={isDirty}
            isValid={isValid}
            isSaving={saveMutation.isPending || resetMutation.isPending}
            isLoading={isLoading}
            isError={isError}
            onSave={handleSave}
            onDiscard={() => setLocalState(savedState)}
            extraActions={
                canEdit ? (
                    <div className="flex justify-end">
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => setResetOpen(true)}
                            disabled={resetLocked || resetMutation.isPending}
                        >
                            Reset to Org settings
                        </Button>
                    </div>
                ) : null
            }
        >
            <SmtpSection value={localState} disabled={!canEdit} readonly={readonly} onChange={setLocalState} />
            <ConfirmDialog
                open={resetOpen}
                onOpenChange={open => !open && setResetOpen(false)}
                title="Reset branded senders?"
                description="This deletes the environment override so branded sender rules fall back to the organization. Unsaved changes on this page will be discarded."
                confirmLabel="Reset to Org settings"
                isPending={resetMutation.isPending}
                onConfirm={handleResetToOrg}
            />
        </OrgSettingsFormShell>
    );
}
