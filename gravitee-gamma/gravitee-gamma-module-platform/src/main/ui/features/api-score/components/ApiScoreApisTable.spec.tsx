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
import userEvent from '@testing-library/user-event';
import type { ComponentProps } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { ApiScoreApisTable } from './ApiScoreApisTable';
import type { EnvironmentApiScore } from '../types/scoring';

const SCORED: EnvironmentApiScore = {
    id: 'api-petstore',
    name: 'Petstore',
    score: 0.84,
    errors: 2,
    warnings: 3,
    infos: 1,
    hints: 0,
};

const UNSCORED: EnvironmentApiScore = {
    id: 'api-unscored',
    name: 'Unscored API',
    score: null,
    errors: null,
    warnings: null,
    infos: null,
    hints: null,
};

const AMBER: EnvironmentApiScore = {
    id: 'api-amber',
    name: 'Amber API',
    score: 0.66,
    errors: 0,
    warnings: 4,
    infos: 0,
    hints: 1,
};

function renderTable(apis: EnvironmentApiScore[], overrides: Partial<ComponentProps<typeof ApiScoreApisTable>> = {}) {
    return render(
        <MemoryRouter>
            <ApiScoreApisTable
                apis={apis}
                totalCount={apis.length}
                loading={false}
                page={1}
                pageSize={10}
                detailHref={apiId => `/environments/default/apim/apis/${apiId}/api-score`}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
                {...overrides}
            />
        </MemoryRouter>,
    );
}

describe('ApiScoreApisTable', () => {
    it('renders columns in API Name, Score, Errors, Warnings, Infos, Hints order', () => {
        renderTable([SCORED]);
        const headers = screen.getAllByRole('columnheader').map(header => header.textContent);
        expect(headers.slice(0, 6)).toEqual(['API Name', 'Score', 'Errors', 'Warnings', 'Infos', 'Hints']);
    });

    it('shows a score pill, zero as a plain number, and non-zero as a badge', () => {
        renderTable([SCORED]);
        expect(screen.getByText('84%')).not.toBeNull();
        expect(screen.getByText('Petstore')).not.toBeNull();
        expect(screen.getByText('0')).not.toBeNull();
        expect(screen.getByText('2')).not.toBeNull();
    });

    it('shows Not available and em dashes for an unscored API', () => {
        renderTable([UNSCORED]);
        expect(screen.getByText('Not available')).not.toBeNull();
        expect(screen.getAllByText('—')).toHaveLength(4);
    });

    it('shows an amber score pill at 66%', () => {
        renderTable([AMBER]);
        expect(screen.getByText('66%')).not.toBeNull();
    });

    it('links the kebab to API-level Score Details', async () => {
        const user = userEvent.setup();
        renderTable([SCORED]);

        await user.click(screen.getByRole('button', { name: /Actions for Petstore/i }));
        const item = await screen.findByRole('menuitem', { name: 'View API-level Score Details' });
        expect(item).toHaveAttribute('href', '/environments/default/apim/apis/api-petstore/api-score');
    });

    it('shows Console empty copy when there are no APIs', () => {
        renderTable([], { totalCount: 0 });
        expect(screen.getByText('No items to display')).not.toBeNull();
    });
});
