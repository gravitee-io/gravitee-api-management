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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { useApiDetail } from '../../../../hooks/useApiDetail';
import { useResourcePlugins, useResourceSchema, useUpdateApiResources } from '../../../../hooks/useApiResources';
import type { ApiResource, ResourcePlugin } from '../../../../types/resource';
import { ApiResourceWizardPage } from '../ApiResourceWizardPage';

jest.mock('../../../../hooks/useApiDetail');
jest.mock('../../../../hooks/useApiResources');

const mockApiDetail = useApiDetail as jest.Mock;
const mockPlugins = useResourcePlugins as jest.Mock;
const mockSchema = useResourceSchema as jest.Mock;
const mockUpdate = useUpdateApiResources as jest.Mock;

const cachePlugin: ResourcePlugin = {
    id: 'cache',
    name: 'Redis Cache',
    description: 'A shared cache',
    category: 'Cache',
};

function renderWizard(resources: ApiResource[] = []) {
    mockApiDetail.mockReturnValue({ data: { resources }, isLoading: false });
    return render(
        <MemoryRouter>
            <ApiResourceWizardPage />
        </MemoryRouter>,
    );
}

describe('ApiResourceWizardPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockPlugins.mockReturnValue({ data: [cachePlugin], isLoading: false });
        mockSchema.mockReturnValue({ data: { type: 'object', properties: {} }, isLoading: false, isError: false });
    });

    it('creates a resource and submits the full list with it appended', async () => {
        const mutate = jest.fn();
        mockUpdate.mockReturnValue({ mutate, isPending: false });
        renderWizard();
        const user = userEvent.setup();

        await user.click(screen.getByRole('button', { name: /redis cache/i }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        await user.type(await screen.findByLabelText('Resource name'), 'my-cache');
        await user.click(screen.getByRole('button', { name: /create resource/i }));

        await waitFor(() => expect(mutate).toHaveBeenCalledTimes(1));
        expect(mutate).toHaveBeenCalledWith([{ name: 'my-cache', type: 'cache', enabled: true, configuration: {} }], expect.anything());
    });

    /*
     * A review step whose only distinct content was a raw JSON dump added a click with nothing new to say —
     * often literally rendering "{}" for a plugin with no configuration fields. Configuring a resource is two
     * steps: pick the type, then name and configure it; that step is also where it is created.
     */
    it('has no review step: naming and configuring a resource is the step that creates it', async () => {
        mockUpdate.mockReturnValue({ mutate: jest.fn(), isPending: false });
        renderWizard();
        const user = userEvent.setup();

        await user.click(screen.getByRole('button', { name: /redis cache/i }));
        await user.click(screen.getByRole('button', { name: 'Next' }));

        expect(await screen.findByText('Step 2 of 2')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /create resource/i })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Next' })).not.toBeInTheDocument();
        expect(screen.queryByText(/review/i)).not.toBeInTheDocument();
    });

    it('returns to the resources list when leaving the edit wizard', async () => {
        mockUpdate.mockReturnValue({ mutate: jest.fn(), isPending: false });
        mockApiDetail.mockReturnValue({
            data: { resources: [{ name: 'my-cache', type: 'cache', enabled: true, configuration: {} }] },
            isLoading: false,
        });
        render(
            <MemoryRouter initialEntries={['/apis/api-1/resources/my-cache/edit']}>
                <Routes>
                    <Route path="apis/:apiId/resources">
                        <Route index element={<div>RESOURCES LIST</div>} />
                        <Route path=":resourceName/edit" element={<ApiResourceWizardPage />} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
        const user = userEvent.setup();

        await user.click(await screen.findByRole('button', { name: /back to resources/i }));

        expect(await screen.findByText('RESOURCES LIST')).toBeInTheDocument();
    });
});
