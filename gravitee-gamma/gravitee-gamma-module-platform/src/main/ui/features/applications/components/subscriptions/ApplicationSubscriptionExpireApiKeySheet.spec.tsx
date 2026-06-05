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

import { ApplicationSubscriptionExpireApiKeySheet } from './ApplicationSubscriptionExpireApiKeySheet';
import type { ApplicationSubscriptionApiKeyRow } from '../../types/applicationSubscription';
import { querySheetHeading } from '../test/sheetSpecHelpers';

const apiKey: ApplicationSubscriptionApiKeyRow = {
    id: 'key-1',
    key: 'secret-key-1234',
    maskedKey: '••••••••1234',
    isValid: true,
    expireAt: new Date('2099-01-01T10:00:00').getTime(),
};

function renderSheet(overrides: Partial<Parameters<typeof ApplicationSubscriptionExpireApiKeySheet>[0]> = {}) {
    return render(
        <ApplicationSubscriptionExpireApiKeySheet
            apiKey={apiKey}
            onClose={jest.fn()}
            onConfirm={jest.fn()}
            isLoading={false}
            {...overrides}
        />,
    );
}

describe('ApplicationSubscriptionExpireApiKeySheet', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        jest.setSystemTime(new Date('2025-01-01T12:00:00'));
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    function submitButton() {
        return screen.getByRole('button', { name: /Change expiration date/i }) as HTMLButtonElement;
    }

    it('does not show sheet content when apiKey is null', () => {
        render(<ApplicationSubscriptionExpireApiKeySheet apiKey={null} onClose={jest.fn()} onConfirm={jest.fn()} isLoading={false} />);
        expect(querySheetHeading(/Change your API Key/i)).toBeNull();
    });

    it('invokes onClose when Cancel is clicked', () => {
        const onClose = jest.fn();
        renderSheet({ onClose });
        fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('disables submit until the user changes the expiration date', () => {
        renderSheet();
        expect(submitButton().disabled).toBe(true);
    });

    it('enables submit after choosing a future date', () => {
        renderSheet();
        const input = screen.getByLabelText(/Expire date/i);
        fireEvent.change(input, { target: { value: '2099-06-15T14:00' } });
        expect(submitButton().disabled).toBe(false);
    });

    it('shows validation when the chosen date is not in the future', () => {
        renderSheet();
        const input = screen.getByLabelText(/Expire date/i);
        fireEvent.change(input, { target: { value: '2020-01-01T00:00' } });
        expect(screen.getByText(/Date and time must be in the future/i)).not.toBeNull();
        expect(submitButton().disabled).toBe(true);
    });

    it('shows validation when the chosen date is not a valid datetime', () => {
        renderSheet();
        const input = screen.getByLabelText(/Expire date/i);
        fireEvent.change(input, { target: { value: '2025-02-31T12:00' } });
        expect(screen.getByText(/Enter a valid date and time/i)).not.toBeNull();
        expect(submitButton().disabled).toBe(true);
    });

    it('submits a strictly parsed local datetime', () => {
        const onConfirm = jest.fn();
        renderSheet({ onConfirm });
        const input = screen.getByLabelText(/Expire date/i);

        fireEvent.change(input, { target: { value: '2099-06-15T14:00' } });
        fireEvent.click(submitButton());

        expect(onConfirm).toHaveBeenCalledWith(new Date(2099, 5, 15, 14, 0));
    });
});
