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

import { ApiResourcesPage } from './ApiResourcesPage';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { useResourcePlugins, useUpdateApiResources } from '../../../hooks/useApiResources';

jest.mock('../../../hooks/useApiDetail');
jest.mock('../../../hooks/useApiResources');

const mockUseApiDetail = useApiDetail as jest.Mock;

describe('ApiResourcesPage status badge', () => {
    beforeEach(() => {
        (useResourcePlugins as jest.Mock).mockReturnValue({ data: [{ id: 'oauth2-am-resource', name: 'AM Resource' }] });
        (useUpdateApiResources as jest.Mock).mockReturnValue({ isPending: false, mutate: jest.fn() });
    });

    it('uses the success badge when the resource is enabled and not when it is disabled', () => {
        mockUseApiDetail.mockReturnValue({
            data: {
                resources: [
                    { name: 'am', type: 'oauth2-am-resource', enabled: true, configuration: {} },
                    { name: 'cache', type: 'cache', enabled: false, configuration: {} },
                ],
            },
            isLoading: false,
            isError: false,
        });

        render(
            <MemoryRouter initialEntries={['/apis/api-1/resources']}>
                <Routes>
                    <Route path="/apis/:apiId/resources" element={<ApiResourcesPage />} />
                </Routes>
            </MemoryRouter>,
        );

        const enabled = screen.getByText('Enabled').closest('[class]');
        const disabled = screen.getByText('Disabled').closest('[class]');
        expect(enabled?.className).toContain('success');
        expect(disabled?.className ?? '').not.toContain('success');
    });
});
