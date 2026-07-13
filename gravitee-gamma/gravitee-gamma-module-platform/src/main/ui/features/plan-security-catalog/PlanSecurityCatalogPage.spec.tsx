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
import { render, screen, within } from '@testing-library/react';

import { PLAN_SECURITY_CATALOG } from './planSecurityCatalog';
import { PlanSecurityCatalogPage } from './PlanSecurityCatalogPage';

describe('PlanSecurityCatalogPage', () => {
    beforeEach(() => {
        render(<PlanSecurityCatalogPage />);
    });

    it('renders the page title and description', () => {
        expect(screen.queryByText('Plan Security Type Catalog')).not.toBeNull();
        expect(screen.queryByText('Reference catalog of available plan security types supported by the platform.')).not.toBeNull();
    });

    it('renders the section heading', () => {
        expect(screen.queryByText('Security Types')).not.toBeNull();
    });

    it('renders column headers', () => {
        expect(screen.queryByText('Security Type ID')).not.toBeNull();
        expect(screen.queryByText('Name')).not.toBeNull();
        expect(screen.queryByText('Policy Name')).not.toBeNull();
    });

    it('renders each catalog row with matching id, name, and policy name', () => {
        const dataRows = screen.getAllByRole('row').slice(1);

        expect(dataRows.length).toBe(PLAN_SECURITY_CATALOG.length);

        PLAN_SECURITY_CATALOG.forEach((entry, index) => {
            const cells = within(dataRows[index]).getAllByRole('cell');
            expect(cells[0].textContent).toContain(entry.id);
            expect(cells[1].textContent).toContain(entry.name);
            expect(cells[2].textContent).toContain(entry.policyName);
        });
    });

    it('includes the required catalog types from the specification', () => {
        const ids = PLAN_SECURITY_CATALOG.map(entry => entry.id);
        expect(ids).toContain('OAUTH2');
        expect(ids).toContain('JWT');
        expect(ids).toContain('API_KEY');
        expect(ids).toContain('KEY_LESS');
        expect(ids.length).toBe(4);
    });

    it('does not render any edit controls', () => {
        expect(screen.queryByRole('button')).toBeNull();
        expect(screen.queryByRole('switch')).toBeNull();
        expect(screen.queryByText('Save changes')).toBeNull();
        expect(screen.queryByText('Discard')).toBeNull();
    });
});
