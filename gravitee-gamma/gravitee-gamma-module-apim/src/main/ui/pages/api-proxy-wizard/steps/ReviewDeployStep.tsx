import { Alert, AlertDescription, Button, Card, CardContent } from '@gravitee/graphene-core';
import { ClipboardCheck, Globe, Info, Pencil, Rocket, Shield } from 'lucide-react';

import type { ApiProxyWizardModel, SecurityModel } from '../apiProxyWizardModels';

type Props = {
    readonly model: ApiProxyWizardModel;
    readonly onEditStep: (stepIndex: number) => void;
    readonly onToggleDeployImmediately: (value: boolean) => void;
    readonly onDeploy: () => void;
};

export function ReviewDeployStep({ model, onEditStep, onToggleDeployImmediately, onDeploy }: Props) {
    return (
        <div className="space-y-6">
            <Alert className="rounded-xl border p-5 bg-muted/30">
                <Info className="size-4 shrink-0 text-blue-500 mt-0.5" aria-hidden />
                <AlertDescription className="text-muted-foreground text-sm">Review the configuration below before deploying.</AlertDescription>
            </Alert>

            <Card className="rounded-xl border">
                <CardContent className="p-6 space-y-5">
                    <div className="flex items-center gap-2">
                        <ClipboardCheck className="size-5 text-primary" aria-hidden />
                        <div className="text-lg font-semibold">Review Configuration</div>
                    </div>

                    <Section
                        title="API Details"
                        icon={<ClipboardCheck className="size-4 text-muted-foreground" aria-hidden />}
                        editLabel="Edit"
                        onEdit={() => onEditStep(0)}
                    >
                        <div className="grid gap-3 text-sm sm:grid-cols-3">
                            <Field label="Name" value={model.details.name || '—'} />
                            <Field label="Version" value={model.details.version || '—'} />
                            <div>
                                <p className="text-xs text-muted-foreground mb-1">Protocol</p>
                                <span className="inline-flex items-center rounded px-2 py-0.5 text-xs font-medium bg-muted text-muted-foreground">REST</span>
                            </div>
                            {model.details.description ? (
                                <div className="sm:col-span-3">
                                    <p className="text-xs text-muted-foreground mb-1">Description</p>
                                    <p className="text-sm">{model.details.description}</p>
                                </div>
                            ) : null}
                        </div>
                    </Section>

                    <Section
                        title="Proxy Configuration"
                        icon={<Globe className="size-4 text-muted-foreground" aria-hidden />}
                        editLabel="Edit"
                        onEdit={() => onEditStep(1)}
                    >
                        <div className="grid gap-4 sm:grid-cols-2">
                            <div>
                                <p className="text-xs text-muted-foreground mb-1">Gateway URL</p>
                                <code className="text-sm font-mono bg-muted px-2 py-1 rounded block truncate">{previewGatewayUrl(model.proxy.entrypoints)}</code>
                            </div>
                            <div>
                                <p className="text-xs text-muted-foreground mb-1">Upstream URL</p>
                                <code className="text-sm font-mono bg-muted px-2 py-1 rounded block truncate">{model.proxy.upstreamUrl || '—'}</code>
                            </div>
                        </div>
                    </Section>

                    <Section
                        title="Security"
                        icon={<Shield className="size-4 text-muted-foreground" aria-hidden />}
                        editLabel="Edit"
                        onEdit={() => onEditStep(2)}
                    >
                        <SecuritySummary security={model.security} />
                    </Section>
                </CardContent>
            </Card>

            <Card className="rounded-xl border-emerald-500/20 bg-emerald-500/5">
                <CardContent className="p-6">
                    <div className="flex items-center justify-between gap-4">
                        <div className="flex items-center gap-3">
                            <div className="size-10 rounded-lg bg-emerald-500/10 flex items-center justify-center">
                                <Rocket className="size-5 text-emerald-500" aria-hidden />
                            </div>
                            <div>
                                <p className="text-sm font-semibold">Deploy and start API immediately</p>
                                <p className="text-xs text-muted-foreground">
                                    When enabled, the API proxy will be deployed to the gateway and start accepting traffic right away.
                                </p>
                            </div>
                        </div>
                        <label className="inline-flex items-center gap-2 text-sm">
                            <input
                                type="checkbox"
                                checked={model.deployment.deployImmediately}
                                onChange={e => onToggleDeployImmediately(e.target.checked)}
                                className="size-4 rounded border"
                            />
                            <span className="text-muted-foreground">Enabled</span>
                        </label>
                    </div>
                </CardContent>
            </Card>

            <div className="flex justify-end">
                <Button type="button" onClick={onDeploy}>
                    <Rocket className="mr-2 size-4" aria-hidden />
                    {model.deployment.deployImmediately ? 'Deploy Proxy' : 'Create Proxy'}
                </Button>
            </div>
        </div>
    );
}

