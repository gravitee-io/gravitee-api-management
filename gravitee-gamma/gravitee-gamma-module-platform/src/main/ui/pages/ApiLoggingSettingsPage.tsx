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

import { ApiLoggingSection } from '../features/api-logging/components/ApiLoggingSection';
import { buildApiLoggingFormState, getApiLoggingReadonlyState } from '../features/api-logging/utils/apiLoggingFormState';
import {
    isApiLoggingFormValid,
    toApiLoggingSettingsPayload,
    validateApiLoggingForm,
    type ApiLoggingFormState,
} from '../features/api-logging/utils/apiLoggingValidators';
import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import { useOrgConsoleSettings } from '../features/organization-settings/hooks/useOrgConsoleSettings';
import { useSaveOrgConsoleSettings } from '../features/organization-settings/hooks/useSaveOrgConsoleSettings';
import { buildConsoleSettingsSavePayload } from '../features/organization-settings/utils/buildConsoleSettingsSavePayload';

function formStatesEqual(left: ApiLoggingFormState, right: ApiLoggingFormState): boolean {
    return (Object.keys(left) as (keyof ApiLoggingFormState)[]).every(key => left[key] === right[key]);
}

export function ApiLoggingSettingsPage() {
    const canEdit = useHasPermission({ anyOf: ['organization-settings-u'] });
    const { data: settings, isLoading, isError } = useOrgConsoleSettings();
    const saveMutation = useSaveOrgConsoleSettings();

    const [localState, setLocalState] = useState<ApiLoggingFormState>(() => buildApiLoggingFormState(settings));
    const [savedState, setSavedState] = useState<ApiLoggingFormState>(() => buildApiLoggingFormState(settings));

    const isDirty = !formStatesEqual(localState, savedState);
    const isDirtyRef = useRef(isDirty);
    isDirtyRef.current = isDirty;

    useEffect(() => {
        if (!settings) {
            return;
        }
        const next = buildApiLoggingFormState(settings);
        setSavedState(next);
        if (!isDirtyRef.current) {
            setLocalState(next);
        }
    }, [settings]);

    const readonly = useMemo(() => getApiLoggingReadonlyState(settings), [settings]);
    const fieldErrors = useMemo(() => validateApiLoggingForm(localState), [localState]);
    const isValid = isApiLoggingFormValid(localState);

    function handleSave() {
        if (!settings) {
            return;
        }
        if (!isDirty) {
            return;
        }
        if (!isValid) {
            return;
        }
        if (saveMutation.isPending) {
            return;
        }

        const payload = buildConsoleSettingsSavePayload(settings, 'logging', {
            logging: toApiLoggingSettingsPayload(localState),
        });
        saveMutation.mutate(payload, { onSuccess: () => setSavedState(localState) });
    }

    return (
        <OrgSettingsFormShell
            title="API Logging"
            description="Cap how long APIs may log full payloads, audit who reads those logs, and set the default sampling for message APIs."
            canEdit={canEdit}
            isDirty={isDirty}
            isValid={isValid}
            isSaving={saveMutation.isPending}
            isLoading={isLoading}
            isError={isError}
            onSave={handleSave}
            onDiscard={() => setLocalState(savedState)}
        >
            <ApiLoggingSection value={localState} errors={fieldErrors} disabled={!canEdit} readonly={readonly} onChange={setLocalState} />
        </OrgSettingsFormShell>
    );
}
