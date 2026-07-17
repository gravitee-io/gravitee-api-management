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
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { setupCatalogDatabaseTests } from '../../editor/services/catalog.test-utils';
import { ApplicationsView } from './ApplicationsView';

describe('ApplicationsView', () => {
    setupCatalogDatabaseTests();

    it('should render paginated application list with mock data', async () => {
        render(<ApplicationsView />);

        expect(await screen.findByRole('heading', { name: 'Applications' })).toBeInTheDocument();
        expect(await screen.findByText('Mobile Banking App')).toBeInTheDocument();
        expect(screen.getByText('Internal Tools')).toBeInTheDocument();
    });

    it('should open create form when Create is clicked', async () => {
        const user = userEvent.setup();
        render(<ApplicationsView />);

        await screen.findByText('Mobile Banking App');
        await user.click(screen.getByRole('button', { name: 'Create' }));

        expect(await screen.findByRole('heading', { name: 'Create application' })).toBeInTheDocument();
        expect(screen.getByRole('radiogroup', { name: 'Application type' })).toBeInTheDocument();
    });

    it('should adapt form fields based on application type', async () => {
        const user = userEvent.setup();
        render(<ApplicationsView />);

        await screen.findByText('Mobile Banking App');
        await user.click(screen.getByRole('button', { name: 'Create' }));
        await screen.findByRole('heading', { name: 'Create application' });

        await user.click(screen.getByRole('radio', { name: /SPA/i }));
        expect(screen.getByText('Redirect URIs *')).toBeInTheDocument();
        expect(screen.queryByText(/Client Credentials \(Mandatory\)/i)).not.toBeInTheDocument();

        await user.click(screen.getByRole('radio', { name: /Backend to backend/i }));
        expect(screen.queryByText('Redirect URIs *')).not.toBeInTheDocument();
        expect(screen.getByText(/Client Credentials \(Mandatory\)/i)).toBeInTheDocument();
    });

    it('should open details with settings when a card is clicked', async () => {
        const user = userEvent.setup();
        render(<ApplicationsView />);

        await screen.findByText('Partner Integration');
        await user.click(screen.getByRole('button', { name: /Partner Integration/i }));

        expect(await screen.findByRole('heading', { name: 'Partner Integration' })).toBeInTheDocument();
        expect(screen.getByText('partner-b2b-client')).toBeInTheDocument();
    });

    it('should pre-fill edit form values from selected application', async () => {
        const user = userEvent.setup();
        render(<ApplicationsView />);

        await screen.findByText('Customer Web Portal');
        await user.click(screen.getByRole('button', { name: /Customer Web Portal/i }));
        await screen.findByRole('heading', { name: 'Customer Web Portal' });

        await user.click(screen.getByRole('button', { name: 'Edit' }));
        expect(await screen.findByRole('heading', { name: 'Edit application' })).toBeInTheDocument();

        const nameInput = screen.getByPlaceholderText('Application name') as HTMLInputElement;
        expect(nameInput.value).toBe('Customer Web Portal');
    });

    it('should show members tab with mock members', async () => {
        const user = userEvent.setup();
        render(<ApplicationsView />);

        await screen.findByText('Mobile Banking App');
        await user.click(screen.getByRole('button', { name: /Mobile Banking App/i }));
        await screen.findByRole('heading', { name: 'Mobile Banking App' });

        await user.click(screen.getByRole('tab', { name: 'Members' }));

        expect(await screen.findByText('Members (2)')).toBeInTheDocument();
        expect(screen.getByText('Admin User')).toBeInTheDocument();
        expect(screen.getByText('Mobile Developer')).toBeInTheDocument();
    });

    it('should delete application and return to list', async () => {
        const user = userEvent.setup();
        render(<ApplicationsView />);

        await screen.findByText('Internal Tools');
        await user.click(screen.getByRole('button', { name: /Internal Tools/i }));
        await screen.findByRole('heading', { name: 'Internal Tools' });

        await user.click(screen.getByRole('button', { name: 'Delete' }));

        const dialog = await screen.findByRole('alertdialog');
        expect(dialog).toHaveTextContent('Are you sure you want to delete');
        await user.click(within(dialog).getByRole('button', { name: 'Delete' }));

        await waitFor(() => {
            expect(screen.getByRole('heading', { name: 'Applications' })).toBeInTheDocument();
        });
        expect(screen.queryByText('Internal Tools')).not.toBeInTheDocument();
    });

    it('should not navigate when block is in edit mode', async () => {
        const user = userEvent.setup();
        render(<ApplicationsView isEditable />);

        await screen.findByText('Mobile Banking App');
        await user.click(screen.getByRole('button', { name: /Mobile Banking App/i }));

        await waitFor(() => {
            expect(screen.queryByRole('heading', { name: 'Mobile Banking App', level: 2 })).not.toBeInTheDocument();
        });
    });
});
