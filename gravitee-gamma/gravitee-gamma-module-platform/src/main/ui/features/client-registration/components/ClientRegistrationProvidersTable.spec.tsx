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
import userEvent from '@testing-library/user-event';

import { ClientRegistrationProvidersTable } from './ClientRegistrationProvidersTable';
import type { ClientRegistrationProvider } from '../types/clientRegistrationProvider';

const PROVIDER: ClientRegistrationProvider = {
    id: 'prov-1',
    name: 'Okta DCR',
    description: 'Prod IdP',
    discovery_endpoint: 'https://idp.example.com/.well-known/openid-configuration',
    initial_access_token_type: 'CLIENT_CREDENTIALS',
    updated_at: Date.parse('2026-01-15T12:00:00.000Z'),
};

describe('ClientRegistrationProvidersTable', () => {
    it('renders provider name and description', () => {
        render(<ClientRegistrationProvidersTable providers={[PROVIDER]} canDelete onEdit={jest.fn()} onDelete={jest.fn()} />);
        expect(screen.getByText('Okta DCR')).not.toBeNull();
        expect(screen.getByText('Prod IdP')).not.toBeNull();
    });

    it('hides Delete when the user cannot delete', async () => {
        const user = userEvent.setup();
        render(<ClientRegistrationProvidersTable providers={[PROVIDER]} canDelete={false} onEdit={jest.fn()} onDelete={jest.fn()} />);
        await user.click(screen.getByRole('button', { name: 'Provider actions' }));
        expect(screen.getByText('Edit')).not.toBeNull();
        expect(screen.queryByText('Delete')).toBeNull();
    });

    it('calls onDelete from the kebab', async () => {
        const onDelete = jest.fn();
        const user = userEvent.setup();
        render(<ClientRegistrationProvidersTable providers={[PROVIDER]} canDelete onEdit={jest.fn()} onDelete={onDelete} />);
        await user.click(screen.getByRole('button', { name: 'Provider actions' }));
        await user.click(screen.getByText('Delete'));
        expect(onDelete).toHaveBeenCalledWith(PROVIDER);
    });
});
