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

import { Card, CardContent, CardHeader, CardTitle, Input, Switch } from '@gravitee/graphene-core';

import { BrandedSendersSection, type BrandedSenderFieldErrors } from './BrandedSendersSection';
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
    return /^[^\s@]+@[^\s@]+$/.test(extractEmailAddress(from));
}

export function parseSmtpPort(port: string): number | null {
    if (!/^\d+$/.test(port.trim())) return null;
    const parsed = Number(port);
    return parsed >= 0 && parsed <= 65535 ? parsed : null;
}

/** Classic `branded-senders` DOMAIN_PATTERN: dot-separated host ending in an alphabetic or punycode TLD. */
const BRANDED_TLD = '(?:[a-zA-Z]{2,}|xn--[a-zA-Z0-9-]{1,59})';
const BRANDED_DOMAIN_PATTERN = new RegExp(`^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+${BRANDED_TLD}$`, 'i');
const BRANDED_SUBJECT_MAX_LENGTH = 255;
const BRANDED_MAX_SERIALIZED_LENGTH = 4000;

function normalizeBrandedDomain(domain: string): string {
    return domain.trim().toLowerCase();
}

export function invalidBrandedDomains(domains: readonly string[]): string[] {
    return domains.filter(domain => !BRANDED_DOMAIN_PATTERN.test(normalizeBrandedDomain(domain)));
}

export function duplicateBrandedDomains(senders: readonly BrandedSender[]): string[] {
    const seen = new Set<string>();
    const duplicates = new Set<string>();
    for (const sender of senders) {
        for (const domain of sender.domains) {
            const normalized = normalizeBrandedDomain(domain);
            if (!normalized) continue;
            if (seen.has(normalized)) duplicates.add(normalized);
            seen.add(normalized);
        }
    }
    return [...duplicates];
}

export function brandedSendersSerializedLength(senders: readonly BrandedSender[]): number {
    const json = JSON.stringify(senders);
    return json.length + (json.match(/;/g)?.length ?? 0) * 5;
}

export function getBrandedSenderFieldErrors(sender: BrandedSender): BrandedSenderFieldErrors {
    const errors: BrandedSenderFieldErrors = {};
    if (sender.domains.length === 0 || sender.domains.some(domain => domain.trim().length === 0)) {
        errors.domains = 'At least one domain is required.';
    } else {
        const invalid = invalidBrandedDomains(sender.domains);
        if (invalid.length > 0) {
            errors.domains = `Invalid domain(s): ${invalid.join(', ')}`;
        }
    }
    if (!sender.from.trim()) {
        errors.from = 'From is required.';
    } else if (!isSmtpFromValid(sender.from)) {
        errors.from = 'Enter a valid email address, optionally with a display name.';
    }
    if (sender.subject.length > BRANDED_SUBJECT_MAX_LENGTH) {
        errors.subject = `Must be at most ${BRANDED_SUBJECT_MAX_LENGTH} characters.`;
    }
    return errors;
}

export function getBrandedSendersListError(senders: readonly BrandedSender[]): string | undefined {
    const duplicates = duplicateBrandedDomains(senders);
    if (duplicates.length > 0) {
        return `Each domain may only be used in one configuration. Used more than once: ${duplicates.join(', ')}.`;
    }
    if (brandedSendersSerializedLength(senders) > BRANDED_MAX_SERIALIZED_LENGTH) {
        return 'These configurations are too large to save. Remove some domains or configurations and try again.';
    }
    return undefined;
}

export function isBrandedSenderValid(sender: BrandedSender): boolean {
    return Object.keys(getBrandedSenderFieldErrors(sender)).length === 0;
}

export function isBrandedSendersListValid(senders: readonly BrandedSender[]): boolean {
    return senders.every(isBrandedSenderValid) && getBrandedSendersListError(senders) === undefined;
}

export function isSmtpFormValid(state: SmtpFormState): boolean {
    if (!state.enabled) return true;
    return (
        state.host.trim().length > 0 &&
        parseSmtpPort(state.port) !== null &&
        isSmtpFromValid(state.from) &&
        isBrandedSendersListValid(state.brandedSenders)
    );
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
            <Card>
                <CardContent className="space-y-4">
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
                </CardContent>
            </Card>

            {value.enabled ? (
                <Card>
                    <CardHeader>
                        <CardTitle>Mail Properties</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
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
                    </CardContent>
                </Card>
            ) : null}

            <Card>
                <CardContent>
                    <SystemReadonlyHint locked={Boolean(readonly.brandedSenders)}>
                        <BrandedSendersSection
                            defaultFrom={value.from}
                            defaultSubject={value.subject}
                            senders={value.brandedSenders}
                            disabled={disabled || !value.enabled || Boolean(readonly.brandedSenders)}
                            senderErrors={value.brandedSenders.map(getBrandedSenderFieldErrors)}
                            listError={getBrandedSendersListError(value.brandedSenders)}
                            onChange={brandedSenders => onChange({ ...value, brandedSenders })}
                        />
                    </SystemReadonlyHint>
                </CardContent>
            </Card>
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
