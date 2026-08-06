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
import { buttonHarness, inputHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { GenerateUserTokenDialog } from './GenerateUserTokenDialog';
import { notify } from '../../../shared/notify';
import { useCreateOrganizationUserToken } from '../hooks/useUserMutations';

jest.mock('../utils/userTokenDisplay', () => ({
    ...jest.requireActual('../utils/userTokenDisplay'),
    buildTokenUsageExample: jest
        .fn()
        .mockResolvedValue(
            'curl -H "Authorization: Bearer generated-token" "http://localhost:8083/management/organizations/DEFAULT/environments/DEFAULT"',
        ),
}));

jest.mock('../hooks/useUserMutations', () => ({
    useCreateOrganizationUserToken: jest.fn(),
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

const mockUseCreateOrganizationUserToken = jest.mocked(useCreateOrganizationUserToken);

function renderDialog(overrides: Partial<Parameters<typeof GenerateUserTokenDialog>[0]> = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    return renderWithGraphene(
        <QueryClientProvider client={queryClient}>
            <GenerateUserTokenDialog open userId="user-1" environmentId="DEFAULT" onOpenChange={jest.fn()} {...overrides} />
        </QueryClientProvider>,
    );
}

describe('GenerateUserTokenDialog', () => {
    beforeEach(() => {
        mockUseCreateOrganizationUserToken.mockReturnValue({
            mutate: jest.fn(),
            isPending: false,
        } as unknown as ReturnType<typeof useCreateOrganizationUserToken>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('submits a token name and shows the generated token with usage example', async () => {
        const mutate = jest.fn((_payload, options?: { onSuccess?: (token: unknown) => void }) =>
            options?.onSuccess?.({
                id: 'token-1',
                name: 'CI token',
                token: 'generated-token',
                created_at: Date.now(),
            }),
        );
        mockUseCreateOrganizationUserToken.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useCreateOrganizationUserToken>);

        renderDialog();

        await inputHarness({ name: /name/i }).type('CI token');
        await buttonHarness({ name: 'Generate' }).click();

        await waitFor(() => expect(mutate).toHaveBeenCalledWith({ name: 'CI token' }, expect.any(Object)));
        expect(await screen.findByText('generated-token')).toBeTruthy();
        expect(await screen.findByText(/curl -H "Authorization: Bearer generated-token"/)).toBeTruthy();
        expect(notify.success).toHaveBeenCalledWith('Token successfully created!');
    });

    it('keeps generate disabled until the name is valid', async () => {
        renderDialog();

        const generateButton = () => screen.getByRole('button', { name: 'Generate' }) as HTMLButtonElement;
        expect(generateButton().disabled).toBe(true);

        await inputHarness({ name: /name/i }).type('a');
        expect(generateButton().disabled).toBe(true);

        await inputHarness({ name: /name/i }).type('b');
        expect(generateButton().disabled).toBe(false);
    });

    it('shows duplicate token errors inline instead of a toast', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn((_payload, options?: { onError?: (error: Error) => void }) =>
            options?.onError?.(new Error('A token with the name [CI token] already exists.')),
        );
        mockUseCreateOrganizationUserToken.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useCreateOrganizationUserToken>);

        renderDialog();

        await inputHarness({ name: /name/i }).type('CI token');
        await user.click(screen.getByRole('button', { name: 'Generate' }));

        expect(await screen.findByText('A token with the name [CI token] already exists.')).toBeTruthy();
        expect(notify.error).not.toHaveBeenCalled();
    });

    it('shows the copy warning only once in the success state', async () => {
        const user = userEvent.setup();
        const mutate = jest.fn((_payload, options?: { onSuccess?: (token: unknown) => void }) =>
            options?.onSuccess?.({
                id: 'token-1',
                name: 'CI token',
                token: 'generated-token',
                created_at: Date.now(),
            }),
        );
        mockUseCreateOrganizationUserToken.mockReturnValue({
            mutate,
            isPending: false,
        } as unknown as ReturnType<typeof useCreateOrganizationUserToken>);

        renderDialog();

        await inputHarness({ name: /name/i }).type('CI token');
        await user.click(screen.getByRole('button', { name: 'Generate' }));

        await screen.findByText('generated-token');
        expect(screen.getAllByText(/Make sure to copy your new personal access token now/i)).toHaveLength(1);
    });
});
