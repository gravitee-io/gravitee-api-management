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

import { EntrypointsAndShardingTagsPage } from './EntrypointsAndShardingTagsPage';
import { useEntrypointConfigurations } from '../features/entrypoints/hooks/useEntrypointConfigurations';
import { useEntrypointMappings } from '../features/entrypoints/hooks/useEntrypointMappings';
import type { EntrypointMappingRow } from '../features/entrypoints/types/entrypoint';

jest.mock('../features/entrypoints/hooks/useEntrypointConfigurations');
jest.mock('../features/entrypoints/hooks/useEntrypointMappings');

jest.mock('../features/entrypoints/components/EntrypointConfigurationSection', () => ({
    EntrypointConfigurationSection: () => <div data-testid="entrypoint-configuration-section" />,
}));

jest.mock('../features/entrypoints/components/EntrypointMappingsTable', () => ({
    EntrypointMappingsTable: ({
        rows,
        canCreate,
        onOpenDetail,
    }: {
        rows: EntrypointMappingRow[];
        canCreate: boolean;
        onOpenDetail: (row: EntrypointMappingRow) => void;
        onCreate?: () => void;
    }) => (
        <div>
            <div data-testid="mappings-can-create">{String(canCreate)}</div>
            {rows.map(row => (
                <button key={row.id} type="button" onClick={() => onOpenDetail(row)}>
                    Open {row.value}
                </button>
            ))}
            {rows.length === 0 ? <div>No entrypoints</div> : null}
        </div>
    ),
}));

jest.mock('../features/entrypoints/components/EntrypointDetailSheet', () => ({
    EntrypointDetailSheet: ({ entrypoint, onClose }: { entrypoint: EntrypointMappingRow | null; onClose: () => void }) =>
        entrypoint ? (
            <div>
                <div>Detail {entrypoint.value}</div>
                <button type="button" onClick={onClose}>
                    Close detail
                </button>
            </div>
        ) : null,
}));

const mockUseEntrypointConfigurations = jest.mocked(useEntrypointConfigurations);
const mockUseEntrypointMappings = jest.mocked(useEntrypointMappings);

const STUB_ROWS: EntrypointMappingRow[] = [
    {
        id: 'ep-1',
        value: 'https://api.example.com',
        target: 'HTTP',
        targetLabel: 'HTTP',
        tags: [],
        tagsName: [],
        environmentIds: [],
        environmentNames: [],
    },
];

describe('EntrypointsAndShardingTagsPage', () => {
    beforeEach(() => {
        mockUseEntrypointConfigurations.mockReturnValue({
            data: { configs: [], failedEnvironmentNames: [] },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useEntrypointConfigurations>);
        mockUseEntrypointMappings.mockReturnValue({
            rows: STUB_ROWS,
            isLoading: false,
            isError: false,
            isNameResolutionError: false,
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the page title and both sections', () => {
        render(<EntrypointsAndShardingTagsPage />);
        expect(screen.getByRole('heading', { name: 'Entrypoints & Sharding Tags' })).not.toBeNull();
        expect(screen.getByTestId('entrypoint-configuration-section')).not.toBeNull();
        expect(screen.getByText('Entrypoint Mappings')).not.toBeNull();
    });

    it('uses read-only page copy without create CTA', () => {
        render(<EntrypointsAndShardingTagsPage />);
        expect(
            screen.getByText(/View entrypoint configuration and mappings used by the Developer Portal based on API sharding tags/),
        ).not.toBeNull();
        expect(screen.queryByRole('button', { name: /Add a mapping/i })).toBeNull();
        expect(screen.getByTestId('mappings-can-create').textContent).toBe('false');
    });

    it('opens detail sheet when a mapping row is selected', () => {
        render(<EntrypointsAndShardingTagsPage />);
        fireEvent.click(screen.getByRole('button', { name: 'Open https://api.example.com' }));
        expect(screen.getByText('Detail https://api.example.com')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'Close detail' }));
        expect(screen.queryByText('Detail https://api.example.com')).toBeNull();
    });

    it('shows inline error when mappings fail to load', () => {
        mockUseEntrypointMappings.mockReturnValue({
            rows: [],
            isLoading: false,
            isError: true,
            isNameResolutionError: false,
        });
        render(<EntrypointsAndShardingTagsPage />);
        expect(screen.getByText(/Failed to load entrypoint mappings/)).not.toBeNull();
    });

    it('warns when environment or tag name resolution fails', () => {
        mockUseEntrypointMappings.mockReturnValue({
            rows: STUB_ROWS,
            isLoading: false,
            isError: false,
            isNameResolutionError: true,
        });
        render(<EntrypointsAndShardingTagsPage />);
        expect(screen.getByText(/Some environment or sharding tag names could not be loaded/)).not.toBeNull();
    });
});
