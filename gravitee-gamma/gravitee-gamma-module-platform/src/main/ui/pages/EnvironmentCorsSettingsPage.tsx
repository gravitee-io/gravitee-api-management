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

import { useBoundPortalSettingsForm } from '../features/environment-settings/hooks/useBoundPortalSettingsForm';
import { useSaveEnvironmentPortalSettings } from '../features/environment-settings/hooks/useSaveEnvironmentPortalSettings';
import { buildPortalSettingsSectionPayload } from '../features/environment-settings/utils/buildPortalSettingsSectionPayload';
import { CorsSection, type CorsFieldReadonly, type CorsFormState } from '../features/organization-settings/components/CorsSection';
import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import { DEFAULT_CORS_MAX_AGE, getInvalidAllowOrigins, parseCorsMaxAge } from '../features/organization-settings/utils/corsValidators';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import type { PortalSettings } from '../features/security-plan-types/services/portalSettings';
import { isPortalSettingReadonly } from '../features/security-plan-types/utils/isPortalSettingReadonly';

function buildState(settings: PortalSettings | undefined): CorsFormState {
    return {
        allowOrigin: settings?.cors?.allowOrigin ?? [],
        allowMethods: settings?.cors?.allowMethods ?? [],
        allowHeaders: settings?.cors?.allowHeaders ?? [],
        exposedHeaders: settings?.cors?.exposedHeaders ?? [],
        maxAge: String(settings?.cors?.maxAge ?? DEFAULT_CORS_MAX_AGE),
    };
}

export function EnvironmentCorsSettingsPage() {
    const canEdit = useHasPermission({ anyOf: ['environment-settings-u'] });
    const { data: settings, isLoading, isError } = usePortalSettings();
    const saveMutation = useSaveEnvironmentPortalSettings();
    const { localState, setLocalState, savedState, setSavedState, isDirty } = useBoundPortalSettingsForm(settings, buildState);

    const readonly = useMemo<CorsFieldReadonly>(
        () => ({
            allowOrigin: isPortalSettingReadonly(settings, 'http.api.portal.cors.allow-origin'),
            allowMethods: isPortalSettingReadonly(settings, 'http.api.portal.cors.allow-methods'),
            allowHeaders: isPortalSettingReadonly(settings, 'http.api.portal.cors.allow-headers'),
            exposedHeaders: isPortalSettingReadonly(settings, 'http.api.portal.cors.exposed-headers'),
            maxAge: isPortalSettingReadonly(settings, 'http.api.portal.cors.max-age'),
        }),
        [settings],
    );

    const maxAge = parseCorsMaxAge(localState.maxAge);
    const isValid = maxAge !== null && getInvalidAllowOrigins(localState.allowOrigin).length === 0;

    function handleSave() {
        if (!settings || !isDirty || !isValid || saveMutation.isPending) return;
        const payload = buildPortalSettingsSectionPayload(settings, 'cors', {
            cors: {
                allowOrigin: localState.allowOrigin,
                allowMethods: localState.allowMethods,
                allowHeaders: localState.allowHeaders,
                exposedHeaders: localState.exposedHeaders,
                maxAge,
            },
        });
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
            title="CORS"
            description="Control which browser origins may call the developer portal API from this environment."
            canEdit={canEdit}
            isDirty={isDirty}
            isValid={isValid}
            isSaving={saveMutation.isPending}
            isLoading={isLoading}
            isError={isError}
            onSave={handleSave}
            onDiscard={() => setLocalState(savedState)}
        >
            <CorsSection
                value={localState}
                disabled={!canEdit}
                readonly={readonly}
                wildcardWarningTarget="developer portal API"
                onChange={setLocalState}
            />
        </OrgSettingsFormShell>
    );
}
