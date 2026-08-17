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

import { CorsSection, type CorsFormState } from '../features/organization-settings/components/CorsSection';
import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import { useOrgConsoleSettings } from '../features/organization-settings/hooks/useOrgConsoleSettings';
import { useSaveOrgConsoleSettings } from '../features/organization-settings/hooks/useSaveOrgConsoleSettings';
import type { ConsoleSettings } from '../features/organization-settings/types/consoleSettings';
import { buildConsoleSettingsSavePayload } from '../features/organization-settings/utils/buildConsoleSettingsSavePayload';
import { DEFAULT_CORS_MAX_AGE, getInvalidAllowOrigins } from '../features/organization-settings/utils/corsValidators';
import { isConsoleSettingReadonly } from '../features/organization-settings/utils/isConsoleSettingReadonly';

function buildState(settings: ConsoleSettings | undefined): CorsFormState {
    return {
        allowOrigin: settings?.cors?.allowOrigin ?? [],
        allowMethods: settings?.cors?.allowMethods ?? [],
        allowHeaders: settings?.cors?.allowHeaders ?? [],
        exposedHeaders: settings?.cors?.exposedHeaders ?? [],
        maxAge: String(settings?.cors?.maxAge ?? DEFAULT_CORS_MAX_AGE),
    };
}

function parseMaxAge(value: string): number | null {
    if (!/^\d+$/.test(value.trim())) return null;
    return Number(value);
}

export function CorsSettingsPage() {
    const canEdit = useHasPermission({ anyOf: ['organization-settings-u'] });
    const { data: settings, isLoading, isError } = useOrgConsoleSettings();
    const saveMutation = useSaveOrgConsoleSettings();
    const [localState, setLocalState] = useState<CorsFormState>(() => buildState(settings));
    const [savedState, setSavedState] = useState<CorsFormState>(() => buildState(settings));

    useEffect(() => {
        if (!settings) return;
        const next = buildState(settings);
        setLocalState(next);
        setSavedState(next);
    }, [settings]);

    const corsLocked = useMemo(
        () =>
            isConsoleSettingReadonly(settings, 'http.api.management.cors.allow-origin') ||
            isConsoleSettingReadonly(settings, 'http.api.management.cors.allow-methods') ||
            isConsoleSettingReadonly(settings, 'http.api.management.cors.allow-headers') ||
            isConsoleSettingReadonly(settings, 'http.api.management.cors.exposed-headers') ||
            isConsoleSettingReadonly(settings, 'http.api.management.cors.max-age'),
        [settings],
    );

    const maxAge = parseMaxAge(localState.maxAge);
    const isValid = maxAge !== null && getInvalidAllowOrigins(localState.allowOrigin).length === 0;
    const isDirty = JSON.stringify(localState) !== JSON.stringify(savedState);

    function handleSave() {
        if (!settings || !isDirty || !isValid || saveMutation.isPending) return;
        const payload = buildConsoleSettingsSavePayload(settings, 'cors', {
            cors: {
                allowOrigin: localState.allowOrigin,
                allowMethods: localState.allowMethods,
                allowHeaders: localState.allowHeaders,
                exposedHeaders: localState.exposedHeaders,
                maxAge,
            },
        });
        saveMutation.mutate(payload, { onSuccess: () => setSavedState(localState) });
    }

    return (
        <OrgSettingsFormShell
            title="CORS"
            description="Control which browser origins may call this organization's management API."
            canEdit={canEdit}
            isDirty={isDirty}
            isValid={isValid}
            isSaving={saveMutation.isPending}
            isLoading={isLoading}
            isError={isError}
            onSave={handleSave}
            onDiscard={() => setLocalState(savedState)}
        >
            <CorsSection value={localState} disabled={!canEdit || corsLocked} onChange={setLocalState} />
        </OrgSettingsFormShell>
    );
}
