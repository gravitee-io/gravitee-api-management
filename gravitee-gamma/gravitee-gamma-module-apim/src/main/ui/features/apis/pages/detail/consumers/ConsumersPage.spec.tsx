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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('@gravitee/graphene-core', () => ({
    Button: ({ children, onClick, disabled }: { children?: ReactNode; onClick?: () => void; disabled?: boolean }) => (
        <button type="button" onClick={onClick} disabled={disabled}>
            {children}
        </button>
    ),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('./ConsumersEmptyState', () => ({ ConsumersEmptyState: () => <div>Why manage consumers?</div> }));
jest.mock('./ConsumersFilterBar', () => ({ ConsumersFilterBar: () => null }));
jest.mock('./ConsumersSummaryCards', () => ({ ConsumersSummaryCards: () => null }));
jest.mock('./ConsumersTable', () => ({ ConsumersTable: () => <div>Subscription list</div> }));
jest.mock('./CreateSubscription', () => ({ CreateSubscription: () => null }));

jest.mock('../../../../../shared/browser', () => ({ downloadBlob: jest.fn() }));
jest.mock('../../../../../shared/notify', () => ({ notify: { success: jest.fn(), error: jest.fn() } }));

jest.mock('../../../hooks/useSubscriptionActions', () => ({ useCreateSubscription: () => ({ mutate: jest.fn(), reset: jest.fn() }) }));
jest.mock('../../../hooks/useSubscriptions', () => ({
    ALL_SUBSCRIPTION_STATUSES: ['PENDING', 'ACCEPTED', 'REJECTED', 'CLOSED', 'PAUSED', 'RESUMED'],
    DEFAULT_STATUSES: ['ACCEPTED', 'PAUSED', 'PENDING'],
    isSubscriptionFiltersDirty: () => false,
    useApiPlans: () => ({ data: [] }),
    useSubscriptionCount: jest.fn(() => ({ data: 5, isLoading: false, isPending: false, isSuccess: true, isError: false })),
    useSubscriptionList: jest.fn(),
}));
jest.mock('../../../services/subscriptions', () => ({ exportSubscriptionsCsv: jest.fn() }));

import { ConsumersPage } from './ConsumersPage';
import { downloadBlob } from '../../../../../shared/browser';
import { useSubscriptionCount, useSubscriptionList } from '../../../hooks/useSubscriptions';
import { exportSubscriptionsCsv } from '../../../services/subscriptions';

const mockUseSubscriptionList = useSubscriptionList as jest.Mock;
const mockUseSubscriptionCount = useSubscriptionCount as jest.Mock;
const mockExport = exportSubscriptionsCsv as jest.Mock;

const KNOWN_COUNT = { isLoading: false, isPending: false, isSuccess: true, isError: false };

describe('ConsumersPage export', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        (useEnvironment as jest.Mock).mockReturnValue({ id: 'DEFAULT' });
        mockExport.mockResolvedValue(new Blob(['csv']));
        mockUseSubscriptionCount.mockReturnValue({ data: 5, ...KNOWN_COUNT });
    });

    it('exports every subscription matching the filters, not only the visible page', async () => {
        mockUseSubscriptionList.mockReturnValue({ data: { data: [], pagination: { totalCount: 25 } }, isLoading: false });
        render(<ConsumersPage ctx={{ type: 'api', entityId: 'api-1' }} canCreate canRead />);

        fireEvent.click(screen.getByRole('button', { name: /export csv/i }));

        await waitFor(() => expect(downloadBlob).toHaveBeenCalled());
        expect(mockExport).toHaveBeenCalledWith(
            'DEFAULT',
            'api-1',
            expect.objectContaining({ page: 1, perPage: 25, statuses: ['ACCEPTED', 'PAUSED', 'PENDING'] }),
        );
    });

    it('offers no export for an API product', () => {
        mockUseSubscriptionList.mockReturnValue({ data: { data: [], pagination: { totalCount: 3 } }, isLoading: false });
        render(<ConsumersPage ctx={{ type: 'api-product', entityId: 'product-1' }} canCreate canRead />);

        expect(screen.queryByRole('button', { name: /export csv/i })).not.toBeInTheDocument();
    });

    it('shows the educational empty state only when the all-status count succeeds at zero', () => {
        mockUseSubscriptionCount.mockReturnValue({ data: 0, ...KNOWN_COUNT });
        mockUseSubscriptionList.mockReturnValue({ data: { data: [], pagination: { totalCount: 0 } }, isLoading: false });
        render(<ConsumersPage ctx={{ type: 'api', entityId: 'api-1' }} canCreate canRead />);

        expect(screen.getByText('Why manage consumers?')).toBeInTheDocument();
        expect(screen.queryByText('Subscription list')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /export csv/i })).not.toBeInTheDocument();
    });

    it('keeps the subscription list when the all-status count fails', () => {
        mockUseSubscriptionCount.mockImplementation((_ctx: unknown, statuses: string[]) => {
            if (statuses.includes('CLOSED')) {
                return { data: undefined, isLoading: false, isPending: false, isSuccess: false, isError: true };
            }
            return { data: 0, ...KNOWN_COUNT };
        });
        mockUseSubscriptionList.mockReturnValue({
            data: { data: [{ id: 'sub-1' }], pagination: { totalCount: 1 } },
            isLoading: false,
            isFetching: false,
        });
        render(<ConsumersPage ctx={{ type: 'api', entityId: 'api-1' }} canCreate canRead />);

        expect(screen.getByText('Subscription list')).toBeInTheDocument();
        expect(screen.queryByText('Why manage consumers?')).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: /export csv/i })).toBeEnabled();
    });
});
