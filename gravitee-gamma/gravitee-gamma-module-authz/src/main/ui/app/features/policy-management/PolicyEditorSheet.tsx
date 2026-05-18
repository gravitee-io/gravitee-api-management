import {
    Badge,
    Button,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Spinner,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
    cn,
} from '@gravitee/graphene-core';
import { Code2, List, RefreshCcw, Rocket, Save, Sparkles } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { PortalModal } from '../../../components/PortalModal';
import { StatusBadge } from '../../../components/StatusBadge';
import { ValidationErrorAlert } from '../../../components/ValidationErrorAlert';
import type { ApiError } from '../../../lib/api/authz-api-client';
import type { CatalogEntry, PolicyRequest, PolicyResponse, PolicyStatus } from '../../../lib/api/authz-api.types';
import { parseGaplToStatements } from '../../../lib/gapl-policy-parser';
import { PolicyBuilder } from './PolicyBuilder';
import { parseUpdatedAt, relativeTime } from './PolicyListTable';
import type { ChipOption } from './PolicyStatementCard';
import type { ServicePageConfig } from './ServicePolicyPage';
import { createEmptyStatement, statementsToGapl, type PolicyStatement } from './statement-to-gapl';

export interface PolicyEditorSheetProps {
    readonly config: ServicePageConfig;
    readonly open: boolean;
    readonly policy: PolicyResponse | null;
    readonly initialTarget: CatalogEntry | null;
    readonly submitError: ApiError | null;
    readonly principalOptions: readonly ChipOption[];
    readonly actionOptions: readonly ChipOption[];
    readonly onOpenChange: (open: boolean) => void;
    readonly onSubmit: (request: PolicyRequest) => Promise<void>;
    readonly onChangeTarget?: () => void;
    /**
     * Optional empty-state hints rendered inside the chip pickers when the
     * corresponding option list is empty. The page-level container is the
     * right owner because it knows whether the schema/catalog/entities have
     * loaded yet — the sheet itself is purely presentational.
     */
    readonly emptyPrincipalsHint?: string;
    readonly emptyActionsHint?: string;
    readonly emptyResourcesHint?: string;
}

const TEMPLATE = 'permit (principal, action, resource);';

type ViewMode = 'visual' | 'code';

