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
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';

import { useHasPermission } from '@gravitee/gamma-modules-sdk';

import { ApiReviewTogglesSection } from '../features/api-review/components/ApiReviewTogglesSection';
import { QualityRulesCard } from '../features/api-review/components/QualityRulesCard';
import {
    buildApiReviewSettingsSavePayload,
    buildApiReviewSettingsState,
    getApiReviewReadonlyState,
    type ApiReviewSettingsState,
} from '../features/api-review/utils/apiReviewSettings';
import { ENVIRONMENT_PORTAL_CONFIGURATION_QUERY_KEY } from '../features/applications/hooks/useEnvironmentPortalConfiguration';
import { OrgSettingsFormShell } from '../features/organization-settings/components/OrgSettingsFormShell';
import { usePortalSettings } from '../features/security-plan-types/hooks/usePortalSettings';
import { useSavePortalSettings } from '../features/security-plan-types/hooks/useSavePortalSettings';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

function statesEqual(left: ApiReviewSettingsState, right: ApiReviewSettingsState): boolean {
    return left.apiScoreEnabled === right.apiScoreEnabled && left.apiReviewEnabled === right.apiReviewEnabled;
}

/**
 * Environment-scoped API Review settings: `platform > environment > API Review`.
 *
 * Classic keeps these on Settings → API Quality together with the v2 quality metrics. Gamma has no v2 APIs,
 * so only the API Score and API Review toggles and the manual rules are surfaced here.
 */
export function ApiReviewSettingsPage() {
    const canEdit = useHasPermission({ anyOf: ['environment-settings-u'] });
    const queryClient = useQueryClient();
    const { data: settings, isLoading, isError, error } = usePortalSettings();
    const saveMutation = useSavePortalSettings({
        successMessage: 'API Review settings saved successfully.',
        errorMessage: 'Failed to save API Review settings.',
    });
    const [localState, setLocalState] = useState<ApiReviewSettingsState>(() => buildApiReviewSettingsState(settings));
    const [savedState, setSavedState] = useState<ApiReviewSettingsState>(() => buildApiReviewSettingsState(settings));

    const isForbidden = isForbiddenApiError(isError, error);
    useForbiddenResourceRedirect({
        isForbidden,
        navItemKey: 'api-review',
        permissionPrefix: 'environment-settings-',
        redirectTo: '../applications',
    });

    const isDirty = !statesEqual(localState, savedState);
    const isDirtyRef = useRef(isDirty);
    isDirtyRef.current = isDirty;

    useEffect(() => {
        if (!settings) return;
        const next = buildApiReviewSettingsState(settings);
        setSavedState(next);
        // Don't clobber in-progress edits when a background refetch (e.g. window refocus) delivers fresh data.
        if (!isDirtyRef.current) {
            setLocalState(next);
        }
    }, [settings]);

    const readonly = useMemo(() => getApiReviewReadonlyState(settings), [settings]);

    function handleSave() {
        if (!settings || !isDirty || saveMutation.isPending) return;
        const payload = buildApiReviewSettingsSavePayload(settings, localState);
        saveMutation.mutate(payload, {
            onSuccess: () => {
                setSavedState(localState);
                // The API Score nav item (platform and API sidebars) reads `apiScore.enabled` from GET /portal.
                void queryClient.invalidateQueries({ queryKey: ENVIRONMENT_PORTAL_CONFIGURATION_QUERY_KEY });
            },
        });
    }

    return (
        <OrgSettingsFormShell
            title="API Review"
            description="Turn on API Score and API Review for this environment, and manage the manual rules reviewers check on HTTP Proxy, Message, and Kafka APIs."
            canEdit={canEdit}
            isDirty={isDirty}
            isSaving={saveMutation.isPending}
            isLoading={isLoading}
            isError={isError && !isForbidden}
            showArchitectureOverrideWarning={false}
            onSave={handleSave}
            onDiscard={() => setLocalState(savedState)}
        >
            <ApiReviewTogglesSection value={localState} disabled={!canEdit} readonly={readonly} onChange={setLocalState} />
            <QualityRulesCard />
        </OrgSettingsFormShell>
    );
}
