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

const BASE = '/environments/DEFAULT/api-products/product-1';

function renderNav(showDefinitionNav: boolean) {
    return render(
        <MemoryRouter initialEntries={[BASE]}>
            <Routes>
                <Route
                    path="/environments/:envId/api-products/:productId/*"
                    element={<ApiProductSidebarNav basePath={BASE} showDefinitionNav={showDefinitionNav} />}
                />
            </Routes>
        </MemoryRouter>,
    );
}

describe('ApiProductSidebarNav', () => {
    it('places Resources in the Design group when showDefinitionNav is true', () => {
        renderNav(true);
        expect(screen.getByText('Design')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /^resources$/i })).toHaveAttribute('href', `${BASE}/resources`);
    });

    it('omits the Design group when showDefinitionNav is false', () => {
        renderNav(false);
        expect(screen.queryByText('Design')).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /^resources$/i })).not.toBeInTheDocument();
    });

    it('links to the Policy Studio right after Plans when showDefinitionNav is true', () => {
        renderNav(true);

        const labels = screen.getAllByRole('link').map(link => link.textContent);
        expect(labels.slice(labels.indexOf('Plans'), labels.indexOf('Plans') + 2)).toEqual(['Plans', 'Policy Studio']);
        expect(screen.getByRole('link', { name: 'Policy Studio' })).toHaveAttribute('href', `${BASE}/policy-studio`);
    });

    it('hides the Policy Studio when showDefinitionNav is false', () => {
        renderNav(false);

        expect(screen.queryByRole('link', { name: 'Policy Studio' })).not.toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Plans' })).toBeInTheDocument();
    });
});
