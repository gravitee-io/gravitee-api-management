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
    Alert,
    AlertDescription,
    AlertTitle,
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    Combobox,
    ComboboxContent,
    ComboboxEmpty,
    ComboboxInput,
    ComboboxItem,
    ComboboxList,
    Field,
    FieldLabel,
    Input,
    PasswordInput,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Spinner,
} from '@gravitee/graphene-core';
import { useEffect, useId, useMemo, useRef, useState } from 'react';

import { resolveOrganizationId } from '../../../shared/api/apimClient';
import {
    getAmConnection,
    getDomain,
    isAmUnavailable,
    listDomainEntrypoints,
    listDomains,
    listEnvironments,
    saveAmConnection,
    testAmConnection,
} from '../services/amManagement';
import type { AmConnectionRequest, AmConnectionView, AmDomain, AmEnvironment, AmGatewayEntrypoint } from '../types/amManagement';
import { type AmConfig, loadAmConfig, saveAmConfig } from '../utils/amConfig';

interface Props {
    onSaved: (cfg: AmConfig) => void;
    onCancel?: () => void;
}

interface FormState {
    baseUrl: string;
    accessToken: string;
    hadAccessToken: boolean;
}

const EMPTY_FORM: FormState = {
    baseUrl: '',
    accessToken: '',
    hadAccessToken: false,
};

