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

import { ApiScoreLayout } from './ApiScoreLayout';

function renderLayout(path = '/api-score') {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="api-score" element={<ApiScoreLayout />}>
                    <Route index element={<div>overview-outlet</div>} />
                    <Route path="rulesets" element={<div>rulesets-outlet</div>} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('ApiScoreLayout', () => {
    it('renders the page title, subtitle, and both tabs', () => {
        renderLayout();

        expect(screen.getByRole('heading', { name: 'API Score' })).not.toBeNull();
        expect(screen.getByText("Get personalized recommendations to enhance your API's quality.")).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Overview' })).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Rulesets & Functions' })).not.toBeNull();
        expect(screen.getByText('overview-outlet')).not.toBeNull();
    });

    it('activates the Rulesets & Functions tab on the rulesets route', () => {
        renderLayout('/api-score/rulesets');

        expect(screen.getByText('rulesets-outlet')).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Rulesets & Functions' }).getAttribute('aria-current')).toBe('page');
        expect(screen.getByRole('link', { name: 'Overview' }).getAttribute('aria-current')).toBeNull();
    });
});
