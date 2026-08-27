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

import { CreateApiGate } from './CreateApiGate';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));

const mockUseHasPermission = useHasPermission as jest.Mock;

function renderGate() {
    return render(
        <MemoryRouter initialEntries={['/apis/new/scratch']}>
            <Routes>
                <Route path="/apis/new" element={<CreateApiGate />}>
                    <Route path="scratch" element={<div>Scratch wizard</div>} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('CreateApiGate', () => {
    afterEach(() => jest.clearAllMocks());

    it('checks the environment-api-c permission and renders the outlet when granted', () => {
        mockUseHasPermission.mockReturnValue(true);
        renderGate();

        expect(mockUseHasPermission).toHaveBeenCalledWith(expect.objectContaining({ anyOf: ['environment-api-c'] }));
        expect(screen.getByText('Scratch wizard')).toBeInTheDocument();
    });

    it('renders the fallback instead of the outlet when the permission is denied', () => {
        mockUseHasPermission.mockReturnValue(false);
        renderGate();

        expect(screen.getByText(/don.t have permission to create apis/i)).toBeInTheDocument();
        expect(screen.queryByText('Scratch wizard')).not.toBeInTheDocument();
    });
});
