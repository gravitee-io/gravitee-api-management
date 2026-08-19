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

import { Input, Switch } from '@gravitee/graphene-core';

import { BrandedSendersSection } from './BrandedSendersSection';
import { SystemReadonlyHint } from './SystemReadonlyHint';
import type { BrandedSender } from '../types/consoleSettings';

export interface SmtpFormState {
    enabled: boolean;
    host: string;
    port: string;
    username: string;
    password: string;
    protocol: string;
    subject: string;
    from: string;
    auth: boolean;
    startTlsEnable: boolean;
    sslTrust: string;
    brandedSenders: BrandedSender[];
}

export function extractEmailAddress(from: string): string {
    const angled = from.match(/<([^>]+)>/);
    return (angled?.[1] ?? from).trim();
}

export function isSmtpFromValid(from: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(extractEmailAddress(from));
}

export function parseSmtpPort(port: string): number | null {
    if (!/^\d+$/.test(port.trim())) return null;
    const parsed = Number(port);
    return parsed >= 0 && parsed <= 65535 ? parsed : null;
}

export function isSmtpFormValid(state: SmtpFormState): boolean {
    if (!state.enabled) return true;
    return state.host.trim().length > 0 && parseSmtpPort(state.port) !== null && isSmtpFromValid(state.from);
}

export interface SmtpFieldReadonly {
    enabled?: boolean;
    host?: boolean;
    port?: boolean;
    username?: boolean;
    password?: boolean;
    protocol?: boolean;
    subject?: boolean;
    from?: boolean;
    auth?: boolean;
    startTlsEnable?: boolean;
    sslTrust?: boolean;
    brandedSenders?: boolean;
}

