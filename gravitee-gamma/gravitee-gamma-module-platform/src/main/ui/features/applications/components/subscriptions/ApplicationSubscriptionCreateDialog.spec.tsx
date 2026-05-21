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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { ApplicationSubscriptionCreateDialog } from './ApplicationSubscriptionCreateDialog';
import {
    useApplicationApiKeySubscriptions,
    useCreateApplicationSubscription,
    useSubscribablePlans,
    useSubscriptionReferenceSearch,
} from '../../hooks/useCreateApplicationSubscription';
import { useEnvironmentPortalConfiguration } from '../../hooks/useEnvironmentPortalConfiguration';
import type { ApplicationListItem } from '../../types/application';

jest.mock('../../hooks/useCreateApplicationSubscription');
jest.mock('../../hooks/useEnvironmentPortalConfiguration');

const mockUsePortalConfig = jest.mocked(useEnvironmentPortalConfiguration);
const mockUseApiKeySubs = jest.mocked(useApplicationApiKeySubscriptions);
const mockUseSearch = jest.mocked(useSubscriptionReferenceSearch);
const mockUsePlans = jest.mocked(useSubscribablePlans);
const mockUseCreate = jest.mocked(useCreateApplicationSubscription);

const application: ApplicationListItem = {
    id: 'app-1',
    name: 'Billing',
    status: 'ACTIVE',
    type: 'SIMPLE',
    api_key_mode: 'UNSPECIFIED',
    created_at: 0,
    updated_at: 0,
};

const apiKeyPlan = {
    id: 'plan-1',
    name: 'Gold',
    security: { type: 'API_KEY' },
};

function renderDialog() {
    return render(
        <MemoryRouter>
            <ApplicationSubscriptionCreateDialog application={application} basePath="/applications/app-1" open onOpenChange={jest.fn()} />
        </MemoryRouter>,
    );
}

async function selectApiReference() {
    fireEvent.change(screen.getByLabelText(/Search an API or API Product/i), { target: { value: 'pay' } });
    fireEvent.click(await screen.findByRole('button', { name: /Payments API/i }));
}

describe('ApplicationSubscriptionCreateDialog', () => {
    const mutateAsync = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        mockUsePortalConfig.mockReturnValue({
            data: { plan: { security: { sharedApiKey: { enabled: true } } } },
        } as ReturnType<typeof useEnvironmentPortalConfiguration>);
        mockUseApiKeySubs.mockReturnValue({
            data: [{ id: 'sub-1', api: 'other-api', status: 'ACCEPTED' }],
        } as ReturnType<typeof useApplicationApiKeySubscriptions>);
        mockUseSearch.mockReturnValue({
            data: [{ type: 'API', value: { id: 'api-1', name: 'Payments API', apiVersion: 'v1' } }],
            isFetching: false,
        } as ReturnType<typeof useSubscriptionReferenceSearch>);
        mockUsePlans.mockReturnValue({
            data: { data: [apiKeyPlan] },
            isLoading: false,
        } as ReturnType<typeof useSubscribablePlans>);
        mockUseCreate.mockReturnValue({
            mutateAsync,
            isPending: false,
        } as ReturnType<typeof useCreateApplicationSubscription>);
    });

    it('shows API key mode choice and sends EXCLUSIVE in payload', async () => {
        renderDialog();
        await selectApiReference();

        expect(await screen.findByText(/this choice is permanent/i)).not.toBeNull();

        fireEvent.click(screen.getByRole('button', { name: 'API Key' }));
        fireEvent.click(screen.getByRole('button', { name: /Create subscription/i }));

        await waitFor(() =>
            expect(mutateAsync).toHaveBeenCalledWith({
                planId: 'plan-1',
                payload: { request: '', apiKeyMode: 'EXCLUSIVE' },
            }),
        );
    });

    it('sends SHARED apiKeyMode when shared mode is selected', async () => {
        renderDialog();
        await selectApiReference();

        fireEvent.click(screen.getByRole('button', { name: 'Shared API Key' }));
        fireEvent.click(screen.getByRole('button', { name: /Create subscription/i }));

        await waitFor(() =>
            expect(mutateAsync).toHaveBeenCalledWith({
                planId: 'plan-1',
                payload: { request: '', apiKeyMode: 'SHARED' },
            }),
        );
    });
});
