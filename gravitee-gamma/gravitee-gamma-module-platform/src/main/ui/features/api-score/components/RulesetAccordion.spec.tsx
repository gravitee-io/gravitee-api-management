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
import { MemoryRouter } from 'react-router-dom';

import { RulesetAccordion } from './RulesetAccordion';
import type { ScoringRuleset } from '../types/rulesets';

const RULESET: ScoringRuleset = {
    id: 'rs-1',
    name: 'Style',
    description: 'lint OpenAPI',
    format: 'GRAVITEE_PROXY',
    payload: 'rules: []',
    createdAt: '2026-01-01T00:00:00Z',
    referenceId: 'DEFAULT',
    referenceType: 'ENVIRONMENT',
};

describe('RulesetAccordion', () => {
    it('shows the name, format badge, and expanded details with Edit/Delete and payload', async () => {
        const user = userEvent.setup();
        const onDelete = jest.fn();
        render(
            <MemoryRouter>
                <RulesetAccordion rulesets={[RULESET]} onDelete={onDelete} />
            </MemoryRouter>,
        );

        expect(screen.getByText('Style')).not.toBeNull();
        expect(screen.getByText('Gravitee Proxy API')).not.toBeNull();

        await user.click(screen.getByRole('button', { name: /Style/ }));
        expect(screen.getByText('lint OpenAPI')).not.toBeNull();
        expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute('href', '/rs-1/edit');
        expect(screen.getByText('rules: []')).not.toBeNull();

        await user.click(screen.getByRole('button', { name: 'Delete' }));
        expect(onDelete).toHaveBeenCalledWith(RULESET);
    });
});
