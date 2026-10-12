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
import { act, fireEvent, render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';

jest.mock('@gravitee/graphene-core', () => ({
    Badge: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
    Button: ({ children, onClick, 'aria-label': ariaLabel }: { children?: ReactNode; onClick?: () => void; 'aria-label'?: string }) => (
        <button type="button" onClick={onClick} aria-label={ariaLabel}>
            {children}
        </button>
    ),
    Card: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    CardContent: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
    Input: ({
        value,
        onChange,
        placeholder,
        'aria-label': ariaLabel,
    }: {
        value?: string;
        onChange?: () => void;
        placeholder?: string;
        'aria-label'?: string;
    }) => <input value={value} onChange={onChange} placeholder={placeholder} aria-label={ariaLabel} />,
    Label: ({ children }: { children?: ReactNode }) => <label>{children}</label>,
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../../../../../shared/components', () => ({ MultiSelectFilter: () => null }));

jest.mock('../../../hooks/useSubscriptions', () => ({
    ...jest.requireActual<object>('../../../hooks/useSubscriptions'),
    useApiSubscriberSearch: jest.fn(),
}));

import { ConsumersFilterBar } from './ConsumersFilterBar';
import { useApiSubscriberSearch } from '../../../hooks/useSubscriptions';
import type { SubscriptionContext, SubscriptionFilters } from '../../../types/subscription';

const mockSearch = useApiSubscriberSearch as jest.Mock;

const NO_FILTERS: SubscriptionFilters = { statuses: [], planIds: [], applicationIds: [], apiKey: '' };
const API_CTX: SubscriptionContext = { type: 'api', entityId: 'api-1' };

function typeApplicationQuery(value: string) {
    fireEvent.change(screen.getByLabelText('Filter by application'), { target: { value } });
    act(() => {
        jest.advanceTimersByTime(300);
    });
}

describe('ConsumersFilterBar application filter', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        mockSearch.mockReset();
        mockSearch.mockReturnValue({ data: [], isLoading: false });
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('does not search for applications until something is typed', () => {
        render(<ConsumersFilterBar filters={NO_FILTERS} plans={[]} ctx={API_CTX} onChange={jest.fn()} />);

        expect(mockSearch).toHaveBeenLastCalledWith(API_CTX, '');
        expect(screen.queryByRole('button', { name: /checkout/i })).not.toBeInTheDocument();
    });

    it('filters by the application picked from the search results', () => {
        mockSearch.mockImplementation((_ctx, term: string) => ({
            data: term ? [{ value: 'app-1', label: 'Checkout' }] : [],
            isLoading: false,
        }));
        const onChange = jest.fn();
        render(<ConsumersFilterBar filters={NO_FILTERS} plans={[]} ctx={API_CTX} onChange={onChange} />);

        typeApplicationQuery('check');
        fireEvent.click(screen.getByRole('button', { name: 'Checkout' }));

        expect(mockSearch).toHaveBeenCalledWith(API_CTX, 'check');
        expect(onChange).toHaveBeenCalledWith({ ...NO_FILTERS, applicationIds: ['app-1'] });
    });

    it('lets the user remove a selected application', () => {
        mockSearch.mockImplementation((_ctx, term: string) => ({
            data: term ? [{ value: 'app-1', label: 'Checkout' }] : [],
            isLoading: false,
        }));
        const onChange = jest.fn();
        const { rerender } = render(<ConsumersFilterBar filters={NO_FILTERS} plans={[]} ctx={API_CTX} onChange={onChange} />);
        typeApplicationQuery('check');
        fireEvent.click(screen.getByRole('button', { name: 'Checkout' }));

        rerender(
            <ConsumersFilterBar filters={{ ...NO_FILTERS, applicationIds: ['app-1'] }} plans={[]} ctx={API_CTX} onChange={onChange} />,
        );
        fireEvent.click(screen.getByRole('button', { name: /remove checkout/i }));

        expect(onChange).toHaveBeenLastCalledWith(NO_FILTERS);
    });

    it('is not offered for an API product', () => {
        render(<ConsumersFilterBar filters={NO_FILTERS} plans={[]} ctx={{ type: 'api-product', entityId: 'p-1' }} onChange={jest.fn()} />);

        expect(screen.queryByLabelText('Filter by application')).not.toBeInTheDocument();
    });

    it('clears the application filter with the other filters on reset', () => {
        const onChange = jest.fn();
        render(<ConsumersFilterBar filters={{ ...NO_FILTERS, applicationIds: ['app-1'] }} plans={[]} ctx={API_CTX} onChange={onChange} />);

        fireEvent.click(screen.getByRole('button', { name: /reset/i }));

        expect(onChange).toHaveBeenCalledWith(NO_FILTERS);
    });
});
