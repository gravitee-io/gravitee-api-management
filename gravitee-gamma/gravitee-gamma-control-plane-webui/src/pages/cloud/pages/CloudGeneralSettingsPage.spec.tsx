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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { CloudGeneralSettingsPage } from './CloudGeneralSettingsPage';

describe('CloudGeneralSettingsPage', () => {
    it('renders Cockpit-style account settings form', () => {
        render(<CloudGeneralSettingsPage />);

        expect(screen.getByRole('heading', { name: 'Account settings' })).toBeTruthy();
        expect(screen.getByTestId('account-id-badge').textContent).toContain('95cb6306-405c-4c87-8b63-06405c9c87eb');
        expect((screen.getByTestId('accountName') as HTMLInputElement).value).toBe('Gravitee');
        expect((screen.getByTestId('accountDescription') as HTMLTextAreaElement).value).toBe('');
        expect(screen.getByText('0 / 4000')).toBeTruthy();
        expect((screen.getByTestId('accountHrid') as HTMLInputElement).value).toBe('gravitee-1773323333030');
        expect((screen.getByTestId('save-account') as HTMLButtonElement).disabled).toBe(true);
        expect((screen.getByRole('button', { name: 'Change HRID' }) as HTMLButtonElement).disabled).toBe(true);
    });

    it('enables save when name changes', async () => {
        const user = userEvent.setup();
        render(<CloudGeneralSettingsPage />);

        await user.clear(screen.getByTestId('accountName'));
        await user.type(screen.getByTestId('accountName'), 'Gravitee Cloud');

        expect((screen.getByTestId('save-account') as HTMLButtonElement).disabled).toBe(false);
    });
});
