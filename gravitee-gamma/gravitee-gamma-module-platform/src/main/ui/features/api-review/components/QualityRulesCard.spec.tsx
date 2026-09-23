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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { useHasPermission } from '@gravitee/gamma-modules-sdk';

import { QualityRulesCard } from './QualityRulesCard';
import { ApimApiError } from '../../../shared/api/apimClient';
import { notify } from '../../../shared/notify';
import { useCreateQualityRule, useDeleteQualityRule, useUpdateQualityRule } from '../hooks/useQualityRuleMutations';
import { useQualityRules } from '../hooks/useQualityRules';
import type { QualityRule } from '../types/qualityRule';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
    useEnvironment: () => ({ id: 'env-1' }),
}));
jest.mock('../hooks/useQualityRules');
jest.mock('../hooks/useQualityRuleMutations');
jest.mock('../../../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

// The table has its own suite; stub it so Radix menus stay out of this one.
jest.mock('./QualityRulesTable', () => ({
    QualityRulesTable: ({
        rules,
        onEdit,
        onDelete,
    }: {
        rules: QualityRule[];
        onEdit: (rule: QualityRule) => void;
        onDelete: (rule: QualityRule) => void;
    }) => (
        <ul>
            {rules.map(rule => (
                <li key={rule.id}>
                    {rule.name}
                    <button type="button" onClick={() => onEdit(rule)}>
                        Edit {rule.name}
                    </button>
                    <button type="button" onClick={() => onDelete(rule)}>
                        Delete {rule.name}
                    </button>
                </li>
            ))}
        </ul>
    ),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseQualityRules = jest.mocked(useQualityRules);
const mockUseCreate = jest.mocked(useCreateQualityRule);
const mockUseUpdate = jest.mocked(useUpdateQualityRule);
const mockUseDelete = jest.mocked(useDeleteQualityRule);

const RULES: QualityRule[] = [
    { id: 'rule-openapi', name: 'OpenAPI specification is complete', description: 'Every operation is documented.', weight: 0 },
    { id: 'rule-owner', name: 'Primary owner is a group', description: 'A team owns the API.', weight: 3 },
];

function mutation(mutateAsync = jest.fn().mockResolvedValue(undefined)) {
    return { mutateAsync, isPending: false } as unknown as ReturnType<typeof useCreateQualityRule>;
}

describe('QualityRulesCard', () => {
    beforeAll(() => {
        global.ResizeObserver = class ResizeObserver {
            observe() {}
            unobserve() {}
            disconnect() {}
        } as typeof ResizeObserver;
    });

    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseQualityRules.mockReturnValue({ data: RULES, isLoading: false, isError: false } as ReturnType<typeof useQualityRules>);
        mockUseCreate.mockReturnValue(mutation());
        mockUseUpdate.mockReturnValue(mutation() as unknown as ReturnType<typeof useUpdateQualityRule>);
        mockUseDelete.mockReturnValue(mutation() as unknown as ReturnType<typeof useDeleteQualityRule>);
    });

    afterEach(() => jest.clearAllMocks());

    it('lists the rules with the add button', () => {
        render(<QualityRulesCard />);
        expect(screen.getByText('API Review Rules')).toBeInTheDocument();
        expect(screen.getByText('OpenAPI specification is complete')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /add manual rule/i })).toBeInTheDocument();
    });

    it('hides the add button without environment-quality_rule-c', () => {
        mockUseHasPermission.mockImplementation(({ anyOf }: { anyOf?: string[] }) => !anyOf?.includes('environment-quality_rule-c'));
        render(<QualityRulesCard />);
        expect(screen.queryByRole('button', { name: /add manual rule/i })).not.toBeInTheDocument();
    });

    it('creates a rule from the sheet and reports success', async () => {
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseCreate.mockReturnValue(mutation(mutateAsync));
        render(<QualityRulesCard />);

        fireEvent.click(screen.getByRole('button', { name: /add manual rule/i }));
        fireEvent.change(screen.getByLabelText(/rule name/i), { target: { value: 'Breaking changes are documented' } });
        fireEvent.change(screen.getByLabelText(/description/i), { target: { value: 'Called out before accepting.' } });
        fireEvent.click(screen.getByRole('button', { name: 'Create rule' }));

        await waitFor(() =>
            expect(mutateAsync).toHaveBeenCalledWith({
                name: 'Breaking changes are documented',
                description: 'Called out before accepting.',
            }),
        );
        expect(notify.success).toHaveBeenCalledWith('Manual rule created successfully');
        await waitFor(() => expect(screen.queryByRole('heading', { name: 'New manual rule' })).not.toBeInTheDocument());
    });

    it('edits a rule keeping the rule identity for the update', async () => {
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseUpdate.mockReturnValue(mutation(mutateAsync) as unknown as ReturnType<typeof useUpdateQualityRule>);
        render(<QualityRulesCard />);

        fireEvent.click(screen.getByRole('button', { name: 'Edit Primary owner is a group' }));
        expect(screen.getByRole('heading', { name: 'Edit manual rule' })).toBeInTheDocument();
        fireEvent.change(screen.getByLabelText(/description/i), { target: { value: 'A team owns the API and its on-call.' } });
        fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

        await waitFor(() =>
            expect(mutateAsync).toHaveBeenCalledWith({
                rule: RULES[1],
                write: { name: 'Primary owner is a group', description: 'A team owns the API and its on-call.' },
            }),
        );
        expect(notify.success).toHaveBeenCalledWith('Manual rule updated successfully');
    });

    it('deletes a rule after confirmation', async () => {
        const mutateAsync = jest.fn().mockResolvedValue(undefined);
        mockUseDelete.mockReturnValue(mutation(mutateAsync) as unknown as ReturnType<typeof useDeleteQualityRule>);
        render(<QualityRulesCard />);

        fireEvent.click(screen.getByRole('button', { name: 'Delete Primary owner is a group' }));
        expect(screen.getByRole('heading', { name: 'Delete manual rule' })).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

        await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith('rule-owner'));
        expect(notify.success).toHaveBeenCalledWith('“Primary owner is a group” has been deleted');
    });

    it('surfaces a failed create and keeps the sheet open', async () => {
        const error = new ApimApiError(400, 'Rule name is required.');
        mockUseCreate.mockReturnValue(mutation(jest.fn().mockRejectedValue(error)));
        render(<QualityRulesCard />);

        fireEvent.click(screen.getByRole('button', { name: /add manual rule/i }));
        fireEvent.change(screen.getByLabelText(/rule name/i), { target: { value: 'x' } });
        fireEvent.change(screen.getByLabelText(/description/i), { target: { value: 'y' } });
        fireEvent.click(screen.getByRole('button', { name: 'Create rule' }));

        await waitFor(() => expect(notify.error).toHaveBeenCalledWith(error, 'Failed to create manual rule'));
        expect(screen.getByRole('heading', { name: 'New manual rule' })).toBeInTheDocument();
    });

    it('shows the load error inside the card', () => {
        mockUseQualityRules.mockReturnValue({ data: undefined, isLoading: false, isError: true } as ReturnType<typeof useQualityRules>);
        render(<QualityRulesCard />);
        expect(screen.getByText(/failed to load manual rules/i)).toBeInTheDocument();
    });
});
