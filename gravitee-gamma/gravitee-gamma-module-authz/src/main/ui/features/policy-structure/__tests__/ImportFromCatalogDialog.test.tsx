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
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import type {
    AgentCatalogItem,
    CatalogItemKind,
    McpServerCatalogItem,
    ModelCatalogItem,
} from '../../../shared/api/aim-catalog.types';
import { ImportFromCatalogDialog } from '../ImportFromCatalogDialog';

beforeAll(() => {
    if (!Element.prototype.scrollIntoView) {
        Element.prototype.scrollIntoView = () => undefined;
    }
});

const listItemsSpy = vi.fn();
const listEntitiesSpy = vi.fn();
const createEntitySpy = vi.fn();
const toastSuccessSpy = vi.fn();
const toastErrorSpy = vi.fn();

vi.mock('../../../shared/api/aim-catalog.service', () => ({
    aimCatalogService: {
        listItems: (env: string, kind: string, page: number, perPage: number) => listItemsSpy(env, kind, page, perPage),
    },
}));

vi.mock('../../../shared/api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        listEntities: (env: string, params?: unknown) => listEntitiesSpy(env, params),
        createEntity: (env: string, req: unknown) => createEntitySpy(env, req),
    },
}));

interface EntityFixture {
    readonly id: string;
    readonly uid: string;
    readonly catalogId?: string;
}

function seedImported(entities: readonly EntityFixture[]) {
    listEntitiesSpy.mockImplementation(() =>
        Promise.resolve({
            data: entities.map(e => ({
                id: e.id,
                environmentId: 'DEFAULT',
                uid: e.uid,
                attributes: e.catalogId ? { _catalogId: e.catalogId } : {},
                parents: [],
                createdAt: '2026-05-27T10:00:00.000Z',
                updatedAt: '2026-05-27T10:00:00.000Z',
            })),
            total: entities.length,
            page: 1,
            perPage: 1000,
        }),
    );
}

vi.mock('@gravitee/graphene-core', async importOriginal => {
    const actual = await importOriginal<Record<string, unknown>>();
    return {
        ...actual,
        toast: {
            success: (msg: string) => toastSuccessSpy(msg),
            error: (msg: string) => toastErrorSpy(msg),
        },
    };
});

function makeModel(
    id: string,
    name: string,
    opts: { provider?: string; sourceKind?: string; entityId?: string | null } = {},
): ModelCatalogItem {
    return {
        id,
        entityId: opts.entityId ?? null,
        kind: 'model',
        sourceId: null,
        sourceKind: opts.sourceKind ?? null,
        parentId: null,
        environmentId: 'DEFAULT',
        organizationId: 'DEFAULT',
        creationDate: '2026-05-27T10:00:00.000Z',
        updateDate: '2026-05-27T10:00:00.000Z',
        definition: { name, queryName: name.toLowerCase(), provider: opts.provider, description: `Model ${name}` },
    };
}

function makeMcp(id: string, name: string, opts: { canonicalEntityId?: string; entityId?: string | null } = {}): McpServerCatalogItem {
    return {
        id,
        entityId: opts.entityId ?? null,
        kind: 'mcp-server',
        sourceId: null,
        sourceKind: null,
        parentId: null,
        environmentId: 'DEFAULT',
        organizationId: 'DEFAULT',
        creationDate: '2026-05-27T10:00:00.000Z',
        updateDate: '2026-05-27T10:00:00.000Z',
        definition: { serverInfo: { name, title: `${name} server` } },
        extensions: { entityId: opts.canonicalEntityId, description: `MCP ${name}` },
    };
}

function makeAgent(id: string, name: string, opts: { entityId?: string | null } = {}): AgentCatalogItem {
    return {
        id,
        entityId: opts.entityId ?? null,
        kind: 'agent',
        sourceId: null,
        sourceKind: null,
        parentId: null,
        environmentId: 'DEFAULT',
        organizationId: 'DEFAULT',
        creationDate: '2026-05-27T10:00:00.000Z',
        updateDate: '2026-05-27T10:00:00.000Z',
        definition: { name, description: `Agent ${name}`, url: `https://${name}.example` },
    };
}

