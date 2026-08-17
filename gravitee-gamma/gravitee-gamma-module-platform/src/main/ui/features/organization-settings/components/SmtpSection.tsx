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
    return parsed >= 1 && parsed <= 65535 ? parsed : null;
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
                    <Switch
                        id="smtp-enabled"
                        checked={value.enabled}
                        onCheckedChange={checked => onChange({ ...value, enabled: checked === true })}
                        disabled={disabled || Boolean(readonly.enabled)}
                        aria-label="Enable Emailing"
                    />
                </div>

                {value.enabled ? (
                    <>
                        <Field
                            id="smtp-host"
                            label="Host"
                            value={value.host}
                            disabled={isFieldDisabled('host')}
                            onChange={host => onChange({ ...value, host })}
                        />
                        <Field
                            id="smtp-port"
                            label="Port"
                            type="number"
                            value={value.port}
                            disabled={isFieldDisabled('port')}
                            onChange={port => onChange({ ...value, port })}
                        />
                        <Field
                            id="smtp-username"
                            label="Username"
                            value={value.username}
                            disabled={isFieldDisabled('username')}
                            onChange={username => onChange({ ...value, username })}
                        />
                        <Field
                            id="smtp-password"
                            label="Password"
                            type="password"
                            value={value.password}
                            disabled={isFieldDisabled('password')}
                            onChange={password => onChange({ ...value, password })}
                        />
                        <Field
                            id="smtp-protocol"
                            label="Protocol"
                            value={value.protocol}
                            disabled={isFieldDisabled('protocol')}
                            onChange={protocol => onChange({ ...value, protocol })}
                        />
                        <div className="space-y-1.5">
                            <label htmlFor="smtp-subject" className="text-sm font-medium">
                                Subject
                            </label>
                            <Input
                                id="smtp-subject"
                                value={value.subject}
                                onChange={e => onChange({ ...value, subject: e.target.value })}
                                disabled={isFieldDisabled('subject')}
                            />
                            <p className="text-xs text-muted-foreground">%s is replaced with the email's subject.</p>
                        </div>
                        <Field
                            id="smtp-from"
                            label="From"
                            value={value.from}
                            disabled={isFieldDisabled('from')}
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
                        <Switch
                            id="smtp-auth"
                            checked={value.auth}
                            onCheckedChange={checked => onChange({ ...value, auth: checked === true })}
                            disabled={isFieldDisabled('auth')}
                            aria-label="Enable Auth"
                        />
                    </div>
                    <div className="flex items-center justify-between gap-4">
                        <label htmlFor="smtp-starttls" className="text-sm font-medium">
                            Enable Start TLS
                        </label>
                        <Switch
                            id="smtp-starttls"
                            checked={value.startTlsEnable}
                            onCheckedChange={checked => onChange({ ...value, startTlsEnable: checked === true })}
                            disabled={isFieldDisabled('startTlsEnable')}
                            aria-label="Enable Start TLS"
                        />
                    </div>
                    <Field
                        id="smtp-ssl-trust"
                        label="SSL Trust"
                        value={value.sslTrust}
                        disabled={isFieldDisabled('sslTrust')}
                        onChange={sslTrust => onChange({ ...value, sslTrust })}
                    />
                </section>
            ) : null}

            {value.enabled ? (
                <section className="rounded-lg border p-4">
                    <BrandedSendersSection
                        defaultFrom={value.from}
                        defaultSubject={value.subject}
                        senders={value.brandedSenders}
                        disabled={isFieldDisabled('brandedSenders')}
                        onChange={brandedSenders => onChange({ ...value, brandedSenders })}
                    />
                </section>
            ) : null}
        </div>
    );
}

function Field({
    id,
    label,
    value,
    disabled,
    onChange,
    type = 'text',
}: Readonly<{
    id: string;
    label: string;
    value: string;
    disabled: boolean;
    onChange: (value: string) => void;
    type?: string;
}>) {
    return (
        <div className="space-y-1.5">
            <label htmlFor={id} className="text-sm font-medium">
                {label}
            </label>
            <Input id={id} type={type} value={value} onChange={e => onChange(e.target.value)} disabled={disabled} />
        </div>
    );
}
