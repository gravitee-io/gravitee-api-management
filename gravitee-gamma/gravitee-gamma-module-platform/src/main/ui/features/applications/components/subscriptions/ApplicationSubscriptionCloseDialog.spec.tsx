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
import { render, screen } from '@testing-library/react';

import { ApplicationSubscriptionCloseDialog } from './ApplicationSubscriptionCloseDialog';
import type { ApplicationSubscriptionCloseTarget } from '../../types/applicationSubscription';

const baseTarget: ApplicationSubscriptionCloseTarget = {
    id: 'sub-1',
    referenceTypeLabel: 'API',
    securityType: 'API Key',
    isSharedApiKey: false,
};

function renderDialog(subscription: ApplicationSubscriptionCloseTarget | null) {
    return render(
        <ApplicationSubscriptionCloseDialog subscription={subscription} onClose={jest.fn()} onConfirm={jest.fn()} isLoading={false} />,
    );
}

describe('ApplicationSubscriptionCloseDialog', () => {
    it('shows API product wording in the confirmation', () => {
        renderDialog({ ...baseTarget, referenceTypeLabel: 'API Product' });
        expect(screen.getByText(/consume this API product anymore/i)).not.toBeNull();
    });

    it('warns about API keys when plan is API Key and keys are not shared', () => {
        renderDialog(baseTarget);
        expect(screen.getByText(/All API keys associated with this subscription will be closed/i)).not.toBeNull();
    });

    it('omits API key warning for shared API key mode', () => {
        renderDialog({ ...baseTarget, isSharedApiKey: true });
        expect(screen.queryByText(/All API keys associated with this subscription will be closed/i)).toBeNull();
    });

    it('omits API key warning for non API Key security', () => {
        renderDialog({ ...baseTarget, securityType: 'OAuth2' });
        expect(screen.queryByText(/All API keys associated with this subscription will be closed/i)).toBeNull();
    });
});
