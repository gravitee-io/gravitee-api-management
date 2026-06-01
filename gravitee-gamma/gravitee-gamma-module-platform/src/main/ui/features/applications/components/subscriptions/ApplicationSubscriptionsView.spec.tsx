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
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { ApplicationSubscriptionsView } from './ApplicationSubscriptionsView';
import { notify } from '../../../../shared/notify';
import { useApplicationSubscriptionPermissions } from '../../hooks/useApplicationSubscriptionPermissions';
import { useApplicationSubscriptions, useSubscribedApis } from '../../hooks/useApplicationSubscriptions';
import { useCloseApplicationSubscription } from '../../hooks/useCloseApplicationSubscription';
import type { ApplicationListItem } from '../../types/application';

jest.mock('../../hooks/useApplicationSubscriptionPermissions');
jest.mock('../../hooks/useApplicationSubscriptions', () => ({
    useApplicationSubscriptions: jest.fn(),
    useSubscribedApis: jest.fn(),
}));
jest.mock('../../hooks/useCloseApplicationSubscription');
jest.mock('../../../shared/hooks/useDetailBasePath', () => ({
    useDetailBasePath: () => '/applications/app-1',
}));
jest.mock('./ApplicationSubscriptionCreateDialog', () => ({
    ApplicationSubscriptionCreateDialog: () => <div data-testid="create-dialog" />,
}));
jest.mock('./ApplicationSubscriptionCloseDialog', () => ({
    ApplicationSubscriptionCloseDialog: () => <div data-testid="close-dialog" />,
}));
jest.mock('./ApplicationSubscriptionsTable', () => ({
    ApplicationSubscriptionsTable: () => <div data-testid="subscriptions-table" />,
}));
jest.mock('./ApplicationSubscriptionStatusDetails', () => ({
    ApplicationSubscriptionStatusDetails: () => null,
}));
jest.mock('./ApplicationSubscriptionMultiSelectFilter', () => ({
    ApplicationSubscriptionMultiSelectFilter: () => null,
}));
jest.mock('../../../../shared/notify', () => ({
    notify: {
        error: jest.fn(),
        success: jest.fn(),
        warning: jest.fn(),
        info: jest.fn(),
    },
}));

const mockNotify = jest.mocked(notify);
const mockUsePermissions = jest.mocked(useApplicationSubscriptionPermissions);
const mockUseSubscriptions = jest.mocked(useApplicationSubscriptions);
const mockUseSubscribedApis = jest.mocked(useSubscribedApis);
const mockUseClose = jest.mocked(useCloseApplicationSubscription);

const application: ApplicationListItem = {
    id: 'app-1',
    name: 'Billing',
    status: 'ACTIVE',
    type: 'SIMPLE',
    created_at: 0,
    updated_at: 0,
};

function renderView() {
    return render(
        <MemoryRouter>
            <ApplicationSubscriptionsView application={application} />
        </MemoryRouter>,
    );
}

describe('ApplicationSubscriptionsView', () => {
    beforeEach(() => {
        mockUseSubscriptions.mockReturnValue({
            data: { rows: [], totalCount: 0 },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useApplicationSubscriptions>);
        mockUseSubscribedApis.mockReturnValue({ data: [] } as ReturnType<typeof useSubscribedApis>);
        mockUseClose.mockReturnValue({
            mutateAsync: jest.fn(),
            isPending: false,
        } as ReturnType<typeof useCloseApplicationSubscription>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('shows Create a subscription when the user can create', () => {
        mockUsePermissions.mockReturnValue({
            permissionsReady: true,
            canRead: true,
            canCreate: true,
            canUpdate: false,
            canDelete: false,
            canViewDetail: true,
        });
        renderView();
        expect(screen.getByRole('button', { name: /Create a subscription/i })).not.toBeNull();
        expect(screen.getByTestId('create-dialog')).not.toBeNull();
    });

    it('hides Create a subscription when the user cannot create', () => {
        mockUsePermissions.mockReturnValue({
            permissionsReady: true,
            canRead: true,
            canCreate: false,
            canUpdate: false,
            canDelete: false,
            canViewDetail: true,
        });
        renderView();
        expect(screen.queryByRole('button', { name: /Create a subscription/i })).toBeNull();
        expect(screen.queryByTestId('create-dialog')).toBeNull();
    });

    it('shows a toaster error and inline alert without the table when subscriptions fail to load', () => {
        mockUsePermissions.mockReturnValue({
            permissionsReady: true,
            canRead: true,
            canCreate: false,
            canUpdate: false,
            canDelete: false,
            canViewDetail: true,
        });
        mockUseSubscriptions.mockReturnValue({
            data: undefined,
            isLoading: false,
            isError: true,
        } as ReturnType<typeof useApplicationSubscriptions>);
        renderView();
        expect(mockNotify.error).toHaveBeenCalledWith('Unable to get subscriptions, please try again');
        expect(screen.getByText(/unable to get subscriptions/i)).not.toBeNull();
        expect(screen.queryByTestId('subscriptions-table')).toBeNull();
    });

    it('hides Create a subscription for archived applications', () => {
        mockUsePermissions.mockReturnValue({
            permissionsReady: true,
            canRead: true,
            canCreate: true,
            canUpdate: false,
            canDelete: false,
            canViewDetail: true,
        });
        render(
            <MemoryRouter>
                <ApplicationSubscriptionsView application={{ ...application, status: 'ARCHIVED' }} />
            </MemoryRouter>,
        );
        expect(screen.queryByRole('button', { name: /Create a subscription/i })).toBeNull();
        expect(screen.queryByTestId('create-dialog')).toBeNull();
    });
});
