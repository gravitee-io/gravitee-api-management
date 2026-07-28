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
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { UserRegistrationPendingBanner } from './UserRegistrationPendingBanner';

beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: jest.fn(),
            removeListener: jest.fn(),
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn(),
        })),
    });
});

describe('UserRegistrationPendingBanner', () => {
    it('invokes accept and reject callbacks from the banner actions', async () => {
        const user = userEvent.setup();
        const onAccept = jest.fn();
        const onReject = jest.fn();

        renderWithGraphene(<UserRegistrationPendingBanner isPending={false} onAccept={onAccept} onReject={onReject} />);

        await user.click(screen.getByRole('button', { name: 'Accept user registration' }));
        await user.click(screen.getByRole('button', { name: 'Reject user registration' }));

        expect(onAccept).toHaveBeenCalledTimes(1);
        expect(onReject).toHaveBeenCalledTimes(1);
    });

    it('disables banner actions while registration is processing', () => {
        renderWithGraphene(<UserRegistrationPendingBanner isPending onAccept={jest.fn()} onReject={jest.fn()} />);

        expect(screen.getByRole('button', { name: 'Accept user registration' })).toHaveProperty('disabled', true);
        expect(screen.getByRole('button', { name: 'Reject user registration' })).toHaveProperty('disabled', true);
    });
});
