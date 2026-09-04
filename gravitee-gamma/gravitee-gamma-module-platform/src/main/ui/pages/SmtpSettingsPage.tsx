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

import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import { isSmtpFormValid, SmtpSection, type SmtpFormState } from '../features/organization-settings/components/SmtpSection';
import { useOrgConsoleSettings } from '../features/organization-settings/hooks/useOrgConsoleSettings';
import { useSaveOrgConsoleSettings } from '../features/organization-settings/hooks/useSaveOrgConsoleSettings';
import { buildConsoleSettingsSavePayload } from '../features/organization-settings/utils/buildConsoleSettingsSavePayload';
import { buildSmtpEmailPatch, buildSmtpFieldReadonly, buildSmtpFormState } from '../features/organization-settings/utils/smtpFormState';

export function SmtpSettingsPage() {
    const canEdit = useHasPermission({ anyOf: ['organization-settings-u'] });
    const { data: settings, isLoading, isError } = useOrgConsoleSettings();
    const saveMutation = useSaveOrgConsoleSettings();
    const [localState, setLocalState] = useState<SmtpFormState>(() => buildSmtpFormState(settings));
    const [savedState, setSavedState] = useState<SmtpFormState>(() => buildSmtpFormState(settings));

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

    function handleSave() {
        if (!settings || !isDirty || !isValid || saveMutation.isPending) return;
        const payload = buildConsoleSettingsSavePayload(settings, 'email', {
            email: buildSmtpEmailPatch(localState),
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
