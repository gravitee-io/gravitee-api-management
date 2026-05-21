import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AiModelTargetPickerDialog, type AiProviderEntry } from '../AiModelTargetPickerDialog';

const PROVIDERS: AiProviderEntry[] = [
    {
        id: 'openai',
        name: 'OpenAI',
        description: 'Frontier reasoning + general models.',
        models: [
            { id: 'gpt-4o', name: 'GPT-4o', description: 'Multimodal flagship.' },
            { id: 'gpt-4o-mini', name: 'GPT-4o mini', description: 'Cheaper sibling.' },
        ],
    },
    {
        id: 'anthropic',
        name: 'Anthropic',
        description: 'Claude family.',
        models: [{ id: 'claude-opus-4', name: 'Claude Opus 4', description: 'Top-tier reasoning.' }],
    },
];

function makeProps(overrides: Partial<Parameters<typeof AiModelTargetPickerDialog>[0]> = {}): Parameters<typeof AiModelTargetPickerDialog>[0] {
    return {
        open: true,
        onOpenChange: vi.fn(),
        providers: PROVIDERS,
        existingTargetIds: [],
        onSelect: vi.fn(),
        ...overrides,
    };
}

describe('AiModelTargetPickerDialog', () => {
    it('renders the provider list and their nested models by default', () => {
        render(<AiModelTargetPickerDialog {...makeProps()} />);

        // Providers visible.
        expect(screen.getByText('OpenAI')).toBeInTheDocument();
        expect(screen.getByText('Anthropic')).toBeInTheDocument();
        // Models from both providers expanded by default (collapsed defaults to false).
        expect(screen.getByText('GPT-4o')).toBeInTheDocument();
        expect(screen.getByText('GPT-4o mini')).toBeInTheDocument();
        expect(screen.getByText('Claude Opus 4')).toBeInTheDocument();
    });

    it('selecting a provider then Continue calls onSelect with a Provider catalog entry', async () => {
        const onSelect = vi.fn();
        render(<AiModelTargetPickerDialog {...makeProps({ onSelect })} />);

        // The provider row exposes a button labelled by name + "Provider" badge.
        // Match by accessible name fragment.
        const openaiBtn = screen.getByRole('button', { name: /openai/i });
        await userEvent.click(openaiBtn);

        await userEvent.click(screen.getByRole('button', { name: /continue/i }));

        expect(onSelect).toHaveBeenCalledTimes(1);
        const arg = onSelect.mock.calls[0][0];
        expect(arg.id).toBe('openai');
        expect(arg.name).toBe('OpenAI');
        expect(arg.type).toBe('LLM');
        expect(arg.subResources).toHaveLength(2);
        expect(arg.subResources[0]).toMatchObject({ id: 'gpt-4o', name: 'GPT-4o', kind: 'LLMModel' });
        expect(arg.badges).toContain('AI Provider');
    });

    it('selecting a model then Continue calls onSelect with a Model catalog entry', async () => {
        const onSelect = vi.fn();
        render(<AiModelTargetPickerDialog {...makeProps({ onSelect })} />);

        // Click the model row.
        const modelBtn = screen.getByRole('button', { name: /gpt-4o mini/i });
        await userEvent.click(modelBtn);

        const continueBtn = screen.getByRole('button', { name: /continue/i });
        expect(continueBtn).not.toBeDisabled();
        await userEvent.click(continueBtn);

        expect(onSelect).toHaveBeenCalledTimes(1);
        const arg = onSelect.mock.calls[0][0];
        expect(arg.id).toBe('gpt-4o-mini');
        // Composite name: "Provider · Model".
        expect(arg.name).toMatch(/OpenAI.*GPT-4o mini/);
        expect(arg.type).toBe('LLM');
        expect(arg.badges).toEqual(expect.arrayContaining(['AI Model', 'OpenAI']));
        expect(arg.subResources).toEqual([]);
    });

    it('search filter narrows the providers list', async () => {
        render(<AiModelTargetPickerDialog {...makeProps()} />);

        const search = screen.getByRole('textbox', { name: /search providers or models/i });
        await userEvent.type(search, 'anthropic');

        // Anthropic remains, OpenAI is filtered out (provider-name-no-match + no matching models).
        expect(screen.getByText('Anthropic')).toBeInTheDocument();
        expect(screen.queryByText('OpenAI')).not.toBeInTheDocument();
        // Anthropic's model still rendered (provider matched).
        expect(screen.getByText('Claude Opus 4')).toBeInTheDocument();
    });

    it('Continue is disabled until something is selected', () => {
        render(<AiModelTargetPickerDialog {...makeProps()} />);

        expect(screen.getByRole('button', { name: /continue/i })).toBeDisabled();
        // Hint copy points the user at the empty selection.
        expect(screen.getByText(/select a provider or a specific model to continue/i)).toBeInTheDocument();
    });

    it('Cancel closes the dialog without calling onSelect', async () => {
        const onSelect = vi.fn();
        const onOpenChange = vi.fn();
        render(<AiModelTargetPickerDialog {...makeProps({ onSelect, onOpenChange })} />);

        await userEvent.click(screen.getByRole('button', { name: /^cancel$/i }));

        expect(onSelect).not.toHaveBeenCalled();
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });
});
