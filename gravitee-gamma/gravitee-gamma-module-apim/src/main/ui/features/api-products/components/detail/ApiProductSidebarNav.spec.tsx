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

import { ApiProductSidebarNav } from './ApiProductSidebarNav';
import { useApiProductResourcePermissions } from '../../hooks/useApiProductPermissions';

jest.mock('../../hooks/useApiProductPermissions');

const mockPermissions = useApiProductResourcePermissions as jest.Mock;

const BASE = '/environments/DEFAULT/api-products/product-1';

function renderNav() {
    return render(
        <MemoryRouter initialEntries={[BASE]}>
            <Routes>
                <Route path="/environments/:envId/api-products/:productId/*" element={<ApiProductSidebarNav basePath={BASE} />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('ApiProductSidebarNav', () => {
    beforeEach(() => {
        mockPermissions.mockReturnValue({ canRead: true, canUpdate: true, isLoading: false });
    });

    it('places Resources in the Design group when definition can be read', () => {
        renderNav();
        expect(screen.getByText('Design')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /^resources$/i })).toHaveAttribute('href', `${BASE}/resources`);
    });

    it('omits the Design group when the user cannot read the product definition', () => {
        mockPermissions.mockReturnValue({ canRead: false, canUpdate: false, isLoading: false });
        renderNav();
        expect(screen.queryByText('Design')).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /^resources$/i })).not.toBeInTheDocument();
    });
});
