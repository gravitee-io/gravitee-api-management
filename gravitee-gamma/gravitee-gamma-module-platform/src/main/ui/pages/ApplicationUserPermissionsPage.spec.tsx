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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApplicationUserPermissionsPage } from './ApplicationUserPermissionsPage';
import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';
import { useApplicationGroupMembers } from '../features/applications/hooks/useApplicationGroupMembers';
import {
    useApplicationAssociatedGroups,
    useApplicationMembers,
    useApplicationRoles,
    useEnvironmentGroups,
} from '../features/applications/hooks/useApplicationMembers';
import { addApplicationMember } from '../features/applications/services/applicationMembers';
import { applicationMemberKeys } from '../features/applications/utils/queryKeys';
import { notify } from '../shared/notify';
import { searchUsers } from '../shared/services/userSearch';
import type { SearchableUser } from '../shared/types/userSearch';

jest.mock('../features/applications/context/ApplicationDetailContext');
jest.mock('../features/applications/hooks/useApplicationMembers');
jest.mock('../features/applications/hooks/useApplicationGroupMembers');
jest.mock('../features/applications/services/applicationMembers');
jest.mock('../shared/services/userSearch');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn() },
}));

const ENV_ID = 'DEFAULT';
const APPLICATION_ID = 'app-1';
const ALICE: SearchableUser = { id: 'user-alice', reference: 'ref-alice', displayName: 'Alice Martin', email: 'alice@example.com' };
const BOB: SearchableUser = { id: 'user-bob', reference: 'ref-bob', displayName: 'Bob Reader', email: 'bob@example.com' };

const mockAddApplicationMember = jest.mocked(addApplicationMember);

type UserEvent = ReturnType<typeof userEvent.setup>;

function givenApplicationWithNoDirectMembers() {
    jest.mocked(useApplicationDetailContext).mockReturnValue({
        application: { id: APPLICATION_ID, name: 'Billing', status: 'ACTIVE', type: 'SIMPLE', created_at: 0, updated_at: 0 },
        isLoading: false,
        permissionsReady: true,
        refetchPermissions: jest.fn(),
    } as unknown as ReturnType<typeof useApplicationDetailContext>);
    jest.mocked(useApplicationMembers).mockReturnValue({ data: [], isLoading: false, isError: false } as unknown as ReturnType<
        typeof useApplicationMembers
    >);
    jest.mocked(useApplicationRoles).mockReturnValue({ data: [{ name: 'USER' }] } as unknown as ReturnType<typeof useApplicationRoles>);
    jest.mocked(useEnvironmentGroups).mockReturnValue({ data: { data: [] } } as unknown as ReturnType<typeof useEnvironmentGroups>);
    jest.mocked(useApplicationAssociatedGroups).mockReturnValue({ data: [], isLoading: false } as unknown as ReturnType<
        typeof useApplicationAssociatedGroups
    >);
    jest.mocked(useApplicationGroupMembers).mockReturnValue({ views: [], isLoading: false } as unknown as ReturnType<
        typeof useApplicationGroupMembers
    >);
    jest.mocked(searchUsers).mockImplementation(async query => [ALICE, BOB].filter(u => u.displayName.startsWith(query)));
}

function givenAddingMemberResolvesFor(succeedingUserIds: string[]) {
    mockAddApplicationMember.mockImplementation(async (_envId, _applicationId, member) => {
        if (succeedingUserIds.includes(member.id)) {
            return undefined as unknown as Awaited<ReturnType<typeof addApplicationMember>>;
        }
        throw new Error('Conflict');
    });
}

function renderPage() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    const invalidateQueries = jest.spyOn(queryClient, 'invalidateQueries');
    render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[`/applications/${APPLICATION_ID}/user-permissions`]}>
                <Routes>
                    <Route path="/applications/:applicationId/user-permissions" element={<ApplicationUserPermissionsPage />} />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>,
    );
    return { invalidateQueries };
}

async function selectUser(user: UserEvent, query: string, displayName: string) {
    await user.type(screen.getByPlaceholderText('Search a user by name or email…'), query);
    await user.click(await screen.findByRole('button', { name: new RegExp(displayName) }));
}

async function addAliceAndBob(user: UserEvent) {
    await user.click(screen.getByRole('button', { name: 'Add members' }));
    await selectUser(user, 'Al', ALICE.displayName);
    await selectUser(user, 'Bo', BOB.displayName);
    await user.click(screen.getByRole('button', { name: 'Add 2 members' }));
}

describe('ApplicationUserPermissionsPage', () => {
    beforeAll(() => {
        Element.prototype.hasPointerCapture = jest.fn();
        Element.prototype.setPointerCapture = jest.fn();
        Element.prototype.releasePointerCapture = jest.fn();
        Element.prototype.scrollIntoView = jest.fn();
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    beforeEach(() => {
        givenApplicationWithNoDirectMembers();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('adding members', () => {
        it('confirms the save, refreshes the member list and closes the sheet when every user is added', async () => {
            givenAddingMemberResolvesFor([ALICE.id!, BOB.id!]);
            const user = userEvent.setup();
            const { invalidateQueries } = renderPage();

            await addAliceAndBob(user);

            await waitFor(() => expect(notify.success).toHaveBeenCalledWith('Changes successfully saved!'));
            expect(notify.error).not.toHaveBeenCalled();
            expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: applicationMemberKeys.list(ENV_ID, APPLICATION_ID) });
            await waitFor(() => expect(screen.queryByRole('heading', { name: 'Add Members' })).not.toBeInTheDocument());
        });

        it('reports the failed user, refreshes the member list and keeps the sheet open when only some users are added', async () => {
            givenAddingMemberResolvesFor([ALICE.id!]);
            const user = userEvent.setup();
            const { invalidateQueries } = renderPage();

            await addAliceAndBob(user);

            await waitFor(() => expect(notify.error).toHaveBeenCalledTimes(1));
            expect(jest.mocked(notify.error).mock.calls[0]![0]).toEqual(
                expect.objectContaining({ message: 'Added 1 of 2 members. Failed to add: Bob Reader (Conflict).' }),
            );
            expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: applicationMemberKeys.list(ENV_ID, APPLICATION_ID) });
            expect(notify.success).not.toHaveBeenCalled();
            expect(screen.getByRole('heading', { name: 'Add Members' })).toBeInTheDocument();
        });

        it('does not refresh the member list when no user is added', async () => {
            givenAddingMemberResolvesFor([]);
            const user = userEvent.setup();
            const { invalidateQueries } = renderPage();

            await addAliceAndBob(user);

            await waitFor(() => expect(notify.error).toHaveBeenCalledTimes(1));
            expect(invalidateQueries).not.toHaveBeenCalledWith({ queryKey: applicationMemberKeys.list(ENV_ID, APPLICATION_ID) });
            expect(screen.getByRole('heading', { name: 'Add Members' })).toBeInTheDocument();
        });
    });
});
