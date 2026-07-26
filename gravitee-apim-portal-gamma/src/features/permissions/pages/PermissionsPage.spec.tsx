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
// The portal storage barrel pulls in the block editor, which Jest cannot parse untransformed.
jest.mock('@blocknote/core', () => ({
    BlockNoteEditor: { create: jest.fn(() => ({})) },
}));
jest.mock('../../../blocks/schema', () => ({ schema: {} }));

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { PermissionsPage } from './PermissionsPage';
import { renderPortalUi } from '../../../testing/render-portal-ui';
import { clearPortalsDatabase } from '../../portals/storage/portals.storage.test-utils';
import { createDefaultPortalTenant } from '../../tenants/storage/create-default-portal-tenant';

describe('PermissionsPage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should land on the richest seeded tenant and group on the Members tab', async () => {
        renderPortalUi(<PermissionsPage />);

        expect(await screen.findByRole('heading', { name: 'Permissions' })).toBeInTheDocument();

        // Acme Corp holds the groups, so it wins over tenants without any.
        expect(await screen.findByRole('heading', { name: 'backend-devs' })).toBeInTheDocument();
        expect(await screen.findByText('Alice Smith')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Add members' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Add asset' })).not.toBeInTheDocument();
    });

    it('should only list tenants that take part in the permissions model', async () => {
        // Every portal gets a bare "Acme" tenant with no management mode on first open.
        await createDefaultPortalTenant('portal-1');

        renderPortalUi(<PermissionsPage />);

        expect(await screen.findByRole('button', { name: /^Acme Corp/ })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /^EMEA Org/ })).toBeInTheDocument();
        expect(screen.getAllByRole('button', { name: /^Acme/ })).toHaveLength(1);
    });

    it('should open the Permissions tab and show seeded asset grants', async () => {
        renderPortalUi(<PermissionsPage />);

        await userEvent.click(await screen.findByRole('button', { name: 'Permissions' }));

        expect(await screen.findAllByRole('button', { name: /Payments API/ })).not.toHaveLength(0);
        expect(screen.getByRole('button', { name: 'Add asset' })).toBeInTheDocument();
    });

    it('should drill down into another tenant and keep the Members tab', async () => {
        renderPortalUi(<PermissionsPage />);

        await userEvent.click(await screen.findByRole('button', { name: /^Acme Corp/ }));
        await userEvent.click(await screen.findByRole('button', { name: /^backend-devs/ }));

        await waitFor(() => expect(screen.getByText('Alice Smith')).toBeInTheDocument());
        expect(screen.getByRole('combobox', { name: 'Role of Alice Smith' })).toBeInTheDocument();
    });
});
