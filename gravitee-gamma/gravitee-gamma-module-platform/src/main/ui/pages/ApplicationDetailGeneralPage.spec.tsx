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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApplicationDetailGeneralPage } from './ApplicationDetailGeneralPage';
import { useApplicationDetailContext } from '../features/applications/context/ApplicationDetailContext';

jest.mock('../features/applications/context/ApplicationDetailContext');
jest.mock('../features/applications/components/general/ApplicationGeneralContent', () => ({
    ApplicationGeneralContent: () => <div data-testid="general-content" />,
}));

const mockUseApplicationDetailContext = jest.mocked(useApplicationDetailContext);

const application = {
    id: 'app-1',
    name: 'Billing',
    status: 'ACTIVE' as const,
    type: 'SIMPLE' as const,
    created_at: 0,
    updated_at: 0,
};

function renderPage(initialEntry: string, state?: object) {
    return render(
        <MemoryRouter initialEntries={[{ pathname: initialEntry, state }]}>
            <Routes>
                <Route path="/applications/:applicationId/general" element={<ApplicationDetailGeneralPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('ApplicationDetailGeneralPage', () => {
    beforeEach(() => {
        mockUseApplicationDetailContext.mockReturnValue({
            application,
            isLoading: false,
            permissionsReady: true,
            refetchPermissions: jest.fn(),
        } as ReturnType<typeof useApplicationDetailContext>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('shows a persistent banner after create navigation state', () => {
        renderPage('/applications/app-1/general', { applicationCreated: true });

        expect(screen.getByTestId('application-created-banner')).not.toBeNull();
        expect(screen.getByTestId('general-content')).not.toBeNull();
    });

    it('does not show the banner without create navigation state', () => {
        renderPage('/applications/app-1/general');

        expect(screen.queryByTestId('application-created-banner')).toBeNull();
    });
});
