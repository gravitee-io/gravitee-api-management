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
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';

import { SharedPolicyGroupHistoryPage } from './SharedPolicyGroupHistoryPage';
import { useRestoreSharedPolicyGroup } from '../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations';
import { useSharedPolicyGroupHistories } from '../features/shared-policy-groups/hooks/useSharedPolicyGroups';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';
import { useHasEnvironmentPermission } from '../shared/hooks/useEnvironmentPermissions';
import { notify } from '../shared/notify';

jest.mock('../shared/hooks/useEnvironmentPermissions');
jest.mock('../features/shared-policy-groups/hooks/useSharedPolicyGroups');
jest.mock('../features/shared-policy-groups/hooks/useSharedPolicyGroupMutations');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));
jest.mock('../features/shared-policy-groups/components/SharedPolicyGroupHistoryDetailsDialog', () => ({
    SharedPolicyGroupHistoryDetailsDialog: ({
        sharedPolicyGroup,
        canRestore,
        onRestore,
    }: {
        sharedPolicyGroup?: SharedPolicyGroup;
        canRestore: boolean;
        onRestore: (sharedPolicyGroup: SharedPolicyGroup) => void;
    }) =>
        sharedPolicyGroup ? (
            <div role="dialog" aria-label={`Version ${sharedPolicyGroup.version} details`}>
                <span>Read-only version state</span>
                {canRestore ? (
                    <button type="button" onClick={() => onRestore(sharedPolicyGroup)}>
                        Restore version
                    </button>
                ) : null}
            </div>
        ) : null,
}));

const CURRENT: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    apiType: 'PROXY',
    phase: 'REQUEST',
    lifecycleState: 'PENDING',
    version: 3,
};

const HISTORIES: SharedPolicyGroup[] = [
    { ...CURRENT, version: 2, lifecycleState: 'DEPLOYED', deployedAt: '2026-08-20T10:00:00.000Z' },
    { ...CURRENT, version: 1, name: 'Original Auth Bundle', lifecycleState: 'DEPLOYED', deployedAt: '2026-08-19T10:00:00.000Z' },
];

const mockUseHasEnvironmentPermission = jest.mocked(useHasEnvironmentPermission);
const mockUseSharedPolicyGroupHistories = jest.mocked(useSharedPolicyGroupHistories);
const mockUseRestoreSharedPolicyGroup = jest.mocked(useRestoreSharedPolicyGroup);

