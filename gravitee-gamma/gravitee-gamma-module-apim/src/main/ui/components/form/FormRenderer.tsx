import {
    Badge,
    Field,
    FieldContent,
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Switch,
    Textarea,
} from '@gravitee/graphene-core';
import { CircleAlert, CircleCheck, Key, Lock, LockOpen, ShieldCheck, Sparkles } from 'lucide-react';
import { useMemo } from 'react';
import type React from 'react';

import type { ApiCreationState } from '../../domain/apiCreation/models';
import type { FieldConfig } from '../../domain/apiCreation/fieldRegistry';
import { usePolicySchema } from '../../pages/create-proxy/apiCreation/queries/usePolicySchema';

export type FormRendererProps = Readonly<{
    fields: readonly FieldConfig[];
    state: ApiCreationState;
    getValue: (path: string) => unknown;
    updateField: (path: string, value: unknown) => void;
    errors: Record<string, string>;
}>;

type AuthTypeValue = 'keyless' | 'api-key' | 'jwt' | 'oauth2' | 'mtls';

type AuthTypeSelectorProps = Readonly<{
    value: AuthTypeValue;
    onChange: (value: AuthTypeValue) => void;
}>;

const AUTH_TYPE_DEFINITIONS: {
    readonly value: AuthTypeValue;
    readonly label: string;
    readonly description: string;
    readonly icon: React.ComponentType<React.SVGProps<SVGSVGElement>>;
    readonly iconBgClass: string;
    readonly iconColorClass: string;
    readonly showDefaultBadge?: boolean;
}[] = [
    {
        value: 'keyless',
        label: 'Keyless (Open)',
        description: 'No authentication required. Any consumer can call this API.',
        icon: LockOpen,
        iconBgClass: 'bg-muted',
        iconColorClass: 'text-emerald-500',
        showDefaultBadge: true,
    },
    {
        value: 'api-key',
        label: 'API Key',
        description: 'Consumers must include a valid API key in requests.',
        icon: Key,
        iconBgClass: 'bg-muted',
        iconColorClass: 'text-blue-500',
    },
    {
        value: 'jwt',
        label: 'JWT',
        description: 'Validate JSON Web Tokens from your identity provider.',
        icon: ShieldCheck,
        iconBgClass: 'bg-muted',
        iconColorClass: 'text-purple-500',
    },
    {
        value: 'oauth2',
        label: 'OAuth 2.0',
        description: 'Enforce OAuth 2.0 access tokens for enterprise security.',
        icon: Lock,
        iconBgClass: 'bg-muted',
        iconColorClass: 'text-amber-500',
    },
    {
        value: 'mtls',
        label: 'mTLS',
        description: 'Mutual TLS based on the client X.509 certificate.',
        icon: ShieldCheck,
        iconBgClass: 'bg-muted',
        iconColorClass: 'text-rose-500',
    },
];

function AuthTypeSelector({ value, onChange }: AuthTypeSelectorProps) {
    return (
        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {AUTH_TYPE_DEFINITIONS.map((option) => {
                const isSelected = option.value === value;
                const Icon = option.icon;

                return (
                    <button
                        key={option.value}
                        type="button"
                        aria-pressed={isSelected}
                        className={`flex h-full w-full items-start gap-3 rounded-xl border-2 p-4 text-left transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
                            isSelected
                                ? 'border-primary bg-primary/5 ring-1 ring-primary/20'
                                : 'border-border hover:border-foreground/20 hover:shadow-sm'
                        }`}
                        onClick={() => onChange(option.value)}
                    >
                        <div className={`flex size-10 shrink-0 items-center justify-center rounded-lg ${option.iconBgClass}`}>
                            <Icon className={`size-5 ${option.iconColorClass}`} aria-hidden="true" />
                        </div>

                        <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-2">
                                <span className="text-sm font-medium">{option.label}</span>
                                {option.showDefaultBadge ? (
                                    <Badge variant="secondary" className="border-transparent bg-secondary px-2 py-0.5 text-[10px]">
                                        Default
                                    </Badge>
                                ) : null}
                            </div>
                            <p className="mt-1 text-xs text-muted-foreground">{option.description}</p>
                        </div>

                        {isSelected ? (
                            <CircleCheck className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden="true" />
                        ) : null}
                    </button>
                );
            })}
        </div>
    );
}

type SecureStepRendererProps = Readonly<{
    fields: readonly FieldConfig[];
    state: ApiCreationState;
    getValue: (path: string) => unknown;
    updateField: (path: string, value: unknown) => void;
    errors: Record<string, string>;
}>;

