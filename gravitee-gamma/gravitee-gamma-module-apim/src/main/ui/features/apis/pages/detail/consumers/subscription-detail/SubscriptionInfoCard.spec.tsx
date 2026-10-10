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
import type { ReactNode } from 'react';

jest.mock('@gravitee/graphene-core', () => ({
    Badge: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
    Card: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    CardContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    CardHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    CardTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
    Skeleton: () => <div data-testid="skeleton" />,
}));

import { SubscriptionInfoCard } from './SubscriptionInfoCard';
import type { Subscription } from '../../../../types/subscription';

const BASE: Subscription = {
    id: 'sub-1',
    status: 'ACCEPTED',
    plan: { id: 'plan-1', name: 'Gold', security: { type: 'API_KEY' } },
    application: { id: 'app-1', name: 'Checkout', description: 'Handles payments', domain: 'checkout.example.com' },
};

function valueOf(label: string): HTMLElement {
    return screen.getByText(label).parentElement!.children[1] as HTMLElement;
}

describe('SubscriptionInfoCard', () => {
    it('shows the application description and domain', () => {
        render(<SubscriptionInfoCard subscription={BASE} isLoading={false} />);

        expect(screen.getByText('Handles payments')).toBeInTheDocument();
        expect(valueOf('Domain')).toHaveTextContent('checkout.example.com');
    });

    it('keeps the subscribed by row, with a dash when nobody is recorded', () => {
        render(<SubscriptionInfoCard subscription={BASE} isLoading={false} />);

        expect(valueOf('Subscribed by')).toHaveTextContent('—');
    });

    it('shows the failure cause when the subscription failed', () => {
        render(<SubscriptionInfoCard subscription={{ ...BASE, failureCause: 'Broker unreachable' }} isLoading={false} />);

        expect(valueOf('Failure cause')).toHaveTextContent('Broker unreachable');
    });

    it('keeps every lifecycle field visible, with a dash when it has no value', () => {
        render(<SubscriptionInfoCard subscription={BASE} isLoading={false} />);

        for (const label of ['Publisher message', 'Subscriber message', 'Paused at', 'Closed at', 'Domain']) {
            expect(screen.getByText(label)).toBeInTheDocument();
        }
    });

    it('hides the start, pause and end dates of a rejected subscription', () => {
        render(<SubscriptionInfoCard subscription={{ ...BASE, status: 'REJECTED' }} isLoading={false} />);

        expect(screen.queryByText('Starting at')).not.toBeInTheDocument();
        expect(screen.queryByText('Paused at')).not.toBeInTheDocument();
        expect(screen.queryByText('Ending at')).not.toBeInTheDocument();
    });
});
