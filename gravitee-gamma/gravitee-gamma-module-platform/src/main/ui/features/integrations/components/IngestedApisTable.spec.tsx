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

import { IngestedApisTable } from './IngestedApisTable';
import { SMALLEST_TABLE_PAGE_SIZE } from '../utils/paginationConstants';

jest.mock('@gravitee/gamma-modules-sdk/routing', () => jest.requireActual('../../users/testing/buildModuleNavPathForTests'));

function renderTable(totalCount: number) {
    return render(
        <MemoryRouter initialEntries={['/environments/env-1/platform/integrations/int-1']}>
            <IngestedApisTable
                apis={[{ id: 'api-1', name: 'Orders API', version: '1.0.0' }]}
                totalCount={totalCount}
                page={1}
                pageSize={SMALLEST_TABLE_PAGE_SIZE}
                loading={false}
                onPageChange={jest.fn()}
                onPageSizeChange={jest.fn()}
            />
        </MemoryRouter>,
    );
}

describe('IngestedApisTable', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    it.each([
        ['hides', 'fits within', SMALLEST_TABLE_PAGE_SIZE, false],
        ['shows', 'exceeds', SMALLEST_TABLE_PAGE_SIZE + 1, true],
    ])('%s pagination when the total count %s the smallest page size', (_shown, _relation, totalCount, paginated) => {
        renderTable(totalCount);

        expect(screen.queryByRole('button', { name: 'Next page' }) !== null).toBe(paginated);
        expect(screen.queryByRole('combobox', { name: 'Items per page' }) !== null).toBe(paginated);
    });
});
