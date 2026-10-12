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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { CreateSubscription } from './CreateSubscription';
import { useCustomApiKeyEnabled, useSharedApiKeyEnabled } from '../../../hooks/usePlanSecuritySettings';
import { useApiPlans } from '../../../hooks/useSubscriptions';
import { listApplicationApiKeySubscriptions, verifyCustomApiKey } from '../../../services/subscriptions';
import type { Application, Plan } from '../../../types/subscription';

jest.mock('../../../hooks/usePlanSecuritySettings', () => ({
    useCustomApiKeyEnabled: jest.fn(() => false),
    useSharedApiKeyEnabled: jest.fn(() => false),
}));

const mockUseApiPlans = useApiPlans as jest.Mock;
const mockCustomApiKey = useCustomApiKeyEnabled as jest.Mock;
const mockSharedApiKey = useSharedApiKeyEnabled as jest.Mock;
const mockVerify = verifyCustomApiKey as jest.Mock;
const mockListApiKeySubscriptions = listApplicationApiKeySubscriptions as jest.Mock;
const mockAppState: { current: Application } = { current: { id: 'app-1', name: 'My App', apiKeyMode: 'EXCLUSIVE' } };

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../../../services/subscriptions', () => {
    const actual = jest.requireActual('../../../services/subscriptions');
    return {
        ...actual,
        verifyCustomApiKey: jest.fn(() => Promise.resolve({ ok: true })),
        listApplicationApiKeySubscriptions: jest.fn(() => Promise.resolve([])),
    };
});

jest.mock('./ApplicationSearchList', () => ({
    ApplicationSearchList: ({ selected, onSelect }: { selected: Application | null; onSelect: (app: Application) => void }) =>
        selected ? (
            <p>Selected application: {selected.name}</p>
        ) : (
            <button type="button" onClick={() => onSelect(mockAppState.current)}>
                Pick application
            </button>
        ),
}));

beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
    Element.prototype.scrollIntoView = jest.fn();
});

jest.mock('../../../hooks/useSubscriptions', () => {
    const actual = jest.requireActual('../../../hooks/useSubscriptions');
    return { ...actual, useApiPlans: jest.fn() };
});

const API_KEY_PLAN: Plan = {
    id: 'plan-1',
    name: 'Standard',
    security: { type: 'API_KEY' },
};
const KEYLESS_PLAN: Plan = {
    id: 'plan-2',
    name: 'Open',
    security: { type: 'KEY_LESS' },
};
const APP: Application = { id: 'app-1', name: 'My App', apiKeyMode: 'EXCLUSIVE' };

function renderDialog(onConfirm = jest.fn(), plans: Plan[] = [API_KEY_PLAN, KEYLESS_PLAN], isFederated = false, open = true) {
    mockUseApiPlans.mockReturnValue({ data: plans, isLoading: false });
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const props = {
        ctx: { type: 'api' as const, entityId: 'api-1' },
        open,
        isPending: false,
        error: null,
        isFederated,
        onConfirm,
        onClose: jest.fn(),
    };
    const view = render(
        <QueryClientProvider client={queryClient}>
            <CreateSubscription {...props} />
        </QueryClientProvider>,
    );
    return {
        ...view,
        rerenderWith: (nextOpen: boolean) =>
            view.rerender(
                <QueryClientProvider client={queryClient}>
                    <CreateSubscription {...props} open={nextOpen} />
                </QueryClientProvider>,
            ),
    };
}

async function selectApiKeyPlanAndApp() {
    fireEvent.click(screen.getByRole('button', { name: 'Pick application' }));
    fireEvent.click(screen.getByRole('combobox'));
    await waitFor(() => fireEvent.click(screen.getByRole('option', { name: /standard/i })));
}

