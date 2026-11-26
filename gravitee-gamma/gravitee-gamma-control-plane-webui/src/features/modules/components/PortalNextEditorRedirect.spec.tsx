/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { PortalNextEditorRedirect } from './PortalNextEditorRedirect';
import { TEST_CONFIG } from '../../../testing/factories';
import { seedBootstrap, seedEnvironments } from '../../../testing/helpers';
import { redirectToPortalNextEditor } from '../portal-next';

jest.mock('../portal-next', () => ({
    ...jest.requireActual('../portal-next'),
    redirectToPortalNextEditor: jest.fn(),
}));

const mockRedirect = redirectToPortalNextEditor as jest.Mock;

describe('PortalNextEditorRedirect', () => {
    afterEach(() => {
        mockRedirect.mockClear();
    });

    it('should replace the current tab with the portal-next editor', () => {
        seedBootstrap();
        seedEnvironments();

        render(
            <MemoryRouter initialEntries={['/environments/env-1/portals']}>
                <Routes>
                    <Route path="/environments/:envHrid/portals/*" element={<PortalNextEditorRedirect />} />
                </Routes>
            </MemoryRouter>,
        );

        expect(mockRedirect).toHaveBeenCalledWith(TEST_CONFIG.consoleUrl, 'env-1');
    });
});
