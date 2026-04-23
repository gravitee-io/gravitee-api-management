import { Alert, AlertDescription, Card, CardContent, cn } from '@gravitee/graphene-core';
import { CheckCircle2, Info, KeyRound, Lock, Shield, ShieldCheck, Sparkles, Unlock } from 'lucide-react';

import type { SecurityModel } from '../apiProxyWizardModels';

type Props = {
    readonly value: SecurityModel;
    readonly onChange: (next: SecurityModel) => void;
};

type JwtSecurityModel = Extract<SecurityModel, { type: 'jwt' }>;

type SecurityOption = {
    readonly id: SecurityModel['type'];
    readonly label: string;
    readonly description: string;
    readonly icon: React.ComponentType<{ className?: string }>;
    readonly colorClassName: string;
};

const SECURITY_OPTIONS: readonly SecurityOption[] = [
    {
        id: 'keyless',
        label: 'Keyless (Open)',
        description: 'No authentication required. Any consumer can call this API.',
        icon: Unlock,
        colorClassName: 'text-emerald-500',
    },
    {
        id: 'api_key',
        label: 'API Key',
        description: 'Consumers must include a valid API key in requests.',
        icon: KeyRound,
        colorClassName: 'text-blue-500',
    },
    {
        id: 'jwt',
        label: 'JWT',
        description: 'Validate JSON Web Tokens from your identity provider.',
        icon: ShieldCheck,
        colorClassName: 'text-violet-500',
    },
    {
        id: 'oauth2',
        label: 'OAuth 2.0',
        description: 'Enforce OAuth 2.0 access tokens for enterprise security.',
        icon: Lock,
        colorClassName: 'text-amber-500',
    },
    {
        id: 'mtls',
        label: 'mTLS',
        description: 'Mutual TLS based on the client X.509 certificate.',
        icon: Shield,
        colorClassName: 'text-rose-500',
    },
] as const;

const JWT_SIGNATURES = [
    { value: 'RS256', label: 'RS256 (RSA + SHA-256)' },
    { value: 'RS384', label: 'RS384 (RSA + SHA-384)' },
    { value: 'RS512', label: 'RS512 (RSA + SHA-512)' },
    { value: 'HS256', label: 'HS256 (HMAC + SHA-256)' },
    { value: 'HS384', label: 'HS384 (HMAC + SHA-384)' },
    { value: 'HS512', label: 'HS512 (HMAC + SHA-512)' },
] as const;

const JWKS_RESOLVERS = [
    { value: 'GIVEN_KEY', label: 'Given key (PEM, single key)' },
    { value: 'GATEWAY_KEYS', label: 'Gateway keys (configured globally)' },
    { value: 'JWKS_URL', label: 'JWKS URL' },
] as const;

const OAUTH2_RESOURCES = [
    { value: 'generic-oauth2', label: 'Generic OAuth2 Resource' },
    { value: 'am-oauth2', label: 'Gravitee Access Management' },
    { value: 'keycloak-oauth2', label: 'Keycloak Adapter' },
] as const;

