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

jest.mock('@gravitee/graphene-core', () => ({
    Badge: ({ children }: { children?: React.ReactNode }) => <span>{children}</span>,
    Button: ({
        children,
        onClick,
        disabled,
        'aria-label': ariaLabel,
    }: {
        children?: React.ReactNode;
        onClick?: () => void;
        disabled?: boolean;
        'aria-label'?: string;
    }) => (
        <button type="button" onClick={onClick} disabled={disabled} aria-label={ariaLabel}>
            {children}
        </button>
    ),
    Card: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    CardContent: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    CardHeader: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    CardTitle: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    Dialog: ({ children, open }: { children?: React.ReactNode; open?: boolean }) => (open ? <div role="dialog">{children}</div> : null),
    DialogContent: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    DialogFooter: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    DialogHeader: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
    DialogTitle: ({ children }: { children?: React.ReactNode }) => <h2>{children}</h2>,
    Tooltip: ({ children }: { children?: React.ReactNode }) => <>{children}</>,
    TooltipContent: () => null,
    TooltipTrigger: ({ children, asChild }: { children?: React.ReactNode; asChild?: boolean }) =>
        asChild ? <>{children}</> : <div>{children}</div>,
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

import { fireEvent, render, screen } from '@testing-library/react';

import { EndpointGroupList } from './EndpointGroupList';
import type { EndpointGroupDto } from '../../../types';

// ─── Stub data ────────────────────────────────────────────────────────────────

const EP_A = { name: 'ep-a', type: 'http-proxy', weight: 1, configuration: { target: 'https://backend-a.example.com' } };
const EP_B = { name: 'ep-b', type: 'http-proxy', weight: 2, configuration: { target: 'https://backend-b.example.com' } };
const EP_C = { name: 'ep-c', type: 'http-proxy', weight: 1, configuration: { target: 'https://backend-c.example.com' } };

const GROUP_1: EndpointGroupDto = {
    name: 'default-group',
    type: 'http-proxy',
    loadBalancer: { type: 'ROUND_ROBIN' },
    endpoints: [EP_A, EP_B],
};

const GROUP_2: EndpointGroupDto = {
    name: 'backup-group',
    type: 'http-proxy',
    loadBalancer: { type: 'RANDOM' },
    endpoints: [EP_C],
};

// ─── Helpers ──────────────────────────────────────────────────────────────────

function makeProps(overrides: Partial<React.ComponentProps<typeof EndpointGroupList>> = {}) {
    return {
        groups: [GROUP_1, GROUP_2],
        isReadOnly: false,
        onEditGroup: jest.fn(),
        onDeleteGroup: jest.fn(),
        onAddEndpoint: jest.fn(),
        onEditEndpoint: jest.fn(),
        onDeleteEndpoint: jest.fn(),
        onReorderEndpoints: jest.fn(),
        ...overrides,
    };
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('EndpointGroupList', () => {
    afterEach(() => jest.clearAllMocks());

    // ── Rendering ─────────────────────────────────────────────────────────────

    describe('rendering', () => {
        it('renders each group name', () => {
            render(<EndpointGroupList {...makeProps()} />);
            expect(screen.getByText('default-group')).toBeInTheDocument();
            expect(screen.getByText('backup-group')).toBeInTheDocument();
        });

        it('renders endpoint names and target URLs', () => {
            render(<EndpointGroupList {...makeProps()} />);
            expect(screen.getByText('ep-a')).toBeInTheDocument();
            expect(screen.getByText('https://backend-a.example.com')).toBeInTheDocument();
        });

        it('shows "Default" badge only on the first group', () => {
            render(<EndpointGroupList {...makeProps()} />);
            expect(screen.getAllByText('Default')).toHaveLength(1);
        });

        it('shows "Round robin" load-balancer label for ROUND_ROBIN', () => {
            render(<EndpointGroupList {...makeProps()} />);
            expect(screen.getByText('Round robin')).toBeInTheDocument();
        });

        it('shows "Random" load-balancer label for RANDOM', () => {
            render(<EndpointGroupList {...makeProps()} />);
            expect(screen.getByText('Random')).toBeInTheDocument();
        });

        it('shows "No endpoints configured." when a group has no endpoints', () => {
            const emptyGroup: EndpointGroupDto = { name: 'empty-group', type: 'http-proxy', endpoints: [] };
            render(<EndpointGroupList {...makeProps({ groups: [emptyGroup, GROUP_2] })} />);
            expect(screen.getByText('No endpoints configured.')).toBeInTheDocument();
        });

        it('renders endpoint weight', () => {
            render(<EndpointGroupList {...makeProps()} />);
            expect(screen.getByText('2')).toBeInTheDocument();
        });
    });

    // ── Read-only mode ────────────────────────────────────────────────────────

    describe('read-only mode', () => {
        it('hides all edit, delete, and add-endpoint buttons when isReadOnly', () => {
            render(<EndpointGroupList {...makeProps({ isReadOnly: true })} />);
            expect(screen.queryByRole('button', { name: /edit group/i })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: /delete group/i })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: /add endpoint/i })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: /edit endpoint/i })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: /delete endpoint/i })).not.toBeInTheDocument();
        });
    });

    // ── Group actions ─────────────────────────────────────────────────────────

    describe('group actions', () => {
        it('calls onEditGroup with the group index when Edit is clicked', () => {
            const onEditGroup = jest.fn();
            render(<EndpointGroupList {...makeProps({ onEditGroup })} />);
            fireEvent.click(screen.getByRole('button', { name: 'Edit group default-group' }));
            expect(onEditGroup).toHaveBeenCalledWith(0);
        });

        it('calls onEditGroup with index 1 for the second group', () => {
            const onEditGroup = jest.fn();
            render(<EndpointGroupList {...makeProps({ onEditGroup })} />);
            fireEvent.click(screen.getByRole('button', { name: 'Edit group backup-group' }));
            expect(onEditGroup).toHaveBeenCalledWith(1);
        });

        it('disables the delete group button when only one group exists', () => {
            render(<EndpointGroupList {...makeProps({ groups: [GROUP_1] })} />);
            expect(screen.getByRole('button', { name: 'Delete group default-group' })).toBeDisabled();
        });

        it('enables the delete group button when more than one group exists', () => {
            render(<EndpointGroupList {...makeProps()} />);
            expect(screen.getByRole('button', { name: 'Delete group default-group' })).not.toBeDisabled();
        });

        it('opens a confirmation dialog when delete group is clicked', () => {
            render(<EndpointGroupList {...makeProps()} />);
            fireEvent.click(screen.getByRole('button', { name: 'Delete group default-group' }));
            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(screen.getByText('Delete endpoint group')).toBeInTheDocument();
        });

        it('calls onDeleteGroup with the correct index after confirming deletion', () => {
            const onDeleteGroup = jest.fn();
            render(<EndpointGroupList {...makeProps({ onDeleteGroup })} />);
            fireEvent.click(screen.getByRole('button', { name: 'Delete group default-group' }));
            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
            expect(onDeleteGroup).toHaveBeenCalledWith(0);
        });

        it('does not call onDeleteGroup when the confirmation dialog is cancelled', () => {
            const onDeleteGroup = jest.fn();
            render(<EndpointGroupList {...makeProps({ onDeleteGroup })} />);
            fireEvent.click(screen.getByRole('button', { name: 'Delete group default-group' }));
            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
            expect(onDeleteGroup).not.toHaveBeenCalled();
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });
    });

    // ── Endpoint actions ──────────────────────────────────────────────────────

    describe('endpoint actions', () => {
        it('calls onAddEndpoint with the correct group index', () => {
            const onAddEndpoint = jest.fn();
            render(<EndpointGroupList {...makeProps({ onAddEndpoint })} />);
            // First "Add endpoint" button belongs to GROUP_1 (index 0)
            fireEvent.click(screen.getAllByRole('button', { name: /add endpoint/i })[0]);
            expect(onAddEndpoint).toHaveBeenCalledWith(0);
        });

        it('calls onAddEndpoint with index 1 for the second group', () => {
            const onAddEndpoint = jest.fn();
            render(<EndpointGroupList {...makeProps({ onAddEndpoint })} />);
            fireEvent.click(screen.getAllByRole('button', { name: /add endpoint/i })[1]);
            expect(onAddEndpoint).toHaveBeenCalledWith(1);
        });

        it('calls onEditEndpoint with correct group and endpoint indices', () => {
            const onEditEndpoint = jest.fn();
            render(<EndpointGroupList {...makeProps({ onEditEndpoint })} />);
            fireEvent.click(screen.getByRole('button', { name: 'Edit endpoint ep-a' }));
            expect(onEditEndpoint).toHaveBeenCalledWith(0, 0);
        });

        it('disables delete endpoint when the group has only one endpoint', () => {
            render(<EndpointGroupList {...makeProps()} />);
            // GROUP_2 has only EP_C
            expect(screen.getByRole('button', { name: 'Delete endpoint ep-c' })).toBeDisabled();
        });

        it('enables delete endpoint when the group has more than one endpoint', () => {
            render(<EndpointGroupList {...makeProps()} />);
            // GROUP_1 has EP_A and EP_B
            expect(screen.getByRole('button', { name: 'Delete endpoint ep-a' })).not.toBeDisabled();
        });

        it('opens a confirmation dialog when delete endpoint is clicked', () => {
            render(<EndpointGroupList {...makeProps()} />);
            fireEvent.click(screen.getByRole('button', { name: 'Delete endpoint ep-a' }));
            expect(screen.getByRole('dialog')).toBeInTheDocument();
            expect(screen.getByText('Delete endpoint')).toBeInTheDocument();
        });

        it('calls onDeleteEndpoint with correct indices after confirming deletion', () => {
            const onDeleteEndpoint = jest.fn();
            render(<EndpointGroupList {...makeProps({ onDeleteEndpoint })} />);
            fireEvent.click(screen.getByRole('button', { name: 'Delete endpoint ep-a' }));
            fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
            expect(onDeleteEndpoint).toHaveBeenCalledWith(0, 0);
        });

        it('does not call onDeleteEndpoint when the confirmation dialog is cancelled', () => {
            const onDeleteEndpoint = jest.fn();
            render(<EndpointGroupList {...makeProps({ onDeleteEndpoint })} />);
            fireEvent.click(screen.getByRole('button', { name: 'Delete endpoint ep-a' }));
            fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
            expect(onDeleteEndpoint).not.toHaveBeenCalled();
        });
    });

    // ── Endpoint reordering ───────────────────────────────────────────────────

    describe('endpoint reordering', () => {
        it('disables Move up for the first endpoint in a group', () => {
            render(<EndpointGroupList {...makeProps()} />);
            expect(screen.getByRole('button', { name: 'Move ep-a up' })).toBeDisabled();
        });

        it('disables Move down for the last endpoint in a group', () => {
            render(<EndpointGroupList {...makeProps()} />);
            // ep-b is the last endpoint in GROUP_1
            expect(screen.getByRole('button', { name: 'Move ep-b down' })).toBeDisabled();
        });

        it('enables Move down for a non-last endpoint', () => {
            render(<EndpointGroupList {...makeProps()} />);
            expect(screen.getByRole('button', { name: 'Move ep-a down' })).not.toBeDisabled();
        });

        it('enables Move up for a non-first endpoint', () => {
            render(<EndpointGroupList {...makeProps()} />);
            expect(screen.getByRole('button', { name: 'Move ep-b up' })).not.toBeDisabled();
        });

        it('calls onReorderEndpoints(groupIdx, fromIdx, toIdx) when Move up is clicked', () => {
            const onReorderEndpoints = jest.fn();
            render(<EndpointGroupList {...makeProps({ onReorderEndpoints })} />);
            // ep-b is at index 1 in GROUP_1 — move up → (0, 1, 0)
            fireEvent.click(screen.getByRole('button', { name: 'Move ep-b up' }));
            expect(onReorderEndpoints).toHaveBeenCalledWith(0, 1, 0);
        });

        it('calls onReorderEndpoints(groupIdx, fromIdx, toIdx) when Move down is clicked', () => {
            const onReorderEndpoints = jest.fn();
            render(<EndpointGroupList {...makeProps({ onReorderEndpoints })} />);
            // ep-a is at index 0 in GROUP_1 — move down → (0, 0, 1)
            fireEvent.click(screen.getByRole('button', { name: 'Move ep-a down' }));
            expect(onReorderEndpoints).toHaveBeenCalledWith(0, 0, 1);
        });
    });
});