describe('CreateSubscription', () => {
    beforeEach(() => {
        mockCustomApiKey.mockReturnValue(false);
        mockSharedApiKey.mockReturnValue(false);
        mockAppState.current = APP;
        mockVerify.mockReset();
        mockVerify.mockResolvedValue({ ok: true });
        mockListApiKeySubscriptions.mockReset();
        mockListApiKeySubscriptions.mockResolvedValue([]);
    });

    it('does not show the custom API key field when the setting is disabled', async () => {
        renderDialog();
        await selectApiKeyPlanAndApp();
        await waitFor(() => expect(screen.queryByLabelText('Custom API Key')).toBeNull());
    });

    it('shows the custom API key field for an API Key plan when enabled', async () => {
        mockCustomApiKey.mockReturnValue(true);
        renderDialog();
        await selectApiKeyPlanAndApp();
        await waitFor(() => expect(screen.getByLabelText('Custom API Key')).toBeInTheDocument());
    });

    it('hides the custom API key field for federated APIs even when the setting is enabled', async () => {
        mockCustomApiKey.mockReturnValue(true);
        renderDialog(jest.fn(), [API_KEY_PLAN, KEYLESS_PLAN], true);
        await selectApiKeyPlanAndApp();
        await waitFor(() => expect(screen.queryByLabelText('Custom API Key')).toBeNull());
    });

    it('keeps the typed custom API key value', async () => {
        mockCustomApiKey.mockReturnValue(true);
        const onConfirm = jest.fn();
        renderDialog(onConfirm);
        await selectApiKeyPlanAndApp();
        await waitFor(() => fireEvent.change(screen.getByLabelText('Custom API Key'), { target: { value: 'my-custom-key' } }));
        expect(screen.getByLabelText('Custom API Key')).toHaveValue('my-custom-key');
    });

    it('blocks create and explains a custom key that fails the classic format', async () => {
        mockCustomApiKey.mockReturnValue(true);
        const onConfirm = jest.fn();
        renderDialog(onConfirm);
        await selectApiKeyPlanAndApp();
        fireEvent.change(await screen.findByLabelText('Custom API Key'), { target: { value: 'bad key' } });

        expect(screen.getByText('No special characters allowed')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Create subscription' })).toBeDisabled();
        fireEvent.click(screen.getByRole('button', { name: 'Create subscription' }));
        expect(onConfirm).not.toHaveBeenCalled();
        expect(mockVerify).not.toHaveBeenCalled();
    });

    it('blocks create until verify accepts the custom key', async () => {
        mockCustomApiKey.mockReturnValue(true);
        mockVerify.mockResolvedValue({ ok: false });
        renderDialog();
        await selectApiKeyPlanAndApp();
        fireEvent.change(await screen.findByLabelText('Custom API Key'), { target: { value: 'validkey1' } });

        expect(await screen.findByText('Key already exists with given value')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Create subscription' })).toBeDisabled();
    });

    it('still creates when the custom key is left blank', async () => {
        mockCustomApiKey.mockReturnValue(true);
        const onConfirm = jest.fn();
        renderDialog(onConfirm);
        await selectApiKeyPlanAndApp();
        await screen.findByLabelText('Custom API Key');
        fireEvent.click(screen.getByRole('button', { name: 'Create subscription' }));

        expect(onConfirm).toHaveBeenCalledWith('app-1', 'plan-1', { customApiKey: undefined, apiKeyMode: undefined });
        expect(mockVerify).not.toHaveBeenCalled();
    });

    it('keeps create disabled and hides the custom key while the API key mode lookup is in flight', async () => {
        mockCustomApiKey.mockReturnValue(true);
        mockSharedApiKey.mockReturnValue(true);
        mockAppState.current = { id: 'app-1', name: 'My App', apiKeyMode: 'UNSPECIFIED' };
        let resolveList: (rows: { api: string }[]) => void = () => undefined;
        mockListApiKeySubscriptions.mockImplementation(
            () =>
                new Promise(resolve => {
                    resolveList = resolve;
                }),
        );
        renderDialog();
        await selectApiKeyPlanAndApp();

        await waitFor(() => expect(mockListApiKeySubscriptions).toHaveBeenCalled());
        expect(screen.queryByLabelText('Custom API Key')).toBeNull();
        expect(screen.getByRole('button', { name: 'Create subscription' })).toBeDisabled();

        resolveList([]);
        expect(await screen.findByLabelText('Custom API Key')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Create subscription' })).toBeEnabled();
    });

    it('creates without a mode choice when the lookup fails', async () => {
        mockCustomApiKey.mockReturnValue(true);
        mockSharedApiKey.mockReturnValue(true);
        mockAppState.current = { id: 'app-1', name: 'My App', apiKeyMode: 'UNSPECIFIED' };
        mockListApiKeySubscriptions.mockRejectedValue(new Error('down'));
        const onConfirm = jest.fn();
        renderDialog(onConfirm);
        await selectApiKeyPlanAndApp();

        expect(await screen.findByText('Could not check existing API keys for this application.')).toBeInTheDocument();
        expect(screen.queryByLabelText('Custom API Key')).toBeNull();
        expect(screen.queryByRole('radiogroup', { name: 'API Key Mode' })).toBeNull();
        expect(screen.getByRole('button', { name: 'Create subscription' })).toBeEnabled();

        fireEvent.click(screen.getByRole('button', { name: 'Create subscription' }));
        expect(onConfirm).toHaveBeenCalledWith('app-1', 'plan-1', { customApiKey: undefined, apiKeyMode: undefined });
    });

    it('opens empty after a subscription was created and the page closed it', async () => {
        const { rerenderWith } = renderDialog();
        await selectApiKeyPlanAndApp();
        expect(screen.getByText('Selected application: My App')).toBeInTheDocument();

        rerenderWith(false);
        rerenderWith(true);

        expect(screen.queryByText(/Selected application/)).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Pick application' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Create subscription' })).toBeDisabled();
    });

    it('keeps the selection while the dialog stays open, for instance after a failed create', () => {
        const { rerenderWith } = renderDialog();
        fireEvent.click(screen.getByRole('button', { name: 'Pick application' }));

        rerenderWith(true);

        expect(screen.getByText('Selected application: My App')).toBeInTheDocument();
    });
});
