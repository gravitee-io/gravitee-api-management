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
import { fireEvent, render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';

jest.mock('@gravitee/graphene-core', () => ({
    Alert: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    AlertDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    Badge: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
    Button: ({ children, onClick, disabled }: { children?: ReactNode; onClick?: () => void; disabled?: boolean }) => (
        <button type="button" onClick={onClick} disabled={disabled}>
            {children}
        </button>
    ),
    Label: ({ children }: { children?: ReactNode }) => <label>{children}</label>,
    Select: ({ value, onValueChange, children }: { value: string; onValueChange: (v: string) => void; children?: ReactNode }) => (
        <select aria-label="Subscription Plan" value={value} onChange={e => onValueChange(e.target.value)}>
            <option value="">Select a plan</option>
            {children}
        </select>
    ),
    SelectContent: ({ children }: { children?: ReactNode }) => <>{children}</>,
    SelectItem: ({ value, children }: { value: string; children?: ReactNode }) => <option value={value}>{children}</option>,
    SelectTrigger: () => null,
    SelectValue: () => null,
    Separator: () => <hr />,
    Sheet: ({ open, children }: { open: boolean; children?: ReactNode }) => (open ? <div role="dialog">{children}</div> : null),
    SheetContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetDescription: ({ children }: { children?: ReactNode }) => <p>{children}</p>,
    SheetFooter: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetHeader: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    SheetTitle: ({ children }: { children?: ReactNode }) => <h2>{children}</h2>,
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('./ApplicationSearchList', () => ({
    ApplicationSearchList: ({ selected, onSelect }: { selected: { name: string } | null; onSelect: (app: unknown) => void }) =>
        selected ? (
            <p>Selected application: {selected.name}</p>
        ) : (
            <button type="button" onClick={() => onSelect({ id: 'app-1', name: 'Checkout' })}>
                Pick Checkout
            </button>
        ),
}));

jest.mock('../../../hooks/useSubscriptions', () => ({ useApiPlans: jest.fn() }));

import { CreateSubscription } from './CreateSubscription';
import { useApiPlans } from '../../../hooks/useSubscriptions';

const CTX = { type: 'api', entityId: 'api-1' } as const;

function renderDialog(open: boolean) {
    const props = { ctx: CTX, isPending: false, error: null, onConfirm: jest.fn(), onClose: jest.fn() };
    const view = render(<CreateSubscription {...props} open={open} />);
    return { ...view, rerenderWith: (nextOpen: boolean) => view.rerender(<CreateSubscription {...props} open={nextOpen} />) };
}

describe('CreateSubscription', () => {
    beforeEach(() => {
        (useApiPlans as jest.Mock).mockReturnValue({ data: [{ id: 'plan-1', name: 'Gold' }], isLoading: false });
    });

    it('opens empty after a subscription was created and the page closed it', () => {
        const { rerenderWith } = renderDialog(true);
        fireEvent.click(screen.getByRole('button', { name: 'Pick Checkout' }));
        fireEvent.change(screen.getByLabelText('Subscription Plan'), { target: { value: 'plan-1' } });
        expect(screen.getByText('Selected application: Checkout')).toBeInTheDocument();

        rerenderWith(false);
        rerenderWith(true);

        expect(screen.queryByText(/Selected application/)).not.toBeInTheDocument();
        expect(screen.getByLabelText('Subscription Plan')).toHaveValue('');
        expect(screen.getByRole('button', { name: 'Create subscription' })).toBeDisabled();
    });

    it('keeps the selection while the dialog stays open, for instance after a failed create', () => {
        const { rerenderWith } = renderDialog(true);
        fireEvent.click(screen.getByRole('button', { name: 'Pick Checkout' }));

        rerenderWith(true);

        expect(screen.getByText('Selected application: Checkout')).toBeInTheDocument();
    });
});
