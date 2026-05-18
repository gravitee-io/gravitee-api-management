/**
 * Entity schema types ported from prototype schema-data.ts.
 * Only schema metadata (categories, entity definitions, types) — no mock instances.
 */

// ---------- Categories --------------------------------------------------------

export type EntityCategoryId = 'principal' | 'mcp' | 'api' | 'agent' | 'llm' | 'event' | 'custom';

export interface EntityCategory {
    id: EntityCategoryId;
    label: string;
    tone: string;
}

export const CATEGORIES: EntityCategory[] = [
    { id: 'principal', label: 'Principals', tone: 'text-blue-600 dark:text-blue-400' },
    { id: 'mcp', label: 'MCP', tone: 'text-teal-600 dark:text-teal-400' },
    { id: 'api', label: 'APIs', tone: 'text-indigo-600 dark:text-indigo-400' },
    { id: 'agent', label: 'Agents', tone: 'text-orange-600 dark:text-orange-400' },
    { id: 'llm', label: 'LLMs', tone: 'text-fuchsia-600 dark:text-fuchsia-400' },
    { id: 'event', label: 'Events', tone: 'text-cyan-600 dark:text-cyan-400' },
    { id: 'custom', label: 'Custom', tone: 'text-slate-600 dark:text-slate-400' },
];

export function getCategory(id: EntityCategoryId): EntityCategory | undefined {
    return CATEGORIES.find(c => c.id === id);
}

// ---------- Entity types ------------------------------------------------------
//
// Only the *category* of each known entity name lives here — it controls UI
// presentation (sidebar grouping, icon, accent color). The full schema (which
// attributes exist on each entity, which entities can be parents of others) is
// authoritative on the server: load it through `useEntitySchema(envId)` which
// parses the GAPL `schemaText` returned by `GET .../authz/schema`.

const ENTITY_CATEGORIES: Readonly<Record<string, EntityCategoryId>> = {
    // ---- Principals ----
    User: 'principal',
    Group: 'principal',
    ServiceAccount: 'principal',
    AgentIdentity: 'principal',

    // ---- MCP ----
    MCPServer: 'mcp',
    MCPTool: 'mcp',
    MCPPrompt: 'mcp',
    MCPResource: 'mcp',

    // ---- APIs ----
    API: 'api',
    Endpoint: 'api',
    DataField: 'api',

    // ---- Agents ----
    Agent: 'agent',
    AgentSkill: 'agent',
    AgentTool: 'agent',
    AgentMemory: 'agent',
    AgentKnowledge: 'agent',

    // ---- LLMs ----
    LLMRoute: 'llm',
    LLMModel: 'llm',
    LLMProvider: 'llm',

    // ---- Events ----
    EventStream: 'event',
    Topic: 'event',
    SchemaField: 'event',

    // ---- Custom ----
    Application: 'custom',
    Asset: 'custom',
    Resource: 'custom',
};

export function getEntityCategoryId(name: string): EntityCategoryId | undefined {
    return ENTITY_CATEGORIES[name];
}

// ---------- Instance types ----------------------------------------------------

export type AttrValue = string | number | boolean;

/**
 * Where an entity record came from.
 *   - `local`     : hand-created in Authorization (fully editable).
 *   - `scim`      : synced from an external IdP via SCIM (read-only).
 *   - `directory` : synced from the built-in Gravitee User Directory (read-only).
 */
export type EntitySource = 'local' | 'scim' | 'directory';

export interface EntityInstance {
    uid: { type: string; id: string };
    /** Human-readable display name (falls back to attrs.name). */
    displayName?: string;
    attrs: Record<string, AttrValue>;
    parents: Array<{ type: string; id: string }>;
    /** Where this record was materialized from. Drives editability. */
    source: EntitySource;
    /**
     * Identity provider/source label for principals:
     *  - SCIM: the IdP name (e.g. `Okta`, `Azure AD`).
     *  - Directory: the directory name (e.g. `Gravitee User Directory`).
     */
    principalProvider?: string;
    /** ISO timestamp of last import/sync (stored as _importedAt in backend attributes). */
    importedAt?: string;
    /** Backend record id (used for update/delete). Set after fromBackend(). */
    _backendId?: string;
    /** Backend createdAt ISO. */
    createdAt?: string;
    /** Backend updatedAt ISO. */
    updatedAt?: string;
}
