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

const EVALUATED_AT = new Date('2026-01-01T00:00:00Z');

describe('ApiScoreSummaryRow', () => {
    it('renders Average score then Errors, Warnings, Hints, Infos', () => {
        render(<ApiScoreSummaryRow overview={OVERVIEW} evaluatedAt={EVALUATED_AT} />);

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

    it('shows when the environment score was last evaluated', () => {
        jest.useFakeTimers();
        jest.setSystemTime(new Date('2026-01-11T00:00:00Z'));
        render(<ApiScoreSummaryRow overview={OVERVIEW} evaluatedAt={new Date('2026-01-01T00:00:00Z')} />);

        expect(screen.getByText(/last evaluated 1 week ago/i)).not.toBeNull();
        jest.useRealTimers();
    });
});
