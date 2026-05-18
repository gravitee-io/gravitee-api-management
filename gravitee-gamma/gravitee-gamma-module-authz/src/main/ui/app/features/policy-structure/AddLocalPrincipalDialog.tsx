/**
 * Dialog for creating a local principal (User / Group / ServiceAccount / AgentIdentity).
 * Ported from prototype add-local-principal-dialog.tsx, using the real useEntities hook.
 */
import {
    Alert,
    AlertDescription,
    Badge,
    Button,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    cn,
} from '@gravitee/graphene-core';
import { Bot, KeyRound, Plus, UserCog, Users } from 'lucide-react';
import { useMemo, useState } from 'react';
import { PortalModal } from '../../../components/PortalModal';
import { toBackend } from '../../../lib/entity-adapter';
import type { UseEntitiesResult } from '../../../lib/hooks/useEntities';
import type { EntityInstance } from './entity-types';

type PrincipalKind = 'User' | 'Group' | 'ServiceAccount' | 'AgentIdentity';

interface AddLocalPrincipalDialogProps {
    open: boolean;
    onOpenChange: (next: boolean) => void;
    create: UseEntitiesResult['create'];
    allEntities: EntityInstance[];
    onAdded?: (entity: { type: string; id: string }) => void;
}

const KIND_OPTIONS: Array<{
    value: PrincipalKind;
    label: string;
    description: string;
    icon: React.ComponentType<{ className?: string }>;
    tone: string;
}> = [
    {
        value: 'User',
        label: 'User',
        description: 'An individual human principal with an email.',
        icon: Users,
        tone: 'border-blue-200 bg-blue-50 text-blue-700 dark:border-blue-900/40 dark:bg-blue-950/40 dark:text-blue-300',
    },
    {
        value: 'Group',
        label: 'Group',
        description: 'A collection of users / identities used in policies.',
        icon: UserCog,
        tone: 'border-violet-200 bg-violet-50 text-violet-700 dark:border-violet-900/40 dark:bg-violet-950/40 dark:text-violet-300',
    },
    {
        value: 'ServiceAccount',
        label: 'Service Account',
        description: 'A machine identity scoped to a tenant.',
        icon: KeyRound,
        tone: 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-900/40 dark:bg-amber-950/40 dark:text-amber-300',
    },
    {
        value: 'AgentIdentity',
        label: 'Agent Identity',
        description: 'An autonomous / semi-autonomous agent principal.',
        icon: Bot,
        tone: 'border-orange-200 bg-orange-50 text-orange-700 dark:border-orange-900/40 dark:bg-orange-950/40 dark:text-orange-300',
    },
];

function slugify(value: string): string {
    return value
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/(^-|-$)/g, '');
}

function buildEntityId(kind: PrincipalKind, slug: string): string {
    switch (kind) {
        case 'User':
            return `user.${slug}`;
        case 'Group':
            return `group.${slug}`;
        case 'ServiceAccount':
            return `svc.${slug}`;
        case 'AgentIdentity':
            return `agent-id.${slug}`;
    }
}

