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
import { TooltipProvider } from '@gravitee/graphene-core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';

import { SubscriptionApiKeysCard } from './SubscriptionApiKeysCard';
import { toDatetimeLocalValue } from '../../../../../../shared/time';
import { useCustomApiKeyEnabled } from '../../../../hooks/usePlanSecuritySettings';
import { useApiKeyList, useExpireApiKey } from '../../../../hooks/useSubscriptionApiKeys';
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
    useApiKeyList: jest.fn(() => ({ data: { data: [], pagination: { totalCount: 0 } }, isLoading: false })),
    useRenewApiKey: jest.fn(() => ({ mutate: jest.fn(), isPending: false })),
    useRevokeApiKey: jest.fn(() => ({ mutate: jest.fn(), isPending: false })),
    useExpireApiKey: jest.fn(() => ({ mutate: jest.fn(), isPending: false })),
    useReactivateApiKey: jest.fn(() => ({ mutate: jest.fn(), isPending: false })),
}));

jest.mock('../../../../services/subscriptions', () => ({
    verifyCustomApiKey: jest.fn(() => Promise.resolve({ ok: true })),
}));

const mockVerify = verifyCustomApiKey as jest.Mock;
const mockCustomApiKey = useCustomApiKeyEnabled as jest.Mock;
const mockUseApiKeyList = useApiKeyList as jest.Mock;
const mockUseExpireApiKey = useExpireApiKey as jest.Mock;

const EMPTY_KEYS = { data: { data: [], pagination: { totalCount: 0 } }, isLoading: false };

function setDatetimeValidity(input: HTMLInputElement, badInput: boolean) {
    Object.defineProperty(input, 'validity', {
        configurable: true,
        get: () => ({ badInput }) as ValidityState,
    });
}

function renderCard() {
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <TooltipProvider>
                <SubscriptionApiKeysCard ctx={{ type: 'api', entityId: 'api-1' }} subscription={SUBSCRIPTION} canUpdate />
            </TooltipProvider>
        </QueryClientProvider>,
    );
}

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

describe('SubscriptionApiKeysCard expiry', () => {
    const expireAt = '2026-12-15T18:30:00.000Z';
    let expireMutate: jest.Mock;

    beforeEach(() => {
        jest.useFakeTimers();
        jest.setSystemTime(new Date('2026-06-15T10:05:30.000Z'));
        expireMutate = jest.fn();
        mockUseExpireApiKey.mockReturnValue({ mutate: expireMutate, isPending: false });
        mockUseApiKeyList.mockReturnValue({
            data: {
                data: [{ id: 'key-1', key: 'abcd1234', createdAt: '2026-06-01T00:00:00.000Z', expireAt }],
                pagination: { totalCount: 1 },
            },
            isLoading: false,
        });
    });

    afterEach(() => {
        jest.useRealTimers();
        mockUseApiKeyList.mockReturnValue(EMPTY_KEYS);
    });

    function openExpiry() {
        renderCard();
        fireEvent.click(screen.getByRole('button', { name: /set expiry date/i }));
        return screen.getByLabelText(/set expiry date/i) as HTMLInputElement;
    }

    it('prefills the local expiry date and time and keeps set disabled until that value changes', () => {
        const input = openExpiry();
        const prefilled = toDatetimeLocalValue(new Date(expireAt));

        expect(input.type).toBe('datetime-local');
        expect(input.value).toBe(prefilled);
        expect(input.min).toBe(toDatetimeLocalValue(new Date()));
        expect(screen.getByRole('button', { name: 'Set' })).toBeDisabled();

        fireEvent.change(input, { target: { value: '2099-12-01T08:15' } });
        expect(screen.getByRole('button', { name: 'Set' })).toBeEnabled();

        fireEvent.change(input, { target: { value: prefilled } });
        expect(screen.getByRole('button', { name: 'Set' })).toBeDisabled();
    });

    it('sends the chosen local date and time as an ISO instant', () => {
        const input = openExpiry();
        const chosen = '2099-12-01T08:15';

        fireEvent.change(input, { target: { value: chosen } });
        fireEvent.click(screen.getByRole('button', { name: 'Set' }));

        expect(expireMutate).toHaveBeenCalledWith({ apiKeyId: 'key-1', expireAt: new Date(chosen).toISOString() }, expect.any(Object));
    });

    it('blocks a date and time earlier than the current minute', () => {
        const input = openExpiry();

        fireEvent.change(input, { target: { value: '2020-01-01T00:00' } });

        expect(screen.getByRole('button', { name: 'Set' })).toBeDisabled();
        expect(screen.getByText(/must not be earlier than the current minute/i)).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Set' }));
        expect(expireMutate).not.toHaveBeenCalled();
    });

    it('blocks a date entered without a time', () => {
        const input = openExpiry();

        setDatetimeValidity(input, true);
        fireEvent.blur(input);

        expect(screen.getByText('Enter both a date and a time.')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Set' })).toBeDisabled();
        fireEvent.click(screen.getByRole('button', { name: 'Set' }));
        expect(expireMutate).not.toHaveBeenCalled();
    });
});
