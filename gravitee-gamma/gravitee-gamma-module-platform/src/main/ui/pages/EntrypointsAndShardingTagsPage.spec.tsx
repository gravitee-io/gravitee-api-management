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
import { useHasFeature, useHasPermission } from '@gravitee/gamma-modules-sdk';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { EntrypointsAndShardingTagsPage } from './EntrypointsAndShardingTagsPage';
import { useEntrypointConfigurations } from '../features/entrypoints/hooks/useEntrypointConfigurations';
import { useEntrypointMappings } from '../features/entrypoints/hooks/useEntrypointMappings';
import { useCreateEntrypoint, useDeleteEntrypoint, useUpdateEntrypoint } from '../features/entrypoints/hooks/useEntrypointMutations';
import { useShardingTags } from '../features/entrypoints/hooks/useShardingTags';
import type { EntrypointMappingRow, EntrypointTarget, ShardingTagRow } from '../features/entrypoints/types/entrypoint';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
    useHasFeature: jest.fn(),
}));
jest.mock('../features/entrypoints/hooks/useEntrypointConfigurations');
jest.mock('../features/entrypoints/hooks/useEntrypointMappings');
jest.mock('../features/entrypoints/hooks/useEntrypointMutations');
jest.mock('../features/entrypoints/hooks/useShardingTags');
jest.mock('../features/entrypoints/components/EntrypointConfigurationSection', () => ({
    EntrypointConfigurationSection: () => <div data-testid="entrypoint-configuration-section" />,
}));

jest.mock('../features/entrypoints/components/EntrypointMappingsTable', () => ({
    CreateMappingButton: ({ onCreate }: { onCreate: (target: EntrypointTarget) => void }) => (
        <button type="button" onClick={() => onCreate('HTTP')}>
            Add a mapping
        </button>
    ),
    EntrypointMappingsTable: ({
        rows,
        onOpenDetail,
    }: {
        rows: EntrypointMappingRow[];
        canCreate: boolean;
        canEdit: boolean;
        canDelete: boolean;
        onOpenDetail: (row: EntrypointMappingRow) => void;
        onCreate?: (target: EntrypointTarget) => void;
    }) => (
        <div>
            {rows.map(row => (
                <button key={row.id} type="button" onClick={() => onOpenDetail(row)}>
                    Open {row.value}
                </button>
            ))}
            {rows.length === 0 ? <div>No entrypoints</div> : null}
        </div>
    ),
}));

