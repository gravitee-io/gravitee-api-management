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
import { Button, Input, Sheet, SheetContent, SheetTitle, Spinner, ToggleGroup, ToggleGroupItem, toast } from '@gravitee/graphene-core';
import { ListIcon } from '@gravitee/graphene-core/icons';
// Code2 is not in @gravitee/graphene-core/icons yet.
import { Code2 } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { StatusBadge } from '../../components/StatusBadge';
import { ValidationErrorAlert } from '../../components/ValidationErrorAlert';
import type { ApiError } from '../../shared/api/authz-api-client';
import type { PolicyRequest, PolicyResponse, PolicyStatus } from '../../shared/api/authz-api.types';
import type { ChipOption } from '../../shared/chip-option';
import { parseGaplToStatements } from '../../shared/gapl-policy-parser';
import { PolicyBuilder } from './PolicyBuilder';
import { relativeTime } from './PolicyListTable';
import type { CatalogEntry, ServicePageConfig } from './ServicePolicyPage';
import { createEmptyStatement, statementsToGapl, type PolicyStatement } from './statement-to-gapl';

export interface PolicyEditorSheetProps {
    readonly config: ServicePageConfig;
    readonly open: boolean;
    readonly policy: PolicyResponse | null;
    readonly initialTarget: CatalogEntry | null;
    readonly submitError: ApiError | Error | null;
    readonly principalOptions: readonly ChipOption[];
    readonly actionOptions: readonly ChipOption[];
    readonly agentOptions: readonly ChipOption[];
    readonly onOpenChange: (open: boolean) => void;
    readonly onSubmit: (request: PolicyRequest) => Promise<void>;
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
    agentOptions,
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
    const [visualDirty, setVisualDirty] = useState(false);
    const [visualUnavailable, setVisualUnavailable] = useState(false);

    const target = useMemo(
        () => policy?.target ?? (initialTarget ? { id: initialTarget.id, label: initialTarget.name } : null),
        [policy?.target, initialTarget],
    );
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
        if (view === 'code' && !codeDirty && policy === null) {
            setPolicyText(generatedCode);
        }
    }, [generatedCode, view, codeDirty, policy]);

    const switchToCode = () => {
        if (policy && !visualDirty) {
            setView('code');
            return;
        }
        setPolicyText(generatedCode);
        setCodeDirty(false);
        setView('code');
    };

    const switchToVisual = () => {
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

    const runSubmit = async (status: PolicyStatus | undefined, successMessage: string | null) => {
        try {
            await onSubmit(buildRequest(status));
            if (status) setStatus(status);
            if (successMessage) toast.success(successMessage);
        } catch {
            // submitError prop is rendered by the parent.
        }
    };

    const deploy = async () => {
        setDeploying(true);
        await runSubmit('DEPLOYED', 'Policy deployed. Gateway sync expected within 30s.');
        setDeploying(false);
    };

    // Canonical engine has no DEPLOYED → DRAFT transition; undeploy writes DISABLED.
    const undeploy = async () => {
        setDeploying(true);
        await runSubmit('DISABLED', 'Policy undeployed. Gateway sync will drop it within 30s.');
        setDeploying(false);
    };

    const submit = async () => {
        setSubmitting(true);
        await runSubmit(undefined, null);
        setSubmitting(false);
    };

    const createAndDeploy = async () => {
        setSubmitting(true);
        await runSubmit('DEPLOYED', 'Policy created and deployed. Gateway sync expected within 30s.');
        setSubmitting(false);
    };

    const lastEdited = (() => {
        if (!policy?.updatedAt) return '';
        const rel = relativeTime(policy.updatedAt);
        return rel === '—' ? '' : ` · last edited ${rel}`;
    })();

    const headerTitle = (
        <div className="min-w-0">
            <div className="flex items-center gap-2">
                <Input
                    value={name}
                    onChange={e => setName(e.target.value)}
                    placeholder="Policy name"
                    className="h-8 min-w-56 max-w-lg border-transparent bg-transparent px-1 text-base font-semibold shadow-none focus-visible:border-input"
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

    const deployedLabel = policy?.status === 'DEPLOYED' ? `Deployed (${relativeTime(policy.updatedAt)})` : null;
    const deployDisabled = deploying || submitting || name.trim() === '' || policy === null || policy.status === 'DEPLOYED';

    const headerActions = (
        <>
            <ViewToggle
                view={view}
                onVisual={switchToVisual}
                onCode={switchToCode}
                visualDisabled={visualUnavailable}
                visualDisabledTitle="This policy uses GAPL features that the visual editor cannot represent. Use the Code view to edit it directly."
            />
            <Button type="button" size="sm" onClick={deploy} disabled={deployDisabled}>
                {deploying ? <Spinner className="size-3.5 mr-1.5" /> : null}
                {deploying ? 'Deploying…' : (deployedLabel ?? 'Deploy to PDP')}
            </Button>
            {policy?.status === 'DEPLOYED' ? (
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
            ) : null}
        </>
    );

    const subHeader = (
        <div className="shrink-0 border-b px-6 py-2">
            <Input
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="Description (optional)"
                className="h-8 border-transparent bg-transparent px-1 text-sm shadow-none focus-visible:border-input"
                aria-label="Policy description"
            />
        </div>
    );

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
                    {submitting ? 'Saving…' : policy ? 'Update policy' : 'Create policy'}
                </Button>
                {policy ? null : (
                    <Button type="button" onClick={createAndDeploy} disabled={submitting || deploying || name.trim() === ''}>
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
                            statements={statements}
                            principalOptions={principalOptions}
                            actionOptions={actionOptions}
                            resourceOptions={resourceOptions}
                            agentOptions={agentOptions}
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
            value={view}
            onValueChange={v => {
                if (v === 'visual') onVisual();
                else if (v === 'code') onCode();
            }}
            aria-label="View mode"
            size="sm"
        >
            <ToggleGroupItem value="visual" disabled={visualDisabled} title={visualDisabled ? visualDisabledTitle : undefined}>
                <ListIcon className="size-3.5" aria-hidden />
                Visual
            </ToggleGroupItem>
            <ToggleGroupItem value="code">
                <Code2 className="size-3.5" aria-hidden />
                Code
            </ToggleGroupItem>
        </ToggleGroup>
    );
}

function CodePane({ code, onCodeChange }: { code: string; onCodeChange: (next: string) => void }) {
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
