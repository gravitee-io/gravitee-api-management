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
import { shouldShowApiKeyModeChoice } from './createSubscriptionApiKeyMode';

describe('shouldShowApiKeyModeChoice', () => {
    const ctx = { type: 'api' as const, entityId: 'api-1' };

    it('shows the choice when the app is unspecified and has another API-key subscription', () => {
        expect(
            shouldShowApiKeyModeChoice({
                applicationApiKeyMode: 'UNSPECIFIED',
                isApiKeyPlan: true,
                canUseSharedApiKeys: true,
                isFederated: false,
                ctx,
                apiKeySubscriptions: [{ api: 'other-api' }],
            }),
        ).toBe(true);
    });

    it('hides the choice when the only existing subscription is for this API', () => {
        expect(
            shouldShowApiKeyModeChoice({
                applicationApiKeyMode: 'UNSPECIFIED',
                isApiKeyPlan: true,
                canUseSharedApiKeys: true,
                isFederated: false,
                ctx,
                apiKeySubscriptions: [{ api: 'api-1' }],
            }),
        ).toBe(false);
    });

    it('hides the choice for federated APIs', () => {
        expect(
            shouldShowApiKeyModeChoice({
                applicationApiKeyMode: 'UNSPECIFIED',
                isApiKeyPlan: true,
                canUseSharedApiKeys: true,
                isFederated: true,
                ctx,
                apiKeySubscriptions: [{ api: 'other-api' }],
            }),
        ).toBe(false);
    });
});
