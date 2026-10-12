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
import { getEnvironmentPortalConfiguration, getEnvironmentPortalSettings } from '../../../../settings/services/portalSettings';
import { useApiPlans } from '../../../hooks/useSubscriptions';
import type { Application, Plan } from '../../../types/subscription';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../../../../settings/services/portalSettings', () => ({
    getEnvironmentPortalConfiguration: jest.fn(),
    getEnvironmentPortalSettings: jest.fn(),
}));

jest.mock('../../../hooks/useSubscriptions', () => {
    const actual = jest.requireActual('../../../hooks/useSubscriptions');
    return { ...actual, useApiPlans: jest.fn() };
});

jest.mock('./ApplicationSearchList', () => ({
    ApplicationSearchList: ({ selected, onSelect }: { selected: Application | null; onSelect: (app: Application) => void }) => (
        <button type="button" onClick={() => onSelect({ id: 'app-1', name: 'My App', apiKeyMode: 'EXCLUSIVE' })}>
            {selected ? selected.name : 'Pick application'}
        </button>
    ),
}));

const mockUseApiPlans = useApiPlans as jest.Mock;
const mockGetPortal = getEnvironmentPortalConfiguration as jest.Mock;
const mockGetSettings = getEnvironmentPortalSettings as jest.Mock;

const API_KEY_PLAN: Plan = { id: 'plan-1', name: 'Standard', security: { type: 'API_KEY' } };

beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
    Element.prototype.scrollIntoView = jest.fn();
});

it('shows the custom API key field from GET /portal when GET /settings is forbidden', async () => {
    mockGetSettings.mockRejectedValue(new Error('403'));
    mockGetPortal.mockResolvedValue({ plan: { security: { customApiKey: { enabled: true } } } });
    mockUseApiPlans.mockReturnValue({ data: [API_KEY_PLAN], isLoading: false });

    render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <CreateSubscription
                ctx={{ type: 'api', entityId: 'api-1' }}
                open
                isPending={false}
                error={null}
                onConfirm={jest.fn()}
                onClose={jest.fn()}
            />
        </QueryClientProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Pick application' }));
    fireEvent.click(screen.getByRole('combobox'));
    await waitFor(() => fireEvent.click(screen.getByRole('option', { name: /standard/i })));

    expect(await screen.findByLabelText('Custom API Key')).toBeInTheDocument();
    expect(mockGetSettings).not.toHaveBeenCalled();
    expect(mockGetPortal).toHaveBeenCalledWith('DEFAULT');
});
