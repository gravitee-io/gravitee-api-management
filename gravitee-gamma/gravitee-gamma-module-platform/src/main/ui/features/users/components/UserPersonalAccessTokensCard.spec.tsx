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
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { UserPersonalAccessTokensCard } from './UserPersonalAccessTokensCard';
import { notify } from '../../../shared/notify';
import { useOrganizationUserTokens } from '../hooks/useOrganizationUser';
import { useCreateOrganizationUserToken, useRevokeOrganizationUserToken } from '../hooks/useUserMutations';

jest.mock('../hooks/useOrganizationUser', () => ({
    useOrganizationUserTokens: jest.fn(),
}));

jest.mock('../hooks/useUserMutations', () => ({
    useCreateOrganizationUserToken: jest.fn(),
    useRevokeOrganizationUserToken: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: jest.fn(),
            removeListener: jest.fn(),
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn(),
        })),
    });
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    };
});

const mockUseOrganizationUserTokens = jest.mocked(useOrganizationUserTokens);
const mockUseCreateOrganizationUserToken = jest.mocked(useCreateOrganizationUserToken);
const mockUseRevokeOrganizationUserToken = jest.mocked(useRevokeOrganizationUserToken);

function renderCard(overrides: Partial<Parameters<typeof UserPersonalAccessTokensCard>[0]> = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <UserPersonalAccessTokensCard userId="user-1" environmentId="DEFAULT" canGenerate canRevoke {...overrides} />
        </QueryClientProvider>,
    );
}

describe('UserPersonalAccessTokensCard', () => {
    beforeEach(() => {
        mockUseOrganizationUserTokens.mockReturnValue({
            data: [],
            isLoading: false,
        } as ReturnType<typeof useOrganizationUserTokens>);
        mockUseCreateOrganizationUserToken.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useCreateOrganizationUserToken>);
        mockUseRevokeOrganizationUserToken.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useRevokeOrganizationUserToken>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('shows the empty state CTA without a header button when no tokens exist', async () => {
        const user = userEvent.setup();
        renderCard();

        expect(await screen.findByText('No personal access tokens')).toBeTruthy();
        expect(screen.getByRole('button', { name: 'Generate Token' })).toBeTruthy();
        expect(screen.queryAllByRole('button', { name: 'Generate Token' })).toHaveLength(1);

        await user.click(screen.getByRole('button', { name: 'Generate Token' }));
        expect(await screen.findByRole('dialog')).toBeTruthy();
        expect(screen.getByRole('dialog').textContent).toMatch(/Generate a token/i);
    });

    it('shows the header generate button and table when tokens exist', async () => {
        mockUseOrganizationUserTokens.mockReturnValue({
            data: [{ id: 'token-1', name: 'My token', created_at: 1630373735403, last_use_at: 1631017105654 }],
            isLoading: false,
        } as ReturnType<typeof useOrganizationUserTokens>);

        renderCard();

        expect(await screen.findByText('My token')).toBeTruthy();
        expect(screen.getByText('1 active token')).toBeTruthy();
        expect(screen.getAllByRole('button', { name: 'Generate Token' })).toHaveLength(1);
        expect(screen.getByRole('button', { name: 'Revoke token My token' })).toBeTruthy();
    });

    it('hides generate and revoke actions without the corresponding permissions', async () => {
        mockUseOrganizationUserTokens.mockReturnValue({
            data: [{ id: 'token-1', name: 'My token', created_at: 1630373735403 }],
            isLoading: false,
        } as ReturnType<typeof useOrganizationUserTokens>);

        renderCard({ canGenerate: false, canRevoke: false });

        expect(await screen.findByText('My token')).toBeTruthy();
        expect(screen.queryByRole('button', { name: 'Generate Token' })).toBeNull();
        expect(screen.queryByRole('button', { name: 'Revoke token My token' })).toBeNull();
    });

    it('revokes a token after confirmation', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn((_tokenId: string, options?: { onSuccess?: () => void }) => options?.onSuccess?.());
        mockUseOrganizationUserTokens.mockReturnValue({
            data: [{ id: 'token-1', name: 'My token', created_at: 1630373735403 }],
            isLoading: false,
        } as ReturnType<typeof useOrganizationUserTokens>);
        mockUseRevokeOrganizationUserToken.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useRevokeOrganizationUserToken>);

        renderCard({ canGenerate: false, canRevoke: true });

        await user.click(await screen.findByRole('button', { name: 'Revoke token My token' }));
        const dialog = await screen.findByRole('dialog');
        expect(dialog.textContent).toMatch(/revoke the token/i);
        expect(dialog.textContent).toMatch(/My token/);

        await user.click(within(dialog).getByRole('button', { name: 'Revoke' }));

        await waitFor(() => expect(mutate).toHaveBeenCalledWith('token-1', expect.any(Object)));
        expect(notify.success).toHaveBeenCalledWith('Token successfully deleted!');
    });

    it('shows a loading skeleton while tokens are loading', () => {
        mockUseOrganizationUserTokens.mockReturnValue({
            data: undefined,
            isLoading: true,
        } as ReturnType<typeof useOrganizationUserTokens>);

        const { container } = renderCard();

        expect(container.querySelector('.animate-pulse')).toBeTruthy();
    });
});
