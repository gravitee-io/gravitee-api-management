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

import { UserProfileCard } from './UserProfileCard';
import type { OrganizationUser } from '../types/user';

jest.mock('../../../shared/copyToClipboard', () => ({
    copyTextToClipboardWithNotifyHandler: jest.fn(),
}));

const { copyTextToClipboardWithNotifyHandler } = jest.requireMock('../../../shared/copyToClipboard') as {
    copyTextToClipboardWithNotifyHandler: jest.Mock;
};

const USER: OrganizationUser = {
    id: 'user-1',
    displayName: 'Anna Schmidt',
    email: 'anna.schmidt@company.com',
    source: 'ldap',
    status: 'ACTIVE',
    created_at: Date.parse('2025-07-10T09:15:00.000Z'),
    lastConnectionAt: Date.parse('2025-08-01T14:45:00.000Z'),
    roles: [{ id: 'org-user', name: 'USER', scope: 'ORGANIZATION' }],
    customFields: { department: 'Operations' },
};

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

describe('UserProfileCard', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders profile timestamps as dates', () => {
        renderWithGraphene(<UserProfileCard user={USER} />);

        expect(screen.getByText(/Jul 10, 2025|10 Jul 2025/)).toBeTruthy();
        expect(screen.getByText(/Aug 1, 2025|1 Aug 2025/)).toBeTruthy();
    });

    it('copies email, source, and custom field values', async () => {
        const user = userEvent.setup();
        renderWithGraphene(<UserProfileCard user={USER} />);

        await user.click(screen.getByRole('button', { name: 'Copy email' }));
        await user.click(screen.getByRole('button', { name: 'Copy source' }));
        await user.click(screen.getByRole('button', { name: 'Copy department' }));

        expect(copyTextToClipboardWithNotifyHandler).toHaveBeenCalledWith('anna.schmidt@company.com', 'Copied to clipboard');
        expect(copyTextToClipboardWithNotifyHandler).toHaveBeenCalledWith('ldap', 'Copied to clipboard');
        expect(copyTextToClipboardWithNotifyHandler).toHaveBeenCalledWith('Operations', 'Copied to clipboard');
    });
});
