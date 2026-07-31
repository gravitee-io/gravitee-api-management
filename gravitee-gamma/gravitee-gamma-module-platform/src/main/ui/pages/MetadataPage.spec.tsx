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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { MetadataPage } from './MetadataPage';
import { useEnvironmentMetadata } from '../features/metadata/hooks/useEnvironmentMetadata';
import { useCreateMetadata, useDeleteMetadata, useUpdateMetadata } from '../features/metadata/hooks/useMetadataMutations';
import type { Metadata } from '../features/metadata/types/metadata';
import { notify } from '../shared/notify';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useHasPermission: jest.fn(),
}));
jest.mock('../features/metadata/hooks/useEnvironmentMetadata');
jest.mock('../features/metadata/hooks/useMetadataMutations');
jest.mock('../shared/notify', () => ({
    notify: { success: jest.fn(), error: jest.fn() },
}));

// Stub MetadataTable to avoid Radix UI pointer-event complexity in jsdom.
// Exposes Edit/Delete buttons that wire directly to the page's callbacks.
jest.mock('../features/metadata/components/MetadataTable', () => ({
    MetadataTable: ({
        metadata,
        canEdit,
        canDelete,
        onEdit,
        onDelete,
    }: {
        metadata: Metadata[];
        canEdit: boolean;
        canDelete: boolean;
        onEdit: (m: Metadata) => void;
        onDelete: (m: Metadata) => void;
    }) => (
        <div>
            {metadata.map(m => (
                <div key={m.key} data-testid={`row-${m.key}`}>
                    <span>{m.name}</span>
                    {canEdit && (
                        <button type="button" onClick={() => onEdit(m)}>
                            Edit {m.name}
                        </button>
                    )}
                    {canDelete && (
                        <button type="button" onClick={() => onDelete(m)}>
                            Delete {m.name}
                        </button>
                    )}
                </div>
            ))}
        </div>
    ),
}));

const mockUseHasPermission = jest.mocked(useHasPermission);
const mockUseEnvironmentMetadata = jest.mocked(useEnvironmentMetadata);
const mockUseCreateMetadata = jest.mocked(useCreateMetadata);
const mockUseUpdateMetadata = jest.mocked(useUpdateMetadata);
const mockUseDeleteMetadata = jest.mocked(useDeleteMetadata);

const STUB_METADATA: Metadata[] = [
    { key: 'support-email', name: 'Support Email', format: 'MAIL', value: 'support@example.com' },
    { key: 'version', name: 'API Version', format: 'STRING', value: '1.0' },
];

