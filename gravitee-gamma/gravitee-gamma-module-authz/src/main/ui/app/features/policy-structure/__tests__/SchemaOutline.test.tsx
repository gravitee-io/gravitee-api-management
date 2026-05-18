import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { ParsedSchema } from '../../../../lib/gapl-parser';
import { SchemaOutline } from '../SchemaOutline';

const SAMPLE_SCHEMA: ParsedSchema = {
    entities: [
        // User → 'principal' category (known to entity-types.ts)
        { name: 'User', parents: ['Group'], attributes: [{ name: 'name', type: 'String' }], line: 6 },
        // MCPServer → 'mcp' category
        {
            name: 'MCPServer',
            parents: [],
            attributes: [
                { name: 'url', type: 'String' },
                { name: 'transport', type: 'String' },
            ],
            line: 12,
        },
    ],
    actions: [{ name: 'can_invoke', principals: ['User'], resources: ['MCPServer'], line: 20 }],
    diagnostics: [],
};

describe('SchemaOutline', () => {
    it('renders entity names in the outline', () => {
        render(<SchemaOutline parsed={SAMPLE_SCHEMA} onJump={vi.fn()} />);
        expect(screen.getByText('User')).toBeInTheDocument();
        expect(screen.getByText('MCPServer')).toBeInTheDocument();
    });

    it('renders action names in the outline', () => {
        render(<SchemaOutline parsed={SAMPLE_SCHEMA} onJump={vi.fn()} />);
        // action name is rendered as "can_invoke" inside quotes
        expect(screen.getByText(/"can_invoke"/)).toBeInTheDocument();
    });

    it('calls onJump with the correct line when User button is clicked', async () => {
        const onJump = vi.fn();
        render(<SchemaOutline parsed={SAMPLE_SCHEMA} onJump={onJump} />);

        await userEvent.click(screen.getByText('User'));

        expect(onJump).toHaveBeenCalledWith(6);
    });

    it('calls onJump with the correct line when MCPServer button is clicked', async () => {
        const onJump = vi.fn();
        render(<SchemaOutline parsed={SAMPLE_SCHEMA} onJump={onJump} />);

        await userEvent.click(screen.getByText('MCPServer'));

        expect(onJump).toHaveBeenCalledWith(12);
    });

    it('calls onJump with the correct line when action button is clicked', async () => {
        const onJump = vi.fn();
        render(<SchemaOutline parsed={SAMPLE_SCHEMA} onJump={onJump} />);

        await userEvent.click(screen.getByText(/"can_invoke"/));

        expect(onJump).toHaveBeenCalledWith(20);
    });

    it('shows empty state text when schema has no entities or actions', () => {
        const empty: ParsedSchema = { entities: [], actions: [], diagnostics: [] };
        render(<SchemaOutline parsed={empty} onJump={vi.fn()} />);

        expect(screen.getByText(/no entities or actions defined yet/i)).toBeInTheDocument();
    });

    it('groups entities by category', () => {
        render(<SchemaOutline parsed={SAMPLE_SCHEMA} onJump={vi.fn()} />);
        // Principals category header should appear (User is a principal)
        expect(screen.getByText('Principals')).toBeInTheDocument();
        // MCP category header should appear (MCPServer is mcp)
        expect(screen.getByText('MCP')).toBeInTheDocument();
    });

    it('highlights active row based on activeLine', () => {
        render(<SchemaOutline parsed={SAMPLE_SCHEMA} activeLine={6} onJump={vi.fn()} />);
        // The User button should have aria-current="true" since its line is 6
        const userButton = screen.getByText('User').closest('button');
        expect(userButton).toHaveAttribute('aria-current', 'true');
    });

    it('does not highlight inactive rows', () => {
        render(<SchemaOutline parsed={SAMPLE_SCHEMA} activeLine={6} onJump={vi.fn()} />);
        const mcpButton = screen.getByText('MCPServer').closest('button');
        expect(mcpButton).not.toHaveAttribute('aria-current');
    });
});
