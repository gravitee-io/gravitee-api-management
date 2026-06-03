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
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { formatEntityUid } from '../../../../shared/entity-adapter';
import type { EntityInstance } from '../../../../shared/entity.types';
import { EntityRelationshipsTab } from '../EntityRelationshipsTab';

const server: EntityInstance = { uid: { type: 'MCPServer', id: 'flight-status-mcp' }, attrs: {}, parents: [], source: 'gravitee-catalog' };
const tool: EntityInstance = {
    uid: { type: 'MCPTool', id: 'get-flight' },
    attrs: {},
    parents: [{ type: 'MCPServer', id: 'flight-status-mcp' }],
    source: 'gravitee-catalog',
};
const childWithParent: EntityInstance = {
    uid: { type: 'User', id: 'bob' },
    attrs: {},
    parents: [{ type: 'Group', id: 'devs' }],
    source: 'local',
};
const group: EntityInstance = { uid: { type: 'Group', id: 'devs' }, attrs: {}, parents: [], source: 'local' };

describe('EntityRelationshipsTab', () => {
    it('lists referenced-by children and a contains chip', () => {
        render(<EntityRelationshipsTab entity={server} allEntities={[server, tool]} />);
        expect(screen.getByText(formatEntityUid(tool.uid))).toBeInTheDocument();
        expect(screen.getByText(/contains 1 MCPTool/)).toBeInTheDocument();
        expect(screen.getByText('Referenced by')).toBeInTheDocument();
    });

    it('lists parents', () => {
        render(<EntityRelationshipsTab entity={childWithParent} allEntities={[group, childWithParent]} />);
        expect(screen.getByText('Parents')).toBeInTheDocument();
        expect(screen.getByText(formatEntityUid({ type: 'Group', id: 'devs' }))).toBeInTheDocument();
    });

    it('shows an empty state when isolated', () => {
        render(<EntityRelationshipsTab entity={server} allEntities={[server]} />);
        expect(screen.getByText('No relationships.')).toBeInTheDocument();
    });
});