export function PolicyEditorSheet({
    config,
    open,
    policy,
    initialTarget,
    submitError,
    principalOptions,
    actionOptions,
    onOpenChange,
    onSubmit,
    onChangeTarget,
    emptyPrincipalsHint,
    emptyActionsHint,
    emptyResourcesHint,
}: PolicyEditorSheetProps) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [status, setStatus] = useState<PolicyStatus>('DRAFT');
    const [statements, setStatements] = useState<readonly PolicyStatement[]>([createEmptyStatement('permit')]);
    const [policyText, setPolicyText] = useState(TEMPLATE);
    const [view, setView] = useState<ViewMode>('visual');
    const [codeDirty, setCodeDirty] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [deploying, setDeploying] = useState(false);
    // Inline toast shown briefly after a successful deploy. We avoid adding a
    // global Toaster dependency (none exists yet) — a local timed banner keeps
    // the surface small and the feature self-contained.
    const [deployToast, setDeployToast] = useState<string | null>(null);
    // True once the user has touched any visual control on this open. In edit
    // mode we only regenerate the code buffer from the visual statements
    // after the user actually edited the visual side — otherwise switching
    // to Code (or back) would silently overwrite the stored GAPL with
    // re-emitted text that may differ in formatting / casing / unsupported
    // clauses (Bug D).
    const [visualDirty, setVisualDirty] = useState(false);
    // When editing an existing policy whose GAPL the parser cannot represent,
    // we keep the visual toggle disabled so saving never silently overwrites
    // unsupported clauses with the empty visual statement.
    const [visualUnavailable, setVisualUnavailable] = useState(false);

    const target = policy?.target ?? (initialTarget ? { id: initialTarget.id, label: initialTarget.name } : null);

    // Build resource options from catalog entry sub-resources
    const resourceOptions = useMemo((): readonly ChipOption[] => {
        const catalogEntry = initialTarget ?? (policy ? null : null);
        const subResources = catalogEntry?.subResources ?? initialTarget?.subResources ?? [];
        if (subResources.length === 0) {
            // For custom policies or when no catalog entry: return empty
            return config.resourceOptions ?? [];
        }
        return subResources.map(s => ({
            id: s.id,
            label: s.name,
            group: s.kind,
            description: s.description,
        }));
    }, [initialTarget, policy, config.resourceOptions]);

    const generatedCode = useMemo(
        () => statementsToGapl(name || 'new-policy', statements, target ?? undefined),
        [name, statements, target],
    );

    useEffect(() => {
        if (!open) return;
        setName(policy?.name ?? '');
        setDescription(policy?.description ?? '');
        setPolicyText(policy?.policyText ?? TEMPLATE);
        setStatus(policy?.status ?? 'DRAFT');
        setCodeDirty(false);
        setVisualDirty(false);

        if (policy) {
            // Try to roundtrip the stored GAPL into visual statements. On
            // success we seed `statements` and let the user pick either view.
            // On failure we keep an empty placeholder statement, force the
            // code view, and disable the Visual toggle so editing never
            // silently drops unsupported clauses.
            const parsed = parseGaplToStatements(policy.policyText);
            const supported = parsed !== null && parsed.statements.length > 0;
            if (supported) {
                setStatements(parsed!.statements);
                setVisualUnavailable(false);
                setView('visual');
            } else {
                setStatements([createEmptyStatement('permit')]);
                setVisualUnavailable(true);
                setView('code');
            }
        } else {
            setStatements([createEmptyStatement('permit')]);
            setVisualUnavailable(false);
            setView('visual');
        }
    }, [open, policy]);

    useEffect(() => {
        // Only auto-sync the code buffer from the visual statements when
        // creating a new policy. For edits we must not silently overwrite the
        // user's stored GAPL with regenerated text — that loses any GAPL
        // features the visual editor cannot represent. The user can always
        // explicitly hit "Regenerate from visual" in the code pane to opt in.
        if (view === 'code' && !codeDirty && policy === null) {
            setPolicyText(generatedCode);
        }
    }, [generatedCode, view, codeDirty, policy]);

    const switchToCode = () => {
        // When editing an existing policy we never silently overwrite the
        // stored GAPL with regenerated text unless the user has actually
        // edited the visual statements on this open. This preserves the
        // original formatting / casing / `when {}` blocks the visual editor
        // can represent but might re-emit slightly differently.
        if (policy && !visualDirty) {
            setView('code');
            return;
        }
        setPolicyText(generatedCode);
        setCodeDirty(false);
        setView('code');
    };

    const switchToVisual = () => setView('visual');

    const resetCodeFromVisual = () => {
        setPolicyText(generatedCode);
        setCodeDirty(false);
    };

    const handleStatementsChange = (next: readonly PolicyStatement[]) => {
        setStatements(next);
        setVisualDirty(true);
    };

    // Auto-dismiss the deploy toast after 3s. Cleanup cancels the timer if
    // the sheet closes or another deploy fires before the previous toast
    // expires — avoids state updates on an unmounted component.
    useEffect(() => {
        if (!deployToast) return;
        const handle = window.setTimeout(() => setDeployToast(null), 3000);
        return () => window.clearTimeout(handle);
    }, [deployToast]);

    const buildRequest = (overrideStatus?: PolicyStatus): PolicyRequest => {
        const finalText =
            view === 'code' ? policyText : visualDirty || policy === null ? generatedCode : (policy.policyText ?? generatedCode);
        return {
            name,
            description: description || null,
            policyText: finalText,
            type: config.type,
            target: target ?? null,
            status: overrideStatus ?? status,
        };
    };

    const deploy = async () => {
        // Deploy = save current edits AND flip status to DEPLOYED in one
        // round-trip. Saves the user from "Save → wait → click Deploy → wait
        // again". The next gateway sync (every 30s) picks the policy up via
        // the /bundle endpoint.
        setDeploying(true);
        try {
            await onSubmit(buildRequest('DEPLOYED'));
            setStatus('DEPLOYED');
            setDeployToast('Policy deployed. Gateway sync expected within 30s.');
        } finally {
            setDeploying(false);
        }
    };

    const submit = async () => {
        setSubmitting(true);
        try {
            // Choose the policyText to send back to the server:
            // - Code view → whatever's in the buffer (user's last word).
            // - Visual view → regenerated GAPL when the visual side was
            //   actually edited; otherwise echo the original stored text
            //   so editing only the name/description/status does not
            //   silently rewrite the user's hand-crafted GAPL (Bug D).
            await onSubmit(buildRequest());
        } finally {
            setSubmitting(false);
        }
    };

    // ── Header pieces ────────────────────────────────────────────────────
    //
    // The policy editor's header is richer than the icon-and-title most
    // panels need: an editable name input + status pill + view toggle +
    // status select + save / deploy buttons. PortalModal accepts JSX for
    // both `title` and `headerActions`, so we render the custom shell into
    // those slots and the modal handles overlay / esc / click-outside.

    const lastEdited = (() => {
        if (!policy?.updatedAt) return '';
        const d = parseUpdatedAt(policy.updatedAt);
        return Number.isNaN(d.getTime()) ? '' : ` · last edited ${d.toLocaleDateString()}`;
    })();

    const headerTitle = (
        <div className="min-w-0">
            <div className="flex items-center gap-2">
                <Input
                    value={name}
                    onChange={e => setName(e.target.value)}
                    placeholder="Policy name"
                    className="h-8 min-w-[220px] max-w-[480px] border-transparent bg-transparent px-1 text-base font-semibold shadow-none focus-visible:border-input"
                    aria-label="Policy name"
                />
                <StatusBadge status={status} />
            </div>
            <p className="text-muted-foreground" style={{ fontSize: '11px' }}>
                {config.serviceLabel} policy
                {lastEdited}
            </p>
        </div>
    );

    const headerActions = (
        <>
            <ViewToggle
                view={view}
                onVisual={switchToVisual}
                onCode={switchToCode}
                visualDisabled={visualUnavailable}
                visualDisabledTitle="This policy uses GAPL features that the visual editor cannot represent. Use the Code view to edit it directly."
            />
            <div>
                <Label htmlFor="sheet-status" className="sr-only">
                    Status
                </Label>
                <Select value={status} onValueChange={v => setStatus(v as PolicyStatus)}>
                    <SelectTrigger id="sheet-status" className="h-8 w-28">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="DRAFT">Draft</SelectItem>
                        <SelectItem value="DEPLOYED">Deployed</SelectItem>
                        <SelectItem value="DISABLED">Disabled</SelectItem>
                    </SelectContent>
                </Select>
            </div>
            <Button type="button" variant="outline" size="sm" onClick={submit} disabled={submitting || name.trim() === ''}>
                {submitting ? <Spinner className="size-3.5 mr-1.5" /> : <Save className="size-3.5 mr-1.5" />}
                {submitting ? 'Saving…' : 'Save draft'}
            </Button>
            <DeployButton
                policy={policy}
                currentStatus={status}
                deploying={deploying}
                submitting={submitting}
                nameEmpty={name.trim() === ''}
                onDeploy={deploy}
            />
        </>
    );

    const subHeader = (
        <>
            {deployToast ? (
                <div
                    role="status"
                    aria-live="polite"
                    className="shrink-0 border-b bg-emerald-50 px-6 py-2 text-sm text-emerald-900 dark:bg-emerald-950/40 dark:text-emerald-200"
                >
                    {deployToast}
                </div>
            ) : null}

            {config.hasTarget && target ? (
                <div className="shrink-0 border-b bg-muted/30 px-6 py-2">
                    <div className="flex items-center gap-3">
                        <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-2">
                                <p className="text-sm font-medium">{target.label}</p>
                                {initialTarget?.badges?.map(b => (
                                    <Badge key={b} variant="outline" className="capitalize" style={{ fontSize: '10px' }}>
                                        {b}
                                    </Badge>
                                ))}
                                {initialTarget?.subResources && initialTarget.subResources.length > 0 ? (
                                    <span className="text-muted-foreground" style={{ fontSize: '11px' }}>
                                        {initialTarget.subResources.length} sub-resources
                                    </span>
                                ) : null}
                            </div>
                        </div>
                        {onChangeTarget && (
                            <Button type="button" variant="ghost" size="sm" onClick={onChangeTarget} className="shrink-0">
                                Change
                            </Button>
                        )}
                    </div>
                </div>
            ) : config.hasTarget ? null : (
                <div className="shrink-0 border-b bg-muted/20 px-6 py-2 text-muted-foreground" style={{ fontSize: '12px' }}>
                    No scoped target — custom policies apply to whatever principal, action, and resource you define.
                </div>
            )}

            <div className="shrink-0 border-b px-6 py-2">
                <Input
                    value={description}
                    onChange={e => setDescription(e.target.value)}
                    placeholder="Description (optional)"
                    className="border-transparent bg-transparent shadow-none text-sm"
                    aria-label="Policy description"
                />
            </div>
        </>
    );

    const footer = (
        <div className="flex w-full flex-col gap-2">
            <ValidationErrorAlert error={submitError} title="Could not save policy" />
            <div className="flex items-center justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={submitting}>
                    Cancel
                </Button>
                <Button type="button" onClick={submit} disabled={submitting || name.trim() === ''}>
                    {submitting ? 'Saving…' : policy ? 'Update policy' : 'Create policy'}
                </Button>
            </div>
        </div>
    );

    return (
        <PortalModal
            open={open}
            onOpenChange={onOpenChange}
            ariaLabel={policy ? `Edit policy: ${policy.name}` : `New ${config.type} policy`}
            icon={<Rocket size={18} />}
            title={headerTitle}
            headerActions={headerActions}
            subHeader={subHeader}
            footer={footer}
            width="min(960px, 100%)"
        >
            {view === 'visual' ? (
                <PolicyBuilder
                    policyName={name}
                    target={target}
                    statements={statements}
                    principalOptions={principalOptions}
                    actionOptions={actionOptions}
                    resourceOptions={resourceOptions}
                    resourceGroups={config.resourceGroups ?? []}
                    conditionSnippets={config.conditionSnippets}
                    onChange={handleStatementsChange}
                    emptyPrincipalsHint={emptyPrincipalsHint}
                    emptyActionsHint={emptyActionsHint}
                    emptyResourcesHint={emptyResourcesHint}
                />
            ) : (
                <CodePane
                    code={policyText}
                    onCodeChange={next => {
                        setPolicyText(next);
                        setCodeDirty(next !== generatedCode);
                    }}
                    onReset={resetCodeFromVisual}
                    dirty={codeDirty}
                />
            )}
        </PortalModal>
    );
}

