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
import { fireEvent, render, screen } from '@testing-library/react';

import { SubscriptionApproveDialog } from './SubscriptionApproveDialog';
import { toDatetimeLocalValue } from '../../../../../../shared/time';
import { useCustomApiKeyEnabled } from '../../../../hooks/usePlanSecuritySettings';
import { verifyCustomApiKey } from '../../../../services/subscriptions';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../../../../hooks/usePlanSecuritySettings', () => ({
    useCustomApiKeyEnabled: jest.fn(() => true),
}));

jest.mock('../../../../services/subscriptions', () => ({
    verifyCustomApiKey: jest.fn(() => Promise.resolve({ ok: true })),
}));

const mockVerify = verifyCustomApiKey as jest.Mock;
const mockCustomApiKey = useCustomApiKeyEnabled as jest.Mock;

beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

function renderDialog(onConfirm = jest.fn()) {
    return render(
        <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
            <SubscriptionApproveDialog
                open
                ctx={{ type: 'api', entityId: 'api-1' }}
                applicationId="app-1"
                isApiKeyPlan
                isPending={false}
                onConfirm={onConfirm}
                onClose={jest.fn()}
            />
        </QueryClientProvider>,
    );
}

describe('SubscriptionApproveDialog', () => {
    beforeEach(() => {
        mockCustomApiKey.mockReturnValue(true);
        mockVerify.mockResolvedValue({ ok: true });
    });

    it('approves without a custom key', () => {
        const onConfirm = jest.fn();
        renderDialog(onConfirm);
        fireEvent.click(screen.getByRole('button', { name: 'Approve' }));
        expect(onConfirm).toHaveBeenCalledWith({});
        expect(mockVerify).not.toHaveBeenCalled();
    });

    it('blocks approve when the custom key is not valid', () => {
        const onConfirm = jest.fn();
        renderDialog(onConfirm);
        fireEvent.change(screen.getByLabelText('Custom API key'), { target: { value: 'nope' } });

        expect(screen.getByText('Must be longer than 8 characters')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Approve' })).toBeDisabled();
        fireEvent.click(screen.getByRole('button', { name: 'Approve' }));
        expect(onConfirm).not.toHaveBeenCalled();
    });

    describe('start and end date', () => {
        beforeEach(() => {
            jest.useFakeTimers();
            jest.setSystemTime(new Date('2026-06-15T10:05:30.000Z'));
        });

        afterEach(() => {
            jest.useRealTimers();
        });

        function setDatetimeValidity(input: HTMLInputElement, badInput: boolean) {
            Object.defineProperty(input, 'validity', {
                configurable: true,
                get: () => ({ badInput }) as ValidityState,
            });
        }

        function dateInputs() {
            return {
                start: screen.getByLabelText('Starting date') as HTMLInputElement,
                end: screen.getByLabelText('Ending date') as HTMLInputElement,
            };
        }

        it('uses datetime-local inputs whose minimum is the current minute', () => {
            renderDialog();
            const { start, end } = dateInputs();
            const currentMinute = toDatetimeLocalValue(new Date());

            expect(start.type).toBe('datetime-local');
            expect(end.type).toBe('datetime-local');
            expect(start.value).toBe('');
            expect(end.value).toBe('');
            expect(start.min).toBe(currentMinute);
            expect(end.min).toBe(currentMinute);
        });

        it('sends the chosen local start and end as ISO instants', () => {
            const onConfirm = jest.fn();
            renderDialog(onConfirm);
            const { start, end } = dateInputs();
            const startingAt = '2099-03-01T09:00';
            const endingAt = '2099-03-02T18:30';

            fireEvent.change(start, { target: { value: startingAt } });
            expect(end.min).toBe(startingAt);
            fireEvent.change(end, { target: { value: endingAt } });
            fireEvent.click(screen.getByRole('button', { name: 'Approve' }));

            expect(onConfirm).toHaveBeenCalledWith({
                startingAt: new Date(startingAt).toISOString(),
                endingAt: new Date(endingAt).toISOString(),
            });
        });

        it('still approves when only the start date is set', () => {
            const onConfirm = jest.fn();
            renderDialog(onConfirm);
            const startingAt = '2099-03-01T09:00';

            fireEvent.change(screen.getByLabelText('Starting date'), { target: { value: startingAt } });
            fireEvent.click(screen.getByRole('button', { name: 'Approve' }));

            expect(onConfirm).toHaveBeenCalledWith({ startingAt: new Date(startingAt).toISOString() });
        });

        it('blocks a start or end earlier than the current minute', () => {
            const onConfirm = jest.fn();
            renderDialog(onConfirm);
            const { start, end } = dateInputs();

            fireEvent.change(start, { target: { value: '2020-01-01T00:00' } });
            expect(end.min).toBe(toDatetimeLocalValue(new Date()));
            expect(screen.getByRole('button', { name: 'Approve' })).toBeDisabled();
            fireEvent.click(screen.getByRole('button', { name: 'Approve' }));
            expect(onConfirm).not.toHaveBeenCalled();

            fireEvent.change(start, { target: { value: '' } });
            fireEvent.change(end, { target: { value: '2020-01-01T00:00' } });
            expect(screen.getByRole('button', { name: 'Approve' })).toBeDisabled();
            fireEvent.click(screen.getByRole('button', { name: 'Approve' }));
            expect(onConfirm).not.toHaveBeenCalled();
        });

        it('blocks an end date that is not after the start date', () => {
            const onConfirm = jest.fn();
            renderDialog(onConfirm);
            const { start, end } = dateInputs();
            const startingAt = '2099-03-01T09:00';

            fireEvent.change(start, { target: { value: startingAt } });
            fireEvent.change(end, { target: { value: startingAt } });
            expect(screen.getByText(/end date must be after the start date/i)).toBeInTheDocument();
            expect(screen.getByRole('button', { name: 'Approve' })).toBeDisabled();

            fireEvent.change(end, { target: { value: '2099-03-01T08:59' } });
            expect(screen.getByRole('button', { name: 'Approve' })).toBeDisabled();
            fireEvent.click(screen.getByRole('button', { name: 'Approve' }));
            expect(onConfirm).not.toHaveBeenCalled();

            fireEvent.change(end, { target: { value: '2099-03-01T09:01' } });
            expect(screen.getByRole('button', { name: 'Approve' })).toBeEnabled();
        });

        it('blocks an empty start date that has only a date typed, even when change does not fire', () => {
            const onConfirm = jest.fn();
            renderDialog(onConfirm);
            const { start } = dateInputs();

            setDatetimeValidity(start, true);
            fireEvent.click(screen.getByRole('button', { name: 'Approve' }));

            expect(onConfirm).not.toHaveBeenCalled();
            expect(screen.getByText('Enter both a date and a time.')).toBeInTheDocument();
            expect(screen.getByRole('button', { name: 'Approve' })).toBeDisabled();
        });

        it('shows the incomplete warning when an empty date field blurs', () => {
            renderDialog();
            const { end } = dateInputs();

            setDatetimeValidity(end, true);
            fireEvent.blur(end);

            expect(screen.getByText('Enter both a date and a time.')).toBeInTheDocument();
            expect(screen.getByRole('button', { name: 'Approve' })).toBeDisabled();
        });

        it('blocks a date entered without a time', () => {
            const onConfirm = jest.fn();
            renderDialog(onConfirm);
            const { start } = dateInputs();

            fireEvent.change(start, { target: { value: '2099-03-01T09:00' } });
            setDatetimeValidity(start, true);
            fireEvent.change(start, { target: { value: '' } });

            expect(screen.getByText('Enter both a date and a time.')).toBeInTheDocument();
            expect(screen.getByRole('button', { name: 'Approve' })).toBeDisabled();
            fireEvent.click(screen.getByRole('button', { name: 'Approve' }));
            expect(onConfirm).not.toHaveBeenCalled();
        });

        it('refuses a start that passes the current minute while the dialog stays open', () => {
            const onConfirm = jest.fn();
            renderDialog(onConfirm);
            const { start } = dateInputs();

            fireEvent.change(start, { target: { value: toDatetimeLocalValue(new Date()) } });
            expect(screen.getByRole('button', { name: 'Approve' })).toBeEnabled();

            jest.setSystemTime(new Date('2026-06-15T10:07:00.000Z'));
            fireEvent.click(screen.getByRole('button', { name: 'Approve' }));

            expect(onConfirm).not.toHaveBeenCalled();
            expect(screen.getByText(/must not be earlier than the current minute/i)).toBeInTheDocument();
            expect(screen.getByRole('button', { name: 'Approve' })).toBeDisabled();
        });
    });
});
