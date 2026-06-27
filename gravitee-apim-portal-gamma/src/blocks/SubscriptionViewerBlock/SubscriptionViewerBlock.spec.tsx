/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { setupCatalogDatabaseTests } from '../../features/editor/services/catalog.test-utils';
import { SubscriptionViewerView } from './SubscriptionViewerView';

async function findTable() {
    return screen.findByRole('table');
}

describe('SubscriptionViewerView', () => {
    setupCatalogDatabaseTests();

    it('should render subscriptions table with mock data', async () => {
        render(<SubscriptionViewerView />);

        expect(await screen.findByRole('columnheader', { name: 'API' })).toBeInTheDocument();

        const table = await findTable();
        expect(within(table).getAllByText('Accounts API').length).toBeGreaterThan(0);
        expect(within(table).getByText('Internal Tools')).toBeInTheDocument();
    });

    it('should filter subscriptions by API', async () => {
        const user = userEvent.setup();
        render(<SubscriptionViewerView />);

        const table = await findTable();
        await within(table).findByText('Internal Tools');

        const apiFilter = screen.getByLabelText('API');
        await waitFor(() => {
            expect(within(apiFilter).getByRole('option', { name: 'Payments API' })).toBeInTheDocument();
        });
        await user.selectOptions(apiFilter, 'api-payments');

        await waitFor(() => {
            expect(within(table).getByText('Mobile Banking App')).toBeInTheDocument();
            expect(within(table).queryByText('Internal Tools')).not.toBeInTheDocument();
        });
    });

    it('should open slide-over details when a row is clicked', async () => {
        const user = userEvent.setup();
        render(<SubscriptionViewerView />);

        const table = await findTable();
        await within(table).findByText('Mobile Banking App');
        await user.click(within(table).getByRole('button', { name: /Mobile Banking App/i }));

        expect(await screen.findByRole('dialog', { name: /Subscription Details/i })).toBeInTheDocument();
        expect(screen.getByText('sub-001')).toBeInTheDocument();
        expect(screen.getByText('gk-demo-payments-001')).toBeInTheDocument();
    });

    it('should close slide-over when overlay is clicked', async () => {
        const user = userEvent.setup();
        render(<SubscriptionViewerView />);

        const table = await findTable();
        await within(table).findByText('Mobile Banking App');
        await user.click(within(table).getByRole('button', { name: /Mobile Banking App/i }));
        await screen.findByRole('dialog');

        await user.click(screen.getByLabelText('Close subscription details'));

        await waitFor(() => {
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });
    });

    it('should show webhook config in details for push subscriptions', async () => {
        const user = userEvent.setup();
        render(<SubscriptionViewerView />);

        const table = await findTable();
        await within(table).findByText('Internal Tools');
        await user.click(within(table).getByRole('button', { name: /Internal Tools/i }));

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Webhook configuration')).toBeInTheDocument();
        expect(
            within(dialog).getByText('https://internal.example.com/webhooks/accounts'),
        ).toBeInTheDocument();
    });
});
