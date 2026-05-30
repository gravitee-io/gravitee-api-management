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
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { EntityInstance } from '../../../../shared/entity.types';
import { EntityGaplShapeTab } from '../EntityGaplShapeTab';

const entity: EntityInstance = {
    uid: { type: 'User', id: 'alice' },
    attrs: { dept: 'eng' },
    parents: [{ type: 'Group', id: 'devs' }],
    source: 'local',
};

const writeText = vi.fn().mockResolvedValue(undefined);

function stubClipboard(value: unknown) {
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value });
}

beforeEach(() => {
    writeText.mockClear();
    stubClipboard({ writeText });
});

describe('EntityGaplShapeTab', () => {
    it('renders the canonical JSON (uid + attrs + parents)', () => {
        render(<EntityGaplShapeTab entity={entity} />);
        const json = screen.getByTestId('gapl-json').textContent ?? '';
        expect(json).toContain('"type": "User"');
        expect(json).toContain('"id": "user.alice"');
        expect(json).toContain('"dept": "eng"');
        expect(json).toContain('"group.devs"');
    });

    it('copies the JSON to the clipboard and shows a confirmation', async () => {
        render(<EntityGaplShapeTab entity={entity} />);
        fireEvent.click(screen.getByRole('button', { name: /Copy JSON/i }));
        await waitFor(() => expect(writeText).toHaveBeenCalledWith(expect.stringContaining('"id": "user.alice"')));
        expect(await screen.findByRole('button', { name: /Copied/i })).toBeInTheDocument();
    });

    it('does not throw when the clipboard API is unavailable', () => {
        stubClipboard(undefined);
        render(<EntityGaplShapeTab entity={entity} />);
        fireEvent.click(screen.getByRole('button', { name: /Copy JSON/i }));
        expect(screen.getByRole('button', { name: /Copy JSON/i })).toBeInTheDocument();
    });
});
