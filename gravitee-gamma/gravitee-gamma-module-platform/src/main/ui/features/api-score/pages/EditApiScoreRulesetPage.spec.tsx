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
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { EditApiScoreRulesetPage } from './EditApiScoreRulesetPage';
import { notify } from '../../../shared/notify';
import { useDeleteScoringRuleset, useScoringRuleset, useUpdateScoringRuleset } from '../hooks/useScoringRulesets';
import type { ScoringRuleset } from '../types/rulesets';

jest.mock('../hooks/useScoringRulesets', () => ({
    useScoringRuleset: jest.fn(),
    useUpdateScoringRuleset: jest.fn(),
    useDeleteScoringRuleset: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockUseScoringRuleset = jest.mocked(useScoringRuleset);
const mockUseUpdateScoringRuleset = jest.mocked(useUpdateScoringRuleset);
const mockUseDeleteScoringRuleset = jest.mocked(useDeleteScoringRuleset);
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

function mutationMock(mutateAsync = jest.fn().mockResolvedValue(undefined)) {
    return { mutateAsync, isPending: false };
}

function rulesetQuery(overrides: Partial<ReturnType<typeof useScoringRuleset>> = {}): ReturnType<typeof useScoringRuleset> {
    return {
        data: RULESET,
        isLoading: false,
        isError: false,
        error: null,
        refetch: jest.fn().mockResolvedValue({ data: RULESET }),
        ...overrides,
    } as ReturnType<typeof useScoringRuleset>;
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/api-score/rulesets/rs-1/edit']}>
            <Routes>
                <Route path="/api-score/rulesets/:rulesetId/edit" element={<EditApiScoreRulesetPage />} />
                <Route path="/api-score/rulesets" element={<div data-testid="rulesets-list" />} />
            </Routes>
        </MemoryRouter>,
    );
}

describe('EditApiScoreRulesetPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseScoringRuleset.mockReturnValue(rulesetQuery());
        mockUseUpdateScoringRuleset.mockReturnValue(mutationMock() as ReturnType<typeof useUpdateScoringRuleset>);
        mockUseDeleteScoringRuleset.mockReturnValue(mutationMock() as ReturnType<typeof useDeleteScoringRuleset>);
    });

    it('loads name, description, and payload and hides Save until dirty', () => {
        renderPage();

        expect(screen.getByTestId('name-input')).toHaveValue('Style');
        expect(screen.getByTestId('description')).toHaveValue('lint OpenAPI');
        expect(screen.getByText('rules: []')).not.toBeNull();
        expect(screen.getByText('To update your ruleset, delete the current one and upload the new version.')).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
        expect(screen.getByRole('link', { name: /Go back/ })).toHaveAttribute('href', '/api-score/rulesets');
    });

    it('PUTs a trimmed name and description when Save is clicked', async () => {
        const user = userEvent.setup();
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseUpdateScoringRuleset.mockReturnValue(mutationMock(mutateAsync) as ReturnType<typeof useUpdateScoringRuleset>);
        renderPage();

        await user.clear(screen.getByTestId('name-input'));
        await user.type(screen.getByTestId('name-input'), '  Renamed  ');
        await user.click(screen.getByRole('button', { name: 'Save' }));

        await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith({ name: 'Renamed', description: 'lint OpenAPI' }));
        expect(mockNotify.success).toHaveBeenCalledWith('Ruleset updated.');
    });

    it('keeps Discard when the name is cleared, and does not overwrite later query data', async () => {
        const user = userEvent.setup();
        const { rerender } = renderPage();

        await user.clear(screen.getByTestId('name-input'));
        expect(screen.getByRole('button', { name: 'Discard' })).not.toBeNull();
        expect(screen.getByRole('button', { name: 'Save' })).toHaveProperty('disabled', true);

        mockUseScoringRuleset.mockReturnValue(rulesetQuery({ data: { ...RULESET } }));
        rerender(
            <MemoryRouter initialEntries={['/api-score/rulesets/rs-1/edit']}>
                <Routes>
                    <Route path="/api-score/rulesets/:rulesetId/edit" element={<EditApiScoreRulesetPage />} />
                    <Route path="/api-score/rulesets" element={<div data-testid="rulesets-list" />} />
                </Routes>
            </MemoryRouter>,
        );

        expect(screen.getByTestId('name-input')).toHaveValue('');
    });

    it('re-fetches on Discard and applies the server values', async () => {
        const user = userEvent.setup();
        const refetch = jest.fn().mockResolvedValue({ data: { ...RULESET, name: 'FromServer', description: 'fresh' } });
        mockUseScoringRuleset.mockReturnValue(rulesetQuery({ refetch }));
        renderPage();

        await user.clear(screen.getByTestId('name-input'));
        await user.type(screen.getByTestId('name-input'), 'Local');
        await user.click(screen.getByRole('button', { name: 'Discard' }));

        await waitFor(() => expect(refetch).toHaveBeenCalled());
        expect(screen.getByTestId('name-input')).toHaveValue('FromServer');
        expect(screen.getByTestId('description')).toHaveValue('fresh');
        expect(screen.queryByRole('button', { name: 'Discard' })).toBeNull();
    });

    it('toasts Console copy when the ruleset fails to load', () => {
        const error = new Error('boom');
        mockUseScoringRuleset.mockReturnValue(rulesetQuery({ data: undefined, isError: true, error }));

        renderPage();

        expect(mockNotify.error).toHaveBeenCalledWith(error, 'Ruleset error');
    });

    it('deletes from the danger zone and navigates to the list', async () => {
        const user = userEvent.setup();
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseDeleteScoringRuleset.mockReturnValue(mutationMock(mutateAsync) as ReturnType<typeof useDeleteScoringRuleset>);
        renderPage();

        await user.click(screen.getByTestId('delete-ruleset-button'));
        await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete ruleset' }));

        await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith('rs-1'));
        expect(mockNotify.success).toHaveBeenCalledWith('Ruleset successfully deleted!');
        expect(screen.getByTestId('rulesets-list')).not.toBeNull();
    });
});
