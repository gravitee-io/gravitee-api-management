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
import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';

jest.mock('@gravitee/graphene-core', () => {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    const React = require('react');
    return {
        Alert: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
        AlertDescription: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
        Button: ({
            children,
            disabled,
            onClick,
            type,
        }: {
            children?: ReactNode;
            disabled?: boolean;
            onClick?: () => void;
            type?: string;
        }) => (
            <button type={type === 'submit' ? 'submit' : 'button'} disabled={disabled} onClick={onClick}>
                {children}
            </button>
        ),
        Dialog: ({ open, children }: { open?: boolean; children?: ReactNode }) => (open ? <div role="dialog">{children}</div> : null),
        DialogContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
        DialogFooter: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
        DialogHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
        DialogTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
        Input: (props: React.InputHTMLAttributes<HTMLInputElement>) => <input {...props} />,
        Label: ({ children, htmlFor }: { children?: ReactNode; htmlFor?: string }) => <label htmlFor={htmlFor}>{children}</label>,
        RadioGroup: ({ children, 'aria-label': ariaLabel }: { children?: ReactNode; 'aria-label'?: string }) => (
            <div role="radiogroup" aria-label={ariaLabel}>
                {children}
            </div>
        ),
        RadioGroupItem: ({ value, id, disabled }: { value: string; id?: string; disabled?: boolean }) => (
            <input type="radio" id={id} value={value} disabled={disabled} aria-label={value} />
        ),
        Textarea: (props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) => <textarea {...props} />,
    };
});
jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('./SubscriptionApproveDialog', () => ({
    SubscriptionApproveDialog: () => null,
}));

jest.mock('../../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

jest.mock('../../../../hooks/useSubscriptionActions', () => ({
    useApproveSubscription: () => ({ mutate: jest.fn(), isPending: false, reset: jest.fn() }),
    useRejectSubscription: () => ({ mutate: jest.fn(), isPending: false, reset: jest.fn() }),
    usePauseSubscription: () => ({ mutate: jest.fn(), isPending: false }),
    useResumeSubscription: () => ({ mutate: jest.fn(), isPending: false }),
    useResumeFailedSubscription: () => ({ mutate: jest.fn(), isPending: false }),
    useCloseSubscription: () => ({ mutate: jest.fn(), isPending: false }),
    useTransferSubscription: () => ({ mutate: jest.fn(), isPending: false }),
    useUpdateSubscriptionEndDate: () => ({ mutate: jest.fn(), isPending: false }),
}));

jest.mock('../../../../hooks/useSubscriptions', () => ({
    useApiPlans: jest.fn(),
}));

import { getTransferablePlans, SubscriptionActionsBar } from './SubscriptionActionsBar';
import { useApiPlans } from '../../../../hooks/useSubscriptions';
import type { Plan, Subscription, SubscriptionContext } from '../../../../types/subscription';

const mockUseApiPlans = useApiPlans as jest.Mock;

const CTX: SubscriptionContext = { type: 'api', entityId: 'api-1' };

const ACCEPTED_SUB: Subscription = {
    id: 'sub-1',
    status: 'ACCEPTED',
    plan: { id: 'plan-a', name: 'API Key A', security: { type: 'API_KEY' }, mode: 'STANDARD' },
    application: { id: 'app-1', name: 'App' },
};

const PLANS: Plan[] = [
    { id: 'plan-a', name: 'API Key A', security: { type: 'API_KEY' }, mode: 'STANDARD' },
    { id: 'plan-b', name: 'API Key B', security: { type: 'API_KEY' }, mode: 'STANDARD' },
    { id: 'plan-jwt', name: 'JWT Plan', security: { type: 'JWT' }, mode: 'STANDARD' },
    { id: 'plan-push', name: 'Push Plan', mode: 'PUSH' },
    {
        id: 'plan-gc',
        name: 'With Conditions',
        security: { type: 'API_KEY' },
        mode: 'STANDARD',
        generalConditions: 'page-1',
    },
];

function renderBar(subscription: Subscription, isFederated = false) {
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <SubscriptionActionsBar ctx={CTX} subscription={subscription} canUpdate canDelete isFederated={isFederated} />
        </QueryClientProvider>,
    );
}

