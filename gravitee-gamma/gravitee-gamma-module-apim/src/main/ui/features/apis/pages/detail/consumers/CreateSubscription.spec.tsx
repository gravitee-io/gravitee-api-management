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
import type { Application, Plan } from '../../../types/subscription';

jest.mock('../../../hooks/usePlanSecuritySettings', () => ({
    useCustomApiKeyEnabled: jest.fn(() => false),
    useSharedApiKeyEnabled: jest.fn(() => false),
}));

const mockUseApiPlans = useApiPlans as jest.Mock;
const mockCustomApiKey = useCustomApiKeyEnabled as jest.Mock;
const mockSharedApiKey = useSharedApiKeyEnabled as jest.Mock;

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('./ApplicationSearchList', () => ({
    ApplicationSearchList: ({
        selected,
        onSelect,
    }: {
        selected: Application | null;
        onSelect: (app: Application) => void;
    }) => (
        <button type="button" onClick={() => onSelect(APP)}>
            {selected ? selected.name : 'Pick application'}
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

function renderDialog(onConfirm = jest.fn(), plans: Plan[] = [API_KEY_PLAN, KEYLESS_PLAN], isFederated = false) {
    mockUseApiPlans.mockReturnValue({ data: plans, isLoading: false });
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <CreateSubscription
                ctx={{ type: 'api', entityId: 'api-1' }}
                open
                isPending={false}
                error={null}
                isFederated={isFederated}
                onConfirm={onConfirm}
                onClose={jest.fn()}
            />
        </QueryClientProvider>,
    );
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
});
