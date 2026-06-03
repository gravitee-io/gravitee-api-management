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
import type { PolicyResponse } from '../../../../shared/api/authz-api.types';
import type { EntityInstance } from '../../../../shared/entity.types';
import { EntityPoliciesTab } from '../EntityPoliciesTab';

const server: EntityInstance = { uid: { type: 'MCPServer', id: 'flight-status-mcp' }, attrs: {}, parents: [], source: 'gravitee-catalog' };

function policy(over: Partial<PolicyResponse>): PolicyResponse {
    return {
        id: 'x',
        environmentId: 'env',
        name: 'p',
        description: null,
        policyText: '',
        type: 'MCP',
        target: { id: 'mcp.flight-status-mcp', label: 'flight-status-mcp' },
        status: 'DRAFT',
        createdAt: '2026-01-01T00:00:00.000Z',
        updatedAt: '2026-01-01T00:00:00.000Z',
        ...over,
    };
}

describe('EntityPoliciesTab', () => {
    it('lists policies that reference the entity with their status', () => {
        render(
            <EntityPoliciesTab
                entity={server}
                policies={[policy({ id: '1', name: 'allow-invoke', status: 'DEPLOYED' }), policy({ id: '2', name: 'rate-limit' })]}
            />,
        );
        expect(screen.getByText('allow-invoke')).toBeInTheDocument();
        expect(screen.getByText('DEPLOYED')).toBeInTheDocument();
        expect(screen.getByText('rate-limit')).toBeInTheDocument();
    });

    it('ignores policies that target other entities', () => {
        render(
            <EntityPoliciesTab
                entity={server}
                policies={[policy({ name: 'elsewhere', target: { id: 'mcp.payments-mcp', label: 'payments-mcp' } })]}
            />,
        );
        expect(screen.getByText('No policies reference this entity.')).toBeInTheDocument();
    });

    it('shows an empty state when none match', () => {
        render(<EntityPoliciesTab entity={server} policies={[]} />);
        expect(screen.getByText('No policies reference this entity.')).toBeInTheDocument();
    });
});