jest.mock('../features/entrypoints/components/ShardingTagsTable', () => ({
    CreateShardingTagButton: ({ onCreate }: { onCreate?: () => void }) => (
        <button type="button" onClick={onCreate}>
            Add a tag
        </button>
    ),
    ShardingTagsTable: ({
        rows,
        canCreate,
        onOpenDetail,
        onEdit,
        onDelete,
        canEdit,
        canDelete,
    }: {
        rows: ShardingTagRow[];
        canCreate: boolean;
        onOpenDetail: (row: ShardingTagRow) => void;
        onEdit?: (row: ShardingTagRow) => void;
        onDelete?: (row: ShardingTagRow) => void;
        canEdit?: boolean;
        canDelete?: boolean;
        onCreate?: () => void;
        onUpgrade: () => void;
        hasLicense: boolean;
    }) => (
        <div>
            <div data-testid="tags-can-create">{String(canCreate)}</div>
            {rows.map(row => (
                <div key={row.id}>
                    <button type="button" onClick={() => onOpenDetail(row)}>
                        Open tag {row.key}
                    </button>
                    {canEdit ? (
                        <button type="button" onClick={() => onEdit?.(row)}>
                            Edit tag {row.key}
                        </button>
                    ) : null}
                    {canDelete ? (
                        <button type="button" onClick={() => onDelete?.(row)}>
                            Delete tag {row.key}
                        </button>
                    ) : null}
                </div>
            ))}
            {rows.length === 0 ? <div>No sharding tags</div> : null}
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

jest.mock('../features/entrypoints/components/EntrypointSheet', () => ({
    EntrypointSheet: () => null,
}));

jest.mock('../features/entrypoints/components/EntrypointDeleteSheet', () => ({
    EntrypointDeleteSheet: () => null,
}));

jest.mock('../features/entrypoints/components/ShardingTagsLicenseDialog', () => ({
    ShardingTagsLicenseDialog: ({ open }: { open: boolean; onOpenChange: (open: boolean) => void }) =>
        open ? <div>License dialog</div> : null,
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseHasFeature = jest.mocked(useHasFeature);
const mockUseEntrypointConfigurations = jest.mocked(useEntrypointConfigurations);
const mockUseEntrypointMappings = jest.mocked(useEntrypointMappings);
const mockUseShardingTags = jest.mocked(useShardingTags);
const mockUseCreateEntrypoint = jest.mocked(useCreateEntrypoint);
const mockUseUpdateEntrypoint = jest.mocked(useUpdateEntrypoint);
const mockUseDeleteEntrypoint = jest.mocked(useDeleteEntrypoint);

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

const STUB_TAG_ROWS: ShardingTagRow[] = [
    {
        id: 'tag-1',
        key: 'prod',
        name: 'Production',
        description: 'Prod tag',
        restrictedGroupIds: [],
        restrictedGroupNames: [],
    },
];

const idleMutation = {
    mutateAsync: jest.fn(),
    isPending: false,
} as unknown as ReturnType<typeof useCreateEntrypoint>;

describe('EntrypointsAndShardingTagsPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseHasFeature.mockReturnValue(true);
        mockUseEntrypointConfigurations.mockReturnValue({
            data: { configs: [], failedEnvironmentNames: [] },
            isLoading: false,
            isError: false,
        } as ReturnType<typeof useEntrypointConfigurations>);
        mockUseEntrypointMappings.mockReturnValue({
            rows: STUB_ROWS,
            tags: [],
            environments: [],
            isLoading: false,
            isError: false,
            isNameResolutionError: false,
        });
        mockUseShardingTags.mockReturnValue({
            rows: STUB_TAG_ROWS,
            isLoading: false,
            isError: false,
            isGroupNameResolutionError: false,
        });
        mockUseCreateEntrypoint.mockReturnValue(idleMutation);
        mockUseUpdateEntrypoint.mockReturnValue(idleMutation as ReturnType<typeof useUpdateEntrypoint>);
        mockUseDeleteEntrypoint.mockReturnValue(idleMutation as ReturnType<typeof useDeleteEntrypoint>);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('renders the page title and both sections', () => {
        render(<EntrypointsAndShardingTagsPage />);
        expect(screen.getByRole('heading', { name: 'Entrypoints & Sharding Tags' })).not.toBeNull();
        expect(screen.getByTestId('entrypoint-configuration-section')).not.toBeNull();
        expect(screen.getByText('Sharding Tags')).not.toBeNull();
        expect(screen.getByText('Entrypoint Mappings')).not.toBeNull();
        expect(
            screen.getByText(/Manage sharding tags, entrypoints, and mappings between them both for Console and the Developer Portal/),
        ).not.toBeNull();
    });

    it('shows header create button when user can create and mappings exist', () => {
        render(<EntrypointsAndShardingTagsPage />);
        expect(screen.getByRole('button', { name: /Add a mapping/i })).not.toBeNull();
    });

    it('hides create button when user lacks create permission', () => {
        mockUseHasPermission.mockImplementation(options => {
            const anyOf = 'anyOf' in options ? options.anyOf : [];
            return anyOf.includes('environment-entrypoint-r') || anyOf.includes('organization-entrypoint-r');
        });
        render(<EntrypointsAndShardingTagsPage />);
        expect(screen.queryByRole('button', { name: /Add a mapping/i })).toBeNull();
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
            tags: [],
            environments: [],
            isLoading: false,
            isError: true,
            isNameResolutionError: false,
        });
        render(<EntrypointsAndShardingTagsPage />);
        expect(screen.getByText(/Failed to load entrypoint mappings/)).not.toBeNull();
    });

    it('hides sharding tags section when user cannot read tags', () => {
        mockUseHasPermission.mockImplementation(options => {
            const anyOf = 'anyOf' in options ? options.anyOf : [];
            if (anyOf.some(p => typeof p === 'string' && p.includes('-tag-'))) return false;
            return true;
        });
        render(<EntrypointsAndShardingTagsPage />);
        expect(screen.queryByText('Sharding Tags')).toBeNull();
        expect(screen.getByText('Entrypoint Mappings')).not.toBeNull();
    });

    it('opens tag detail sheet when a tag row is selected', () => {
        render(<EntrypointsAndShardingTagsPage />);
        fireEvent.click(screen.getByRole('button', { name: 'Open tag prod' }));
        expect(screen.getByText('Tag detail prod')).not.toBeNull();
        fireEvent.click(screen.getByRole('button', { name: 'Close tag detail' }));
        expect(screen.queryByText('Tag detail prod')).toBeNull();
    });

                            describe('role-scoped create permission', () => {
        it('treats organization-entrypoint-c alone as sufficient for create', () => {
            mockUseHasPermission.mockImplementation(options => {
                const anyOf = 'anyOf' in options ? options.anyOf : [];
                return anyOf.includes('organization-entrypoint-c');
            });
            render(<EntrypointsAndShardingTagsPage />);
            expect(screen.getByRole('button', { name: /Add a mapping/i })).not.toBeNull();
        });

        it('treats environment-entrypoint-c alone as sufficient for create', () => {
            mockUseHasPermission.mockImplementation(options => {
                const anyOf = 'anyOf' in options ? options.anyOf : [];
                return anyOf.includes('environment-entrypoint-c');
            });
            render(<EntrypointsAndShardingTagsPage />);
            expect(screen.getByRole('button', { name: /Add a mapping/i })).not.toBeNull();
        });
    });

    it('warns when environment or tag name resolution fails', () => {
        mockUseEntrypointMappings.mockReturnValue({
            rows: STUB_ROWS,
            tags: [],
            environments: [],
            isLoading: false,
            isError: false,
            isNameResolutionError: true,
        });
        render(<EntrypointsAndShardingTagsPage />);
        expect(screen.getByText(/Some environment or sharding tag names could not be loaded/)).not.toBeNull();
    });
});