// ---------- Deploy button -----------------------------------------------

function DeployButton({
    policy,
    currentStatus,
    deploying,
    submitting,
    nameEmpty,
    onDeploy,
}: {
    policy: PolicyResponse | null;
    currentStatus: PolicyStatus;
    deploying: boolean;
    submitting: boolean;
    nameEmpty: boolean;
    onDeploy: () => void;
}) {
    // No saved policy yet → cannot deploy. The user must save a draft first
    // so the server has an id to flip to DEPLOYED.
    if (!policy?.id) {
        return (
            <TooltipProvider>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span>
                            <Button type="button" size="sm" disabled>
                                <Rocket className="size-3.5 mr-1.5" />
                                Deploy to PDP
                            </Button>
                        </span>
                    </TooltipTrigger>
                    <TooltipContent>Save the policy first</TooltipContent>
                </Tooltip>
            </TooltipProvider>
        );
    }

    // Already deployed → show "Deployed (N min ago)" using the policy's
    // updatedAt as a deploy-timestamp proxy. Re-deploying just clicks Deploy
    // again after editing.
    if (currentStatus === 'DEPLOYED' && policy.status === 'DEPLOYED') {
        const label = `Deployed (${relativeTime(policy.updatedAt)})`;
        return (
            <TooltipProvider>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span>
                            <Button type="button" size="sm" variant="outline" disabled>
                                <Rocket className="size-3.5 mr-1.5" />
                                {label}
                            </Button>
                        </span>
                    </TooltipTrigger>
                    <TooltipContent>Already deployed; edit and click Deploy again to redeploy</TooltipContent>
                </Tooltip>
            </TooltipProvider>
        );
    }

    return (
        <Button type="button" size="sm" onClick={onDeploy} disabled={deploying || submitting || nameEmpty}>
            {deploying ? <Spinner className="size-3.5 mr-1.5" /> : <Rocket className="size-3.5 mr-1.5" />}
            {deploying ? 'Deploying…' : 'Deploy to PDP'}
        </Button>
    );
}

