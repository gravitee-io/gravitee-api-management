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
import { buttonHarness, dataTableHarness, renderWithGraphene } from '@gravitee/graphene-core/testing';
import { screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { CredentialsTable } from './CredentialsTable';
import { useCredentialActions } from '../hooks/useCredentialActions';
import { stubLayoutApis } from '../testing/stubLayoutApis';
import type { Credential } from '../types/credential';

jest.mock('../hooks/useCredentialActions');
jest.mock('./DeleteCredentialDialog', () => ({ DeleteCredentialDialog: () => null }));

const OPENAI: Credential = {
    id: 'cred-1',
    name: 'OpenAI production',
    strategy: 'BEARER',
    serviceId: 'openai',
    ownerId: 'user-1',
    ownerName: 'Alex Doyle',
    expiresAt: '2099-03-01T00:00:00Z',
};
const LEGACY: Credential = {
    id: 'cred-2',
    name: 'Legacy integration',
    strategy: 'BASIC',
    ownerId: 'deleted-user',
    expiresAt: '2020-08-01T00:00:00Z',
};

function renderTable(overrides: Partial<React.ComponentProps<typeof CredentialsTable>> = {}) {
    return renderWithGraphene(
        <MemoryRouter>
            <CredentialsTable
                credentials={[OPENAI, LEGACY]}
                totalCount={2}
                loading={false}
                searchRef={{ current: null }}
                search=""
                hasCriteria={false}
                page={1}
                pageSize={10}
                pageSizeOptions={[10, 25, 50, 100]}
                onSearchChange={jest.fn()}
                onReset={jest.fn()}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                {...overrides}
            />
        </MemoryRouter>,
    );
}

describe('CredentialsTable', () => {
    beforeAll(stubLayoutApis);

    beforeEach(() => {
        jest.mocked(useCredentialActions).mockReturnValue({ canEdit: () => false, canDelete: () => false });
    });

    it('shows the name, service, type, expiry and owner of each credential', () => {
        renderTable();
        const table = dataTableHarness();

        expect(table.getHeaders()).toEqual(expect.arrayContaining(['Name', 'Service', 'Type', 'Expires', 'Owner']));
        const openAi = table.getRow('OpenAI production');
        expect(screen.getByRole('link', { name: 'OpenAI production' })).toHaveAttribute('href', '/cred-1');
        expect(openAi.getCellText('Service')).toBe('openai');
        expect(openAi.getCellText('Type')).toBe('Bearer token');
        expect(openAi.getCellText('Owner')).toBe('Alex Doyle');
        expect(openAi.getCellText('Expires')).not.toContain('Expired');

        const legacy = table.getRow('Legacy integration');
        expect(legacy.getCellText('Service')).toBe('Not set');
        expect(legacy.getCellText('Type')).toBe('Basic auth');
        expect(legacy.getCellText('Expires')).toContain('Expired');
    });

    it('never shows the id of an owner whose name the server does not know', () => {
        renderTable();

        expect(dataTableHarness().getRow('Legacy integration').getCellText('Owner')).toBe('Unknown user');
    });

    it('lets the user choose the columns to show', () => {
        renderTable();

        expect(screen.getByRole('button', { name: /view/i })).toBeInTheDocument();
    });

    it('hides the row actions without the update and delete permissions', () => {
        renderTable();

        expect(screen.queryByRole('button', { name: 'Actions for OpenAI production' })).not.toBeInTheDocument();
    });

    it('offers edit and delete with the permissions, delete last', async () => {
        jest.mocked(useCredentialActions).mockReturnValue({ canEdit: () => true, canDelete: () => true });
        renderTable();

        await buttonHarness({ name: 'Actions for OpenAI production' }).click();

        const items = await screen.findAllByRole('menuitem');
        expect(items.map(item => item.textContent)).toEqual(['Edit', 'Delete']);
        expect(screen.getByRole('menuitem', { name: 'Edit' })).toHaveAttribute('href', '/cred-1/edit');
    });

    it('offers a reset only while a search is active', async () => {
        const onReset = jest.fn();
        const { unmount } = renderTable();
        expect(screen.queryByRole('button', { name: 'Reset' })).not.toBeInTheDocument();
        unmount();

        renderTable({ search: 'open', hasCriteria: true, credentials: [], totalCount: 0, onReset });
        expect(screen.getByText('No credential matches your search')).toBeInTheDocument();
        await buttonHarness({ name: 'Reset' }).click();

        expect(onReset).toHaveBeenCalled();
    });
});
