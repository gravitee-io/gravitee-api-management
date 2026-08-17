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
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';

import { SharedPolicyGroupDetailPage } from './SharedPolicyGroupDetailPage';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';

const SPG: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    description: 'Reusable auth policies',
    prerequisiteMessage: 'Requires the "auth-cache" resource',
    lifecycleState: 'DEPLOYED',
    apiType: 'PROXY',
    phase: 'REQUEST',
    steps: [{ name: 'jwt' }],
    updatedAt: '2024-01-01T00:00:00.000Z',
    deployedAt: '2024-01-02T00:00:00.000Z',
};

function renderPage(sharedPolicyGroup: SharedPolicyGroup = SPG) {
    return render(
        <MemoryRouter initialEntries={['/spg-1/overview']}>
            <Routes>
                <Route path=":sharedPolicyGroupId" element={<Outlet context={sharedPolicyGroup} />}>
                    <Route path="overview" element={<SharedPolicyGroupDetailPage />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('SharedPolicyGroupDetailPage', () => {
    it('renders overview details: API type, phase, and prerequisite', () => {
        renderPage();

        expect(screen.getByTestId('shared-policy-group-overview')).not.toBeNull();
        expect(screen.getByText('Proxy')).not.toBeNull();
        expect(screen.getByText('Request')).not.toBeNull();
        expect(screen.getByText('Requires the "auth-cache" resource')).not.toBeNull();
    });
});
