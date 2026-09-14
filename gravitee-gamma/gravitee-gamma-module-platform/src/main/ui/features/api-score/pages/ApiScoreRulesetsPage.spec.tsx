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
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ApiScoreRulesetsPage } from './ApiScoreRulesetsPage';
import { notify } from '../../../shared/notify';
import { useDeleteScoringFunction, useScoringFunctions } from '../hooks/useScoringFunctions';
import { useDeleteScoringRuleset, useScoringRulesets } from '../hooks/useScoringRulesets';
import type { ScoringFunction, ScoringRuleset } from '../types/rulesets';

jest.mock('../hooks/useScoringRulesets', () => ({
    useScoringRulesets: jest.fn(),
    useDeleteScoringRuleset: jest.fn(),
}));

jest.mock('../hooks/useScoringFunctions', () => ({
    useScoringFunctions: jest.fn(),
    useDeleteScoringFunction: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockUseScoringRulesets = jest.mocked(useScoringRulesets);
const mockUseScoringFunctions = jest.mocked(useScoringFunctions);
const mockUseDeleteScoringRuleset = jest.mocked(useDeleteScoringRuleset);
const mockUseDeleteScoringFunction = jest.mocked(useDeleteScoringFunction);
const mockNotify = jest.mocked(notify);

const RULESET: ScoringRuleset = {
    id: 'rs-1',
    name: 'Style',
    description: 'lint OpenAPI',
    format: 'OPENAPI',
    payload: 'rules: []',
    createdAt: '2026-01-01T00:00:00Z',
    referenceId: 'DEFAULT',
    referenceType: 'ENVIRONMENT',
};

const FN: ScoringFunction = {
    name: 'checkTag.js',
    payload: 'module.exports = {}',
    createdAt: '2026-01-01T00:00:00Z',
    referenceId: 'DEFAULT',
    referenceType: 'ENVIRONMENT',
};

function mutationMock(mutateAsync = jest.fn().mockResolvedValue(undefined)) {
    return { mutateAsync, isPending: false };
}

function rulesetsResult(overrides: Partial<ReturnType<typeof useScoringRulesets>> = {}): ReturnType<typeof useScoringRulesets> {
    return {
        rulesets: [RULESET],
        isLoading: false,
        isError: false,
        error: null,
        ...overrides,
    };
}

function functionsResult(overrides: Partial<ReturnType<typeof useScoringFunctions>> = {}): ReturnType<typeof useScoringFunctions> {
    return {
        functions: [FN],
        isLoading: false,
        isError: false,
        error: null,
        ...overrides,
    };
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/api-score/rulesets']}>
            <Routes>
                <Route path="api-score">
                    <Route path="rulesets" element={<ApiScoreRulesetsPage />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

describe('ApiScoreRulesetsPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseScoringRulesets.mockReturnValue(rulesetsResult());
        mockUseScoringFunctions.mockReturnValue(functionsResult());
        mockUseDeleteScoringRuleset.mockReturnValue(mutationMock() as ReturnType<typeof useDeleteScoringRuleset>);
        mockUseDeleteScoringFunction.mockReturnValue(mutationMock() as ReturnType<typeof useDeleteScoringFunction>);
    });

    it('renders ruleset and function cards with Import links and accordion rows', () => {
        renderPage();

        expect(screen.getByTestId('api-score-rulesets-page')).not.toBeNull();
        expect(screen.getByText('Rulesets')).not.toBeNull();
        expect(
            screen.getByText("Custom rulesets allow you to enforce your organization's API design, quality and security standards."),
        ).not.toBeNull();
        expect(screen.getByTestId('import-ruleset-link')).toHaveAttribute('href', '/api-score/rulesets/import');
        expect(screen.getByTestId('import-function-link')).toHaveAttribute('href', '/api-score/rulesets/import-function');
        expect(screen.getByText('Style')).not.toBeNull();
        expect(screen.getByText('checkTag.js')).not.toBeNull();
    });

    it('shows Console empty copy for both cards', () => {
        mockUseScoringRulesets.mockReturnValue(rulesetsResult({ rulesets: [] }));
        mockUseScoringFunctions.mockReturnValue(functionsResult({ functions: [] }));

        renderPage();

        expect(screen.getByText('No ruleset, yet')).not.toBeNull();
        expect(screen.getByText('No ruleset, yet').closest('.rounded-lg.border')).not.toBeNull();
        expect(screen.getByText('No custom function, yet')).not.toBeNull();
    });

    it('toasts list-load errors with Console copy', () => {
        const error = new Error('boom');
        mockUseScoringRulesets.mockReturnValue(rulesetsResult({ isError: true, error, rulesets: [] }));
        mockUseScoringFunctions.mockReturnValue(functionsResult({ isError: true, error, functions: [] }));

        renderPage();

        expect(mockNotify.error).toHaveBeenCalledWith(error, 'Rulesets error!');
        expect(mockNotify.error).toHaveBeenCalledWith(error, 'Functions error!');
    });

    it('confirms ruleset delete and stays on the list', async () => {
        const user = userEvent.setup();
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseDeleteScoringRuleset.mockReturnValue(mutationMock(mutateAsync) as ReturnType<typeof useDeleteScoringRuleset>);

        renderPage();

        await user.click(screen.getByRole('button', { name: /Style/ }));
        await user.click(within(screen.getByTestId('ruleset-accordion')).getByRole('button', { name: 'Delete' }));
        expect(screen.getByText('Please note that once your ruleset is deleted, it cannot be restored.')).not.toBeNull();
        await user.click(screen.getByRole('button', { name: 'Delete ruleset' }));

        expect(mutateAsync).toHaveBeenCalledWith('rs-1');
        expect(mockNotify.success).toHaveBeenCalledWith('Ruleset successfully deleted!');
        expect(screen.getByTestId('api-score-rulesets-page')).not.toBeNull();
    });

    it('confirms function delete', async () => {
        const user = userEvent.setup();
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseDeleteScoringFunction.mockReturnValue(mutationMock(mutateAsync) as ReturnType<typeof useDeleteScoringFunction>);

        renderPage();

        await user.click(screen.getByRole('button', { name: /checkTag\.js/ }));
        await user.click(within(screen.getByTestId('function-accordion')).getByRole('button', { name: 'Delete' }));
        await user.click(screen.getByRole('button', { name: 'Delete function' }));

        expect(mutateAsync).toHaveBeenCalledWith('checkTag.js');
        expect(mockNotify.success).toHaveBeenCalledWith('Function successfully deleted!');
    });
});
