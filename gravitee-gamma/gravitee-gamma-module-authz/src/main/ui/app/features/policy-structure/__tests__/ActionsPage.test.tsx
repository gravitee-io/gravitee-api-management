import { screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders as render } from '../../../../../../test/ui/render-with-providers';
import { ActionsPage } from '../ActionsPage';

const getSchemaSpy = vi.fn();

vi.mock('../../../../lib/api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    MAX_PER_PAGE: 100,
    authzApiService: {
        getSchema: (env: string) => getSchemaSpy(env),
    },
}));

beforeEach(() => {
    getSchemaSpy.mockReset();
});

describe('ActionsPage', () => {
    it('renders the empty state when the schema has no actions', async () => {
        getSchemaSpy.mockResolvedValue({
            environmentId: 'DEFAULT',
            schemaText: 'entity User {};\nentity Resource {};\n',
            updatedAt: '2026-04-27T10:00:00Z',
        });
        render(<ActionsPage />);
        await waitFor(() => expect(screen.getByText(/No actions defined\./i)).toBeInTheDocument());
        expect(screen.queryByTestId('action-row')).not.toBeInTheDocument();
    });

    it('lists actions parsed from the live schema', async () => {
        getSchemaSpy.mockResolvedValue({
            environmentId: 'DEFAULT',
            schemaText:
                'entity User {};\n' +
                'entity Resource {};\n' +
                'action "read" appliesTo { principal: [User], resource: [Resource] };\n' +
                'action "write" appliesTo { principal: [User], resource: [Resource] };\n',
            updatedAt: '2026-04-27T10:00:00Z',
        });
        render(<ActionsPage />);
        await waitFor(() => expect(screen.getAllByTestId('action-row')).toHaveLength(2));
        expect(screen.getByText('read')).toBeInTheDocument();
        expect(screen.getByText('write')).toBeInTheDocument();
        expect(screen.getByText(/2 action\(s\)/i)).toBeInTheDocument();
    });
});
