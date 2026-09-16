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

import { useEffect, useMemo, useRef, useState } from 'react';

import { useHasPermission } from '@gravitee/gamma-modules-sdk';

import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import { PrimaryOwnerModeSection } from '../features/primary-owner-mode/components/PrimaryOwnerModeSection';
import { buildPrimaryOwnerModeSavePayload } from '../features/primary-owner-mode/utils/buildPrimaryOwnerModeSavePayload';
import {
    applyReadonlyPrimaryOwnerModes,
    buildPrimaryOwnerModeFormState,
    getPrimaryOwnerModeReadonly,
    isPrimaryOwnerModeDirty,
    type PrimaryOwnerModeFormState,
} from '../features/primary-owner-mode/utils/primaryOwnerMode';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import { useSavePortalSettings } from '../features/security-plan-types/hooks/useSavePortalSettings';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

export function PrimaryOwnerModePage() {
    const canEdit = useHasPermission({ anyOf: ['environment-settings-u'] });
    const { data: settings, isLoading, isError, error } = usePortalSettings();
    const saveMutation = useSavePortalSettings({
        successMessage: 'Primary owner mode saved successfully.',
        errorMessage: 'Failed to save primary owner mode.',
    });
    const [localState, setLocalState] = useState<PrimaryOwnerModeFormState>(() => buildPrimaryOwnerModeFormState(settings));
    const [savedState, setSavedState] = useState<PrimaryOwnerModeFormState>(() => buildPrimaryOwnerModeFormState(settings));

    const isForbidden = isForbiddenApiError(isError, error);
    useForbiddenResourceRedirect({
        isForbidden,
        navItemKey: 'primary-owner-mode',
        permissionPrefix: 'environment-settings-',
        redirectTo: '../applications',
    });

    const readonly = useMemo(() => getPrimaryOwnerModeReadonly(settings), [settings]);
    const isDirty = isPrimaryOwnerModeDirty(localState, savedState, readonly);
    const isDirtyRef = useRef(isDirty);
    isDirtyRef.current = isDirty;

    useEffect(() => {
        if (!settings) return;
        const next = buildPrimaryOwnerModeFormState(settings);
        setSavedState(next);
        if (!isDirtyRef.current) {
            setLocalState(next);
        }
    }, [settings]);

    function handleSave() {
        if (!settings || !isDirty || saveMutation.isPending) return;
        const stateAtSaveTime = applyReadonlyPrimaryOwnerModes(localState, savedState, readonly);
        saveMutation.mutate(buildPrimaryOwnerModeSavePayload(settings, stateAtSaveTime), {
            onSuccess: () => {
                setSavedState(stateAtSaveTime);
                setLocalState(stateAtSaveTime);
            },
        });
    }

    return (
        <OrgSettingsFormShell
            title="Primary Owner Mode"
            description="Choose who can be the primary owner when someone creates an API or an API product in this environment."
            canEdit={canEdit}
            isDirty={isDirty}
            isSaving={saveMutation.isPending}
            isLoading={isLoading}
            isError={isError && !isForbidden}
            showArchitectureOverrideWarning={false}
            onSave={handleSave}
            onDiscard={() => setLocalState(savedState)}
        >
            <PrimaryOwnerModeSection value={localState} disabled={!canEdit} readonly={readonly} onChange={setLocalState} />
        </OrgSettingsFormShell>
    );
}
