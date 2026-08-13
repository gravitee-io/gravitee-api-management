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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useNavigate } from 'react-router-dom';

import { AlertsPage } from './AlertsPage';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: jest.fn(),
}));

jest.mock('../features/alerts/components/AlertsEducationalEmptyState', () => ({
    AlertsEducationalEmptyState: () => <div data-testid="alerts-educational-empty-state" />,
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseNavigate = jest.mocked(useNavigate);
const mockNavigate = jest.fn();

function renderPage() {
    return render(
        <MemoryRouter>
            <AlertsPage />
        </MemoryRouter>,
    );
}

describe('AlertsPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseNavigate.mockReturnValue(mockNavigate);
        mockUseHasPermission.mockReturnValue(true);
    });

    it('renders the page header and the educational empty state', () => {
        renderPage();

        expect(screen.getByText('Alerts')).not.toBeNull();
        expect(screen.getByText('Get notified when your gateways or platform need attention.')).not.toBeNull();
        expect(screen.getByTestId('alerts-educational-empty-state')).not.toBeNull();
    });

    it('shows Add alert when the user has create permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => anyOf.includes('environment-alert-c'));
        renderPage();

        expect(screen.getByRole('button', { name: /Add alert/i })).not.toBeNull();
        expect(screen.getByTestId('alerts-educational-empty-state')).not.toBeNull();
    });

    it('hides Add alert when the user lacks create permission', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => !anyOf.includes('environment-alert-c'));
        renderPage();

        expect(screen.queryByRole('button', { name: /Add alert/i })).toBeNull();
        expect(screen.getByTestId('alerts-educational-empty-state')).not.toBeNull();
    });

    it('navigates to new when Add alert is clicked', async () => {
        const user = userEvent.setup();
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf: string[] }) => anyOf.includes('environment-alert-c'));
        renderPage();

        await user.click(screen.getByRole('button', { name: /Add alert/i }));
        expect(mockNavigate).toHaveBeenCalledWith('new');
    });
});