function SecureStepRenderer({ fields, state, getValue, updateField, errors }: SecureStepRendererProps) {
    const visibleFields = useMemo(
        () => fields.filter((f) => (f.visible ? f.visible(state) : true)),
        [fields, state],
    );
    const byId: Record<string, FieldConfig> = Object.fromEntries(visibleFields.map((f) => [f.id, f]));
    const authField = byId['authType'];
    if (!authField) return null;

    const rawAuthValue = getValue(authField.bind);
    const authType: AuthTypeValue = (typeof rawAuthValue === 'string' && rawAuthValue ? rawAuthValue : 'keyless') as AuthTypeValue;
    const authError = errors[authField.bind];
    const authFieldErrors = authError ? [{ message: authError }] : undefined;

    const otherFields = visibleFields.filter((f) => f.id !== 'authType');

    const jwtPlanFieldIds: readonly string[] = ['planName', 'jwtSignature', 'jwtJwksResolver', 'jwtResolverParam'];
    const oauth2PlanFieldIds: readonly string[] = ['planName', 'oauth2Resource'];
    const mtlsPlanFieldIds: readonly string[] = ['planName'];

    const jwtSchema = usePolicySchema('jwt', authType === 'jwt');
    const oauth2Schema = usePolicySchema('oauth2', authType === 'oauth2');

    const readSchemaSelect = (
        schema: unknown,
        propName: string,
    ): { readonly options: readonly { value: string; label: string }[]; readonly defaultValue?: string } | null => {
        if (!schema || typeof schema !== 'object') return null;
        const properties = (schema as { properties?: unknown }).properties;
        if (!properties || typeof properties !== 'object') return null;
        const prop = (properties as Record<string, unknown>)[propName];
        if (!prop || typeof prop !== 'object') return null;
        const anyProp = prop as {
            enum?: unknown;
            default?: unknown;
            ['x-schema-form']?: unknown;
        };
        const enumValues = Array.isArray(anyProp.enum) ? anyProp.enum.filter((v): v is string => typeof v === 'string') : [];
        if (!enumValues.length) return null;
        const xForm = anyProp['x-schema-form'];
        const titleMap =
            xForm && typeof xForm === 'object' && 'titleMap' in xForm && xForm.titleMap && typeof xForm.titleMap === 'object'
                ? (xForm.titleMap as Record<string, unknown>)
                : undefined;
        const options = enumValues.map((v) => ({
            value: v,
            label: typeof titleMap?.[v] === 'string' ? (titleMap[v] as string) : v,
        }));
        const defaultValue = typeof anyProp.default === 'string' ? anyProp.default : undefined;
        return { options, defaultValue };
    };

    const jwtSignatureOptions = readSchemaSelect(jwtSchema.data, 'signature');
    const jwtResolverOptionsRaw = readSchemaSelect(jwtSchema.data, 'publicKeyResolver');
    const jwtResolverOptions = jwtResolverOptionsRaw
        ? {
              ...jwtResolverOptionsRaw,
              options: jwtResolverOptionsRaw.options.map((opt) => {
                  if (opt.value === 'GIVEN_KEY') return { value: opt.value, label: 'Given key (PEM, single key)' };
                  if (opt.value === 'GATEWAY_KEYS') return { value: opt.value, label: 'Gateway keys (configured globally)' };
                  if (opt.value === 'JWKS_URL') return { value: opt.value, label: 'JWKS URL' };
                  return opt;
              }),
          }
        : null;

    const oauth2ResourceOptions =
        readSchemaSelect(oauth2Schema.data, 'oauthResource') ?? readSchemaSelect(oauth2Schema.data, 'resource');

    const oauth2FallbackResourceOptions: readonly { value: string; label: string }[] = [
        { value: 'Generic OAuth2 Resource', label: 'Generic OAuth2 Resource' },
        { value: 'Gravitee Access Management', label: 'Gravitee Access Management' },
        { value: 'Keycloak Adapter', label: 'Keycloak Adapter' },
    ];

    const renderControl = (
        field: FieldConfig,
        value: unknown,
        error: string | undefined,
        overrides?: Readonly<{
            placeholder?: string;
            selectPlaceholder?: string;
            forceOptions?: readonly { value: string; label: string }[];
        }>,
    ) => {
        const current = typeof value === 'string' ? value : '';

        if (authType === 'jwt' && field.id === 'jwtSignature' && jwtSignatureOptions) {
            return (
                <Select value={current || jwtSignatureOptions.defaultValue || ''} onValueChange={(v) => updateField(field.bind, v)}>
                    <SelectTrigger id={field.id} aria-invalid={Boolean(error)} className="w-full">
                        <SelectValue placeholder="Select…" />
                    </SelectTrigger>
                    <SelectContent>
                        {jwtSignatureOptions.options.map((opt) => (
                            <SelectItem key={opt.value} value={opt.value}>
                                {opt.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            );
        }

        if (authType === 'jwt' && field.id === 'jwtJwksResolver' && jwtResolverOptions) {
            return (
                <Select value={current || jwtResolverOptions.defaultValue || ''} onValueChange={(v) => updateField(field.bind, v)}>
                    <SelectTrigger id={field.id} aria-invalid={Boolean(error)} className="w-full">
                        <SelectValue placeholder="Select…" />
                    </SelectTrigger>
                    <SelectContent>
                        {jwtResolverOptions.options.map((opt) => (
                            <SelectItem key={opt.value} value={opt.value}>
                                {opt.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            );
        }

        if (authType === 'oauth2' && field.id === 'oauth2Resource') {
            const options = overrides?.forceOptions ?? oauth2ResourceOptions?.options ?? oauth2FallbackResourceOptions;
            const defaultValue = oauth2ResourceOptions?.defaultValue;
            return (
                <Select value={current || defaultValue || ''} onValueChange={(v) => updateField(field.bind, v)}>
                    <SelectTrigger id={field.id} aria-invalid={Boolean(error)} className="w-full">
                        <SelectValue placeholder={overrides?.selectPlaceholder ?? 'Select…'} />
                    </SelectTrigger>
                    <SelectContent>
                        {options.map((opt) => (
                            <SelectItem key={opt.value} value={opt.value}>
                                {opt.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
            );
        }

        return (
            <Input
                id={field.id}
                type={field.inputType ?? 'text'}
                pattern={field.pattern}
                title={field.validationTitle}
                placeholder={
                    overrides?.placeholder ??
                    (field.id === 'jwtResolverParam' ? 'https://idp.example.com/.well-known/jwks.json' : undefined)
                }
                value={current}
                aria-invalid={Boolean(error)}
                onChange={(e) => updateField(field.bind, e.target.value)}
            />
        );
    };

    const renderConfigField = (
        field: FieldConfig,
        descriptionPlacement: 'above' | 'below' = 'above',
        overrides?: Readonly<{
            label?: string;
            description?: string;
            placeholder?: string;
            selectPlaceholder?: string;
            forceOptions?: readonly { value: string; label: string }[];
        }>,
    ) => {
        const value = getValue(field.bind);
        const error = errors[field.bind];
        const fieldErrors = error ? [{ message: error }] : undefined;
        const label =
            overrides?.label ??
            (field.id === 'planName' && authType === 'jwt' ? 'JWT Plan Name' : field.label);
        const description = overrides?.description ?? field.description;

        return (
            <Field key={field.id} orientation="vertical">
                <FieldLabel htmlFor={field.id}>
                    {label}
                    {field.required ? <span className="ml-1 text-destructive">*</span> : null}
                </FieldLabel>
                {descriptionPlacement === 'above' && description ? <FieldDescription>{description}</FieldDescription> : null}
                <FieldContent>{renderControl(field, value, error, overrides)}</FieldContent>
                {descriptionPlacement === 'below' && description ? (
                    <p className="mt-1 text-xs text-muted-foreground">{description}</p>
                ) : null}
                <FieldError errors={fieldErrors} />
            </Field>
        );
    };

    const onChangeAuthType = (next: AuthTypeValue) => {
        updateField(authField.bind, next);

        if (next === 'api-key') {
            const currentPlanName = getValue('security.planName');
            if (typeof currentPlanName !== 'string' || currentPlanName.trim() === '') {
                updateField('security.planName', 'Default API Key plan');
            }
        }

        if (next === 'jwt') {
            const currentPlanName = getValue('security.planName');
            if (typeof currentPlanName !== 'string' || currentPlanName.trim() === '') {
                updateField('security.planName', 'Default JWT plan');
            }

            const currentSignature = getValue('security.signature');
            if (typeof currentSignature !== 'string' || currentSignature.trim() === '') {
                updateField('security.signature', jwtSignatureOptions?.defaultValue ?? 'RSA_RS256');
            }

            const currentResolver = getValue('security.jwksResolver');
            if (typeof currentResolver !== 'string' || currentResolver.trim() === '') {
                updateField('security.jwksResolver', jwtResolverOptions?.defaultValue ?? 'JWKS_URL');
            }
        }

        if (next === 'mtls') {
            const currentPlanName = getValue('security.planName');
            if (typeof currentPlanName !== 'string' || currentPlanName.trim() === '') {
                updateField('security.planName', 'Default mTLS plan');
            }
        }
    };

    return (
        <div className="space-y-6">
            <Field orientation="vertical">
                <FieldLabel htmlFor={authField.id}>
                    {authField.label}
                    {authField.required ? <span className="ml-1 text-destructive">*</span> : null}
                </FieldLabel>
                {authField.description ? <FieldDescription>{authField.description}</FieldDescription> : null}
                <FieldContent>
                    <AuthTypeSelector value={authType} onChange={onChangeAuthType} />
                </FieldContent>
                <FieldError errors={authFieldErrors} />
            </Field>

            {authType === 'keyless' ? (
                <div className="rounded-lg border border-destructive/25 bg-destructive/5 p-3">
                    <div className="flex items-start gap-2">
                        <Sparkles className="mt-0.5 size-4 text-destructive" aria-hidden="true" />
                        <div className="space-y-1">
                            <p className="text-sm font-medium text-foreground">Keyless (Open)</p>
                            <p className="text-xs text-muted-foreground leading-relaxed">
                                A Keyless plan is open and requires no consumer authentication. No additional configuration is needed.
                            </p>
                        </div>
                    </div>
                </div>
            ) : null}

            {authType === 'api-key' ? (
                <div className="space-y-4 rounded-xl border bg-card/40 p-4 sm:p-5">
                    <div className="text-sm font-semibold text-foreground">API Key Plan</div>
                    {byId['planName'] ? renderConfigField(byId['planName'], 'below') : null}
                </div>
            ) : null}

            {authType === 'jwt' ? (
                <div className="space-y-4 rounded-xl border bg-card/40 p-4 sm:p-5">
                    <div className="text-sm font-semibold text-foreground">JWT Plan</div>
                    <div className="grid gap-4 md:grid-cols-2">
                        {jwtPlanFieldIds.map((id) => {
                            const field = byId[id];
                            if (!field) return null;
                            return renderConfigField(field, 'below');
                        })}
                    </div>
                </div>
            ) : null}

            {authType === 'oauth2' ? (
                <div className="space-y-4 rounded-xl border border-border/60 bg-card/40 p-4 sm:p-5">
                    <div className="text-sm font-semibold text-foreground">OAuth 2.0 Plan</div>
                    <div className="grid gap-4 md:grid-cols-2">
                        {oauth2PlanFieldIds.map((id) => {
                            const field = byId[id];
                            if (!field) return null;
                            if (id === 'planName') {
                                return renderConfigField(field, 'below', {
                                    label: 'OAuth2 Plan Name',
                                    placeholder: 'e.g. Default OAuth2 plan',
                                    description: 'Name shown to consumers when they subscribe.',
                                });
                            }
                            if (id === 'oauth2Resource') {
                                return renderConfigField(field, 'below', {
                                    label: 'OAuth2 Resource',
                                    selectPlaceholder: 'Select an OAuth2 resource',
                                    description: 'OAuth2 resource used to validate token. Supports EL.',
                                    forceOptions: oauth2ResourceOptions?.options?.length ? undefined : oauth2FallbackResourceOptions,
                                });
                            }
                            return renderConfigField(field, 'below');
                        })}
                    </div>
                </div>
            ) : null}

            {authType === 'mtls' ? (
                <div className="space-y-4 rounded-xl border border-border/60 bg-card/40 p-4 sm:p-5">
                    <div className="text-sm font-semibold text-foreground">Mutual TLS Plan</div>
                    <div className="grid gap-4 md:grid-cols-2">
                        {mtlsPlanFieldIds.map((id) => {
                            const field = byId[id];
                            if (!field) return null;
                            return renderConfigField(field, 'below', {
                                label: 'MTLS Plan Name',
                                placeholder: 'e.g. Default mTLS plan',
                                description: 'Consumers are identified by the X.509 certificate presented during the TLS handshake.',
                            });
                        })}
                    </div>
                </div>
            ) : null}

            {authType !== 'jwt' && authType !== 'api-key' && authType !== 'keyless' && authType !== 'oauth2' && authType !== 'mtls' ? (
                <div className="space-y-5">
                    {otherFields.map((field) => {
                        const value = getValue(field.bind);
                        const error = errors[field.bind];
                        const fieldErrors = error ? [{ message: error }] : undefined;

                        return (
                            <Field key={field.id} orientation="vertical">
                                <FieldLabel htmlFor={field.id}>
                                    {field.label}
                                    {field.required ? <span className="ml-1 text-destructive">*</span> : null}
                                </FieldLabel>
                                {field.description ? <FieldDescription>{field.description}</FieldDescription> : null}
                                <FieldContent>
                                    {field.type === 'input' ? (
                                        field.inputKind === 'textarea' ? (
                                            <Textarea
                                                id={field.id}
                                                value={typeof value === 'string' ? value : ''}
                                                aria-invalid={Boolean(error)}
                                                onChange={(e) => updateField(field.bind, e.target.value)}
                                            />
                                        ) : (
                                            renderControl(field, value, error)
                                        )
                                    ) : null}

                                    {field.type === 'select' ? (
                                        <Select
                                            value={typeof value === 'string' ? value : ''}
                                            onValueChange={(v) => updateField(field.bind, v)}
                                        >
                                            <SelectTrigger id={field.id} aria-invalid={Boolean(error)}>
                                                <SelectValue placeholder="Select…" />
                                            </SelectTrigger>
                                            <SelectContent>
                                                {(field.options ?? []).map((opt) => (
                                                    <SelectItem key={opt.value} value={opt.value}>
                                                        {opt.label}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    ) : null}

                                    {field.type === 'switch' ? (
                                        <div className="flex items-center justify-between gap-3 rounded-md border bg-card px-3 py-2">
                                            <div className="min-w-0">
                                                <div className="text-sm font-medium text-foreground">{field.label}</div>
                                                {field.description ? (
                                                    <div className="text-xs text-muted-foreground">{field.description}</div>
                                                ) : null}
                                            </div>
                                            <Switch
                                                id={field.id}
                                                checked={Boolean(value)}
                                                onCheckedChange={(checked) => updateField(field.bind, checked)}
                                            />
                                        </div>
                                    ) : null}
                                </FieldContent>
                                <FieldError errors={fieldErrors} />
                            </Field>
                        );
                    })}
                </div>
            ) : null}
        </div>
    );
}

export function FormRenderer({ fields, state, getValue, updateField, errors }: FormRendererProps) {
    const visibleFields = useMemo(
        () => fields.filter((f) => (f.visible ? f.visible(state) : true)),
        [fields, state],
    );

    const hasAuthField = visibleFields.some((f) => f.id === 'authType');
    if (hasAuthField) {
        return <SecureStepRenderer fields={fields} state={state} getValue={getValue} updateField={updateField} errors={errors} />;
    }

    return (
        <div className="space-y-5">
            {visibleFields.map((field) => {
                const value = getValue(field.bind);
                const error = errors[field.bind];
                const fieldErrors = error ? [{ message: error }] : undefined;

                return (
                    <Field key={field.id} orientation="vertical">
                        <FieldLabel htmlFor={field.id}>
                            {field.label}
                            {field.required ? <span className="ml-1 text-destructive">*</span> : null}
                        </FieldLabel>
                        {field.description ? <FieldDescription>{field.description}</FieldDescription> : null}
                        <FieldContent>
                            {field.type === 'input' ? (
                                field.inputKind === 'textarea' ? (
                                    <Textarea
                                        id={field.id}
                                        value={typeof value === 'string' ? value : ''}
                                        aria-invalid={Boolean(error)}
                                        onChange={(e) => updateField(field.bind, e.target.value)}
                                    />
                                ) : (
                                    <Input
                                        id={field.id}
                                        type={field.inputType ?? 'text'}
                                        pattern={field.pattern}
                                        title={field.validationTitle}
                                        value={typeof value === 'string' ? value : ''}
                                        aria-invalid={Boolean(error)}
                                        onChange={(e) => updateField(field.bind, e.target.value)}
                                    />
                                )
                            ) : null}

                            {field.type === 'select' ? (
                                <Select value={typeof value === 'string' ? value : ''} onValueChange={(v) => updateField(field.bind, v)}>
                                    <SelectTrigger id={field.id} aria-invalid={Boolean(error)}>
                                        <SelectValue placeholder="Select…" />
                                    </SelectTrigger>
                                    <SelectContent>
                                        {(field.options ?? []).map((opt) => (
                                            <SelectItem key={opt.value} value={opt.value}>
                                                {opt.label}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                            ) : null}

                            {field.type === 'switch' ? (
                                <div className="flex items-center justify-between gap-3 rounded-md border bg-card px-3 py-2">
                                    <div className="min-w-0">
                                        <div className="text-sm font-medium text-foreground">{field.label}</div>
                                        {field.description ? <div className="text-xs text-muted-foreground">{field.description}</div> : null}
                                    </div>
                                    <Switch
                                        id={field.id}
                                        checked={Boolean(value)}
                                        onCheckedChange={(checked) => updateField(field.bind, checked)}
                                    />
                                </div>
                            ) : null}
                        </FieldContent>
                        <FieldError errors={fieldErrors} />
                    </Field>
                );
            })}
        </div>
    );
}

