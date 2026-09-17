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
import type { PortalSettings } from '../../security-plan-types/services/portalSettings';
import { isPortalSettingReadonly } from '../../security-plan-types/utils/isPortalSettingReadonly';

export interface ApiReviewSettingsState {
    apiScoreEnabled: boolean;
    apiReviewEnabled: boolean;
}

export type ApiReviewSettingsKey = keyof ApiReviewSettingsState;

/** gravitee.yml keys reported in `metadata.readonly` when the system pins the value. */
export const API_REVIEW_READONLY_PROPERTY: Record<ApiReviewSettingsKey, string> = {
    apiScoreEnabled: 'api.score.enabled',
    apiReviewEnabled: 'api.review.enabled',
};

export function buildApiReviewSettingsState(settings: PortalSettings | undefined): ApiReviewSettingsState {
    return {
        apiScoreEnabled: settings?.apiScore?.enabled ?? false,
        apiReviewEnabled: settings?.apiReview?.enabled ?? false,
    };
}

export function getApiReviewReadonlyState(settings: PortalSettings | undefined): Record<ApiReviewSettingsKey, boolean> {
    return {
        apiScoreEnabled: isPortalSettingReadonly(settings, API_REVIEW_READONLY_PROPERTY.apiScoreEnabled),
        apiReviewEnabled: isPortalSettingReadonly(settings, API_REVIEW_READONLY_PROPERTY.apiReviewEnabled),
    };
}

export function buildApiReviewSettingsSavePayload(current: PortalSettings, state: ApiReviewSettingsState): PortalSettings {
    return {
        ...current,
        apiScore: { ...current.apiScore, enabled: state.apiScoreEnabled },
        apiReview: { ...current.apiReview, enabled: state.apiReviewEnabled },
    };
}