// ---------- View toggle -------------------------------------------------

function ViewToggle({
    view,
    onVisual,
    onCode,
    visualDisabled = false,
    visualDisabledTitle,
}: {
    view: ViewMode;
    onVisual: () => void;
    onCode: () => void;
    visualDisabled?: boolean;
    visualDisabledTitle?: string;
}) {
    return (
        <div className="inline-flex rounded-md border bg-muted p-0.5 text-xs">
            <button
                type="button"
                onClick={onVisual}
                disabled={visualDisabled}
                title={visualDisabled ? visualDisabledTitle : undefined}
                className={cn(
                    'flex items-center gap-1.5 rounded px-2.5 py-1',
                    view === 'visual' ? 'bg-background font-medium shadow-sm' : 'text-muted-foreground',
                    visualDisabled && 'cursor-not-allowed opacity-50',
                )}
            >
                <List className="size-3.5" />
                Visual
            </button>
            <button
                type="button"
                onClick={onCode}
                className={cn(
                    'flex items-center gap-1.5 rounded px-2.5 py-1',
                    view === 'code' ? 'bg-background font-medium shadow-sm' : 'text-muted-foreground',
                )}
            >
                <Code2 className="size-3.5" />
                Code
            </button>
        </div>
    );
}

// ---------- Code pane ---------------------------------------------------

