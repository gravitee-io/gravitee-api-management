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
import { buildApiReviewSettingsSavePayload, buildApiReviewSettingsState, getApiReviewReadonlyState } from './apiReviewSettings';
import type { PortalSettings } from '../../security-plan-types/services/portalSettings';

describe('api review settings helpers', () => {
    it('reads both toggles from the portal settings and defaults them to off', () => {
        expect(buildApiReviewSettingsState({ apiScore: { enabled: true }, apiReview: { enabled: false } })).toEqual({
            apiScoreEnabled: true,
            apiReviewEnabled: false,
        });
        expect(buildApiReviewSettingsState(undefined)).toEqual({ apiScoreEnabled: false, apiReviewEnabled: false });
        expect(buildApiReviewSettingsState({})).toEqual({ apiScoreEnabled: false, apiReviewEnabled: false });
    });

    it('marks a toggle read-only when the system pins its gravitee.yml key', () => {
        const settings: PortalSettings = { metadata: { readonly: ['api.review.enabled'] } };
        expect(getApiReviewReadonlyState(settings)).toEqual({ apiScoreEnabled: false, apiReviewEnabled: true });
        expect(getApiReviewReadonlyState(undefined)).toEqual({ apiScoreEnabled: false, apiReviewEnabled: false });
    });

    it('writes the toggles back without dropping the rest of the settings', () => {
        const current: PortalSettings = {
            company: { name: 'Acme' },
            apiScore: { enabled: false },
            apiReview: { enabled: false },
            plan: { security: { keyless: { enabled: true } } },
        };

        expect(buildApiReviewSettingsSavePayload(current, { apiScoreEnabled: true, apiReviewEnabled: true })).toEqual({
            company: { name: 'Acme' },
            apiScore: { enabled: true },
            apiReview: { enabled: true },
            plan: { security: { keyless: { enabled: true } } },
        });
    });
});