interface CatalogSeed {
    readonly models?: readonly ModelCatalogItem[];
    readonly mcpServers?: readonly McpServerCatalogItem[];
    readonly agents?: readonly AgentCatalogItem[];
}

function seedCatalog(seed: CatalogSeed) {
    const models = seed.models ?? [];
    const mcpServers = seed.mcpServers ?? [];
    const agents = seed.agents ?? [];
    listItemsSpy.mockImplementation((_env: string, kind: CatalogItemKind) => {
        if (kind === 'model') return Promise.resolve({ data: models, page: 1, perPage: 100, total: models.length });
        if (kind === 'mcp-server') return Promise.resolve({ data: mcpServers, page: 1, perPage: 100, total: mcpServers.length });
        if (kind === 'agent') return Promise.resolve({ data: agents, page: 1, perPage: 100, total: agents.length });
        return Promise.resolve({ data: [], page: 1, perPage: 100, total: 0 });
    });
}

interface RenderOptions {
    readonly onImported?: () => void;
    readonly onOpenChange?: (open: boolean) => void;
}

function renderDialog(opts: RenderOptions = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    return render(
        <ImportFromCatalogDialog
            open={true}
            environmentId="DEFAULT"
            onOpenChange={opts.onOpenChange ?? (() => undefined)}
            onImported={opts.onImported ?? (() => undefined)}
        />,
        { wrapper },
    );
}

async function waitForList() {
    await waitFor(() => expect(listItemsSpy).toHaveBeenCalled());
}

function getDialog() {
    return screen.getByRole('dialog');
}

beforeEach(() => {
    listItemsSpy.mockReset();
    listEntitiesSpy.mockReset();
    createEntitySpy.mockReset();
    toastSuccessSpy.mockReset();
    toastErrorSpy.mockReset();
    // Default: no prior imports — tests can override via seedImported().
    seedImported([]);
});