export function SecureStep({ value, onChange }: Props) {
    const selectedType = value.type;

    const selectType = (type: SecurityModel['type']) => {
        switch (type) {
            case 'keyless':
                onChange({ type: 'keyless' });
                return;
            case 'api_key':
                onChange({ type: 'api_key', planName: 'Default API Key plan' });
                return;
            case 'jwt':
                onChange({
                    type: 'jwt',
                    planName: 'Default JWT plan',
                    signature: 'RS256',
                    jwksResolver: 'JWKS_URL',
                    resolverParameter: '',
                });
                return;
            case 'oauth2':
                onChange({ type: 'oauth2', planName: 'Default OAuth2 plan', resource: '' });
                return;
            case 'mtls':
                onChange({ type: 'mtls', planName: 'Default mTLS plan' });
                return;
        }
    };

    return (
        <div className="space-y-6">
            <Alert className="rounded-xl border p-5 bg-muted/30">
                <Info className="size-4 shrink-0 text-blue-500 mt-0.5" aria-hidden />
                <AlertDescription className="text-muted-foreground text-sm">
                    Choose how consumers authenticate when calling your API. You can always add or change plans later. For getting started, Keyless is the
                    simplest option.
                </AlertDescription>
            </Alert>

            <Card className="rounded-xl border">
                <CardContent className="p-6 space-y-4">
                    <div className="flex items-center gap-2">
                        <Shield className="size-4 text-primary" aria-hidden />
                        <div className="text-base font-semibold">Consumer Authentication</div>
                    </div>
                    <div className="text-xs text-muted-foreground">Pick a security type to create the API’s default plan.</div>

                    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                        {SECURITY_OPTIONS.map(opt => {
                            const Icon = opt.icon;
                            const selected = selectedType === opt.id;

                            return (
                                <button
                                    key={opt.id}
                                    type="button"
                                    onClick={() => selectType(opt.id)}
                                    className={cn(
                                        'flex items-start gap-3 rounded-xl border-2 p-4 text-left transition-all',
                                        selected
                                            ? 'border-primary bg-primary/5 ring-1 ring-primary/20'
                                            : 'border-border hover:border-foreground/20 hover:shadow-sm',
                                    )}
                                >
                                    <div
                                        className={cn(
                                            'size-10 rounded-lg flex items-center justify-center shrink-0',
                                            selected ? 'bg-primary/15' : 'bg-muted',
                                        )}
                                    >
                                        <Icon className={cn('size-5', selected ? 'text-primary' : opt.colorClassName)} aria-hidden />
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 flex-wrap">
                                            <span className="text-sm font-medium">{opt.label}</span>
                                            {opt.id === 'keyless' ? (
                                                <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] text-muted-foreground">Default</span>
                                            ) : null}
                                        </div>
                                        <p className="text-xs text-muted-foreground mt-1">{opt.description}</p>
                                    </div>
                                    {selected ? <CheckCircle2 className="size-5 text-primary shrink-0 mt-0.5" aria-hidden /> : null}
                                </button>
                            );
                        })}
                    </div>

                    {value.type === 'keyless' ? (
                        <div className="flex items-start gap-3 rounded-lg border border-emerald-500/20 bg-emerald-500/5 px-4 py-3">
                            <Sparkles className="size-4 shrink-0 text-emerald-500 mt-0.5" aria-hidden />
                            <p className="text-xs text-muted-foreground leading-relaxed">
                                A Keyless plan is open and requires no consumer authentication. No additional configuration is needed.
                            </p>
                        </div>
                    ) : null}

                    {value.type === 'api_key' ? (
                        <div className="rounded-lg border bg-muted/20 p-4 space-y-2">
                            <p className="text-sm font-medium">API Key Plan</p>
                            <label className="text-sm font-medium">
                                Plan name <span className="text-destructive">*</span>
                            </label>
                            <input
                                value={value.planName}
                                onChange={e => onChange({ ...value, planName: e.target.value })}
                                placeholder="e.g. Default API Key plan"
                                className={cn(
                                    'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none',
                                    'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                )}
                            />
                            <p className="text-xs text-muted-foreground">Name shown to consumers when they subscribe to the plan.</p>
                        </div>
                    ) : null}

                    {value.type === 'jwt' ? (
                        <div className="rounded-lg border bg-muted/20 p-4 space-y-4">
                            <p className="text-sm font-medium">JWT Plan</p>
                            <div className="grid gap-4 sm:grid-cols-2">
                                <div className="space-y-2">
                                    <label className="text-sm font-medium">
                                        Plan name <span className="text-destructive">*</span>
                                    </label>
                                    <input
                                        value={value.planName}
                                        onChange={e => onChange({ ...value, planName: e.target.value })}
                                        className={cn(
                                            'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none',
                                            'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                        )}
                                    />
                                </div>

                                <div className="space-y-2">
                                    <label className="text-sm font-medium">
                                        Signature <span className="text-destructive">*</span>
                                    </label>
                                    <select
                                        value={value.signature}
                                        onChange={e => onChange({ ...value, signature: e.target.value })}
                                        className={cn(
                                            'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none',
                                            'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                        )}
                                    >
                                        {JWT_SIGNATURES.map(sig => (
                                            <option key={sig.value} value={sig.value}>
                                                {sig.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="space-y-2">
                                    <label className="text-sm font-medium">
                                        JWKS Resolver <span className="text-destructive">*</span>
                                    </label>
                                    <select
                                        value={value.jwksResolver}
                                        onChange={e =>
                                            onChange({
                                                ...value,
                                                jwksResolver: e.target.value as JwtSecurityModel['jwksResolver'],
                                            })
                                        }
                                        className={cn(
                                            'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none',
                                            'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                        )}
                                    >
                                        {JWKS_RESOLVERS.map(r => (
                                            <option key={r.value} value={r.value}>
                                                {r.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="space-y-2">
                                    <label className="text-sm font-medium">
                                        Resolver parameter <span className="text-destructive">*</span>
                                    </label>
                                    <input
                                        value={value.resolverParameter}
                                        onChange={e => onChange({ ...value, resolverParameter: e.target.value })}
                                        placeholder={
                                            value.jwksResolver === 'JWKS_URL'
                                                ? 'https://idp.example.com/.well-known/jwks.json'
                                                : value.jwksResolver === 'GIVEN_KEY'
                                                  ? '-----BEGIN PUBLIC KEY-----...'
                                                  : 'Configured globally'
                                        }
                                        className={cn(
                                            'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none font-mono text-xs',
                                            'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                        )}
                                    />
                                </div>
                            </div>
                        </div>
                    ) : null}

                    {value.type === 'oauth2' ? (
                        <div className="rounded-lg border bg-muted/20 p-4 space-y-4">
                            <p className="text-sm font-medium">OAuth 2.0 Plan</p>
                            <div className="grid gap-4 sm:grid-cols-2">
                                <div className="space-y-2">
                                    <label className="text-sm font-medium">
                                        Plan name <span className="text-destructive">*</span>
                                    </label>
                                    <input
                                        value={value.planName}
                                        onChange={e => onChange({ ...value, planName: e.target.value })}
                                        className={cn(
                                            'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none',
                                            'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                        )}
                                    />
                                </div>
                                <div className="space-y-2">
                                    <label className="text-sm font-medium">
                                        OAuth2 Resource <span className="text-destructive">*</span>
                                    </label>
                                    <select
                                        value={value.resource}
                                        onChange={e => onChange({ ...value, resource: e.target.value })}
                                        className={cn(
                                            'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none',
                                            'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                        )}
                                    >
                                        <option value="" disabled>
                                            Select an OAuth2 resource
                                        </option>
                                        {OAUTH2_RESOURCES.map(r => (
                                            <option key={r.value} value={r.value}>
                                                {r.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            </div>
                        </div>
                    ) : null}

                    {value.type === 'mtls' ? (
                        <div className="rounded-lg border bg-muted/20 p-4 space-y-2">
                            <p className="text-sm font-medium">Mutual TLS Plan</p>
                            <label className="text-sm font-medium">
                                Plan name <span className="text-destructive">*</span>
                            </label>
                            <input
                                value={value.planName}
                                onChange={e => onChange({ ...value, planName: e.target.value })}
                                className={cn(
                                    'h-9 w-full rounded-md border bg-background px-3 text-sm outline-none',
                                    'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                )}
                            />
                            <p className="text-xs text-muted-foreground">
                                Consumers are identified by the X.509 certificate presented during the TLS handshake.
                            </p>
                        </div>
                    ) : null}
                </CardContent>
            </Card>
        </div>
    );
}

