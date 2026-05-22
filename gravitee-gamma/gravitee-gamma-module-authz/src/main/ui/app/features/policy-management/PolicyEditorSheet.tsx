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
import {
    Button,
    Input,
    Sheet,
    SheetContent,
    SheetTitle,
    Spinner,
    ToggleGroup,
    ToggleGroupItem,
    cn,
    toast,
} from '@gravitee/graphene-core';
import { ListIcon } from '@gravitee/graphene-core/icons';
// `Code2` isn't in `@gravitee/graphene-core/icons` yet, so we keep the lucide
// import for that single glyph; everything else uses the Graphene icon set.
import { Code2 } from 'lucide-react';
import { useMemo, useRef, useState, useEffect } from 'react';
import { StatusBadge } from '../../../components/StatusBadge';
import { ValidationErrorAlert } from '../../../components/ValidationErrorAlert';
import type { ApiError } from '../../../lib/api/authz-api-client';
import type { CatalogEntry, PolicyRequest, PolicyResponse, PolicyStatus } from '../../../lib/api/authz-api.types';
import { parseGaplToStatements } from '../../../lib/gapl-policy-parser';
import { PolicyBuilder } from './PolicyBuilder';
import { parseUpdatedAt } from './PolicyListTable';
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

    // Resource options come straight from the page-level catalog
    // (`config.resourceOptions`), which already filters by service type for
    // target-bound pages (mcp.* / llm.* / api.*) and lists every authorable
    // resource for Custom policies. Sub-resources are no longer surfaced via
    // CatalogEntry — the entity collection itself carries every addressable
    // resource as a top-level row, so any tool/endpoint shows up alongside
    // its parent service.
    const resourceOptions = config.resourceOptions ?? [];

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
            // Fresh policy: empty first statement. The user picks all three
            // slots (principal / action / resource) from the in-editor chip
            // comboboxes.
            setStatements([createEmptyStatement('permit')]);
            setVisualUnavailable(false);
            setView('visual');
        }
    }, [open, policy]);

    useEffect(() => {
        // Only auto-sync the code buffer from the visual statements when
        // creating a new policy. For edits we must not silently overwrite the
        // user's stored GAPL with regenerated text — that loses any GAPL
        // features the visual editor cannot represent.
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

    const switchToVisual = () => {
        // Re-parse the current code buffer so anything the user typed in the
        // Code view shows up in the Visual statements immediately. If the
        // parser can't represent the GAPL (advanced when-clauses etc.) we
        // keep the user in the Code view and surface a warning toast — the
        // policy text in the buffer is left intact so the user doesn't lose
        // their edits.
        if (codeDirty) {
            const parsed = parseGaplToStatements(policyText);
            if (parsed && parsed.statements.length > 0) {
                setStatements(parsed.statements);
                setVisualDirty(false);
                setCodeDirty(false);
                setVisualUnavailable(false);
            } else {
                setVisualUnavailable(true);
                setView('code');
                toast.warning('This GAPL uses features the visual editor cannot represent. Continue in the Code view.');
                return;
            }
        }
        setView('visual');
    };

    const handleStatementsChange = (next: readonly PolicyStatement[]) => {
        setStatements(next);
        setVisualDirty(true);
    };

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
            toast.success('Policy deployed. Gateway sync expected within 30s.');
        } finally {
            setDeploying(false);
        }
    };

    const undeploy = async () => {
        // Canonical engine only exposes two state-transition verbs: deploy and
        // disable. DRAFT is an implicit-after-create state; you can't flip
        // DEPLOYED → DRAFT directly. Undeploy therefore writes DISABLED, which
        // routes through /policies/:id/disable on the backend so the gateway
        // sync drops the policy from the next bundle.
        setDeploying(true);
        try {
            await onSubmit(buildRequest('DISABLED'));
            setStatus('DISABLED');
            toast.success('Policy undeployed. Gateway sync will drop it within 30s.');
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
    // status select + save / deploy buttons. Built into the Sheet shell
    // below — Graphene's Sheet handles overlay / esc / focus trap.

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
                    className="h-8 min-w-56 max-w-[30rem] border-transparent bg-transparent px-1 text-base font-semibold shadow-none focus-visible:border-input"
                    aria-label="Policy name"
                />
                <StatusBadge status={status} />
            </div>
            <p className="pl-1 text-xs text-muted-foreground">
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
            {policy ? (
                policy.status === 'DEPLOYED' ? (
                    <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        onClick={undeploy}
                        disabled={deploying || submitting || name.trim() === ''}
                        className="border-destructive text-destructive hover:bg-destructive/10 hover:text-destructive"
                    >
                        {deploying ? <Spinner className="size-3.5 mr-1.5" /> : null}
                        {deploying ? 'Undeploying…' : 'Undeploy'}
                    </Button>
                ) : (
                    <Button type="button" size="sm" onClick={deploy} disabled={deploying || submitting || name.trim() === ''}>
                        {deploying ? <Spinner className="size-3.5 mr-1.5" /> : null}
                        {deploying ? 'Deploying…' : 'Deploy'}
                    </Button>
                )
            ) : null}
        </>
    );

    const subHeader = (
        <>
            <div className="shrink-0 border-b px-6 py-2">
                <Input
                    value={description}
                    onChange={e => setDescription(e.target.value)}
                    placeholder="Description (optional)"
                    className="h-8 border-transparent bg-transparent px-1 text-sm shadow-none focus-visible:border-input"
                    aria-label="Policy description"
                />
            </div>
        </>
    );

    const createAndDeploy = async () => {
        setSubmitting(true);
        try {
            await onSubmit(buildRequest('DEPLOYED'));
            setStatus('DEPLOYED');
            toast.success('Policy created and deployed. Gateway sync expected within 30s.');
        } finally {
            setSubmitting(false);
        }
    };

    const footer = (
        <div className="flex w-full flex-col gap-2">
            <ValidationErrorAlert error={submitError} title="Could not save policy" />
            <div className="flex flex-wrap items-center justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={submitting || deploying}>
                    Cancel
                </Button>
                <Button
                    type="button"
                    variant={policy ? 'default' : 'outline'}
                    onClick={submit}
                    disabled={submitting || deploying || name.trim() === ''}
                >
                    {submitting ? 'Saving…' : policy ? 'Save' : 'Create policy'}
                </Button>
                {policy ? null : (
                    <Button
                        type="button"
                        onClick={createAndDeploy}
                        disabled={submitting || deploying || name.trim() === ''}
                    >
                        {submitting || deploying ? 'Creating…' : 'Create and Deploy policy'}
                    </Button>
                )}
            </div>
        </div>
    );

    const ariaLabel = policy ? `Edit policy: ${policy.name}` : `New ${config.type} policy`;

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent
                side="right"
                showCloseButton={false}
                aria-label={ariaLabel}
                // Inline style overrides Graphene's `w-3/4 sm:max-w-sm` defaults
                // directly — Tailwind's `cn()` merge was keeping Graphene's
                // classes because our overrides arrived "behind" theirs in the
                // merge, and Tailwind doesn't always purge arbitrary data-side
                // utilities consistently.
                style={{ width: 'min(720px, 100vw)', maxWidth: 'min(720px, 100vw)' }}
                className="flex h-full flex-col gap-0 p-0"
            >
                <SheetTitle className="sr-only">{ariaLabel}</SheetTitle>

                <div className="flex items-start gap-3 border-b px-6 py-4">
                    <div className="min-w-0 flex-1">{headerTitle}</div>
                    <div className="flex flex-none items-center gap-2">{headerActions}</div>
                </div>

                {subHeader}

                <div className="flex-1 overflow-y-auto px-6 py-5">
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
                        />
                    )}
                </div>

                <div className="border-t bg-muted/40 px-6 py-3.5">{footer}</div>
            </SheetContent>
        </Sheet>
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
        <ToggleGroup
            type="single"
            size="sm"
            value={view}
            onValueChange={value => {
                if (value === 'visual') onVisual();
                else if (value === 'code') onCode();
            }}
            className="bg-muted rounded-md p-0.5 text-xs"
        >
            <ToggleGroupItem
                value="visual"
                disabled={visualDisabled}
                title={visualDisabled ? visualDisabledTitle : undefined}
                className={cn(
                    'gap-1.5',
                    view === 'visual' &&
                        'bg-primary/10 text-primary border-primary font-semibold shadow-sm hover:bg-primary/10 hover:text-primary',
                )}
            >
                <ListIcon className="size-3.5" aria-hidden />
                Visual
            </ToggleGroupItem>
            <ToggleGroupItem
                value="code"
                className={cn(
                    'gap-1.5',
                    view === 'code' &&
                        'bg-primary/10 text-primary border-primary font-semibold shadow-sm hover:bg-primary/10 hover:text-primary',
                )}
            >
                <Code2 className="size-3.5" />
                Code
            </ToggleGroupItem>
        </ToggleGroup>
    );
}

// ---------- Code pane ---------------------------------------------------

function CodePane({
    code,
    onCodeChange,
}: {
    code: string;
    onCodeChange: (next: string) => void;
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
            <div className="min-h-0 flex-1 overflow-hidden bg-background">
                <div className="flex h-full font-mono text-sm leading-relaxed">
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
        </div>
    );
}
