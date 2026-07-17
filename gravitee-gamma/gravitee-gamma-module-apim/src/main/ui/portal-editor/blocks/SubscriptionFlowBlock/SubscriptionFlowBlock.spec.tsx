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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { PortalPageProvider } from '../../portal-shell/context/PortalPageContext';
import type { PortalNavigationApi, PortalNavigationItem } from '../../portals/types';
import { SubscriptionFlowView } from './SubscriptionFlowView';
import { setupCatalogDatabaseTests } from '../../editor/services/catalog.test-utils';
import { resetSubscriptionsForTests } from '../../editor/services/subscriptions.service';

const navItems: PortalNavigationItem[] = [
    {
        id: 'api-nav',
        portalId: 'portal-1',
        title: 'Payments API',
        type: 'API',
        apiId: 'api-payments',
        parentId: null,
        order: 0,
        slug: 'payments-api',
    } as PortalNavigationApi,
    {
        id: 'page-nav',
        portalId: 'portal-1',
        title: 'Subscribe',
        type: 'PAGE',
        parentId: 'api-nav',
        order: 0,
        slug: 'subscribe-page',
    },
];

function renderFlow(apiIdOverride?: string) {
    return render(
        <PortalPageProvider portalId="portal-1" selectedNavItemId="page-nav" navItems={navItems}>
            <SubscriptionFlowView apiIdOverride={apiIdOverride} />
        </PortalPageProvider>,
    );
}

describe('SubscriptionFlowView', () => {
    setupCatalogDatabaseTests();

    beforeEach(async () => {
        await resetSubscriptionsForTests();
    });

    it('should render choose plan step by default', async () => {
        renderFlow();

        expect(await screen.findByRole('heading', { name: 'Choose a plan' })).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: /Standard API Key/i })).toBeInTheDocument();
    });

    it('should show no API message without context', () => {
        render(<SubscriptionFlowView />);

        expect(screen.getByText('No API context')).toBeInTheDocument();
    });

    it('should advance through plan and application steps', async () => {
        const user = userEvent.setup();
        renderFlow();

        await screen.findByRole('heading', { name: 'Choose a plan' });
        await user.click(screen.getByRole('radio', { name: /Standard API Key/i }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        expect(await screen.findByRole('heading', { name: 'Choose an application' })).toBeInTheDocument();
        await user.click(screen.getByRole('radio', { name: /Internal Tools/i }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        expect(await screen.findByRole('heading', { name: 'Review' })).toBeInTheDocument();
    });

    it('should create subscription on review submit', async () => {
        const user = userEvent.setup();
        renderFlow();

        await screen.findByRole('heading', { name: 'Choose a plan' });
        await user.click(screen.getByRole('radio', { name: /Standard API Key/i }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        await screen.findByRole('heading', { name: 'Choose an application' });
        await user.click(screen.getByRole('radio', { name: /Internal Tools/i }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        await screen.findByRole('heading', { name: 'Review' });
        await user.click(screen.getByRole('button', { name: 'Subscribe' }));

        await waitFor(() => {
            expect(screen.getByText('Subscription created')).toBeInTheDocument();
        });
    });

    it('should navigate back to previous step', async () => {
        const user = userEvent.setup();
        renderFlow();

        await screen.findByRole('heading', { name: 'Choose a plan' });
        await user.click(screen.getByRole('radio', { name: /Standard API Key/i }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        await screen.findByRole('heading', { name: 'Choose an application' });
        await user.click(screen.getByRole('button', { name: 'Previous' }));

        expect(screen.getByRole('heading', { name: 'Choose a plan' })).toBeInTheDocument();
    });

    it('should include push configuration step for push plans', async () => {
        const user = userEvent.setup();
        renderFlow('api-accounts');

        await screen.findByRole('heading', { name: 'Choose a plan' });
        await user.click(screen.getByRole('radio', { name: /Account Events Webhook/i }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        await screen.findByRole('heading', { name: 'Choose an application' });
        await user.click(screen.getByRole('radio', { name: /Partner Integration/i }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        expect(await screen.findByRole('heading', { name: 'Configure consumer' })).toBeInTheDocument();

        const input = screen.getByLabelText('Callback URL');
        fireEvent.change(input, { target: { value: 'https://example.com/webhook' } });
        await user.click(screen.getByRole('button', { name: 'Next' }));

        expect(await screen.findByRole('heading', { name: 'Review' })).toBeInTheDocument();
        expect(screen.getByText('https://example.com/webhook')).toBeInTheDocument();
    });
});
