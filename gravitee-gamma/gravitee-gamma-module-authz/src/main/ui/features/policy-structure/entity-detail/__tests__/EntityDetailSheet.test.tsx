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
import userEvent from '@testing-library/user-event';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import type { PolicyResponse } from '../../../../shared/api/authz-api.types';
import type { EntityInstance } from '../../../../shared/entity.types';
import { EntityDetailSheet } from '../EntityDetailSheet';

beforeAll(() => {
    if (!Element.prototype.scrollIntoView) Element.prototype.scrollIntoView = () => undefined;
});

beforeEach(() => {
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: vi.fn().mockResolvedValue(undefined) } });
});

const local: EntityInstance = {
    uid: { type: 'User', id: 'alice' },
    displayName: 'Alice',
    attrs: { dept: 'eng' },
    parents: [],
    source: 'local',
};
const catalog: EntityInstance = {
    uid: { type: 'MCPServer', id: 'flight' },
    displayName: 'Flight',
    attrs: {},
    parents: [],
    source: 'gravitee-catalog',
};
const policies: PolicyResponse[] = [{ id: '1', name: 'allow', kind: 'RESOURCE', entityId: 'user.alice', policyText: '', status: 'DRAFT' }];

function renderSheet(entity: EntityInstance, onEdit = vi.fn()) {
    render(<EntityDetailSheet entity={entity} allEntities={[entity]} policies={policies} open onOpenChange={vi.fn()} onEdit={onEdit} />);
    return { onEdit };
}

describe('EntityDetailSheet', () => {
    it('shows the title, uid, and the count chips', () => {
        renderSheet(local);
        expect(screen.getByText('Alice')).toBeInTheDocument();
        expect(screen.getByText('user.alice')).toBeInTheDocument();
        expect(screen.getByText('1 attrs')).toBeInTheDocument();
        expect(screen.getByText('1 policies')).toBeInTheDocument();
    });

    it('opens on the Overview tab and shows the attribute', () => {
        renderSheet(local);
        expect(screen.getByText('dept')).toBeInTheDocument();
    });

    it('switches to the GAPL shape tab', async () => {
        const user = userEvent.setup();
        renderSheet(local);
        await user.click(screen.getByRole('tab', { name: /GAPL shape/i }));
        expect(screen.getByTestId('gapl-json').textContent).toContain('"id": "user.alice"');
    });

    it('offers Edit for a local entity and calls onEdit', async () => {
        const user = userEvent.setup();
        const { onEdit } = renderSheet(local);
        await user.click(screen.getByRole('button', { name: /^Edit$/i }));
        expect(onEdit).toHaveBeenCalledWith(local);
    });

    it('hides Edit for a catalog entity', () => {
        renderSheet(catalog);
        expect(screen.queryByRole('button', { name: /^Edit$/i })).not.toBeInTheDocument();
    });

    it('copies the uid from the header', async () => {
        const writeText = vi.fn().mockResolvedValue(undefined);
        Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } });
        renderSheet(local);
        fireEvent.click(screen.getByRole('button', { name: 'Copy user.alice' }));
        await waitFor(() => expect(writeText).toHaveBeenCalledWith('user.alice'));
    });
});