function CodePane({
    code,
    onCodeChange,
    onReset,
    dirty,
}: {
    code: string;
    onCodeChange: (next: string) => void;
    onReset: () => void;
    dirty: boolean;
}) {
    const textareaRef = useRef<HTMLTextAreaElement | null>(null);
    const gutterRef = useRef<HTMLDivElement | null>(null);
    const lineCount = useMemo(() => Math.max(1, code.split('\n').length), [code]);

    const onScroll = () => {
        if (!textareaRef.current || !gutterRef.current) return;
        gutterRef.current.scrollTop = textareaRef.current.scrollTop;
    };

    return (
        <div className="flex h-full flex-col">
            <div className="flex shrink-0 items-center justify-between border-b bg-muted/30 px-6 py-2 text-xs">
                <div className="flex items-center gap-2">
                    <span className="font-mono text-muted-foreground" style={{ fontSize: '11px' }}>
                        policy.gapl
                    </span>
                    {dirty ? (
                        <Badge
                            variant="outline"
                            className="border-amber-300 text-amber-700 dark:border-amber-700 dark:text-amber-400"
                            style={{ fontSize: '10px' }}
                        >
                            Modified in code
                        </Badge>
                    ) : (
                        <Badge variant="outline" className="text-muted-foreground" style={{ fontSize: '10px' }}>
                            Synced with visual
                        </Badge>
                    )}
                </div>
                <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={onReset}
                    disabled={!dirty}
                    title="Discard code edits and regenerate from the visual editor"
                >
                    <RefreshCcw className="size-3.5 mr-1.5" />
                    Regenerate from visual
                </Button>
            </div>

            <div className="min-h-0 flex-1 overflow-hidden bg-background">
                <div className="flex h-full font-mono leading-[1.6]" style={{ fontSize: '13px' }}>
                    <div
                        ref={gutterRef}
                        className="w-12 shrink-0 overflow-hidden border-r bg-muted/30 py-3 pr-2 text-right text-muted-foreground/70 tabular-nums"
                    >
                        {Array.from({ length: lineCount }, (_, i) => (
                            <div key={i} className="select-none">
                                {i + 1}
                            </div>
                        ))}
                    </div>
                    <textarea
                        ref={textareaRef}
                        value={code}
                        onChange={e => onCodeChange(e.target.value)}
                        onScroll={onScroll}
                        spellCheck={false}
                        className="flex-1 resize-none bg-transparent px-3 py-3 outline-none whitespace-pre overflow-auto"
                        aria-label="GAPL policy text"
                    />
                </div>
            </div>

            <div className="shrink-0 border-t bg-muted/20 px-6 py-2 text-muted-foreground" style={{ fontSize: '11px' }}>
                <span className="inline-flex items-center gap-1">
                    <Sparkles className="size-3" />
                    Code edits stay in this buffer for review. The visual editor remains the source of truth — use{' '}
                    <span className="font-medium">Regenerate from visual</span> to sync back.
                </span>
            </div>
        </div>
    );
}
