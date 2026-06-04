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
import type { EntityInstance } from '../../../../shared/entity.types';
import { EntityOverviewTab } from '../EntityOverviewTab';

const entity: EntityInstance = {
    uid: { type: 'MCPServer', id: 'flight-status-mcp' },
    attrs: { name: 'Flight Status MCP', port: 8080, secure: true },
    parents: [],
    source: 'gravitee-catalog',
    importedAt: '2026-04-14T11:12:00.000Z',
};

describe('EntityOverviewTab', () => {
    it('renders the attribute table with names, inferred types, and values', () => {
        render(<EntityOverviewTab entity={entity} />);
        expect(screen.getByText('name')).toBeInTheDocument();
        expect(screen.getByText('Flight Status MCP')).toBeInTheDocument();
        expect(screen.getByText('Integer')).toBeInTheDocument(); // port
        expect(screen.getByText('Boolean')).toBeInTheDocument(); // secure
    });

    it('renders the provenance source and imported-at', () => {
        render(<EntityOverviewTab entity={entity} />);
        expect(screen.getByText('Gravitee Catalog')).toBeInTheDocument();
        expect(screen.getByText('Imported at')).toBeInTheDocument();
        expect(screen.getByText('2026-04-14T11:12:00.000Z')).toBeInTheDocument();
    });

    it('labels a local source as "Local"', () => {
        render(<EntityOverviewTab entity={{ ...entity, source: 'local', importedAt: undefined }} />);
        expect(screen.getByText('Local')).toBeInTheDocument();
        expect(screen.queryByText('Imported at')).not.toBeInTheDocument();
    });

    it('shows an empty state when there are no attributes', () => {
        render(<EntityOverviewTab entity={{ ...entity, attrs: {} }} />);
        expect(screen.getByText('No attributes.')).toBeInTheDocument();
    });
});