describe('ImportFromCatalogDialog', () => {
    it('queries all three catalog kinds when opened', async () => {
        seedCatalog({});
        renderDialog();

        await waitFor(() => expect(listItemsSpy).toHaveBeenCalledTimes(3));
        const kinds = listItemsSpy.mock.calls.map(args => args[1]).sort();
        expect(kinds).toEqual(['agent', 'mcp-server', 'model']);
    });

    it('shows MCP servers tab by default and switches to AI Models on click', async () => {
        seedCatalog({
            models: [makeModel('m1', 'Gemini')],
            mcpServers: [makeMcp('s1', 'flight-status')],
        });
        renderDialog();
        await waitForList();

        const dialog = getDialog();
        // MCP Servers tab open initially → server item visible.
        await waitFor(() => expect(within(dialog).getByText('flight-status server')).toBeInTheDocument());

        const user = userEvent.setup();
        await user.click(within(dialog).getByRole('tab', { name: /AI Models/i }));

        await waitFor(() => expect(within(dialog).getByText('Gemini')).toBeInTheDocument());
        expect(within(dialog).queryByText('flight-status server')).not.toBeInTheDocument();
    });

    it('filters the visible list by search text', async () => {
        seedCatalog({
            mcpServers: [makeMcp('s1', 'flight-status'), makeMcp('s2', 'hotel-booking')],
        });
        renderDialog();
        await waitForList();

        const dialog = getDialog();
        await waitFor(() => expect(within(dialog).getByText('flight-status server')).toBeInTheDocument());
        expect(within(dialog).getByText('hotel-booking server')).toBeInTheDocument();

        const search = within(dialog).getByLabelText('Search catalog entries');
        const user = userEvent.setup();
        await user.type(search, 'flight');

        await waitFor(() => {
            expect(within(dialog).queryByText('hotel-booking server')).not.toBeInTheDocument();
            expect(within(dialog).getByText('flight-status server')).toBeInTheDocument();
        });
    });

    it('disables Import until at least one item is selected', async () => {
        seedCatalog({ mcpServers: [makeMcp('s1', 'flight-status')] });
        renderDialog();
        await waitForList();

        const dialog = getDialog();
        const importBtn = within(dialog).getByRole('button', { name: /^Import$/i });
        expect(importBtn).toBeDisabled();

        await waitFor(() => expect(within(dialog).getByText('flight-status server')).toBeInTheDocument());
        const user = userEvent.setup();
        await user.click(within(dialog).getByLabelText('Select flight-status server'));

        expect(importBtn).not.toBeDisabled();
        expect(within(dialog).getByText('1 item selected')).toBeInTheDocument();
    });

    it('imports selected items as RESOURCE entities with the gravitee-catalog source label', async () => {
        seedCatalog({
            mcpServers: [makeMcp('s1', 'flight-status', { canonicalEntityId: 'flight-status' })],
            models: [makeModel('m1', 'Gemini', { provider: 'google' })],
        });
        createEntitySpy.mockResolvedValue({});
        const onImported = vi.fn();
        const onOpenChange = vi.fn();
        renderDialog({ onImported, onOpenChange });
        await waitForList();

        const dialog = getDialog();
        const user = userEvent.setup();

        await waitFor(() => expect(within(dialog).getByText('flight-status server')).toBeInTheDocument());
        await user.click(within(dialog).getByLabelText('Select flight-status server'));

        await user.click(within(dialog).getByRole('tab', { name: /AI Models/i }));
        await waitFor(() => expect(within(dialog).getByText('Gemini')).toBeInTheDocument());
        await user.click(within(dialog).getByLabelText('Select Gemini'));

        expect(within(dialog).getByText('2 items selected')).toBeInTheDocument();
        await user.click(within(dialog).getByRole('button', { name: /^Import$/i }));

        await waitFor(() => expect(createEntitySpy).toHaveBeenCalledTimes(2));

        const callsByEntityId = new Map<string, unknown>();
        for (const [, req] of createEntitySpy.mock.calls) {
            const r = req as { entityId: string };
            callsByEntityId.set(r.entityId, req);
        }
        // Catalog convention: mcp.{extensions.entityId}, model.{provider}.{queryName}.
        expect(callsByEntityId.has('mcp.flight-status')).toBe(true);
        expect(callsByEntityId.has('model.google.gemini')).toBe(true);

        for (const [, req] of createEntitySpy.mock.calls) {
            const r = req as {
                kind: string;
                source: string;
                attributes: Record<string, unknown>;
                parents: readonly string[];
            };
            expect(r.kind).toBe('RESOURCE');
            expect(r.source).toBe('gravitee-catalog');
            expect(r.parents).toEqual([]);
            expect(r.attributes._displayName).toBeTypeOf('string');
            expect(r.attributes._catalogId).toBeTypeOf('string');
            expect(r.attributes._importedAt).toBeTypeOf('string');
            // _kind intentionally omitted — the entityId prefix carries the same information.
            expect(r.attributes._kind).toBeUndefined();
        }

        await waitFor(() => expect(onImported).toHaveBeenCalledTimes(1));
        expect(toastSuccessSpy).toHaveBeenCalledWith('Imported 2 entities');
        await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
    });

    it('uses the agent canonical prefix for A2A catalog agents (not a2a)', async () => {
        seedCatalog({ agents: [makeAgent('a1', 'Researcher')] });
        createEntitySpy.mockResolvedValue({});
        renderDialog();
        await waitForList();

        const dialog = getDialog();
        const user = userEvent.setup();
        await user.click(within(dialog).getByRole('tab', { name: /^Agents/i }));
        await waitFor(() => expect(within(dialog).getByText('Researcher')).toBeInTheDocument());
        await user.click(within(dialog).getByLabelText('Select Researcher'));
        await user.click(within(dialog).getByRole('button', { name: /^Import$/i }));

        await waitFor(() => expect(createEntitySpy).toHaveBeenCalledTimes(1));
        const [, req] = createEntitySpy.mock.calls[0];
        expect((req as { entityId: string }).entityId).toBe('agent.researcher');
    });

    it('reports partial failure via toast.error and keeps the dialog open', async () => {
        seedCatalog({
            mcpServers: [makeMcp('s1', 'flight-status'), makeMcp('s2', 'hotel-booking')],
        });
        createEntitySpy.mockResolvedValueOnce({}).mockRejectedValueOnce(new Error('boom'));
        const onOpenChange = vi.fn();
        renderDialog({ onOpenChange });
        await waitForList();

        const dialog = getDialog();
        const user = userEvent.setup();
        await waitFor(() => expect(within(dialog).getByText('flight-status server')).toBeInTheDocument());
        await user.click(within(dialog).getByRole('button', { name: /Select visible/i }));
        await user.click(within(dialog).getByRole('button', { name: /^Import$/i }));

        await waitFor(() => expect(createEntitySpy).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(toastSuccessSpy).toHaveBeenCalledWith('Imported 1 entity'));
        expect(toastErrorSpy).toHaveBeenCalledWith(expect.stringContaining('Failed to import 1'));
        // partial failure ⇒ stay open so user sees the error
        expect(onOpenChange).not.toHaveBeenCalledWith(false);
    });

    it('shows an empty-state when a tab has no catalog items', async () => {
        seedCatalog({});
        renderDialog();
        await waitForList();

        const dialog = getDialog();
        await waitFor(() => expect(within(dialog).getByText(/No items in this catalog category yet/)).toBeInTheDocument());
    });

    describe('entityId convention (mirrors AIM catalog fixtures)', () => {
        async function importSingleAndCaptureEntityId(): Promise<string> {
            createEntitySpy.mockResolvedValue({});
            renderDialog();
            await waitForList();
            const dialog = getDialog();
            const user = userEvent.setup();
            // Caller seeded exactly one item across all kinds — find it and select it.
            const importBtn = within(dialog).getByRole('button', { name: /^Import$/i });
            // Walk through the tabs until something is visible to select.
            for (const tabName of ['MCP Servers', 'AI Models', 'Agents']) {
                await user.click(within(dialog).getByRole('tab', { name: new RegExp(tabName, 'i') }));
                const selectables = within(dialog).queryAllByRole('checkbox');
                const enabled = selectables.find(cb => !(cb as HTMLInputElement).disabled);
                if (enabled) {
                    await user.click(enabled);
                    break;
                }
            }
            await user.click(importBtn);
            await waitFor(() => expect(createEntitySpy).toHaveBeenCalledTimes(1));
            return (createEntitySpy.mock.calls[0][1] as { entityId: string }).entityId;
        }

        it('model with provider → `model.{provider}.{queryName}` (matches model.openai.gpt-4o)', async () => {
            seedCatalog({ models: [makeModel('m1', 'GPT-4o', { provider: 'openai' })] });
            const entityId = await importSingleAndCaptureEntityId();
            expect(entityId).toBe('model.openai.gpt-4o');
        });

        it('model without explicit provider falls back to sourceKind (catalog uses `llm.provider.openai`)', async () => {
            seedCatalog({ models: [makeModel('m1', 'GPT-4o', { sourceKind: 'llm.provider.openai' })] });
            const entityId = await importSingleAndCaptureEntityId();
            expect(entityId).toBe('model.openai.gpt-4o');
        });

        it('model with neither provider nor sourceKind falls back to single-segment slug', async () => {
            seedCatalog({ models: [makeModel('m1', 'GPT-4o')] });
            const entityId = await importSingleAndCaptureEntityId();
            // No provider info → user gets the legacy short form; collision risk surfaces via Imported badge.
            expect(entityId).toBe('model.gpt-4o');
        });

        it('mcp-server uses extensions.entityId when present (matches mcp.filesystem)', async () => {
            seedCatalog({ mcpServers: [makeMcp('s1', 'filesystem-server', { canonicalEntityId: 'filesystem' })] });
            const entityId = await importSingleAndCaptureEntityId();
            expect(entityId).toBe('mcp.filesystem');
        });

        it('mcp-server without extensions.entityId falls back to serverInfo.name', async () => {
            seedCatalog({ mcpServers: [makeMcp('s1', 'filesystem')] });
            const entityId = await importSingleAndCaptureEntityId();
            expect(entityId).toBe('mcp.filesystem');
        });

        it('agent uses single-segment `agent.{name}` (matches agent.code-reviewer)', async () => {
            seedCatalog({ agents: [makeAgent('a1', 'Code Reviewer')] });
            const entityId = await importSingleAndCaptureEntityId();
            expect(entityId).toBe('agent.code-reviewer');
        });

        it('prefers the server-derived `item.entityId` over re-deriving from definition for models', async () => {
            // aim ≥ 1.0.0-alpha.71 persists `entityId` on the CatalogItem; we keep the
            // legacy {definition.queryName} fallback for older rows.
            seedCatalog({ models: [makeModel('m1', 'Display Name', { provider: 'openai', entityId: 'gpt-4o-mini' })] });
            const entityId = await importSingleAndCaptureEntityId();
            expect(entityId).toBe('model.openai.gpt-4o-mini');
        });

        it('prefers the server-derived `item.entityId` over re-deriving for MCP servers', async () => {
            seedCatalog({ mcpServers: [makeMcp('s1', 'wrong-name', { entityId: 'github' })] });
            const entityId = await importSingleAndCaptureEntityId();
            expect(entityId).toBe('mcp.github');
        });

        it('prefers the server-derived `item.entityId` over re-deriving for agents', async () => {
            seedCatalog({ agents: [makeAgent('a1', 'Wrong Name', { entityId: 'planner' })] });
            const entityId = await importSingleAndCaptureEntityId();
            expect(entityId).toBe('agent.planner');
        });

        it('renders a truncation banner when the catalog reports more items than the hook fetched', async () => {
            // Pretend the catalog has 12k models, but page 2 returns empty so the loop stops early.
            listItemsSpy.mockImplementation((_env: string, kind: CatalogItemKind, page: number) => {
                if (kind === 'model') {
                    if (page === 1) {
                        return Promise.resolve({
                            data: [makeModel('m1', 'GPT-4o', { provider: 'openai' })],
                            page: 1,
                            perPage: 500,
                            total: 12_000,
                        });
                    }
                    return Promise.resolve({ data: [], page, perPage: 500, total: 12_000 });
                }
                return Promise.resolve({ data: [], page: 1, perPage: 500, total: 0 });
            });
            renderDialog();
            await waitFor(() => expect(listItemsSpy).toHaveBeenCalled());

            const dialog = getDialog();
            const user = userEvent.setup();
            await user.click(within(dialog).getByRole('tab', { name: /AI Models/i }));

            await waitFor(() => expect(within(dialog).getByRole('status')).toHaveTextContent(/Showing the first 1 of 12000/));
        });

        it('two providers hosting the same queryName produce distinct entityIds (collision-safe)', async () => {
            seedCatalog({
                models: [
                    makeModel('m1', 'Claude Sonnet 4', { provider: 'anthropic' }),
                    makeModel('m2', 'Claude Sonnet 4', { provider: 'bedrock' }),
                ],
            });
            createEntitySpy.mockResolvedValue({});
            renderDialog();
            await waitForList();

            const dialog = getDialog();
            const user = userEvent.setup();
            await user.click(within(dialog).getByRole('tab', { name: /AI Models/i }));
            await waitFor(() => expect(within(dialog).getAllByText('Claude Sonnet 4').length).toBe(2));
            await user.click(within(dialog).getByRole('button', { name: /Select visible/i }));
            await user.click(within(dialog).getByRole('button', { name: /^Import$/i }));

            await waitFor(() => expect(createEntitySpy).toHaveBeenCalledTimes(2));
            const ids = new Set(createEntitySpy.mock.calls.map(c => (c[1] as { entityId: string }).entityId));
            expect(ids).toEqual(new Set(['model.anthropic.claude-sonnet-4', 'model.bedrock.claude-sonnet-4']));
        });
    });

    describe('idempotency — items already imported', () => {
        it('queries authz for catalog-sourced resources to detect duplicates', async () => {
            seedCatalog({ mcpServers: [makeMcp('s1', 'flight-status')] });
            renderDialog();

            await waitFor(() => expect(listEntitiesSpy).toHaveBeenCalled());
            expect(listEntitiesSpy).toHaveBeenCalledWith(
                'DEFAULT',
                expect.objectContaining({ kind: 'RESOURCE', source: 'gravitee-catalog' }),
            );
        });

        it('marks already-imported items with an Imported badge and a disabled checkbox', async () => {
            seedCatalog({ mcpServers: [makeMcp('s1', 'flight-status')] });
            seedImported([{ id: 'e1', uid: 'mcp.flight-status', catalogId: 's1' }]);
            renderDialog();
            await waitForList();

            const dialog = getDialog();
            await waitFor(() => expect(within(dialog).getByText('flight-status server')).toBeInTheDocument());
            await waitFor(() =>
                expect(within(dialog).getByLabelText('flight-status server already imported')).toBeDisabled(),
            );
            expect(within(dialog).getByText('Imported')).toBeInTheDocument();
        });

        it('toggleSelect is a no-op for already-imported items', async () => {
            seedCatalog({ mcpServers: [makeMcp('s1', 'flight-status')] });
            seedImported([{ id: 'e1', uid: 'mcp.flight-status', catalogId: 's1' }]);
            renderDialog();
            await waitForList();

            const dialog = getDialog();
            await waitFor(() =>
                expect(within(dialog).getByLabelText('flight-status server already imported')).toBeDisabled(),
            );
            const user = userEvent.setup();
            // userEvent skips disabled controls, so just confirm the counter never changes
            // and Import stays disabled — we don't want a side door via keyboard either.
            await user.click(within(dialog).getByLabelText('flight-status server already imported'));
            expect(within(dialog).getByText('No items selected')).toBeInTheDocument();
            expect(within(dialog).getByRole('button', { name: /^Import$/i })).toBeDisabled();
        });

        it('"Select visible" skips already-imported items', async () => {
            seedCatalog({
                mcpServers: [makeMcp('s1', 'flight-status'), makeMcp('s2', 'hotel-booking')],
            });
            seedImported([{ id: 'e1', uid: 'mcp.flight-status', catalogId: 's1' }]);
            renderDialog();
            await waitForList();

            const dialog = getDialog();
            await waitFor(() =>
                expect(within(dialog).getByLabelText('flight-status server already imported')).toBeDisabled(),
            );
            const user = userEvent.setup();
            await user.click(within(dialog).getByRole('button', { name: /Select visible/i }));

            // Only the not-yet-imported item gets selected.
            expect(within(dialog).getByText('1 item selected')).toBeInTheDocument();
        });

        it('a successful import invalidates the entities cache so badges refresh', async () => {
            seedCatalog({ mcpServers: [makeMcp('s1', 'flight-status')] });
            createEntitySpy.mockResolvedValue({});
            const onImported = vi.fn();
            renderDialog({ onImported });
            await waitForList();

            const dialog = getDialog();
            const user = userEvent.setup();
            await waitFor(() => expect(within(dialog).getByText('flight-status server')).toBeInTheDocument());
            await user.click(within(dialog).getByLabelText('Select flight-status server'));
            await user.click(within(dialog).getByRole('button', { name: /^Import$/i }));

            // onImported is the parent's invalidation hook; firing it is the
            // observable signal that downstream caches will be refreshed.
            await waitFor(() => expect(onImported).toHaveBeenCalledTimes(1));
        });
    });
});
