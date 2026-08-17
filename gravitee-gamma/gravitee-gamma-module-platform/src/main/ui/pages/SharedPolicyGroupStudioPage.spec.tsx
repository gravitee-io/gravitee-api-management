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

import { SharedPolicyGroupStudioPage } from './SharedPolicyGroupStudioPage';
import { useSharedPolicyGroupDetail } from '../features/shared-policy-groups/hooks/useSharedPolicyGroups';
import type { SharedPolicyGroup } from '../features/shared-policy-groups/types/sharedPolicyGroup';

jest.mock('../features/shared-policy-groups/hooks/useSharedPolicyGroups');

const mockUseDetail = jest.mocked(useSharedPolicyGroupDetail);

const BASE: SharedPolicyGroup = {
    id: 'spg-1',
    name: 'Auth Bundle',
    apiType: 'PROXY',
    phase: 'REQUEST',
};

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/spg-1/studio']}>
            <Routes>
                <Route path=":sharedPolicyGroupId/studio" element={<SharedPolicyGroupStudioPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('SharedPolicyGroupStudioPage', () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it('shows the empty studio state when there are no policies', () => {
        mockUseDetail.mockReturnValue({
            data: { ...BASE, steps: [] },
            isLoading: false,
            isError: false,
        } as never);

        renderPage();
        expect(screen.getByTestId('shared-policy-group-studio-empty')).not.toBeNull();
    });

    it('shows a read-only summary when policies are already configured', () => {
        mockUseDetail.mockReturnValue({
            data: {
                ...BASE,
                steps: [{ name: 'JWT' }, { policy: 'rate-limit' }],
            },
            isLoading: false,
            isError: false,
        } as never);

        renderPage();
        expect(screen.getByTestId('shared-policy-group-studio-configured')).not.toBeNull();
        expect(screen.getByText('2 policies configured')).not.toBeNull();
        expect(screen.getByText('JWT')).not.toBeNull();
        expect(screen.getByText('rate-limit')).not.toBeNull();
    });
});
