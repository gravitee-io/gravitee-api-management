import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { CatalogEntry } from '../../../../lib/api/authz-api.types';
import { TargetPickerDialog } from '../TargetPickerDialog';

const catalog: CatalogEntry[] = [
    {
        id: 'mcp-1',
        name: 'Flight Status MCP',
        description: 'Real-time flight data via MCP',
        type: 'MCP',
        subResources: [
            { id: 'tool-1', name: 'get_flight', kind: 'MCPTool' },
            { id: 'tool-2', name: 'search_flights', kind: 'MCPTool' },
        ],
        badges: ['catalog', 'sse'],
    },
    {
        id: 'mcp-2',
        name: 'Payments MCP',
        description: 'Charge and refund APIs',
        type: 'MCP',
        subResources: [],
        badges: ['catalog', 'http'],
    },
];

describe('TargetPickerDialog', () => {
    it('renders catalog entries', () => {
        render(
            <TargetPickerDialog
                open={true}
                onOpenChange={vi.fn()}
                catalog={catalog}
                existingTargetIds={[]}
                title="Pick MCP"
                description="Choose an MCP server"
                onSelect={vi.fn()}
            />,
        );

        expect(screen.getByText('Flight Status MCP')).toBeInTheDocument();
        expect(screen.getByText('Payments MCP')).toBeInTheDocument();
    });

    it('filters by search query', async () => {
        render(
            <TargetPickerDialog
                open={true}
                onOpenChange={vi.fn()}
                catalog={catalog}
                existingTargetIds={[]}
                title="Pick MCP"
                description="Choose an MCP server"
                onSelect={vi.fn()}
            />,
        );

        const searchInput = screen.getByRole('textbox', { name: /search catalog/i });
        await userEvent.type(searchInput, 'flight');

        expect(screen.getByText('Flight Status MCP')).toBeInTheDocument();
        expect(screen.queryByText('Payments MCP')).not.toBeInTheDocument();
    });

    it('hides already-targeted entries', () => {
        render(
            <TargetPickerDialog
                open={true}
                onOpenChange={vi.fn()}
                catalog={catalog}
                existingTargetIds={['mcp-1']}
                title="Pick MCP"
                description="Choose an MCP server"
                onSelect={vi.fn()}
            />,
        );

        expect(screen.queryByText('Flight Status MCP')).not.toBeInTheDocument();
        expect(screen.getByText('Payments MCP')).toBeInTheDocument();
    });

    it('calls onSelect when continue is clicked after selection', async () => {
        const onSelect = vi.fn();

        render(
            <TargetPickerDialog
                open={true}
                onOpenChange={vi.fn()}
                catalog={catalog}
                existingTargetIds={[]}
                title="Pick MCP"
                description="Choose an MCP server"
                onSelect={onSelect}
            />,
        );

        // Click on an entry
        await userEvent.click(screen.getByRole('button', { name: /flight status mcp/i }));
        // Click continue
        await userEvent.click(screen.getByRole('button', { name: /continue/i }));

        expect(onSelect).toHaveBeenCalledWith(catalog[0]);
    });

    it('continue is disabled when nothing is selected', () => {
        render(
            <TargetPickerDialog
                open={true}
                onOpenChange={vi.fn()}
                catalog={catalog}
                existingTargetIds={[]}
                title="Pick MCP"
                description="Choose an MCP server"
                onSelect={vi.fn()}
            />,
        );

        expect(screen.getByRole('button', { name: /continue/i })).toBeDisabled();
    });

    it('shows sub-resource counts', () => {
        render(
            <TargetPickerDialog
                open={true}
                onOpenChange={vi.fn()}
                catalog={catalog}
                existingTargetIds={[]}
                title="Pick MCP"
                description="Choose an MCP server"
                onSelect={vi.fn()}
            />,
        );

        expect(screen.getByText(/2 tools/)).toBeInTheDocument();
    });
});
