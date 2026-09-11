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

import { ApiScoreDashboardPage } from './ApiScoreDashboardPage';
import { notify } from '../../../shared/notify';
import { useApisScoring } from '../hooks/useApisScoring';
import { useScoringOverview } from '../hooks/useScoringOverview';
import type { EnvironmentApiScore, EnvironmentScoringOverview } from '../types/scoring';

jest.mock('../hooks/useScoringOverview', () => ({
    useScoringOverview: jest.fn(),
}));

jest.mock('../hooks/useApisScoring', () => ({
    useApisScoring: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

jest.mock('../utils/apiScoreDetailPath', () => ({
    apiScoreDetailPath: (_pathname: string, apiId: string) => `/environments/default/apim/apis/${apiId}/api-score`,
}));

const mockUseScoringOverview = jest.mocked(useScoringOverview);
const mockUseApisScoring = jest.mocked(useApisScoring);
const mockNotify = jest.mocked(notify);

const OVERVIEW: EnvironmentScoringOverview = {
    id: 'env-1',
    score: 0.83,
    errors: 3,
    warnings: 5,
    hints: 1,
    infos: 2,
};

const API: EnvironmentApiScore = {
    id: 'api-petstore',
    name: 'Petstore',
    score: 0.84,
    errors: 1,
    warnings: 0,
    infos: 2,
    hints: 1,
};

function overviewResult(overrides: Partial<ReturnType<typeof useScoringOverview>> = {}): ReturnType<typeof useScoringOverview> {
    return {
        data: OVERVIEW,
        isLoading: false,
        isError: false,
        error: null,
        ...overrides,
    } as ReturnType<typeof useScoringOverview>;
}

function apisResult(overrides: Partial<ReturnType<typeof useApisScoring>> = {}): ReturnType<typeof useApisScoring> {
    return {
        apis: [API],
        totalCount: 1,
        isLoading: false,
        isError: false,
        error: null,
        ...overrides,
    };
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/api-score']}>
            <ApiScoreDashboardPage />
        </MemoryRouter>,
    );
}

describe('ApiScoreDashboardPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseScoringOverview.mockReturnValue(overviewResult());
        mockUseApisScoring.mockReturnValue(apisResult());
    });

    it('renders the Overview stats and APIs table without an Evaluate button', () => {
        renderPage();

        expect(screen.getByText('Overview')).not.toBeNull();
        expect(screen.getByText('83%')).not.toBeNull();
        expect(screen.getByText('Average score')).not.toBeNull();
        expect(screen.getByText('Petstore')).not.toBeNull();
        expect(screen.queryByRole('button', { name: /evaluate/i })).toBeNull();
    });

    it('shows Console empty copy when the environment has no score yet', () => {
        mockUseScoringOverview.mockReturnValue(overviewResult({ data: { ...OVERVIEW, score: null } }));
        mockUseApisScoring.mockReturnValue(apisResult({ apis: [], totalCount: 0 }));

        renderPage();

        expect(screen.getByText('No score results yet')).not.toBeNull();
        expect(
            screen.getByText(
                'There are no results to display. Please open an API and run the API score evaluation to start scoring your APIs.',
            ),
        ).not.toBeNull();
        expect(screen.queryByText('Overview')).toBeNull();
        expect(screen.getByText('No items to display')).not.toBeNull();
    });

    it('toasts Classic list-load copy when overview or APIs fail', () => {
        const error = new Error('boom');
        mockUseScoringOverview.mockReturnValue(overviewResult({ isError: true, error, data: undefined }));

        renderPage();

        expect(mockNotify.error).toHaveBeenCalledWith(error, 'An error occurred while loading list.');
    });
});
