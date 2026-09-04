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
import { useEffect, useMemo, useRef, useState } from 'react';

import { useEnvironmentConsoleSettings } from '../features/environment-settings/hooks/useEnvironmentConsoleSettings';
import { useResetEnvironmentBrandedSenders } from '../features/environment-settings/hooks/useResetEnvironmentBrandedSenders';
import { useSaveEnvironmentConsoleSettings } from '../features/environment-settings/hooks/useSaveEnvironmentConsoleSettings';
import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import {
    isSmtpFormValid,
    parseSmtpPort,
    SmtpSection,
    type SmtpFieldReadonly,
    type SmtpFormState,
} from '../features/organization-settings/components/SmtpSection';
import { PASSWORD_SENTINEL, type ConsoleSettings } from '../features/organization-settings/types/consoleSettings';
import { buildConsoleSettingsSavePayload } from '../features/organization-settings/utils/buildConsoleSettingsSavePayload';
import { isConsoleSettingReadonly } from '../features/organization-settings/utils/isConsoleSettingReadonly';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';

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

/**
 * Environment-scoped SMTP settings: `platform > environment > SMTP`.
 *
 * Reuses the same form building blocks as the organization-scoped SMTP page ({@link SmtpSection},
 * {@link OrgSettingsFormShell}) — the underlying `email` settings shape is identical, only the API scope
 * differs (`/organizations/{orgId}/environments/{envId}/settings` instead of `/organizations/{orgId}/settings`).
 * Unlike the organization page, this one also offers "Reset to Org settings" for branded senders, mirroring
 * Classic's `portal-settings.component.ts`.
 */
export function EnvironmentSmtpSettingsPage() {
    const canEdit = useHasPermission({ anyOf: ['environment-settings-u'] });
    const { data: settings, isLoading, isError } = useEnvironmentConsoleSettings();
    const saveMutation = useSaveEnvironmentConsoleSettings();
    const resetMutation = useResetEnvironmentBrandedSenders();
    const [localState, setLocalState] = useState<SmtpFormState>(() => buildState(settings));
    const [savedState, setSavedState] = useState<SmtpFormState>(() => buildState(settings));
    const [resetConfirmOpen, setResetConfirmOpen] = useState(false);

    const isDirty = JSON.stringify(localState) !== JSON.stringify(savedState);
    const isDirtyRef = useRef(isDirty);
    isDirtyRef.current = isDirty;

    useEffect(() => {
        if (!settings) return;
        const next = buildState(settings);
        setSavedState(next);
        // Don't clobber in-progress edits when a background refetch (e.g. window refocus) delivers fresh data.
        if (!isDirtyRef.current) {
            setLocalState(next);
        }
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
            startTlsEnable: isConsoleSettingReadonly(settings, 'email.properties.starttls.enable'),
            sslTrust: isConsoleSettingReadonly(settings, 'email.properties.ssl.trust'),
            brandedSenders: isConsoleSettingReadonly(settings, 'email.branded_senders'),
        }),
        [settings],
    );
    const isValid = isSmtpFormValid(localState);

    // Reset is only offered when there is an environment-level override to drop (not inherited), the field is
    // editable (not system-locked, email enabled) and the user may update settings. The inherited check is
    // strict (=== false): an absent flag means "unknown", so the reset action is hidden rather than offered
    // against an endpoint that may not resolve. Mirrors Classic's `canResetBrandedSenders` getter.
    const canResetBrandedSenders =
        canEdit && localState.enabled && !readonly.brandedSenders && settings?.email?.brandedSendersInherited === false;

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

    function requestReset() {
        // Reset re-fetches from the server, discarding any unsaved edits elsewhere on the page — confirm first
        // when there's something to lose.
        if (isDirty) {
            setResetConfirmOpen(true);
            return;
        }
        resetMutation.mutate();
    }

    function confirmReset() {
        setResetConfirmOpen(false);
        setLocalState(savedState);
        resetMutation.mutate();
    }

    return (
        <>
            <OrgSettingsFormShell
                title="SMTP"
                description="Configure the mail server this environment uses for notifications, invitations, and other emails."
                canEdit={canEdit}
                isDirty={!trialHidesSmtp && isDirty}
                isValid={isValid}
                isSaving={saveMutation.isPending}
                isLoading={isLoading}
                isError={isError}
                showArchitectureOverrideWarning={false}
                onSave={handleSave}
                onDiscard={() => setLocalState(savedState)}
            >
                {trialHidesSmtp ? (
                    <p className="text-sm text-muted-foreground">SMTP is not available on trial instances.</p>
                ) : (
                    <SmtpSection
                        value={localState}
                        disabled={!canEdit}
                        readonly={readonly}
                        onChange={setLocalState}
                        canResetBrandedSenders={canResetBrandedSenders}
                        isResettingBrandedSenders={resetMutation.isPending}
                        onResetBrandedSenders={requestReset}
                    />
                )}
            </OrgSettingsFormShell>
            <ConfirmDialog
                open={resetConfirmOpen}
                onOpenChange={setResetConfirmOpen}
                title="Reset branded senders"
                description="You have unsaved changes on this page that will be discarded. Do you want to reset the branded senders to the organization configuration?"
                confirmLabel="Reset"
                pendingLabel="Resetting…"
                isPending={resetMutation.isPending}
                onConfirm={confirmReset}
            />
        </>
    );
}
