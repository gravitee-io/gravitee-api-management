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
import { useMemo } from 'react';

import { ApiLoggingSection } from '../features/environment-settings/components/ApiLoggingSection';
import { useBoundPortalSettingsForm } from '../features/environment-settings/hooks/useBoundPortalSettingsForm';
import { useSaveEnvironmentPortalSettings } from '../features/environment-settings/hooks/useSaveEnvironmentPortalSettings';
import { buildPortalSettingsSectionPayload } from '../features/environment-settings/utils/buildPortalSettingsSectionPayload';
import {
    API_LOGGING_READONLY_KEYS,
    DEFAULT_COUNT_DEFAULT,
    DEFAULT_COUNT_LIMIT,
    DEFAULT_LOGGING_MAX_DURATION_MS,
    DEFAULT_PROBABILISTIC_DEFAULT,
    DEFAULT_PROBABILISTIC_LIMIT,
    DEFAULT_TEMPORAL_DEFAULT,
    DEFAULT_TEMPORAL_LIMIT,
    DEFAULT_WINDOWED_COUNT_DEFAULT,
    DEFAULT_WINDOWED_COUNT_LIMIT,
    isApiLoggingFormValid,
    toPortalSettingsLogging,
    type ApiLoggingFieldReadonly,
    type ApiLoggingFormState,
} from '../features/environment-settings/utils/loggingValidators';
import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import type { PortalSettings } from '../features/security-plan-types/services/portalSettings';
import { isPortalSettingReadonly } from '../features/security-plan-types/utils/isPortalSettingReadonly';

function numberOrDefault(value: number | undefined, fallback: number): string {
    return String(value ?? fallback);
}

function buildState(settings: PortalSettings | undefined): ApiLoggingFormState {
    const logging = settings?.logging;
    return {
        maxDurationMillis: numberOrDefault(logging?.maxDurationMillis, DEFAULT_LOGGING_MAX_DURATION_MS),
        auditEnabled: logging?.audit?.enabled ?? false,
        auditTrailEnabled: logging?.audit?.trail?.enabled ?? false,
        userDisplayed: logging?.user?.displayed ?? false,
        probabilisticDefault: numberOrDefault(logging?.messageSampling?.probabilistic?.default, DEFAULT_PROBABILISTIC_DEFAULT),
        probabilisticLimit: numberOrDefault(logging?.messageSampling?.probabilistic?.limit, DEFAULT_PROBABILISTIC_LIMIT),
        countDefault: numberOrDefault(logging?.messageSampling?.count?.default, DEFAULT_COUNT_DEFAULT),
        countLimit: numberOrDefault(logging?.messageSampling?.count?.limit, DEFAULT_COUNT_LIMIT),
        temporalDefault: logging?.messageSampling?.temporal?.default ?? DEFAULT_TEMPORAL_DEFAULT,
        temporalLimit: logging?.messageSampling?.temporal?.limit ?? DEFAULT_TEMPORAL_LIMIT,
        windowedCountDefault: logging?.messageSampling?.windowedCount?.default ?? DEFAULT_WINDOWED_COUNT_DEFAULT,
        windowedCountLimit: logging?.messageSampling?.windowedCount?.limit ?? DEFAULT_WINDOWED_COUNT_LIMIT,
    };
}

export function EnvironmentApiLoggingSettingsPage() {
    const canEdit = useHasPermission({ anyOf: ['environment-settings-u'] });
    const { data: settings, isLoading, isError } = usePortalSettings();
    const saveMutation = useSaveEnvironmentPortalSettings();
    const { localState, setLocalState, savedState, setSavedState, isDirty } = useBoundPortalSettingsForm(settings, buildState);

    const readonly = useMemo<ApiLoggingFieldReadonly>(
        () => ({
            maxDurationMillis: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.maxDuration),
            auditEnabled: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.auditEnabled),
            auditTrailEnabled: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.auditTrailEnabled),
            userDisplayed: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.userDisplayed),
            probabilisticDefault: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.probabilisticDefault),
            probabilisticLimit: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.probabilisticLimit),
            countDefault: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.countDefault),
            countLimit: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.countLimit),
            temporalDefault: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.temporalDefault),
            temporalLimit: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.temporalLimit),
            windowedCountDefault: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.windowedCountDefault),
            windowedCountLimit: isPortalSettingReadonly(settings, API_LOGGING_READONLY_KEYS.windowedCountLimit),
        }),
        [settings],
    );

    const isValid = isApiLoggingFormValid(localState);

    function handleSave() {
        if (!settings || !isDirty || !isValid || saveMutation.isPending) return;
        const logging = toPortalSettingsLogging(localState);
        if (!logging) return;
        const payload = buildPortalSettingsSectionPayload(settings, 'logging', { logging });
        saveMutation.mutate(payload, {
            onSuccess: fresh => {
                const next = buildState(fresh);
                setLocalState(next);
                setSavedState(next);
            },
        });
    }

    return (
        <OrgSettingsFormShell
            title="API Logging"
            description="Cap how long API may log full payloads, audit who made those logs, and set the default sampling for message APIs."
            canEdit={canEdit}
            isDirty={isDirty}
            isValid={isValid}
            isSaving={saveMutation.isPending}
            isLoading={isLoading}
            isError={isError}
            onSave={handleSave}
            onDiscard={() => setLocalState(savedState)}
        >
            <ApiLoggingSection value={localState} disabled={!canEdit} readonly={readonly} onChange={setLocalState} />
        </OrgSettingsFormShell>
    );
}
