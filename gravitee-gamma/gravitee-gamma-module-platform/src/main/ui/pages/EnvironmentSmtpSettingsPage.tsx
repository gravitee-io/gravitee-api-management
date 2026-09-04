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

import { useResetEnvironmentBrandedSenders } from '../features/environment-settings/hooks/useResetEnvironmentBrandedSenders';
import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import { isSmtpFormValid, SmtpSection, type SmtpFormState } from '../features/organization-settings/components/SmtpSection';
import type { ConsoleSettings } from '../features/organization-settings/types/consoleSettings';
import { buildPortalSettingsEmailSavePayload } from '../features/organization-settings/utils/buildPortalSettingsEmailSavePayload';
import {
    buildSmtpEmailPatch,
    buildSmtpFieldReadonly,
    buildSmtpFormState,
    canEnvironmentResetBrandedSenders,
} from '../features/organization-settings/utils/smtpFormState';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import { useSavePortalSettings } from '../features/security-plan-types/hooks/useSavePortalSettings';
import { ConfirmDialog } from '../shared/components/ConfirmDialog';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

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
    const { data: portalSettings, isLoading, isError, error } = usePortalSettings();
    const settings = portalSettings as ConsoleSettings | undefined;
    const saveMutation = useSavePortalSettings({
        successMessage: 'Configuration successfully saved!',
        errorMessage: 'An error occurred while saving the configuration.',
    });
    const resetMutation = useResetEnvironmentBrandedSenders();
    const [localState, setLocalState] = useState<SmtpFormState>(() => buildSmtpFormState(settings));
    const [savedState, setSavedState] = useState<SmtpFormState>(() => buildSmtpFormState(settings));
    const [resetConfirmOpen, setResetConfirmOpen] = useState(false);

    const isForbidden = isForbiddenApiError(isError, error);
    useForbiddenResourceRedirect({
        isForbidden,
        navItemKey: 'environment-smtp',
        permissionPrefix: 'environment-settings-',
        redirectTo: '../applications',
    });

    const isDirty = JSON.stringify(localState) !== JSON.stringify(savedState);
    const isDirtyRef = useRef(isDirty);
    isDirtyRef.current = isDirty;

    useEffect(() => {
        if (!settings) return;
        const next = buildSmtpFormState(settings);
        setSavedState(next);
        // Don't clobber in-progress edits when a background refetch (e.g. window refocus) delivers fresh data.
        if (!isDirtyRef.current) {
            setLocalState(next);
        }
    }, [settings]);

    const trialHidesSmtp = Boolean(settings?.trialInstance?.enabled);
    const readonly = useMemo(() => buildSmtpFieldReadonly(settings), [settings]);
    const isValid = isSmtpFormValid(localState);
    const canResetBrandedSenders = canEnvironmentResetBrandedSenders({ canEdit, localState, readonly, settings });

    function handleSave() {
        if (!settings) return;
        if (!portalSettings) return;
        if (!isDirty) return;
        if (!isValid) return;
        if (saveMutation.isPending) return;
        const payload = buildPortalSettingsEmailSavePayload(portalSettings, buildSmtpEmailPatch(localState));
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

    if (isForbidden) {
        return null;
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
