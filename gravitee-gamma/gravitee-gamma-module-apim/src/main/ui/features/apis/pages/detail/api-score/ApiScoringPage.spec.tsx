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
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';

import { ApiScoringPage } from './ApiScoringPage';
import { notify } from '../../../../../shared/notify';
import { useApiScoreEnabled } from '../../../hooks/useApiScoreEnabled';
import { useApiScoring } from '../../../hooks/useApiScoring';
import type { ApiScoring } from '../../../types/scoring';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
}));

jest.mock('../../../hooks/useApiScoreEnabled', () => ({
    useApiScoreEnabled: jest.fn(() => ({ enabled: true, isFetched: true })),
}));

jest.mock('../../../hooks/useApiScoring', () => ({
    useApiScoring: jest.fn(),
}));

jest.mock('../../../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

const mockUseApiScoring = useApiScoring as jest.Mock;
const mockUseApiScoreEnabled = useApiScoreEnabled as jest.Mock;
const mockNotify = jest.mocked(notify);

const SCORED: ApiScoring = {
    createdAt: new Date(Date.now() - 10 * 86_400_000).toISOString(),
    summary: { all: 3, errors: 2, warnings: 1, infos: 0, hints: 0, score: 0.67 },
    assets: [
        {
            name: 'petstore.yaml',
            type: 'SWAGGER',
            diagnostics: [
                {
                    severity: 'ERROR',
                    message: 'Operation is missing a security requirement.',
                    range: { start: { line: 84, character: 4 }, end: { line: 84, character: 10 } },
                    path: '$.paths./pet.findByStatus.get',
                },
                {
                    severity: 'ERROR',
                    message: 'Response 200 is missing a schema.',
                    range: { start: { line: 142, character: 12 }, end: { line: 142, character: 20 } },
                    path: '$.paths./pet/{petId}.get.responses.200',
                },
                {
                    severity: 'WARN',
                    message: 'API description is shorter than 50 characters.',
                    range: { start: { line: 8, character: 2 }, end: { line: 8, character: 20 } },
                    path: '$.info.description',
                },
            ],
        },
    ],
};

function idleScoring(overrides: Partial<ReturnType<typeof useApiScoring>> = {}) {
    mockUseApiScoring.mockReturnValue({
        scoring: undefined,
        jobs: [],
        isLoading: false,
        isError: false,
        error: null,
        pending: false,
        evaluate: jest.fn(),
        isEvaluating: false,
        ...overrides,
    });
}

function renderPage() {
    render(
        <MemoryRouter initialEntries={['/apis/api-1/api-score']}>
            <Routes>
                <Route path="apis/:apiId/api-score" element={<ApiScoringPage />} />
            </Routes>
        </MemoryRouter>,
    );
}

beforeEach(() => {
    jest.clearAllMocks();
    mockUseApiScoreEnabled.mockReturnValue({ enabled: true, isFetched: true });
    idleScoring();
});

describe('ApiScoringPage', () => {
    it('shows the never-evaluated empty state', () => {
        renderPage();
        expect(screen.getByText('This API has never been scored before')).toBeInTheDocument();
        expect(screen.getByText('Click on the Evaluate button to get the first score.')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /^evaluate$/i })).toBeEnabled();
    });

    it('calls evaluate when the Evaluate button is clicked', () => {
        const evaluate = jest.fn();
        idleScoring({ evaluate });
        renderPage();
        fireEvent.click(screen.getByRole('button', { name: /^evaluate$/i }));
        expect(evaluate).toHaveBeenCalled();
    });

    it('disables Evaluate while a SCORING_REQUEST job is pending', () => {
        idleScoring({ pending: true, scoring: SCORED });
        renderPage();
        expect(screen.getByRole('button', { name: /^evaluate$/i })).toBeDisabled();
        expect(screen.getByText(/a request is currently processing/i)).toBeInTheDocument();
    });

    it('renders the score percent, last evaluated time, and severity pills', () => {
        idleScoring({ scoring: SCORED });
        renderPage();
        expect(screen.getByText('67%')).toBeInTheDocument();
        expect(screen.getByText(/last evaluated 10 days ago/i)).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: 'All (3)' })).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: 'Errors (2)' })).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: 'Warnings (1)' })).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: 'Infos (0)' })).toBeDisabled();
        expect(screen.getByRole('radio', { name: 'Hints (0)' })).toBeDisabled();
    });

    it('filters diagnostic rows when a severity pill is selected', () => {
        idleScoring({ scoring: SCORED });
        renderPage();
        expect(screen.getByText('Operation is missing a security requirement.')).toBeInTheDocument();
        expect(screen.getByText('API description is shorter than 50 characters.')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('radio', { name: 'Errors (2)' }));
        expect(screen.getByText('Operation is missing a security requirement.')).toBeInTheDocument();
        expect(screen.queryByText('API description is shorter than 50 characters.')).not.toBeInTheDocument();
    });

    it('shows the all-clear empty state when the score is 100% with no findings', () => {
        idleScoring({
            scoring: {
                createdAt: new Date().toISOString(),
                summary: { all: 0, errors: 0, warnings: 0, infos: 0, hints: 0, score: 1 },
                assets: [],
            },
        });
        renderPage();
        expect(screen.getByText('All clear')).toBeInTheDocument();
        expect(screen.getByText(/everything looks great/i)).toBeInTheDocument();
    });

    it('shows the no-scorable-assets empty state when the report has no summary', () => {
        idleScoring({ scoring: { createdAt: new Date().toISOString(), assets: [] } });
        renderPage();
        expect(screen.getByText('No scorable assets')).toBeInTheDocument();
        expect(screen.getByText("This API's assets did not match any rulesets.")).toBeInTheDocument();
    });

    it('toasts Console ERROR copy for a recent failed job', () => {
        idleScoring({
            scoring: SCORED,
            jobs: [
                {
                    id: 'job-1',
                    sourceId: 'api-1',
                    type: 'SCORING_REQUEST',
                    status: 'ERROR',
                    errorMessage: 'boom',
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString(),
                },
            ],
        });
        renderPage();
        expect(mockNotify.error).toHaveBeenCalledWith(expect.stringContaining('The last evaluation was failed at'));
    });

    it('toasts Console TIMEOUT copy for a recent timed-out job', () => {
        idleScoring({
            scoring: SCORED,
            jobs: [
                {
                    id: 'job-2',
                    sourceId: 'api-1',
                    type: 'SCORING_REQUEST',
                    status: 'TIMEOUT',
                    createdAt: new Date().toISOString(),
                    updatedAt: new Date().toISOString(),
                },
            ],
        });
        renderPage();
        expect(mockNotify.error).toHaveBeenCalledWith(expect.stringContaining('Evaluation timed out at'));
    });

    it('toasts asset evaluation errors with Console copy', () => {
        idleScoring({
            scoring: {
                createdAt: new Date().toISOString(),
                assets: [
                    {
                        name: 'Asset name',
                        type: 'GRAVITEE_DEFINITION',
                        diagnostics: [],
                        errors: [{ code: 'undefined-function', path: ['rules', 'function'] }],
                    },
                ],
            },
        });
        renderPage();
        expect(mockNotify.error).toHaveBeenCalledWith(expect.stringContaining('Errors occurred while scoring this API:'));
        expect(mockNotify.error).toHaveBeenCalledWith(expect.stringContaining('Asset: Asset name'));
    });

    it('toasts load errors without an inline alert', () => {
        const error = new Error('boom');
        idleScoring({ isError: true, error });
        renderPage();
        expect(mockNotify.error).toHaveBeenCalledWith(error, 'An error occurred while getting your API Scoring.');
        expect(screen.queryByText('An error occurred while getting your API Scoring.')).not.toBeInTheDocument();
        expect(screen.queryByText('This API has never been scored before')).not.toBeInTheDocument();
    });

    it('does not show no-scorable-assets when the report has asset errors and no summary', () => {
        idleScoring({
            scoring: {
                createdAt: new Date().toISOString(),
                assets: [
                    {
                        name: 'Asset name',
                        type: 'GRAVITEE_DEFINITION',
                        diagnostics: [],
                        errors: [{ code: 'undefined-function', path: ['rules', 'function'] }],
                    },
                ],
            },
        });
        renderPage();
        expect(screen.queryByText('No scorable assets')).not.toBeInTheDocument();
    });

    it('redirects to the parent API route when apiScore.enabled is false', () => {
        mockUseApiScoreEnabled.mockReturnValue({ enabled: false, isFetched: true });
        render(
            <MemoryRouter initialEntries={['/apis/api-1/api-score']}>
                <Routes>
                    <Route
                        path="apis/:apiId"
                        element={
                            <>
                                <div>parent-overview</div>
                                <Outlet />
                            </>
                        }
                    >
                        <Route path="api-score" element={<ApiScoringPage />} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
        expect(screen.getByText('parent-overview')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /^evaluate$/i })).not.toBeInTheDocument();
    });
});
