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

import { CloudAccountTokensPage } from './CloudAccountTokensPage';

describe('CloudAccountTokensPage', () => {
    it('renders Cockpit-style account tokens empty state', () => {
        render(<CloudAccountTokensPage />);

        expect(screen.getByRole('heading', { name: 'Account Tokens' })).toBeTruthy();
        expect(screen.getByTestId('account-id-badge').textContent).toContain('95cb6306-405c-4c87-8b63-06405c9c87eb');
        expect(screen.getByText('Your Account Tokens')).toBeTruthy();
        expect(screen.getByTestId('generate-token-button')).toBeTruthy();
        expect(
            screen.getByText(
                'Account tokens enable interaction with the Gravitee Cloud REST API, facilitating the automation of various Gravitee Cloud tasks, including creating environments and organizations.',
            ),
        ).toBeTruthy();
        expect(screen.getByText('A maximum of 10 account tokens can be active at the same time.')).toBeTruthy();
        expect(screen.getByTestId('account-tokens-empty')).toBeTruthy();
        expect(screen.getByText('No tokens... yet')).toBeTruthy();
        expect(screen.getByText('There are no tokens generated for this account.')).toBeTruthy();
    });
});
