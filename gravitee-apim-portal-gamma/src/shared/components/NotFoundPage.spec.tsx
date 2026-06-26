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
import { screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { renderPortalUi } from '../../testing/render-portal-ui';
import { NotFoundPage } from './NotFoundPage';

describe('NotFoundPage', () => {
    it('should render 404 message and link to homepage', () => {
        renderPortalUi(
            <MemoryRouter>
                <NotFoundPage homePath="/portals/portal-1" />
            </MemoryRouter>,
        );

        expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Back to homepage' })).toHaveAttribute('href', '/portals/portal-1');
    });

    it('should support custom labels', () => {
        renderPortalUi(
            <MemoryRouter>
                <NotFoundPage
                    homePath="/"
                    homeLabel="Back to dashboards"
                    title="Portal not found"
                    description="This portal does not exist."
                />
            </MemoryRouter>,
        );

        expect(screen.getByRole('heading', { name: 'Portal not found' })).toBeInTheDocument();
        expect(screen.getByText('This portal does not exist.')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Back to dashboards' })).toHaveAttribute('href', '/');
    });
});
