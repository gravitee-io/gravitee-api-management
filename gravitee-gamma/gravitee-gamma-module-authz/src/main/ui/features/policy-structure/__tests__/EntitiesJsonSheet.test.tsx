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
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { EntityInstance } from '../../../shared/entity.types';
import { EntitiesJsonSheet } from '../EntitiesJsonSheet';

const entities: EntityInstance[] = [
    { uid: { type: 'User', id: 'alice' }, attrs: { dept: 'eng' }, parents: [{ type: 'Group', id: 'devs' }], source: 'local' },
    { uid: { type: 'Mcp', id: 'flight' }, attrs: {}, parents: [], source: 'gravitee-catalog' },
];

const writeText = vi.fn().mockResolvedValue(undefined);

function stubClipboard(value: unknown) {
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value });
}

function parsedJson() {
    return JSON.parse(screen.getByTestId('entities-json').textContent ?? '');
}

beforeEach(() => {
    writeText.mockClear();
    stubClipboard({ writeText });
});

afterEach(() => {
    vi.restoreAllMocks();
});

describe('EntitiesJsonSheet', () => {
    it('renders the canonical GAPL shape of every entity', () => {
        render(<EntitiesJsonSheet entities={entities} open onOpenChange={() => {}} />);
        const json = screen.getByTestId('entities-json').textContent ?? '';
        expect(json).toContain('"id": "user.alice"');
        expect(json).toContain('"group.devs"');
        expect(json).toContain('"id": "mcp.flight"');
    });

    it('renders valid, parseable JSON that mirrors the entities array', () => {
        render(<EntitiesJsonSheet entities={entities} open onOpenChange={() => {}} />);
        const parsed = parsedJson();
        expect(parsed).toHaveLength(2);
        expect(parsed[0]).toEqual({ uid: { type: 'User', id: 'user.alice' }, attrs: { dept: 'eng' }, parents: ['group.devs'] });
        expect(parsed[1]).toEqual({ uid: { type: 'Mcp', id: 'mcp.flight' }, attrs: {}, parents: [] });
    });

    it('reports the entity count in the description', () => {
        render(<EntitiesJsonSheet entities={entities} open onOpenChange={() => {}} />);
        expect(screen.getByText(/all 2 principals and resources/i)).toBeInTheDocument();
    });

    it('renders an empty array and a zero count when there are no entities', () => {
        render(<EntitiesJsonSheet entities={[]} open onOpenChange={() => {}} />);
        expect(parsedJson()).toEqual([]);
        expect(screen.getByText(/all 0 principals and resources/i)).toBeInTheDocument();
    });

    it('does not render any content while closed', () => {
        render(<EntitiesJsonSheet entities={entities} open={false} onOpenChange={() => {}} />);
        expect(screen.queryByTestId('entities-json')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Copy JSON/i })).not.toBeInTheDocument();
    });

    it('copies the JSON to the clipboard and shows a confirmation', async () => {
        render(<EntitiesJsonSheet entities={entities} open onOpenChange={() => {}} />);
        fireEvent.click(screen.getByRole('button', { name: /Copy JSON/i }));
        await waitFor(() => expect(writeText).toHaveBeenCalledWith(expect.stringContaining('"id": "user.alice"')));
        expect(await screen.findByRole('button', { name: /Copied/i })).toBeInTheDocument();
    });

    it('copies the exact same JSON that is rendered', async () => {
        render(<EntitiesJsonSheet entities={entities} open onOpenChange={() => {}} />);
        const rendered = screen.getByTestId('entities-json').textContent;
        fireEvent.click(screen.getByRole('button', { name: /Copy JSON/i }));
        await waitFor(() => expect(writeText).toHaveBeenCalledWith(rendered));
    });

    it('does not throw when the clipboard API is unavailable', () => {
        stubClipboard(undefined);
        render(<EntitiesJsonSheet entities={entities} open onOpenChange={() => {}} />);
        fireEvent.click(screen.getByRole('button', { name: /Copy JSON/i }));
        expect(screen.getByRole('button', { name: /Copy JSON/i })).toBeInTheDocument();
    });

    it('downloads the JSON as a file named entities.json', () => {
        const createObjectURL = vi.fn(() => 'blob:entities');
        const revokeObjectURL = vi.fn();
        Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: createObjectURL });
        Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: revokeObjectURL });

        const realCreate = document.createElement.bind(document);
        let anchor: HTMLAnchorElement | undefined;
        vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
            const el = realCreate(tag);
            if (tag === 'a') {
                anchor = el as HTMLAnchorElement;
                vi.spyOn(anchor, 'click').mockImplementation(() => {});
            }
            return el;
        });

        render(<EntitiesJsonSheet entities={entities} open onOpenChange={() => {}} />);
        fireEvent.click(screen.getByRole('button', { name: /Download/i }));

        expect(createObjectURL).toHaveBeenCalledTimes(1);
        expect(createObjectURL.mock.calls[0][0]).toBeInstanceOf(Blob);
        expect(anchor?.getAttribute('download')).toBe('entities.json');
        expect(anchor?.click).toHaveBeenCalledTimes(1);
    });

    it('does not download when createObjectURL is unsupported', () => {
        Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: undefined });
        const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

        render(<EntitiesJsonSheet entities={entities} open onOpenChange={() => {}} />);
        fireEvent.click(screen.getByRole('button', { name: /Download/i }));

        expect(click).not.toHaveBeenCalled();
    });

    it('calls onOpenChange(false) when dismissed with Escape', () => {
        const onOpenChange = vi.fn();
        render(<EntitiesJsonSheet entities={entities} open onOpenChange={onOpenChange} />);
        fireEvent.keyDown(document, { key: 'Escape' });
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });
});
