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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { ImportApiScoreRulesetPage } from './ImportApiScoreRulesetPage';
import { notify } from '../../../shared/notify';
import { useCreateScoringRuleset } from '../hooks/useScoringRulesets';
import { GRAVITEE_API_DEFINITION } from '../types/rulesets';

jest.mock('../hooks/useScoringRulesets', () => ({
    useCreateScoringRuleset: jest.fn(),
}));

jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn(), warning: jest.fn(), info: jest.fn() },
}));

const mockUseCreateScoringRuleset = jest.mocked(useCreateScoringRuleset);
const mockNotify = jest.mocked(notify);

function mutationMock(mutateAsync = jest.fn().mockResolvedValue(undefined)) {
    return { mutateAsync, isPending: false };
}

function renderPage() {
    return render(
        <MemoryRouter initialEntries={['/api-score/rulesets/import']}>
            <Routes>
                <Route path="/api-score/rulesets/import" element={<ImportApiScoreRulesetPage />} />
                <Route path="/api-score/rulesets" element={<div data-testid="rulesets-list" />} />
            </Routes>
        </MemoryRouter>,
    );
}

async function uploadRulesetFile(user: ReturnType<typeof userEvent.setup>, content = 'rules: []') {
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    await user.upload(input, new File([content], 'style.yaml', { type: 'text/yaml' }));
}

describe('ImportApiScoreRulesetPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockUseCreateScoringRuleset.mockReturnValue(mutationMock() as ReturnType<typeof useCreateScoringRuleset>);
    });

    it('keeps Import disabled until format, name, and file are set', async () => {
        const user = userEvent.setup();
        renderPage();

        expect(screen.getByTestId('import-button')).toHaveProperty('disabled', true);
        expect(screen.getByRole('link', { name: 'Cancel' })).toHaveAttribute('href', '/api-score/rulesets');
        expect(screen.getByRole('link', { name: /Go back/ })).toHaveAttribute('href', '/api-score/rulesets');

        await user.click(screen.getByTestId('definition-format-OPENAPI'));
        await user.type(screen.getByTestId('name-input'), 'Style');
        expect(screen.getByTestId('import-button')).toHaveProperty('disabled', true);

        await uploadRulesetFile(user);
        await waitFor(() => expect(screen.getByTestId('import-button')).toHaveProperty('disabled', false));
    });

    it('POSTs OpenAPI format and navigates back on success', async () => {
        const user = userEvent.setup();
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseCreateScoringRuleset.mockReturnValue(mutationMock(mutateAsync) as ReturnType<typeof useCreateScoringRuleset>);
        renderPage();

        await user.click(screen.getByTestId('definition-format-OPENAPI'));
        await user.type(screen.getByTestId('name-input'), '  Style  ');
        await user.type(screen.getByTestId('description'), 'lint');
        await uploadRulesetFile(user);
        await user.click(screen.getByTestId('import-button'));

        await waitFor(() =>
            expect(mutateAsync).toHaveBeenCalledWith({
                format: 'OPENAPI',
                name: 'Style',
                description: 'lint',
                payload: 'rules: []',
            }),
        );
        expect(mockNotify.success).toHaveBeenCalledWith('Ruleset imported.');
        expect(screen.getByTestId('rulesets-list')).not.toBeNull();
    });

    it('POSTs the nested Gravitee Proxy format, not GraviteeAPI', async () => {
        const user = userEvent.setup();
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseCreateScoringRuleset.mockReturnValue(mutationMock(mutateAsync) as ReturnType<typeof useCreateScoringRuleset>);
        renderPage();

        await user.click(screen.getByTestId(`definition-format-${GRAVITEE_API_DEFINITION}`));
        await user.click(screen.getByTestId('gravitee-api-format-GRAVITEE_PROXY'));
        await user.type(screen.getByTestId('name-input'), 'Proxy rules');
        await uploadRulesetFile(user);
        await user.click(screen.getByTestId('import-button'));

        await waitFor(() =>
            expect(mutateAsync).toHaveBeenCalledWith({
                format: 'GRAVITEE_PROXY',
                name: 'Proxy rules',
                description: '',
                payload: 'rules: []',
            }),
        );
    });

    it('toasts when the dropped file is empty', async () => {
        const user = userEvent.setup();
        renderPage();

        const input = document.querySelector('input[type="file"]') as HTMLInputElement;
        await user.upload(input, new File([], 'empty.yaml', { type: 'text/yaml' }));

        await waitFor(() =>
            expect(mockNotify.error).toHaveBeenCalledWith(expect.objectContaining({ message: 'The file can not be empty' })),
        );
        expect(screen.getByTestId('import-button')).toHaveProperty('disabled', true);
    });
});