function renderPage(sharedPolicyGroup: SharedPolicyGroup = CURRENT) {
    return render(
        <MemoryRouter initialEntries={['/shared-policy-groups/spg-1/history']}>
            <Routes>
                <Route path="shared-policy-groups/:sharedPolicyGroupId" element={<Outlet context={sharedPolicyGroup} />}>
                    <Route path="history" element={<SharedPolicyGroupHistoryPage />} />
                    <Route path="studio" element={<div>Studio destination</div>} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('SharedPolicyGroupHistoryPage', () => {
    const restoreAsync = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
        mockUseHasEnvironmentPermission.mockReturnValue(true);
        mockUseSharedPolicyGroupHistories.mockReturnValue({
            data: {
                data: HISTORIES,
                pagination: { page: 1, perPage: 25, pageCount: 1, pageItemsCount: 2, totalCount: 2 },
            },
            isLoading: false,
            isError: false,
        } as unknown as ReturnType<typeof useSharedPolicyGroupHistories>);
        mockUseRestoreSharedPolicyGroup.mockReturnValue({
            mutateAsync: restoreAsync,
            isPending: false,
        } as unknown as ReturnType<typeof useRestoreSharedPolicyGroup>);
        restoreAsync.mockResolvedValue(CURRENT);
    });

    it('lists historical versions and opens their JSON source', () => {
        renderPage();

        expect(screen.getByText('Original Auth Bundle')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'Show JSON for version 1' }));

        expect(screen.getByRole('dialog', { name: 'Version 1 JSON Source' })).not.toBeNull();
        expect(screen.getByText(/"version": 1/)).not.toBeNull();
    });

    it('compares two selected versions and a selected version with pending changes', () => {
        renderPage();

        fireEvent.click(screen.getByRole('checkbox', { name: 'Select version 1' }));
        fireEvent.click(screen.getByRole('checkbox', { name: 'Select version 2' }));
        fireEvent.click(screen.getByRole('button', { name: 'Compare selected versions' }));
        expect(screen.getByRole('dialog', { name: 'Comparing version 1 with version 2' })).not.toBeNull();
        fireEvent.click(screen.getAllByRole('button', { name: 'Close' }).at(-1)!);

        fireEvent.click(screen.getByRole('button', { name: 'Compare selected version with pending changes' }));
        expect(screen.getByRole('dialog', { name: 'Comparing version 2 with version to be deployed' })).not.toBeNull();
    });

    it('restores a historical version after confirmation', async () => {
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: 'View or restore version 1' }));
        expect(screen.getByText('Read-only version state')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'Restore version' }));
        fireEvent.click(screen.getByRole('button', { name: 'Restore' }));

        await waitFor(() => expect(restoreAsync).toHaveBeenCalledWith(HISTORIES[1]));
        expect(notify.success).toHaveBeenCalledWith(
            'Version has been restored. Review changes and click ‘Deploy’ to finalize the restoration.',
        );
        expect(await screen.findByText('Studio destination')).not.toBeNull();
    });

    it('reports a restore failure without leaving history', async () => {
        restoreAsync.mockRejectedValueOnce(new Error('Restore failed'));
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: 'View or restore version 1' }));
        fireEvent.click(screen.getByRole('button', { name: 'Restore version' }));
        fireEvent.click(screen.getByRole('button', { name: 'Restore' }));

        await waitFor(() => expect(notify.error).toHaveBeenCalledWith(expect.any(Error), 'Error during Shared Policy Group restore!'));
        expect(screen.queryByText('Studio destination')).toBeNull();
    });

    it('does not offer restore without update permission or for Kubernetes-managed groups', () => {
        mockUseHasEnvironmentPermission.mockReturnValue(false);
        const readOnlyView = renderPage();
        fireEvent.click(screen.getByRole('button', { name: 'View or restore version 1' }));
        expect(screen.queryByRole('button', { name: 'Restore version' })).toBeNull();
        readOnlyView.unmount();

        mockUseHasEnvironmentPermission.mockReturnValue(true);
        renderPage({ ...CURRENT, originContext: { origin: 'KUBERNETES' } });
        fireEvent.click(screen.getByRole('button', { name: 'View or restore version 1' }));
        expect(screen.queryByRole('button', { name: 'Restore version' })).toBeNull();
    });

    it('resets pagination when history sorting changes', async () => {
        mockUseSharedPolicyGroupHistories.mockReturnValue({
            data: {
                data: HISTORIES,
                pagination: { page: 1, perPage: 25, pageCount: 2, pageItemsCount: 2, totalCount: 30 },
            },
            isLoading: false,
            isError: false,
        } as unknown as ReturnType<typeof useSharedPolicyGroupHistories>);
        renderPage();

        fireEvent.click(screen.getByRole('button', { name: 'Next page' }));
        await waitFor(() => expect(mockUseSharedPolicyGroupHistories).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2 })));
        fireEvent.click(screen.getByRole('button', { name: 'Version' }));

        await waitFor(() => expect(mockUseSharedPolicyGroupHistories).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 })));
    });

    it('shows loading, error, and empty states', () => {
        mockUseSharedPolicyGroupHistories.mockReturnValueOnce({
            isLoading: true,
            isError: false,
        } as ReturnType<typeof useSharedPolicyGroupHistories>);
        const { unmount } = renderPage();
        expect(document.querySelector('[data-slot="skeleton"]')).not.toBeNull();
        unmount();

        mockUseSharedPolicyGroupHistories.mockReturnValueOnce({
            isLoading: false,
            isError: true,
        } as ReturnType<typeof useSharedPolicyGroupHistories>);
        const errorView = renderPage();
        expect(screen.getByText('Failed to load Shared Policy Group history. Please refresh and try again.')).not.toBeNull();
        errorView.unmount();

        mockUseSharedPolicyGroupHistories.mockReturnValue({
            data: {
                data: [],
                pagination: { page: 1, perPage: 25, pageCount: 0, pageItemsCount: 0, totalCount: 0 },
            },
            isLoading: false,
            isError: false,
        } as unknown as ReturnType<typeof useSharedPolicyGroupHistories>);
        renderPage();
        expect(screen.getByText('No deployed versions yet')).not.toBeNull();
    });
});