export function SmtpSection({
    value,
    disabled,
    readonly = {},
    onChange,
}: Readonly<{
    value: SmtpFormState;
    disabled: boolean;
    readonly?: SmtpFieldReadonly;
    onChange: (next: SmtpFormState) => void;
}>) {
    const fieldsOff = disabled || !value.enabled;

    function isFieldDisabled(key: keyof SmtpFieldReadonly): boolean {
        return fieldsOff || Boolean(readonly[key]);
    }

    return (
        <div className="space-y-6">
            <section className="rounded-lg border p-4 space-y-4">
                <div className="flex items-center justify-between gap-4">
                    <label htmlFor="smtp-enabled" className="text-sm font-medium">
                        Enable Emailing
                    </label>
                    <SystemReadonlyHint locked={Boolean(readonly.enabled)} className="inline-flex">
                        <Switch
                            id="smtp-enabled"
                            checked={value.enabled}
                            onCheckedChange={checked => onChange({ ...value, enabled: checked === true })}
                            disabled={disabled || Boolean(readonly.enabled)}
                            aria-label="Enable Emailing"
                        />
                    </SystemReadonlyHint>
                </div>

                {value.enabled ? (
                    <>
                        <Field
                            id="smtp-host"
                            label="Host"
                            value={value.host}
                            disabled={isFieldDisabled('host')}
                            systemReadonly={Boolean(readonly.host)}
                            onChange={host => onChange({ ...value, host })}
                        />
                        <Field
                            id="smtp-port"
                            label="Port"
                            type="number"
                            min={0}
                            value={value.port}
                            disabled={isFieldDisabled('port')}
                            systemReadonly={Boolean(readonly.port)}
                            onChange={port => onChange({ ...value, port })}
                        />
                        <Field
                            id="smtp-username"
                            label="Username"
                            value={value.username}
                            disabled={isFieldDisabled('username')}
                            systemReadonly={Boolean(readonly.username)}
                            onChange={username => onChange({ ...value, username })}
                        />
                        <Field
                            id="smtp-password"
                            label="Password"
                            type="password"
                            value={value.password}
                            disabled={isFieldDisabled('password')}
                            systemReadonly={Boolean(readonly.password)}
                            onChange={password => onChange({ ...value, password })}
                        />
                        <Field
                            id="smtp-protocol"
                            label="Protocol"
                            value={value.protocol}
                            disabled={isFieldDisabled('protocol')}
                            systemReadonly={Boolean(readonly.protocol)}
                            onChange={protocol => onChange({ ...value, protocol })}
                        />
                        <div className="space-y-1.5">
                            <label htmlFor="smtp-subject" className="text-sm font-medium">
                                Subject
                            </label>
                            <SystemReadonlyHint locked={Boolean(readonly.subject)}>
                                <Input
                                    id="smtp-subject"
                                    value={value.subject}
                                    onChange={e => onChange({ ...value, subject: e.target.value })}
                                    disabled={isFieldDisabled('subject')}
                                />
                            </SystemReadonlyHint>
                            <p className="text-xs text-muted-foreground">{"%s is replaced with the email's subject."}</p>
                        </div>
                        <Field
                            id="smtp-from"
                            label="From"
                            value={value.from}
                            disabled={isFieldDisabled('from')}
                            systemReadonly={Boolean(readonly.from)}
                            onChange={from => onChange({ ...value, from })}
                        />
                    </>
                ) : null}
            </section>

            {value.enabled ? (
                <section className="rounded-lg border p-4 space-y-4">
                    <h2 className="text-base font-semibold">Mail Properties</h2>
                    <div className="flex items-center justify-between gap-4">
                        <label htmlFor="smtp-auth" className="text-sm font-medium">
                            Enable Auth
                        </label>
                        <SystemReadonlyHint locked={Boolean(readonly.auth)} className="inline-flex">
                            <Switch
                                id="smtp-auth"
                                checked={value.auth}
                                onCheckedChange={checked => onChange({ ...value, auth: checked === true })}
                                disabled={isFieldDisabled('auth')}
                                aria-label="Enable Auth"
                            />
                        </SystemReadonlyHint>
                    </div>
                    <div className="flex items-center justify-between gap-4">
                        <label htmlFor="smtp-starttls" className="text-sm font-medium">
                            Enable Start TLS
                        </label>
                        <SystemReadonlyHint locked={Boolean(readonly.startTlsEnable)} className="inline-flex">
                            <Switch
                                id="smtp-starttls"
                                checked={value.startTlsEnable}
                                onCheckedChange={checked => onChange({ ...value, startTlsEnable: checked === true })}
                                disabled={isFieldDisabled('startTlsEnable')}
                                aria-label="Enable Start TLS"
                            />
                        </SystemReadonlyHint>
                    </div>
                    <Field
                        id="smtp-ssl-trust"
                        label="SSL Trust"
                        value={value.sslTrust}
                        disabled={isFieldDisabled('sslTrust')}
                        systemReadonly={Boolean(readonly.sslTrust)}
                        onChange={sslTrust => onChange({ ...value, sslTrust })}
                    />
                </section>
            ) : null}

            <section className="rounded-lg border p-4">
                <SystemReadonlyHint locked={Boolean(readonly.brandedSenders)}>
                    <BrandedSendersSection
                        defaultFrom={value.from}
                        defaultSubject={value.subject}
                        senders={value.brandedSenders}
                        disabled={disabled || !value.enabled || Boolean(readonly.brandedSenders)}
                        onChange={brandedSenders => onChange({ ...value, brandedSenders })}
                    />
                </SystemReadonlyHint>
            </section>
        </div>
    );
}

function Field({
    id,
    label,
    value,
    disabled,
    systemReadonly = false,
    onChange,
    type = 'text',
    min,
}: Readonly<{
    id: string;
    label: string;
    value: string;
    disabled: boolean;
    systemReadonly?: boolean;
    onChange: (value: string) => void;
    type?: string;
    min?: number;
}>) {
    return (
        <div className="space-y-1.5">
            <label htmlFor={id} className="text-sm font-medium">
                {label}
            </label>
            <SystemReadonlyHint locked={systemReadonly}>
                <Input id={id} type={type} min={min} value={value} onChange={e => onChange(e.target.value)} disabled={disabled} />
            </SystemReadonlyHint>
        </div>
    );
}
