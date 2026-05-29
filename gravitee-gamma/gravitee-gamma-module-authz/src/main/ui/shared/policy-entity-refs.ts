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
import type { PolicyResponse } from './api/authz-api.types';
import { formatEntityUid } from './entity-adapter';
import { canonicalKindOf, deriveServiceType } from './entity-kind-registry';
import type { EntityInstance } from './entity.types';

export type PolicyClause = 'principal' | 'action' | 'resource';

export interface EntityRefInPolicy {
    readonly type: string;
    readonly id: string;
    readonly clause: PolicyClause;
}

export interface PolicyRef {
    readonly policy: PolicyResponse;
    readonly clauses: PolicyClause[];
}

const CLAUSE_KEYWORDS: PolicyClause[] = ['principal', 'action', 'resource'];

/**
 * Scan the policy text for every `Type::"id"` token, classifying each by the
 * nearest preceding clause keyword (`principal`, `action`, `resource`).
 *
 * Lenient: malformed input (unbalanced braces, garbage) returns whatever
 * tokens could be matched — never throws.
 */
export function extractEntityRefsFromPolicyText(text: string): EntityRefInPolicy[] {
    if (!text || typeof text !== 'string') return [];

    const refs: EntityRefInPolicy[] = [];

    // Keyword must NOT be immediately followed by `::` — otherwise it's the
    // type-prefix of a Type::"id" token (e.g. `action::"read"`), not a clause.
    const tokenRe = /\b(principal|action|resource)\b(?!::)|([A-Za-z_][A-Za-z0-9_]*)::"([^"]*)"/g;

    let currentClause: PolicyClause | null = null;
    let m: RegExpExecArray | null;
    try {
        while ((m = tokenRe.exec(text)) !== null) {
            const [, keyword, type, id] = m;
            if (keyword) {
                currentClause = keyword as PolicyClause;
                continue;
            }
            if (type !== undefined && id !== undefined) {
                if (currentClause && CLAUSE_KEYWORDS.includes(currentClause)) {
                    refs.push({ type, id, clause: currentClause });
                }
            }
        }
    } catch {
        return refs;
    }

    return refs;
}

/**
 * Derive the service entity a policy should bind to from its text. Scans for the
 * first `resource` clause token whose kind maps to a service type
 * (mcp/llm/api/agent/event) and reduces it to the owning service entity — the
 * first two dotted segments, e.g. `mcp.bookings.cancel-booking` → `mcp.bookings`.
 *
 * This is what lands a policy on its service page: the page filters by the
 * entityId prefix (see `deriveServiceType`). Returns null when the policy only
 * references generic/custom resources (or none), leaving it unbound → Custom.
 */
export function deriveTargetEntityId(policyText: string | null | undefined): string | null {
    if (!policyText) return null;
    for (const ref of extractEntityRefsFromPolicyText(policyText)) {
        if (ref.clause !== 'resource') continue;
        const entityId = `${canonicalKindOf(ref.type)}.${ref.id}`;
        if (deriveServiceType(entityId) === 'CUSTOM') continue;
        // entityId always carries the kind prefix + a literal dot, so split
        // yields at least [kind, server] — reduce to the owning service entity.
        const [kind, server] = entityId.split('.');
        return `${kind}.${server}`;
    }
    return null;
}

function entityKey(type: string, id: string): string {
    return `${type}::${id}`;
}

/**
 * For each entity, find the policies whose GAPL text references it in any
 * clause. Returns a map keyed by `Type::id` so the EntitiesPage can do an
 * O(1) lookup per row.
 *
 * - Policies with malformed `policyText` simply contribute no refs.
 * - Multiple references to the same entity in the same policy are folded
 *   into a single `PolicyRef` whose `clauses` array lists each distinct
 *   clause kind.
 * - Entities with no matching policy still appear in the map with an empty
 *   array, so callers can distinguish "no matches" from "not yet computed".
 */
export function buildPolicyEntityRefs(entities: readonly EntityInstance[], policies: readonly PolicyResponse[]): Map<string, PolicyRef[]> {
    const result = new Map<string, PolicyRef[]>();
    for (const e of entities) {
        result.set(entityKey(e.uid.type, e.uid.id), []);
    }
    if (entities.length === 0 || policies.length === 0) return result;

    const policyRefs: Array<{ policy: PolicyResponse; refs: EntityRefInPolicy[] }> = policies.map(p => ({
        policy: p,
        refs: extractEntityRefsFromPolicyText(p.policyText ?? ''),
    }));

    for (const e of entities) {
        const key = entityKey(e.uid.type, e.uid.id);
        const entityUid = formatEntityUid(e.uid);
        const matches: PolicyRef[] = [];

        for (const { policy, refs } of policyRefs) {
            const clauseSet = new Set<PolicyClause>();
            for (const r of refs) {
                if (r.type === e.uid.type && r.id === e.uid.id) {
                    clauseSet.add(r.clause);
                }
            }
            // Policy.target.id is the explicit target attachment (e.g. policy created via
            // "+ Create Policy for LLM" → target.id="llm.gog"). Treat it as a resource-clause
            // reference even when the policyText body uses the unbound `resource` keyword.
            if (policy.target?.id === entityUid) {
                clauseSet.add('resource');
            }
            if (clauseSet.size > 0) {
                matches.push({ policy, clauses: Array.from(clauseSet) });
            }
        }

        result.set(key, matches);
    }

    return result;
}
