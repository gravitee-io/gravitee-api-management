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
import { MemoryRouter } from 'react-router-dom';

import { SharedApplicationApiKeysHint } from './SharedApplicationApiKeysHint';

describe('SharedApplicationApiKeysHint', () => {
    it('points a shared subscription at the application shared keys page', () => {
        render(
            <MemoryRouter>
                <SharedApplicationApiKeysHint to="/applications/app-1/shared-api-keys" />
            </MemoryRouter>,
        );

        expect(screen.getByRole('link', { name: 'Shared API Keys' })).toHaveAttribute('href', '/applications/app-1/shared-api-keys');
        expect(screen.getByText(/manage it from/i)).toBeInTheDocument();
    });
});
