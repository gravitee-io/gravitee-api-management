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

import { ScoringOverviewCards } from './ScoringOverviewCards';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

describe('ScoringOverviewCards', () => {
    it('renders overview cards with placeholders when nothing has been scored', () => {
        render(<ScoringOverviewCards summary={{ score: null, errors: 0, warnings: 0, hints: 0, infos: 0 }} isLoading={false} />);

        expect(screen.getByText('Overview')).toBeInTheDocument();
        expect(screen.getByText('Average score')).toBeInTheDocument();
        expect(screen.getByText('—')).toBeInTheDocument();
        expect(screen.getByText('Errors')).toBeInTheDocument();
    });

    it('renders the average score badge when data is available', () => {
        render(<ScoringOverviewCards summary={{ score: 0.79, errors: 8, warnings: 24, hints: 15, infos: 20 }} isLoading={false} />);

        expect(screen.getByText('79%')).toBeInTheDocument();
        expect(screen.getByText('8')).toBeInTheDocument();
        expect(screen.getByText('24')).toBeInTheDocument();
    });
});