export function AmConfigPanel({ onSaved, onCancel }: Props) {
    const [cfg, setCfg] = useState<AmConfig>(() => loadAmConfig());
    const [form, setForm] = useState<FormState>(EMPTY_FORM);
    const [loadingConn, setLoadingConn] = useState(true);
    const [envs, setEnvs] = useState<AmEnvironment[] | null>(null);
    const [domains, setDomains] = useState<AmDomain[] | null>(null);
    const [loadingScope, setLoadingScope] = useState(false);
    const [error, setError] = useState<string | null>(null);
    // AM forbids fetching a domain by id, so we persist the hrid and use it as the label on reload.
    const [savedDomain, setSavedDomain] = useState<{ id: string; hrid: string | null } | null>(null);
    const [pickedDomain, setPickedDomain] = useState<{ id: string; hrid: string | null } | null>(null);
    // hrid for the selected domain, each source matched to cfg.domainId so it can't go stale.
    const domainHrid =
        (pickedDomain?.id === cfg.domainId ? pickedDomain.hrid : null) ??
        (savedDomain?.id === cfg.domainId ? savedDomain.hrid : null) ??
        domains?.find(d => d.id === cfg.domainId)?.hrid ??
        null;
    const [gatewayUrl, setGatewayUrl] = useState<string | null>(null);
    const [gatewayEntrypoints, setGatewayEntrypoints] = useState<AmGatewayEntrypoint[] | null>(null);
    const [testing, setTesting] = useState(false);
    const [testResult, setTestResult] = useState<{ ok: boolean; message?: string } | null>(null);
    const [saving, setSaving] = useState(false);
    // Gates the env/domain picker; set by a successful test/save or a previously-saved connection,
    // reset when any field is edited so the user must re-verify.
    const [connectionVerified, setConnectionVerified] = useState(false);
    // Bumped to force the env/domain effects to re-query when connectionVerified hasn't changed.
    const [scopeRefreshKey, setScopeRefreshKey] = useState(0);

    const set = <K extends keyof AmConfig>(key: K, value: AmConfig[K]) => setCfg(prev => ({ ...prev, [key]: value }));
    const setField = <K extends keyof FormState>(key: K, value: FormState[K]) => {
        setForm(prev => ({ ...prev, [key]: value }));
        setConnectionVerified(false);
        setTestResult(null);
    };

    const { organizationId, environmentId } = cfg;

    // Seed the organization from the bootstrap context when nothing is stored yet.
    useEffect(() => {
        if (organizationId) return;
        let cancelled = false;
        resolveOrganizationId()
            .then(org => {
                if (!cancelled) setCfg(prev => (prev.organizationId ? prev : { ...prev, organizationId: org }));
            })
            .catch(e => {
                if (cancelled) return;
                // No org → the connection effect can't run; drop the spinner and surface the error.
                console.error('Failed to resolve organization', e);
                setError(e instanceof Error ? e.message : String(e));
                setLoadingConn(false);
            });
        return () => {
            cancelled = true;
        };
    }, [organizationId]);

    // Stable org-only cfg so the connection/env/domain fetches don't re-run on domain changes.
    const orgCfg = useMemo<AmConfig>(() => ({ organizationId, environmentId: '', domainId: '' }), [organizationId]);

    useEffect(() => {
        if (!organizationId) return;
        let cancelled = false;
        const run = async () => {
            setLoadingConn(true);
            setError(null);
            try {
                const view: AmConnectionView = await getAmConnection(orgCfg);
                if (cancelled) return;
                setForm({
                    baseUrl: view.baseUrl,
                    accessToken: '',
                    hadAccessToken: view.hasAccessToken,
                });
                setSavedDomain(view.defaultDomainId ? { id: view.defaultDomainId, hrid: view.defaultDomainHrid ?? null } : null);
                // Restore the server's domain only when localStorage has none; reading prev inside
                // the updater keeps cfg.domainId out of the effect deps.
                if (view.defaultDomainId) {
                    setCfg(prev => (prev.domainId ? prev : { ...prev, domainId: view.defaultDomainId! }));
                }
                if (view.gatewayUrl) {
                    setGatewayUrl(view.gatewayUrl);
                }
                setConnectionVerified(Boolean(view.baseUrl && view.hasAccessToken));
            } catch (e) {
                if (!cancelled) {
                    console.error('Failed to load AM connection', e);
                    setError(e instanceof Error ? e.message : String(e));
                }
            } finally {
                if (!cancelled) setLoadingConn(false);
            }
        };
        void run();
        return () => {
            cancelled = true;
        };
    }, [organizationId, orgCfg]);

    useEffect(() => {
        if (!organizationId || !connectionVerified) return;
        let cancelled = false;
        const run = async () => {
            setLoadingScope(true);
            setError(null);
            try {
                const list = await listEnvironments(orgCfg);
                if (cancelled) return;
                setEnvs(list);
                if (list.length === 1) {
                    setCfg(prev => (prev.environmentId ? prev : { ...prev, environmentId: list[0].id }));
                }
            } catch (e) {
                if (!cancelled) {
                    setEnvs(null);
                    console.error('Failed to list environments', e);
                    if (isAmUnavailable(e)) {
                        setError('Gravitee Access Management is not reachable. Check the connection and try again.');
                    } else {
                        setError(e instanceof Error ? e.message : String(e));
                    }
                }
            } finally {
                if (!cancelled) setLoadingScope(false);
            }
        };
        void run();
        return () => {
            cancelled = true;
        };
    }, [organizationId, connectionVerified, scopeRefreshKey, orgCfg]);

    useEffect(() => {
        if (!organizationId || !environmentId || !connectionVerified) return;
        let cancelled = false;
        const run = async () => {
            setError(null);
            try {
                const list = await listDomains(orgCfg, environmentId);
                if (cancelled) return;
                setDomains(list);
                if (list.length === 1) {
                    setCfg(prev => (prev.domainId ? prev : { ...prev, domainId: list[0].id }));
                }
            } catch (e) {
                if (!cancelled) {
                    console.error('Failed to list domains', e);
                    setError(e instanceof Error ? e.message : String(e));
                }
            }
        };
        void run();
        return () => {
            cancelled = true;
        };
    }, [organizationId, environmentId, connectionVerified, scopeRefreshKey, orgCfg]);

    // Stable cfg slice so the entrypoints effect doesn't re-run on unrelated cfg changes.
    const entrypointCfg = useMemo(
        () => ({ organizationId, environmentId, domainId: cfg.domainId }),
        [organizationId, environmentId, cfg.domainId],
    );

    useEffect(() => {
        if (!entrypointCfg.organizationId || !entrypointCfg.environmentId || !entrypointCfg.domainId || !connectionVerified) return;
        let cancelled = false;
        const run = async () => {
            try {
                const entries = await listDomainEntrypoints(
                    entrypointCfg,
                    entrypointCfg.environmentId,
                    entrypointCfg.domainId,
                );
                if (cancelled) return;
                setGatewayEntrypoints(entries);
                const def = entries.find(e => e.defaultEntrypoint) ?? entries[0];
                if (def && entries.length === 1) {
                    setGatewayUrl(def.url.replace(/\/$/, ''));
                } else {
                    setGatewayUrl(null);
                }
            } catch {
                if (!cancelled) setGatewayEntrypoints(null);
            }
        };
        void run();
        return () => {
            cancelled = true;
        };
    }, [entrypointCfg, connectionVerified]);

    const buildRequest = (): AmConnectionRequest => ({
        baseUrl: form.baseUrl,
        serviceAccountAccessToken: form.accessToken || undefined,
        defaultDomainId: cfg.domainId || null,
        defaultDomainHrid: domainHrid || null,
        gatewayUrl: gatewayUrl || null,
    });

    const handleTest = async () => {
        setTesting(true);
        setTestResult(null);
        try {
            const result = await testAmConnection(cfg, buildRequest());
            setTestResult({
                ok: result.ok,
                message: result.ok ? 'Connection succeeded' : `${result.status ?? '?'}: ${result.message ?? 'failed'}`,
            });
            if (result.ok) {
                // Persist immediately so the env/domain queries below run against the tested creds,
                // not whatever is still saved server-side.
                try {
                    const updated = await saveAmConnection(cfg, buildRequest());
                    setForm(prev => ({ ...prev, accessToken: '', hadAccessToken: updated.hasAccessToken }));
                } catch (saveErr) {
                    console.warn('Failed to persist tested AM connection', saveErr);
                }
                setScopeRefreshKey(k => k + 1);
            }
            setConnectionVerified(result.ok);
        } catch (e) {
            setTestResult({ ok: false, message: e instanceof Error ? e.message : String(e) });
            setConnectionVerified(false);
        } finally {
            setTesting(false);
        }
    };

    const canSaveConnection = Boolean(form.baseUrl) && Boolean(form.accessToken || form.hadAccessToken);
    const canSaveSelection = Boolean(cfg.organizationId && cfg.environmentId && cfg.domainId);

    const submit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!canSaveConnection) return;
        setSaving(true);
        setError(null);
        try {
            const updated = await saveAmConnection(cfg, buildRequest());
            setForm(prev => ({
                ...prev,
                accessToken: '',
                hadAccessToken: updated.hasAccessToken,
            }));
            setConnectionVerified(Boolean(updated.baseUrl && updated.hasAccessToken));
            if (canSaveSelection) {
                saveAmConfig(cfg);
                onSaved(cfg);
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : String(err));
        } finally {
            setSaving(false);
        }
    };

    if (loadingConn) {
        return (
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Spinner /> Loading Gravitee Access Management settings…
            </div>
        );
    }

    return (
        <form onSubmit={submit} className="grid gap-4 max-w-2xl">
            <Card>
                <CardHeader>
                    <CardTitle>Gravitee Access Management connection</CardTitle>
                    <CardDescription>
                        Configure how this module reaches Gravitee Access Management. The service-account access token is encrypted at rest.
                    </CardDescription>
                </CardHeader>
                <CardContent className="grid gap-4">
                    <TextField
                        label="Organization"
                        value={cfg.organizationId}
                        onChange={v => set('organizationId', v)}
                        placeholder="DEFAULT"
                    />

                    <TextField
                        label="Gravitee Access Management base URL"
                        value={form.baseUrl}
                        onChange={v => setField('baseUrl', v)}
                        placeholder="http://localhost:8093"
                    />

                    <TextField
                        label="Service-account access token"
                        value={form.accessToken}
                        onChange={v => setField('accessToken', v)}
                        placeholder={form.hadAccessToken ? '●●●●●●●● (saved — leave blank to keep)' : 'Bearer token issued by AM'}
                        type="password"
                    />

                    <div className="flex items-center gap-2">
                        <Button type="button" variant="outline" onClick={handleTest} disabled={testing || !form.baseUrl}>
                            {testing ? 'Testing…' : 'Test connection'}
                        </Button>
                        {testResult && (
                            <span className={testResult.ok ? 'text-sm text-success' : 'text-sm text-destructive'}>
                                {testResult.message}
                            </span>
                        )}
                    </div>
                </CardContent>
            </Card>

            {connectionVerified && (
                <Card>
                    <CardHeader>
                        <CardTitle>Scope</CardTitle>
                        <CardDescription>
                            Pick the Gravitee Access Management environment and domain this module should target.
                        </CardDescription>
                    </CardHeader>
                    <CardContent className="grid gap-4">
                        {loadingScope && (
                            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                <Spinner /> Loading scope…
                            </div>
                        )}

                        {envs && (
                            <SelectField
                                label="Environment"
                                value={cfg.environmentId}
                                onChange={id => {
                                    set('environmentId', id);
                                    set('domainId', '');
                                    setPickedDomain(null);
                                }}
                                options={envs.map(e => ({ value: e.id, label: e.name ? `${e.name} (${e.id})` : e.id }))}
                            />
                        )}

                        {domains &&
                            (domains.length === 0 ? (
                                <Alert>
                                    <AlertTitle>No domains in this environment</AlertTitle>
                                    <AlertDescription>Create a domain in Gravitee Access Management first.</AlertDescription>
                                </Alert>
                            ) : (
                                <>
                                    <DomainComboboxField
                                        key={environmentId}
                                        label="Domain"
                                        cfg={cfg}
                                        envId={environmentId}
                                        initialDomains={domains}
                                        value={cfg.domainId}
                                        valueLabel={domainHrid}
                                        onChange={(id, hrid) => {
                                            set('domainId', id);
                                            setPickedDomain(id ? { id, hrid } : null);
                                            // Reset gateway state; the discovery effect re-populates it.
                                            setGatewayEntrypoints(null);
                                            setGatewayUrl(null);
                                        }}
                                    />
                                    {cfg.domainId &&
                                        gatewayEntrypoints !== null &&
                                        (gatewayEntrypoints.length === 0 ? (
                                            <Alert variant="destructive">
                                                <AlertTitle>No gateway entrypoints found</AlertTitle>
                                                <AlertDescription>
                                                    No AM gateway is configured for this domain. The OAuth2 resource will fall back to the
                                                    management URL.
                                                </AlertDescription>
                                            </Alert>
                                        ) : gatewayEntrypoints.length === 1 ? (
                                            <Alert>
                                                <AlertTitle>Gateway discovered</AlertTitle>
                                                <AlertDescription className="font-mono text-xs">
                                                    {gatewayEntrypoints[0].url}
                                                </AlertDescription>
                                            </Alert>
                                        ) : (
                                            <SelectField
                                                label="Gateway entrypoint"
                                                value={gatewayUrl ?? ''}
                                                onChange={url => setGatewayUrl(url)}
                                                options={gatewayEntrypoints.map(e => ({
                                                    value: e.url,
                                                    label: e.name ? `${e.name} — ${e.url}` : e.url,
                                                }))}
                                            />
                                        ))}
                                </>
                            ))}
                    </CardContent>
                </Card>
            )}

            {error && (
                <Alert variant="destructive">
                    <AlertTitle>Failed</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            <div className="flex gap-2">
                <Button type="submit" disabled={!canSaveConnection || saving}>
                    {saving ? 'Saving…' : 'Save'}
                </Button>
                {onCancel && (
                    <Button type="button" variant="outline" onClick={onCancel}>
                        Cancel
                    </Button>
                )}
            </div>
        </form>
    );
}

function TextField({
    label,
    value,
    onChange,
    placeholder,
    type = 'text',
}: {
    label: string;
    value: string;
    onChange: (v: string) => void;
    placeholder?: string;
    type?: string;
}) {
    const id = useId();
    return (
        <Field>
            <FieldLabel htmlFor={id}>{label}</FieldLabel>
            {type === 'password' ? (
                <PasswordInput id={id} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} />
            ) : (
                <Input id={id} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} type={type} />
            )}
        </Field>
    );
}

