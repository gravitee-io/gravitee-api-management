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
import { LayoutSlotsProvider } from '@gravitee/graphene-core';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { CloudLayout } from '../components/CloudLayout';
import { CloudOverviewPage } from './CloudOverviewPage';
import { useEnvironmentStore } from '../../../features/environment/environment.store';
import { addCloudEnvironment, useCloudEnvironmentStore } from '../cloud-environment.store';

function renderCloudOverview() {
    useCloudEnvironmentStore.getState().reset();
    useEnvironmentStore.setState({
        environments: [{ id: 'env-1', name: 'Default', organizationId: 'org-1', hrids: ['default'] }],
        currentEnvironment: { id: 'env-1', name: 'Default', organizationId: 'org-1', hrids: ['default'] },
        initialized: true,
        loading: false,
    });

    return render(
        <MemoryRouter initialEntries={['/environments/default/cloud/dashboard']}>
            <LayoutSlotsProvider>
                <Routes>
                    <Route path="/environments/:envHrid/cloud" element={<CloudLayout />}>
                        <Route path="dashboard" element={<CloudOverviewPage />} />
                    </Route>
                </Routes>
            </LayoutSlotsProvider>
        </MemoryRouter>,
    );
}

describe('CloudOverviewPage', () => {
    it('renders Cockpit-style overview sections', () => {
        renderCloudOverview();

        expect(screen.getByRole('heading', { name: 'Overview' })).toBeTruthy();
        expect(screen.getByText('Get an overview of your Gravitee Cloud account.')).toBeTruthy();
        expect(screen.getByTestId('cloud-home-environments')).toBeTruthy();
        expect(screen.getByTestId('cloud-home-gateways')).toBeTruthy();
        expect(screen.getByTestId('deploy-gateways-button')).toBeTruthy();
        expect(screen.getAllByTestId('gateway-list-row').length).toBe(2);
        expect(screen.getByText('Staging Gateway')).toBeTruthy();
        expect(screen.getByTestId('cloud-home-trial-countdown')).toBeTruthy();
        expect(screen.getByTestId('cloud-home-carousel')).toBeTruthy();
        expect(screen.getByTestId('environment-list-title').textContent).toBe('Default');
    });

    it('shows product filter toggles including AM', () => {
        renderCloudOverview();

        const filterGroup = screen.getByLabelText('Filter by product');
        expect(filterGroup.textContent).toContain('All Products');
        expect(filterGroup.textContent).toContain('APIM');
        expect(screen.getByRole('radio', { name: 'AM' })).toBeTruthy();
        expect(screen.getByRole('radio', { name: 'AM' }).hasAttribute('disabled')).toBe(false);
    });

    it('filters environments by product when APIM or AM is selected', async () => {
        const user = userEvent.setup();
        renderCloudOverview();

        addCloudEnvironment({ name: 'APIM Staging', hrid: 'apim-staging', product: 'APIM' });
        addCloudEnvironment({ name: 'AM Production', hrid: 'am-production', product: 'AM' });

        await waitFor(() => {
            expect(screen.getAllByTestId('environment-list-title').map(node => node.textContent)).toEqual([
                'Default',
                'AM Production',
                'APIM Staging',
            ]);
        });

        await user.click(screen.getByRole('radio', { name: 'APIM' }));
        await waitFor(() => {
            expect(screen.getAllByTestId('environment-list-title').map(node => node.textContent)).toEqual([
                'Default',
                'APIM Staging',
            ]);
        });

        await user.click(screen.getByRole('radio', { name: 'AM' }));
        await waitFor(() => {
            expect(screen.getAllByTestId('environment-list-title').map(node => node.textContent)).toEqual(['AM Production']);
        });

        await user.click(screen.getByRole('radio', { name: 'All Products' }));
        await waitFor(() => {
            expect(screen.getAllByTestId('environment-list-title').map(node => node.textContent)).toEqual([
                'Default',
                'AM Production',
                'APIM Staging',
            ]);
        });
    });

    it('shows new environment action on dashboard', () => {
        renderCloudOverview();

        expect(screen.getByTestId('new-environment-button')).toBeTruthy();
    });

    it('adds created environments under Default in the environment list', async () => {
        renderCloudOverview();

        addCloudEnvironment({ name: 'Staging', hrid: 'staging', product: 'APIM' });

        expect(useEnvironmentStore.getState().environments.map(env => env.name)).toEqual(['Default', 'Staging']);
        await waitFor(() => {
            expect(screen.getAllByTestId('environment-list-title').map(node => node.textContent)).toEqual(['Default', 'Staging']);
        });
    });
});