describe('getTransferablePlans', () => {
    it('keeps plans with the same security type and mode, excluding the current plan', () => {
        const result = getTransferablePlans(PLANS, ACCEPTED_SUB.plan);
        expect(result.map(p => p.id)).toEqual(['plan-b', 'plan-gc']);
    });

    it('matches PUSH plans when the current plan has no security type', () => {
        const result = getTransferablePlans(PLANS, { id: 'current-push', name: 'Current', mode: 'PUSH' });
        expect(result.map(p => p.id)).toEqual(['plan-push']);
    });
});

describe('SubscriptionActionsBar transfer', () => {
    beforeEach(() => {
        mockUseApiPlans.mockReturnValue({ data: PLANS });
    });

    it('offers Transfer on ACCEPTED and lists compatible plans as radios', async () => {
        const user = userEvent.setup();
        renderBar(ACCEPTED_SUB);

        await user.click(screen.getByRole('button', { name: /^Transfer$/ }));

        const dialog = screen.getByRole('dialog');
        expect(within(dialog).getByRole('heading', { name: /transfer subscription/i })).toBeInTheDocument();
        expect(within(dialog).getByRole('radio', { name: 'plan-b' })).toBeEnabled();
        expect(within(dialog).queryByRole('radio', { name: 'plan-jwt' })).not.toBeInTheDocument();
        expect(within(dialog).queryByRole('radio', { name: 'plan-a' })).not.toBeInTheDocument();
    });

    it('offers Transfer on PAUSED', () => {
        renderBar({ ...ACCEPTED_SUB, status: 'PAUSED' });
        expect(screen.getByRole('button', { name: /^Transfer$/ })).toBeInTheDocument();
    });

    it('hides Transfer, Change end date and Pause for federated APIs', () => {
        renderBar(ACCEPTED_SUB, true);
        expect(screen.queryByRole('button', { name: /^Transfer$/ })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /change end date/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /^Pause$/ })).not.toBeInTheDocument();
    });

    it('disables radios for plans with general conditions (Classic parity)', async () => {
        const user = userEvent.setup();
        renderBar(ACCEPTED_SUB);

        await user.click(screen.getByRole('button', { name: /^Transfer$/ }));

        const dialog = screen.getByRole('dialog');
        expect(within(dialog).getByRole('radio', { name: 'plan-gc' })).toBeDisabled();
        expect(within(dialog).getByText(/plans with general conditions cannot be used/i)).toBeInTheDocument();
    });
});

const PAUSED_SUBSCRIPTION: Subscription = {
    id: 'sub-1',
    status: 'PAUSED',
    plan: { id: 'plan-1', name: 'API Key', security: { type: 'API_KEY' } },
    application: { id: 'app-1', name: 'App', apiKeyMode: 'EXCLUSIVE' },
};

describe('SubscriptionActionsBar end date', () => {
    beforeEach(() => {
        mockUseApiPlans.mockReturnValue({ data: [] });
    });

    it('shows Transfer, Change end date, and Resume on a paused non-federated subscription', () => {
        renderBar(PAUSED_SUBSCRIPTION);

        expect(screen.getByRole('button', { name: /transfer/i })).not.toBeNull();
        expect(screen.getByRole('button', { name: /change end date/i })).not.toBeNull();
        expect(screen.getByRole('button', { name: /^resume$/i })).not.toBeNull();
        expect(screen.queryByRole('button', { name: /^pause$/i })).toBeNull();
    });

    it('prefills the local end date and keeps save disabled until that date changes', () => {
        renderBar({ ...PAUSED_SUBSCRIPTION, endingAt: '2026-10-19T18:30:00.000Z' });

        fireEvent.click(screen.getByRole('button', { name: /change end date/i }));
        const input = screen.getByLabelText(/End date/) as HTMLInputElement;
        const ending = new Date('2026-10-19T18:30:00.000Z');
        const today = new Date();
        const ymd = (date: Date) =>
            `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

        expect(input.value).toBe(ymd(ending));
        expect(input.min).toBe(ymd(today));
        expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();

        fireEvent.change(input, { target: { value: '2026-12-01' } });
        expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled();

        fireEvent.change(input, { target: { value: ymd(ending) } });
        expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    });

    it('keeps save disabled when the subscription has no end date and the field is still empty', () => {
        renderBar(PAUSED_SUBSCRIPTION);

        fireEvent.click(screen.getByRole('button', { name: /change end date/i }));
        expect((screen.getByLabelText(/End date/) as HTMLInputElement).value).toBe('');
        expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    });
});
