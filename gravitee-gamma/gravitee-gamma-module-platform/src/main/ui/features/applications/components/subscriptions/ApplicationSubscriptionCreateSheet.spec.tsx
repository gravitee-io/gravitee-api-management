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
import { useState } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { ApplicationSubscriptionCreateSheet } from './ApplicationSubscriptionCreateSheet';
import {
    useApplicationApiKeySubscriptions,
    useCreateApplicationSubscription,
    useSubscribablePlans,
    useSubscriptionReferenceSearch,
} from '../../hooks/useCreateApplicationSubscription';
import { useEnvironmentPortalConfiguration } from '../../hooks/useEnvironmentPortalConfiguration';
import type { ApplicationListItem } from '../../types/application';
import { querySheetHeading } from '../test/sheetSpecHelpers';

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

function renderSheet(open = true) {
    const onOpenChange = jest.fn();
    render(
        <MemoryRouter>
            <ApplicationSubscriptionCreateSheet
                application={application}
                basePath="/applications/app-1"
                open={open}
                onOpenChange={onOpenChange}
            />
        </MemoryRouter>,
    );
    return { onOpenChange };
}

async function selectApiReference() {
    fireEvent.change(screen.getByLabelText(/Search an API or API Product/i), { target: { value: 'pay' } });
    fireEvent.click(await screen.findByRole('button', { name: /Payments API/i }));
}

describe('ApplicationSubscriptionCreateSheet', () => {
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

    it('does not show sheet content when closed', () => {
        renderSheet(false);
        expect(querySheetHeading('Create a subscription')).toBeNull();
    });

    it('invokes onOpenChange(false) when Cancel is clicked', () => {
        const { onOpenChange } = renderSheet(true);
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('resets form state when the sheet closes and reopens', async () => {
        function ControlledSheet() {
            const [open, setOpen] = useState(true);
            return (
                <>
                    <button type="button" onClick={() => setOpen(true)}>
                        Reopen
                    </button>
                    <ApplicationSubscriptionCreateSheet
                        application={application}
                        basePath="/applications/app-1"
                        open={open}
                        onOpenChange={setOpen}
                    />
                </>
            );
        }

        render(
            <MemoryRouter>
                <ControlledSheet />
            </MemoryRouter>,
        );

        const searchInput = screen.getByLabelText(/Search an API or API Product/i) as HTMLInputElement;
        fireEvent.change(searchInput, { target: { value: 'pay' } });
        await selectApiReference();
        expect(screen.getByText(/Select a plan to subscribe/i)).not.toBeNull();

        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        fireEvent.click(screen.getByRole('button', { name: 'Reopen' }));

        expect((screen.getByLabelText(/Search an API or API Product/i) as HTMLInputElement).value).toBe('');
        expect(screen.queryByText(/Select a plan to subscribe/i)).toBeNull();
    });

    it('shows API key mode choice and sends EXCLUSIVE in payload', async () => {
        renderSheet();
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

    it('caps API search results list height with vertical scroll', async () => {
        mockUseSearch.mockReturnValue({
            data: [
                { type: 'API', value: { id: 'api-1', name: 'Payments API', apiVersion: 'v1' } },
                { type: 'API', value: { id: 'api-2', name: 'Orders API', apiVersion: 'v1' } },
            ],
            isFetching: false,
        } as ReturnType<typeof useSubscriptionReferenceSearch>);

        renderSheet();
        fireEvent.change(screen.getByLabelText(/Search an API or API Product/i), { target: { value: 'api' } });

        const list = await screen.findByTestId('subscription-reference-search-results');
        expect(list.style.maxHeight).toBe('180px');
        expect(list.style.overflowY).toBe('auto');
    });

    it('sends SHARED apiKeyMode when shared mode is selected', async () => {
        renderSheet();
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