export function AddLocalPrincipalDialog({ open, onOpenChange, create, allEntities, onAdded }: AddLocalPrincipalDialogProps) {
    const existingGroups = useMemo(
        () =>
            allEntities
                .filter(e => e.uid.type === 'Group')
                .sort((a, b) => (a.displayName ?? a.uid.id).localeCompare(b.displayName ?? b.uid.id)),
        [allEntities],
    );

    const [kind, setKind] = useState<PrincipalKind>('User');
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [tenant, setTenant] = useState('gravitee');
    const [description, setDescription] = useState('');
    const [agentType, setAgentType] = useState('autonomous');
    const [parentGroups, setParentGroups] = useState<string[]>([]);
    const [customSlug, setCustomSlug] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    const reset = () => {
        setKind('User');
        setName('');
        setEmail('');
        setTenant('gravitee');
        setDescription('');
        setAgentType('autonomous');
        setParentGroups([]);
        setCustomSlug('');
        setSubmitError(null);
    };

    // Whether the user explicitly supplied an Override slug. When they have,
    // their value is used VERBATIM (after sanitisation) without the auto kind
    // prefix — bug F: typing "alice" into Override produced User::"user.alice"
    // instead of User::"alice".
    const hasCustomSlug = customSlug.trim().length > 0;

    const effectiveSlug = useMemo(() => {
        if (hasCustomSlug) return slugify(customSlug);
        if (kind === 'User' && email.includes('@')) {
            return slugify(email.split('@')[0]);
        }
        return slugify(name);
    }, [hasCustomSlug, customSlug, kind, email, name]);

    // When the user explicitly types an Override slug, use it as-is so power
    // users can choose any id shape (e.g. matching an external IdP). Auto
    // prefix (`user.`, `group.`, …) only kicks in when no override is given.
    const entityId = effectiveSlug ? (hasCustomSlug ? effectiveSlug : buildEntityId(kind, effectiveSlug)) : '';
    const collision = entityId ? allEntities.some(e => e.uid.type === kind && e.uid.id === entityId) : false;

    const canSubmit =
        name.trim().length > 0 && entityId.length > 0 && !collision && !submitting && (kind !== 'User' || email.trim().length > 0);

    const handleSubmit = async () => {
        if (!canSubmit) return;
        setSubmitting(true);
        setSubmitError(null);

        const attrs: Record<string, string | number | boolean> = { name: name.trim() };
        switch (kind) {
            case 'User':
                attrs.email = email.trim();
                if (tenant.trim()) attrs.tenant = tenant.trim();
                break;
            case 'Group':
                if (description.trim()) attrs.description = description.trim();
                break;
            case 'ServiceAccount':
                if (tenant.trim()) attrs.tenant = tenant.trim();
                if (description.trim()) attrs.description = description.trim();
                break;
            case 'AgentIdentity':
                attrs.agent_type = agentType;
                if (description.trim()) attrs.description = description.trim();
                break;
        }

        const parents = kind === 'User' || kind === 'AgentIdentity' ? parentGroups.map(id => ({ type: 'Group', id })) : [];

        const instance: EntityInstance = {
            uid: { type: kind, id: entityId },
            displayName: name.trim(),
            attrs,
            parents,
            source: 'local',
        };

        try {
            await create(toBackend(instance));
            onAdded?.({ type: kind, id: entityId });
            reset();
            onOpenChange(false);
        } catch (e) {
            setSubmitError(e instanceof Error ? e.message : 'Create failed');
        } finally {
            setSubmitting(false);
        }
    };

    const toggleParent = (id: string) => {
        setParentGroups(prev => (prev.includes(id) ? prev.filter(p => p !== id) : [...prev, id]));
    };

    const selectedKind = KIND_OPTIONS.find(k => k.value === kind)!;

    return (
        <PortalModal
            open={open}
            onOpenChange={next => {
                if (!next) reset();
                onOpenChange(next);
            }}
            ariaLabel="Add local principal"
            icon={<Plus size={18} />}
            title="Add local principal"
            description="Create a principal that lives only in Authorization. Use this for one-off identities that aren't synced from SCIM or the Gravitee User Directory."
            width="min(720px, 100%)"
            footer={
                <>
                    <Button
                        variant="outline"
                        onClick={() => {
                            reset();
                            onOpenChange(false);
                        }}
                    >
                        Cancel
                    </Button>
                    <Button onClick={() => void handleSubmit()} disabled={!canSubmit}>
                        <Plus className="mr-2 size-4" />
                        Create principal
                    </Button>
                </>
            }
        >
            <div className="space-y-4">
                {/* Kind picker */}
                <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
                    {KIND_OPTIONS.map(opt => {
                        const Icon = opt.icon;
                        const active = kind === opt.value;
                        return (
                            <button
                                key={opt.value}
                                type="button"
                                onClick={() => setKind(opt.value)}
                                className={cn(
                                    'flex flex-col items-start gap-1 rounded-md border p-2.5 text-left text-xs transition-colors',
                                    active ? 'border-primary bg-primary/5' : 'border-input hover:bg-accent',
                                )}
                            >
                                <div className="flex items-center gap-1.5">
                                    <Icon className="size-3.5 text-muted-foreground" />
                                    <span className="font-medium">{opt.label}</span>
                                </div>
                                <span className="text-[10.5px] text-muted-foreground">{opt.description}</span>
                            </button>
                        );
                    })}
                </div>

                {/* Fields */}
                <div className="space-y-3">
                    <div className="space-y-1.5">
                        <Label htmlFor="principal-name">Display name</Label>
                        <Input
                            id="principal-name"
                            placeholder={
                                kind === 'User'
                                    ? 'Alice Nguyen'
                                    : kind === 'Group'
                                      ? 'Platform Engineering'
                                      : kind === 'ServiceAccount'
                                        ? 'billing-service'
                                        : 'finance-ops-agent'
                            }
                            value={name}
                            onChange={e => setName(e.target.value)}
                        />
                    </div>

                    {kind === 'User' ? (
                        <div className="grid grid-cols-2 gap-3">
                            <div className="space-y-1.5">
                                <Label htmlFor="principal-email">Email</Label>
                                <Input
                                    id="principal-email"
                                    type="email"
                                    placeholder="alice@gravitee.io"
                                    value={email}
                                    onChange={e => setEmail(e.target.value)}
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="principal-tenant">Tenant</Label>
                                <Input
                                    id="principal-tenant"
                                    placeholder="gravitee"
                                    value={tenant}
                                    onChange={e => setTenant(e.target.value)}
                                />
                            </div>
                        </div>
                    ) : null}

                    {kind === 'ServiceAccount' ? (
                        <div className="grid grid-cols-2 gap-3">
                            <div className="space-y-1.5">
                                <Label htmlFor="principal-tenant">Tenant</Label>
                                <Input
                                    id="principal-tenant"
                                    placeholder="gravitee"
                                    value={tenant}
                                    onChange={e => setTenant(e.target.value)}
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="principal-desc">Description</Label>
                                <Input
                                    id="principal-desc"
                                    placeholder="Used by the billing pipeline"
                                    value={description}
                                    onChange={e => setDescription(e.target.value)}
                                />
                            </div>
                        </div>
                    ) : null}

                    {kind === 'AgentIdentity' ? (
                        <div className="grid grid-cols-2 gap-3">
                            <div className="space-y-1.5">
                                <Label htmlFor="principal-agent-type">Agent type</Label>
                                <Select value={agentType} onValueChange={setAgentType}>
                                    <SelectTrigger id="principal-agent-type">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="autonomous">Autonomous</SelectItem>
                                        <SelectItem value="conversational">Conversational</SelectItem>
                                        <SelectItem value="orchestrator">Orchestrator</SelectItem>
                                        <SelectItem value="monitoring">Monitoring</SelectItem>
                                        <SelectItem value="automation">Automation</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="principal-desc">Description</Label>
                                <Input
                                    id="principal-desc"
                                    placeholder="Short agent description"
                                    value={description}
                                    onChange={e => setDescription(e.target.value)}
                                />
                            </div>
                        </div>
                    ) : null}

                    {kind === 'Group' ? (
                        <div className="space-y-1.5">
                            <Label htmlFor="principal-desc">Description</Label>
                            <Input
                                id="principal-desc"
                                placeholder="Short group description"
                                value={description}
                                onChange={e => setDescription(e.target.value)}
                            />
                        </div>
                    ) : null}

                    {kind === 'User' || kind === 'AgentIdentity' ? (
                        <div className="space-y-1.5">
                            <Label>Parent groups (optional)</Label>
                            {existingGroups.length === 0 ? (
                                <div className="rounded-md border border-dashed p-3 text-xs text-muted-foreground">
                                    No groups yet. Create a Group first to nest this principal.
                                </div>
                            ) : (
                                <div className="flex flex-wrap gap-1.5 rounded-md border bg-muted/30 p-2">
                                    {existingGroups.map(g => {
                                        const selected = parentGroups.includes(g.uid.id);
                                        return (
                                            <button
                                                key={g.uid.id}
                                                type="button"
                                                onClick={() => toggleParent(g.uid.id)}
                                                className={cn(
                                                    'rounded border px-2 py-0.5 text-[11px] transition-colors',
                                                    selected
                                                        ? 'border-primary bg-primary/10 text-primary'
                                                        : 'border-input bg-background text-muted-foreground hover:bg-accent',
                                                )}
                                            >
                                                {g.displayName ?? g.uid.id}
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    ) : null}

                    <div className="space-y-1.5">
                        <Label htmlFor="principal-slug" className="flex items-center gap-2">
                            Entity ID
                            <Badge variant="outline" className={cn('font-mono text-[10px]', selectedKind.tone)}>
                                {selectedKind.label}
                            </Badge>
                        </Label>
                        <div className="rounded-md border bg-muted/30 px-3 py-2">
                            {entityId ? (
                                <code className="font-mono text-xs text-muted-foreground">{entityId}</code>
                            ) : (
                                <span className="text-xs text-muted-foreground">Enter a display name to auto-generate an Entity ID.</span>
                            )}
                        </div>
                        <Input
                            id="principal-slug"
                            placeholder="Override slug (optional)"
                            value={customSlug}
                            onChange={e => setCustomSlug(e.target.value)}
                            className="h-8 text-xs"
                        />
                        {collision ? (
                            <div className="text-[11px] text-destructive">A {kind} with this Entity ID already exists.</div>
                        ) : null}
                    </div>

                    {submitError ? (
                        <Alert variant="destructive">
                            <AlertDescription>{submitError}</AlertDescription>
                        </Alert>
                    ) : null}
                </div>
            </div>
        </PortalModal>
    );
}
