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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ApiScoreRulesetsPage } from './ApiScoreRulesetsPage';
import { useDeleteFunction, useImportFunction } from '../hooks/useFunctionMutations';
import { useDeleteRuleset, useImportRuleset, useUpdateRuleset } from '../hooks/useRulesetMutations';
import { useScoringFunctions } from '../hooks/useScoringFunctions';
import { useScoringRulesets } from '../hooks/useScoringRulesets';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(() => ({ id: 'DEFAULT' })),
    useHasPermission: jest.fn(() => true),
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('../hooks/useScoringRulesets');
jest.mock('../hooks/useScoringFunctions');
jest.mock('../hooks/useRulesetMutations');
jest.mock('../hooks/useFunctionMutations');

const mockUseHasPermission = useHasPermission as jest.Mock;
const mockUseScoringRulesets = useScoringRulesets as jest.Mock;
const mockUseScoringFunctions = useScoringFunctions as jest.Mock;

const RULESET = {
    id: 'ruleset-1',
    name: 'Proxy baseline',
    description: 'Quality checks',
    format: 'GRAVITEE_PROXY' as const,
    payload: 'rules: []',
};

const FUNCTION = { name: 'alwaysWarn.js', payload: 'export default () => {}' };

const noopMutation = { mutate: jest.fn(), isPending: false };

describe('ApiScoreRulesetsPage permissions', () => {
    beforeEach(() => {
        mockUseScoringRulesets.mockReturnValue({ data: { data: [RULESET] }, isLoading: false, isError: false });
        mockUseScoringFunctions.mockReturnValue({ data: { data: [FUNCTION] }, isLoading: false, isError: false });
        (useImportRuleset as jest.Mock).mockReturnValue(noopMutation);
        (useUpdateRuleset as jest.Mock).mockReturnValue(noopMutation);
        (useDeleteRuleset as jest.Mock).mockReturnValue(noopMutation);
        (useImportFunction as jest.Mock).mockReturnValue(noopMutation);
        (useDeleteFunction as jest.Mock).mockReturnValue(noopMutation);
    });

    afterEach(() => jest.clearAllMocks());

    it('hides mutation actions when the user lacks environment-integration write permissions', async () => {
        mockUseHasPermission.mockImplementation(
            ({ anyOf }: { anyOf: string[] }) => !anyOf.some(p => p.endsWith('-c') || p.endsWith('-u') || p.endsWith('-d')),
        );

        render(<ApiScoreRulesetsPage />);

        expect(screen.queryByRole('button', { name: /import/i })).toBeNull();

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /proxy baseline/i }));

        expect(screen.queryByRole('button', { name: /edit/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /delete/i })).toBeNull();
    });

    it('shows mutation actions when the user has environment-integration write permissions', async () => {
        mockUseHasPermission.mockReturnValue(true);

        render(<ApiScoreRulesetsPage />);

        expect(screen.getAllByRole('button', { name: /import/i })).toHaveLength(2);

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /proxy baseline/i }));

        expect(screen.getByRole('button', { name: /edit/i })).toBeInTheDocument();
        expect(screen.getAllByRole('button', { name: /delete/i }).length).toBeGreaterThan(0);
    });
});
