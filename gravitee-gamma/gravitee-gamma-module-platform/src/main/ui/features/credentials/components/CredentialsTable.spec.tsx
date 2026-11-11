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
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

import { CredentialsTable } from './CredentialsTable';
import type { Credential } from '../types/credential';

const OPENAI: Credential = {
    id: 'cred-1',
    name: 'OpenAI production',
    strategy: 'BEARER',
    serviceId: 'openai',
    ownerId: 'user-1',
    expiresAt: '2099-03-01T00:00:00Z',
};
const LEGACY: Credential = { id: 'cred-2', name: 'Legacy integration', strategy: 'BASIC', expiresAt: '2020-08-01T00:00:00Z' };

function renderTable(overrides: Partial<React.ComponentProps<typeof CredentialsTable>> = {}) {
    return render(
        <MemoryRouter>
            <CredentialsTable
                credentials={[OPENAI, LEGACY]}
                ownerNames={{ 'user-1': 'Alex Doyle' }}
                totalCount={2}
                loading={false}
                search=""
                page={1}
                pageSize={25}
                canEdit={() => false}
                canDelete={() => false}
                onSearchChange={jest.fn()}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                onEdit={jest.fn()}
                onDelete={jest.fn()}
                {...overrides}
            />
        </MemoryRouter>,
    );
}

describe('CredentialsTable', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    it('shows the name, service, type, owner and expiry of each credential', () => {
        renderTable();

        expect(screen.getByRole('columnheader', { name: /Service/ })).toBeInTheDocument();

        const openAiRow = screen.getByRole('link', { name: 'OpenAI production' }).closest('tr')!;
        expect(screen.getByRole('link', { name: 'OpenAI production' })).toHaveAttribute('href', '/cred-1');
        expect(within(openAiRow).getByText('openai')).toBeInTheDocument();
        expect(within(openAiRow).getByText('Bearer token')).toBeInTheDocument();
        expect(within(openAiRow).getByText('Alex Doyle')).toBeInTheDocument();
        expect(within(openAiRow).queryByText('Expired')).not.toBeInTheDocument();

        const legacyRow = screen.getByRole('link', { name: 'Legacy integration' }).closest('tr')!;
        expect(within(legacyRow).getByText('Not set')).toBeInTheDocument();
        expect(within(legacyRow).getByText('Basic auth')).toBeInTheDocument();
        expect(within(legacyRow).getByText('Expired')).toBeInTheDocument();
    });

    it('hides the row actions without the update and delete permissions', () => {
        renderTable();

        expect(screen.queryByRole('button', { name: 'OpenAI production actions' })).not.toBeInTheDocument();
    });

    it('offers edit and delete with the permissions', async () => {
        const onEdit = jest.fn();
        const onDelete = jest.fn();
        renderTable({ canEdit: () => true, canDelete: () => true, onEdit, onDelete });

        await userEvent.click(screen.getByRole('button', { name: 'OpenAI production actions' }));
        await userEvent.click(await screen.findByRole('menuitem', { name: 'Edit' }));
        expect(onEdit).toHaveBeenCalledWith(OPENAI);

        await userEvent.click(screen.getByRole('button', { name: 'OpenAI production actions' }));
        await userEvent.click(await screen.findByRole('menuitem', { name: 'Delete' }));
        expect(onDelete).toHaveBeenCalledWith(OPENAI);
    });
});
