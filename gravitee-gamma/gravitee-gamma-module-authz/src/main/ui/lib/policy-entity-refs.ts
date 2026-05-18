/**
 * Policy ↔ Entity cross-reference utilities.
 *
 * Pure, side-effect-free helpers that scan the GAPL `policyText` of each
 * policy for `Type::"id"` references and group them by the clause they
 * appear in (principal / action / resource). Used by the Entities table
 * to show "which policies reference this entity?" without any backend
 * cross-reference endpoint.
 *
 * The scanner is intentionally lenient: it does NOT validate the policy
 * grammar and never throws on malformed input. If the text cannot be
 * parsed cleanly we simply emit no refs for that policy.
 */

import type { EntityInstance } from '../app/features/policy-structure/entity-types';
import type { PolicyResponse } from './api/authz-api.types';

export type PolicyClause = 'principal' | 'action' | 'resource';

export interface EntityRefInPolicy {
    /** Entity type, e.g. `User`, `Endpoint`. */
    readonly type: string;
    /** Entity id, e.g. `alice`. */
    readonly id: string;
    /** Which top-level GAPL clause the reference appears in. */
    readonly clause: PolicyClause;
}

export interface PolicyRef {
    readonly policy: PolicyResponse;
    /** Distinct clauses (principal/action/resource) where the entity appears. */
    readonly clauses: PolicyClause[];
}

// ---------------------------------------------------------------------------
// Tokeniser — extract `Type::"id"` references with their surrounding clause.
// ---------------------------------------------------------------------------

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

    // Match either a clause keyword or a Type::"id" token, in order.
    // We walk through the text linearly and remember the last seen clause.
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
                // If there's no clause context yet, fall back to 'resource' — but
                // tests assume a well-formed policy where principal/action/resource
                // always precedes refs. If not, default to 'resource' is harmless
                // because no entity-side filter looks for clause-less refs.
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

// ---------------------------------------------------------------------------
// Build map: Type::id -> [PolicyRef, …]
// ---------------------------------------------------------------------------

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

    // Pre-extract refs per policy so we don't re-scan the text per entity.
    const policyRefs: Array<{ policy: PolicyResponse; refs: EntityRefInPolicy[] }> = policies.map(p => ({
        policy: p,
        refs: extractEntityRefsFromPolicyText(p.policyText ?? ''),
    }));

    for (const e of entities) {
        const key = entityKey(e.uid.type, e.uid.id);
        const matches: PolicyRef[] = [];

        for (const { policy, refs } of policyRefs) {
            const clauseSet = new Set<PolicyClause>();
            for (const r of refs) {
                if (r.type === e.uid.type && r.id === e.uid.id) {
                    clauseSet.add(r.clause);
                }
            }
            if (clauseSet.size > 0) {
                matches.push({ policy, clauses: Array.from(clauseSet) });
            }
        }

        result.set(key, matches);
    }

    return result;
}
