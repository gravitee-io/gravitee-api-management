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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';

import { SubscriptionApiKeysCard } from './SubscriptionApiKeysCard';
import { useCustomApiKeyEnabled } from '../../../../hooks/usePlanSecuritySettings';
import { verifyCustomApiKey } from '../../../../services/subscriptions';
import type { Subscription } from '../../../../types/subscription';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('../../../../hooks/usePlanSecuritySettings', () => ({
    useCustomApiKeyEnabled: jest.fn(() => true),
}));

jest.mock('../../../../hooks/useSubscriptionApiKeys', () => ({
    useApiKeyList: () => ({ data: { data: [], pagination: { totalCount: 0 } }, isLoading: false }),
    useRenewApiKey: () => ({ mutate: jest.fn(), isPending: false }),
    useRevokeApiKey: () => ({ mutate: jest.fn(), isPending: false }),
    useExpireApiKey: () => ({ mutate: jest.fn(), isPending: false }),
    useReactivateApiKey: () => ({ mutate: jest.fn(), isPending: false }),
}));

jest.mock('../../../../services/subscriptions', () => ({
    verifyCustomApiKey: jest.fn(() => Promise.resolve({ ok: true })),
}));

const mockVerify = verifyCustomApiKey as jest.Mock;
const mockCustomApiKey = useCustomApiKeyEnabled as jest.Mock;

const SUBSCRIPTION: Subscription = {
    id: 'sub-1',
    status: 'ACCEPTED',
    plan: { id: 'plan-1', name: 'API Key', security: { type: 'API_KEY' } },
    application: { id: 'app-1', name: 'App', apiKeyMode: 'EXCLUSIVE' },
};

beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
    Element.prototype.hasPointerCapture = jest.fn();
    Element.prototype.setPointerCapture = jest.fn();
    Element.prototype.releasePointerCapture = jest.fn();
    Element.prototype.scrollIntoView = jest.fn();
});

describe('SubscriptionApiKeysCard renew', () => {
    beforeEach(() => {
        mockCustomApiKey.mockReturnValue(true);
        mockVerify.mockResolvedValue({ ok: true });
    });

    it('blocks renew when the custom key is not valid', () => {
        render(
            <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
                <SubscriptionApiKeysCard ctx={{ type: 'api', entityId: 'api-1' }} subscription={SUBSCRIPTION} canUpdate />
            </QueryClientProvider>,
        );

        fireEvent.click(screen.getByRole('button', { name: 'Renew' }));
        fireEvent.change(screen.getByLabelText('Custom API Key'), { target: { value: '#' } });

        expect(screen.getByText('No special characters allowed')).toBeInTheDocument();
        const renewButtons = screen.getAllByRole('button', { name: 'Renew' });
        expect(renewButtons.some(button => (button as HTMLButtonElement).disabled)).toBe(true);
    });

    it('clears the custom key when renew is cancelled', () => {
        render(
            <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
                <SubscriptionApiKeysCard ctx={{ type: 'api', entityId: 'api-1' }} subscription={SUBSCRIPTION} canUpdate />
            </QueryClientProvider>,
        );

        fireEvent.click(screen.getByRole('button', { name: 'Renew' }));
        fireEvent.change(screen.getByLabelText('Custom API Key'), { target: { value: 'validkey1' } });
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        fireEvent.click(screen.getByRole('button', { name: 'Renew' }));

        expect(screen.getByLabelText('Custom API Key')).toHaveValue('');
    });
});