function makeQueryResult(overrides: Partial<ReturnType<typeof useEnvironmentMetadata>> = {}): ReturnType<typeof useEnvironmentMetadata> {
    return {
        data: STUB_METADATA,
        isLoading: false,
        isError: false,
        ...overrides,
    } as ReturnType<typeof useEnvironmentMetadata>;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function makeMutation(mutateAsync = jest.fn()): any {
    return { mutateAsync, isPending: false };
}

function renderPage() {
    return render(<MetadataPage />);
}

describe('MetadataPage', () => {
    beforeEach(() => {
        mockUseHasPermission.mockReturnValue(true);
        mockUseEnvironmentMetadata.mockReturnValue(makeQueryResult());
        mockUseCreateMetadata.mockReturnValue(makeMutation());
        mockUseUpdateMetadata.mockReturnValue(makeMutation());
        mockUseDeleteMetadata.mockReturnValue(makeMutation());
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('page header', () => {
        it('renders the page title', () => {
            renderPage();
            expect(screen.queryByRole('heading', { name: 'Metadata' })).not.toBeNull();
        });

        it('shows the Add Global Metadata button when user can create', () => {
            renderPage();
            expect(screen.queryByRole('button', { name: /Add Global Metadata/i })).not.toBeNull();
        });

        it('hides the Add Global Metadata button when user cannot create', () => {
            mockUseHasPermission.mockImplementation(
                ({ anyOf }) => !!(anyOf?.includes('environment-metadata-u') || anyOf?.includes('environment-metadata-d')),
            );
            renderPage();
            expect(screen.queryByRole('button', { name: /Add Global Metadata/i })).toBeNull();
        });
    });

    describe('loading and error states', () => {
        it('shows skeleton rows while data is loading', () => {
            mockUseEnvironmentMetadata.mockReturnValue(makeQueryResult({ data: undefined, isLoading: true }));
            const { container } = renderPage();
            expect(container.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0);
        });

        it('shows an error message when the query fails', () => {
            mockUseEnvironmentMetadata.mockReturnValue(makeQueryResult({ data: undefined, isError: true }));
            renderPage();
            expect(screen.queryByText('Failed to load metadata. Please refresh and try again.')).not.toBeNull();
        });
    });

    describe('data table', () => {
        it('renders metadata rows in the table', () => {
            renderPage();
            expect(screen.queryByText('Support Email')).not.toBeNull();
            expect(screen.queryByText('API Version')).not.toBeNull();
        });

        it('hides action buttons when user has no permissions', () => {
            mockUseHasPermission.mockReturnValue(false);
            renderPage();
            expect(screen.queryByRole('button', { name: /Edit /i })).toBeNull();
            expect(screen.queryByRole('button', { name: /Delete /i })).toBeNull();
        });

        it('shows action buttons when user has edit and delete permissions', () => {
            renderPage();
            expect(screen.getAllByRole('button', { name: /Edit /i }).length).toBe(STUB_METADATA.length);
            expect(screen.getAllByRole('button', { name: /Delete /i }).length).toBe(STUB_METADATA.length);
        });
    });

    describe('create sheet', () => {
        it('opens the create sheet when Add Global Metadata is clicked', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Add Global Metadata/i }));
            expect(screen.queryByRole('heading', { name: 'Add Global Metadata' })).not.toBeNull();
        });

        it('closes the dialog when Cancel is clicked', () => {
            renderPage();
            fireEvent.click(screen.getByRole('button', { name: /Add Global Metadata/i }));
            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
            expect(screen.queryByRole('heading', { name: 'Add Global Metadata' })).toBeNull();
        });

        it('calls createMutation and shows success toast on submit', async () => {
            const mutateAsync = jest.fn().mockResolvedValue({});
            mockUseCreateMetadata.mockReturnValue(makeMutation(mutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: /Add Global Metadata/i }));
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Metadata' } });
            fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: 'some-value' } });
            fireEvent.click(screen.getByRole('button', { name: 'Add' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith(expect.objectContaining({ name: 'My Metadata', value: 'some-value' }));
                expect(notify.success).toHaveBeenCalledWith('Metadata created successfully');
            });
        });

        it('shows an error toast when create fails', async () => {
            const error = new Error('create failed');
            const mutateAsync = jest.fn().mockRejectedValue(error);
            mockUseCreateMetadata.mockReturnValue(makeMutation(mutateAsync));
            renderPage();

            fireEvent.click(screen.getByRole('button', { name: /Add Global Metadata/i }));
            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'My Metadata' } });
            fireEvent.change(screen.getByLabelText(/Value/i), { target: { value: 'some-value' } });
            fireEvent.click(screen.getByRole('button', { name: 'Add' }));

            await waitFor(() => {
                expect(notify.error).toHaveBeenCalledWith(error, 'Failed to create metadata');
            });
        });
    });

    describe('edit sheet', () => {
        function openEditSheet() {
            fireEvent.click(screen.getByRole('button', { name: `Edit ${STUB_METADATA[0].name}` }));
        }

        it('opens the edit sheet when Edit is clicked for a row', () => {
            renderPage();
            openEditSheet();
            expect(screen.queryByRole('heading', { name: 'Edit Metadata' })).not.toBeNull();
        });

        it('pre-fills the form with the selected metadata name', () => {
            renderPage();
            openEditSheet();
            expect((screen.getByLabelText(/Name/i) as HTMLInputElement).value).toBe('Support Email');
        });

        it('shows a hint that format cannot be changed in edit mode', () => {
            renderPage();
            openEditSheet();
            expect(screen.queryByText('Format cannot be changed after creation.')).not.toBeNull();
        });

        it('calls updateMutation and shows success toast on submit', async () => {
            const mutateAsync = jest.fn().mockResolvedValue({});
            mockUseUpdateMetadata.mockReturnValue(makeMutation(mutateAsync));
            renderPage();
            openEditSheet();

            fireEvent.change(screen.getByLabelText(/Name/i), { target: { value: 'Updated Name' } });
            fireEvent.click(screen.getByRole('button', { name: 'Update' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith(expect.objectContaining({ name: 'Updated Name' }));
                expect(notify.success).toHaveBeenCalledWith('Metadata updated successfully');
            });
        });
    });

    describe('delete dialog', () => {
        function openDeleteDialog() {
            fireEvent.click(screen.getByRole('button', { name: `Delete ${STUB_METADATA[0].name}` }));
        }

        it('opens the delete dialog when Delete is clicked for a row', () => {
            renderPage();
            openDeleteDialog();
            expect(screen.queryByRole('heading', { name: 'Delete Metadata' })).not.toBeNull();
        });

        it('shows the metadata name in the delete confirmation', () => {
            renderPage();
            openDeleteDialog();
            expect(screen.queryAllByText(STUB_METADATA[0].name).length).toBeGreaterThan(0);
        });

        it('calls deleteMutation and shows success toast on confirm', async () => {
            const mutateAsync = jest.fn().mockResolvedValue({});
            mockUseDeleteMetadata.mockReturnValue(makeMutation(mutateAsync));
            renderPage();
            openDeleteDialog();

            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => {
                expect(mutateAsync).toHaveBeenCalledWith('support-email');
                expect(notify.success).toHaveBeenCalledWith('Metadata deleted successfully');
            });
        });

        it('shows an error toast when delete fails', async () => {
            const error = new Error('delete failed');
            const mutateAsync = jest.fn().mockRejectedValue(error);
            mockUseDeleteMetadata.mockReturnValue(makeMutation(mutateAsync));
            renderPage();
            openDeleteDialog();

            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

            await waitFor(() => {
                expect(notify.error).toHaveBeenCalledWith(error, 'Failed to delete metadata');
            });
        });

        it('closes the dialog when Cancel is clicked', () => {
            renderPage();
            openDeleteDialog();
            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
            expect(screen.queryByRole('heading', { name: 'Delete Metadata' })).toBeNull();
        });
    });
});
