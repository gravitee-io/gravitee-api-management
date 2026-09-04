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

import { PortalCorsSection } from '../features/environment-settings/components/PortalCorsSection';
import type { CorsFormState } from '../features/organization-settings/components/CorsSection';
import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import {
    buildCorsFormStateFromPortalSettings,
    buildCorsPatch,
    buildPortalCorsFieldReadonly,
    isCorsFormValid,
} from '../features/organization-settings/utils/corsFormState';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import { useSavePortalSettings } from '../features/security-plan-types/hooks/useSavePortalSettings';
import { buildPortalCorsSavePayload } from '../features/security-plan-types/utils/buildPortalCorsSavePayload';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

/**
 * Environment-scoped CORS settings: `platform > environment > CORS`.
 *
 * Uses {@link PortalCorsSection} for portal-specific field copy and layout (Gamma Baby UX on Classic behavior).
 * Org-scoped CORS uses {@link ManagementCorsSection} via {@link CorsSettingsPage}.
 */
export function EnvironmentCorsSettingsPage() {
    const canEdit = useHasPermission({ anyOf: ['environment-settings-u'] });
    const { data: settings, isLoading, isError, error } = usePortalSettings();
    const saveMutation = useSavePortalSettings({
        successMessage: 'CORS settings saved successfully.',
        errorMessage: 'Failed to save CORS settings.',
    });
    const [localState, setLocalState] = useState<CorsFormState>(() => buildCorsFormStateFromPortalSettings(settings));
    const [savedState, setSavedState] = useState<CorsFormState>(() => buildCorsFormStateFromPortalSettings(settings));

    const isForbidden = isForbiddenApiError(isError, error);
    useForbiddenResourceRedirect({
        isForbidden,
        navItemKey: 'environment-cors',
        permissionPrefix: 'environment-settings-',
        redirectTo: '../applications',
    });

    const isDirty = JSON.stringify(localState) !== JSON.stringify(savedState);
    const isDirtyRef = useRef(isDirty);
    isDirtyRef.current = isDirty;

    useEffect(() => {
        if (!settings) return;
        const next = buildCorsFormStateFromPortalSettings(settings);
        setSavedState(next);
        // Don't clobber in-progress edits when a background refetch (e.g. window refocus) delivers fresh data.
        if (!isDirtyRef.current) {
            setLocalState(next);
        }
    }, [settings]);

    const readonly = useMemo(() => buildPortalCorsFieldReadonly(settings), [settings]);
    const isValid = isCorsFormValid(localState);

    function handleSave() {
        if (!settings || !isDirty || !isValid || saveMutation.isPending) return;
        const payload = buildPortalCorsSavePayload(settings, buildCorsPatch(localState));
        saveMutation.mutate(payload, { onSuccess: () => setSavedState(localState) });
    }

    return (
        <OrgSettingsFormShell
            title="CORS"
            description="Control which browser origins may call the developer portal API from this environment."
            canEdit={canEdit}
            isDirty={isDirty}
            isValid={isValid}
            isSaving={saveMutation.isPending}
            isLoading={isLoading}
            isError={isError && !isForbidden}
            showArchitectureOverrideWarning={false}
            onSave={handleSave}
            onDiscard={() => setLocalState(savedState)}
        >
            <PortalCorsSection value={localState} disabled={!canEdit} readonly={readonly} onChange={setLocalState} />
        </OrgSettingsFormShell>
    );
}
