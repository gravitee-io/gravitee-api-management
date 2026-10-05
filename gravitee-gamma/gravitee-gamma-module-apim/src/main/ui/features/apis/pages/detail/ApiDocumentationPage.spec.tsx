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
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiDocumentationPage } from './ApiDocumentationPage';
import { useApiDetailContext } from '../../context/ApiDetailContext';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(() => true),
}));

jest.mock('../../context/ApiDetailContext', () => ({
    useApiDetailContext: jest.fn(),
}));

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseApiDetailContext = useApiDetailContext as jest.Mock;

function renderPage() {
    render(
        <MemoryRouter initialEntries={['/apis/api-1/documentation']}>
            <Routes>
                <Route path="apis/:apiId/documentation" element={<ApiDocumentationPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseHasPermission.mockReturnValue(true);
    mockUseApiDetailContext.mockReturnValue({ api: { id: 'api-1' }, isLoading: false, permissionsReady: true });
});

describe('ApiDocumentationPage', () => {
    it('renders the documentation heading for a user who can read it', () => {
        renderPage();

        expect(screen.getByRole('heading', { name: 'Documentation' })).toBeInTheDocument();
    });

    it('renders nothing until the permission request has resolved', () => {
        mockUseApiDetailContext.mockReturnValue({ api: { id: 'api-1' }, isLoading: false, permissionsReady: false });
        renderPage();

        expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    });

    // The nav entry is hidden without the permission, but the route stays reachable by URL.
    it('tells a user without api-documentation-r that they cannot view the documentation', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderPage();

        expect(screen.getByText(/don.t have permission to view/i)).toBeInTheDocument();
    });

    it('asks for api-documentation-r, the same permission the nav entry is gated on', () => {
        renderPage();

        expect(mockUseHasPermission).toHaveBeenCalledWith({ anyOf: ['api-documentation-r'] });
    });
});