function SelectField({
    label,
    value,
    onChange,
    options,
}: {
    label: string;
    value: string;
    onChange: (v: string) => void;
    options: { value: string; label: string }[];
}) {
    const id = useId();
    return (
        <Field>
            <FieldLabel htmlFor={id}>{label}</FieldLabel>
            <Select value={value} onValueChange={onChange}>
                <SelectTrigger id={id}>
                    <SelectValue placeholder={`Select ${label.toLowerCase()}`} />
                </SelectTrigger>
                <SelectContent>
                    {options.map(o => (
                        <SelectItem key={o.value} value={o.value}>
                            {o.label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </Field>
    );
}

function domainLabel(d: AmDomain): string {
    return d.name || d.hrid || d.id;
}

// Combobox input-change reasons that represent the user actually editing the search query.
const USER_INPUT_REASONS = new Set(['input-change', 'input-paste', 'input-clear', 'clear-press']);

interface DomainOption {
    value: string;
    label: string;
}

// Searchable domain picker — AM pages domains, so typing debounces into a server-side ?q= search.
function DomainComboboxField({
    label,
    cfg,
    envId,
    initialDomains,
    value,
    valueLabel,
    onChange,
}: {
    label: string;
    cfg: AmConfig;
    envId: string;
    initialDomains: AmDomain[];
    value: string;
    // Saved hrid used to label a value outside the first-page list, skipping the forbidden by-id fetch.
    valueLabel?: string | null;
    onChange: (v: string, hrid: string | null) => void;
}) {
    const id = useId();
    // null = no active search, fall back to the parent's first-page list.
    const [searchResults, setSearchResults] = useState<AmDomain[] | null>(null);
    // Label captured on pick/by-id fetch so a selection outside the first-page list keeps its name.
    const [pickedOption, setPickedOption] = useState<DomainOption | null>(null);
    const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
    // Bumped per search so a slow earlier response can't overwrite a newer query's results.
    const searchSeq = useRef(0);

    const domains = searchResults ?? initialDomains;
    const options = useMemo<DomainOption[]>(() => domains.map(d => ({ value: d.id, label: domainLabel(d) })), [domains]);

    // Resolve from stable sources only (not live search results), or base-ui's value reference
    // churns while typing and resets the focused input.
    const selected = useMemo<DomainOption | null>(() => {
        if (!value) return null;
        const match = initialDomains.find(d => d.id === value);
        if (match) return { value, label: domainLabel(match) };
        if (pickedOption?.value === value) return pickedOption;
        if (valueLabel) return { value, label: valueLabel };
        return { value, label: value };
    }, [value, pickedOption, initialDomains, valueLabel]);

    // Last-resort label for a saved domain with no hrid: fetch by id, falling back to the raw id.
    useEffect(() => {
        if (!value || valueLabel || pickedOption?.value === value || initialDomains.some(d => d.id === value)) return;
        let cancelled = false;
        getDomain(cfg, envId, value)
            .then(d => {
                if (!cancelled) setPickedOption({ value: d.id, label: domainLabel(d) });
            })
            .catch(e => {
                console.error('Failed to load selected domain', e);
                if (!cancelled) setPickedOption({ value, label: value });
            });
        return () => {
            cancelled = true;
        };
    }, [value, valueLabel, pickedOption, initialDomains, cfg, envId]);

    useEffect(() => () => clearTimeout(timer.current), []);

    const search = (term: string, details: { reason: string }) => {
        // base-ui also fires this on blur/selection resets; only react to real query edits.
        if (!USER_INPUT_REASONS.has(details.reason)) return;
        if (timer.current) clearTimeout(timer.current);
        if (!term) {
            searchSeq.current += 1;
            setSearchResults(null);
            return;
        }
        const seq = (searchSeq.current += 1);
        timer.current = setTimeout(async () => {
            try {
                const list = await listDomains(cfg, envId, term);
                if (seq === searchSeq.current) setSearchResults(list);
            } catch (e) {
                console.error('Failed to search domains', e);
            }
        }, 300);
    };

    // Show a loading state while a by-id fetch resolves, rather than flashing the raw id.
    const resolving = Boolean(value) && !valueLabel && !initialDomains.some(d => d.id === value) && pickedOption?.value !== value;
    if (resolving) {
        return (
            <Field>
                <FieldLabel htmlFor={id}>{label}</FieldLabel>
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Spinner /> Loading domain…
                </div>
            </Field>
        );
    }

    return (
        <Field>
            <FieldLabel htmlFor={id}>{label}</FieldLabel>
            <Combobox
                items={options}
                value={selected}
                onValueChange={(v: DomainOption | null) => {
                    setPickedOption(v);
                    const picked = v ? domains.find(d => d.id === v.value) : undefined;
                    onChange(v?.value ?? '', picked?.hrid ?? null);
                }}
                onInputValueChange={search}
                filter={null}
            >
                <ComboboxInput id={id} placeholder={`Select ${label.toLowerCase()}`} />
                <ComboboxContent>
                    <ComboboxEmpty>No matching domains</ComboboxEmpty>
                    <ComboboxList>
                        {(o: DomainOption) => (
                            <ComboboxItem key={o.value} value={o}>
                                {o.label}
                            </ComboboxItem>
                        )}
                    </ComboboxList>
                </ComboboxContent>
            </Combobox>
        </Field>
    );
}