function Section(props: {
    readonly title: string;
    readonly icon: React.ReactNode;
    readonly editLabel: string;
    readonly onEdit: () => void;
    readonly children: React.ReactNode;
}) {
    return (
        <div className="rounded-lg border">
            <div className="flex items-center justify-between border-b px-4 py-3 bg-muted/30">
                <div className="flex items-center gap-2">
                    {props.icon}
                    <p className="text-sm font-medium">{props.title}</p>
                </div>
                <Button variant="ghost" size="sm" className="h-7 text-xs" onClick={props.onEdit}>
                    <Pencil className="mr-1.5 size-3" aria-hidden />
                    {props.editLabel}
                </Button>
            </div>
            <div className="p-4 space-y-3">{props.children}</div>
        </div>
    );
}

function Field({ label, value }: { readonly label: string; readonly value: string }) {
    return (
        <div>
            <p className="text-xs text-muted-foreground mb-1">{label}</p>
            <p className="font-medium">{value}</p>
        </div>
    );
}

function SecuritySummary({ security }: { readonly security: SecurityModel }) {
    return (
        <div className="space-y-2 text-xs text-muted-foreground">
            <div>
                <p className="text-xs text-muted-foreground mb-1">Authentication</p>
                <span className="inline-flex items-center rounded px-2 py-0.5 text-xs font-medium bg-muted text-muted-foreground">
                    {securityLabel(security)}
                </span>
            </div>

            {security.type === 'api_key' ? (
                <div>
                    Plan name: <code className="bg-muted px-1 rounded">{security.planName}</code>
                </div>
            ) : null}

            {security.type === 'jwt' ? (
                <div className="space-y-1">
                    <div>
                        Plan name: <code className="bg-muted px-1 rounded">{security.planName}</code>
                    </div>
                    <div>
                        Signature: <code className="bg-muted px-1 rounded">{security.signature}</code> · Resolver:{' '}
                        <code className="bg-muted px-1 rounded">{security.jwksResolver}</code>
                    </div>
                    {security.resolverParameter ? (
                        <div className="truncate">
                            Resolver parameter: <code className="bg-muted px-1 rounded">{security.resolverParameter}</code>
                        </div>
                    ) : null}
                </div>
            ) : null}

            {security.type === 'oauth2' ? (
                <div className="space-y-1">
                    <div>
                        Plan name: <code className="bg-muted px-1 rounded">{security.planName}</code>
                    </div>
                    <div>
                        Resource: <code className="bg-muted px-1 rounded">{security.resource || '—'}</code>
                    </div>
                </div>
            ) : null}

            {security.type === 'mtls' ? (
                <div>
                    Plan name: <code className="bg-muted px-1 rounded">{security.planName}</code>
                </div>
            ) : null}
        </div>
    );
}

function securityLabel(security: SecurityModel): string {
    switch (security.type) {
        case 'keyless':
            return 'Keyless (Open)';
        case 'api_key':
            return 'API Key';
        case 'jwt':
            return 'JWT';
        case 'oauth2':
            return 'OAuth 2.0';
        case 'mtls':
            return 'mTLS';
    }
}

function previewGatewayUrl(entrypoints: ApiProxyWizardModel['proxy']['entrypoints']): string {
    if (entrypoints.type === 'virtual_hosts') {
        const first = entrypoints.virtualHosts[0];
        const host = first?.host?.trim() || 'your-host.example.com';
        const path = first?.path?.trim() || '/';
        return `https://${host}${path === '/' ? '' : path}`;
    }
    const path = entrypoints.contextPath.trim() || '/...';
    return `https://gateway.company.com${path}`;
}

