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

import { ApiScoreSummaryRow } from './ApiScoreSummaryRow';
import type { EnvironmentScoringOverview } from '../types/scoring';

const OVERVIEW: EnvironmentScoringOverview = {
    id: 'env-1',
    score: 0.83,
    errors: 3,
    warnings: 5,
    hints: 1,
    infos: 2,
};

describe('ApiScoreSummaryRow', () => {
    it('renders Average score then Errors, Warnings, Hints, Infos', () => {
        render(<ApiScoreSummaryRow overview={OVERVIEW} />);

        expect(screen.getAllByText(/Average score|Errors|Warnings|Hints|Infos/).map(node => node.textContent)).toEqual([
            'Average score',
            'Errors',
            'Warnings',
            'Hints',
            'Infos',
        ]);
        expect(screen.getByText('83%')).not.toBeNull();
        expect(screen.getByText('3')).not.toBeNull();
        expect(screen.getByText('5')).not.toBeNull();
        expect(screen.getByText('1')).not.toBeNull();
        expect(screen.getByText('2')).not.toBeNull();
    });
});
