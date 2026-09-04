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

import { Button, Card, CardContent, Input } from '@gravitee/graphene-core';
import { PlusIcon, RefreshCwIcon, Trash2Icon } from '@gravitee/graphene-core/icons';

import { ChipInput } from '../../shared/components/ChipInput';
import type { BrandedSender } from '../types/consoleSettings';

export interface BrandedSenderFieldErrors {
    domains?: string;
    from?: string;
    subject?: string;
}

export function BrandedSendersSection({
    defaultFrom,
    defaultSubject,
    senders,
    disabled,
    senderErrors = [],
    listError,
    onChange,
    canReset = false,
    isResetting = false,
    onReset,
}: Readonly<{
    defaultFrom: string;
    defaultSubject: string;
    senders: BrandedSender[];
    disabled: boolean;
    senderErrors?: readonly BrandedSenderFieldErrors[];
    listError?: string;
    onChange: (next: BrandedSender[]) => void;
    /** Environment-scoped settings only: shows a "Reset to Org settings" action next to "Add configuration". */
    canReset?: boolean;
    isResetting?: boolean;
    onReset?: () => void;
}>) {
    function updateAt(index: number, patch: Partial<BrandedSender>) {
        onChange(senders.map((sender, senderIndex) => (senderIndex === index ? { ...sender, ...patch } : sender)));
    }

    return (
        <div className="space-y-6">
            <section className="space-y-3">
                <div>
                    <h2 className="text-sm font-semibold">Default notification email</h2>
                    <p className="text-xs text-muted-foreground">{"Used when no branded rule matches the recipient's email domain."}</p>
                </div>
                <div className="space-y-1.5">
                    <label htmlFor="smtp-default-from" className="text-sm font-medium">
                        Default From
                    </label>
                    <Input id="smtp-default-from" value={defaultFrom} readOnly disabled />
                    <p className="text-xs text-muted-foreground">Read-only preview of the From configured above.</p>
                </div>
                <div className="space-y-1.5">
                    <label htmlFor="smtp-default-subject" className="text-sm font-medium">
                        Default subject prefix
                    </label>
                    <Input id="smtp-default-subject" value={defaultSubject} readOnly disabled />
                    <p className="text-xs text-muted-foreground">{"%s is replaced with the email's subject."}</p>
                </div>
            </section>

            <section className="space-y-3">
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <h2 className="text-sm font-semibold">Branded notification email</h2>
                        <p className="text-xs text-muted-foreground">
                            {
                                "Add one or more rules. When a recipient's domain matches a rule, that rule's From and Subject prefix are used instead of the defaults above."
                            }
                        </p>
                    </div>
                    {disabled ? null : (
                        <div className="flex items-center gap-2">
                            {canReset && onReset ? (
                                <Button type="button" variant="ghost" size="sm" onClick={onReset} disabled={isResetting}>
                                    <RefreshCwIcon className="size-4" aria-hidden />
                                    {isResetting ? 'Resetting…' : 'Reset to Org settings'}
                                </Button>
                            ) : null}
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => onChange([...senders, { domains: [], from: '', subject: '' }])}
                            >
                                <PlusIcon className="size-4" aria-hidden />
                                Add configuration
                            </Button>
                        </div>
                    )}
                </div>

                {listError ? (
                    <p id="branded-senders-list-error" className="text-sm text-destructive" role="alert">
                        {listError}
                    </p>
                ) : null}
                {senders.map((sender, index) => {
                    const fieldErrors = senderErrors[index] ?? {};
                    const domainsErrorId = `branded-domains-${index}-error`;
                    const fromErrorId = `branded-from-${index}-error`;
                    const subjectErrorId = `branded-subject-${index}-error`;
                    return (
                        <Card key={`branded-sender-${index}`}>
                            <CardContent className="space-y-3 relative">
                                <p className="text-sm font-medium">Configuration {index + 1}</p>
                                {disabled ? null : (
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon"
                                        className="absolute right-2 top-2"
                                        aria-label="Delete configuration"
                                        onClick={() => onChange(senders.filter((_, senderIndex) => senderIndex !== index))}
                                    >
                                        <Trash2Icon className="size-4" aria-hidden />
                                    </Button>
                                )}
                                <div className="space-y-1.5 pr-10">
                                    <label htmlFor={`branded-domains-${index}`} className="text-sm font-medium">
                                        Recipient domains
                                    </label>
                                    <ChipInput
                                        id={`branded-domains-${index}`}
                                        values={sender.domains}
                                        onChange={domains => updateAt(index, { domains })}
                                        placeholder="partners.example.com"
                                        disabled={disabled}
                                        addOnComma
                                        invalid={Boolean(fieldErrors.domains)}
                                        describedBy={fieldErrors.domains ? domainsErrorId : undefined}
                                    />
                                    <p className="text-xs text-muted-foreground">
                                        Match is case-insensitive on the part after @ in the recipient address.
                                    </p>
                                    {fieldErrors.domains ? (
                                        <p id={domainsErrorId} className="text-sm text-destructive" role="alert">
                                            {fieldErrors.domains}
                                        </p>
                                    ) : null}
                                </div>
                                <div className="space-y-1.5">
                                    <label htmlFor={`branded-from-${index}`} className="text-sm font-medium">
                                        From
                                    </label>
                                    <Input
                                        id={`branded-from-${index}`}
                                        value={sender.from}
                                        onChange={e => updateAt(index, { from: e.target.value })}
                                        disabled={disabled}
                                        placeholder="noreply@example.com"
                                        aria-invalid={Boolean(fieldErrors.from)}
                                        aria-describedby={fieldErrors.from ? fromErrorId : undefined}
                                    />
                                    {fieldErrors.from ? (
                                        <p id={fromErrorId} className="text-sm text-destructive" role="alert">
                                            {fieldErrors.from}
                                        </p>
                                    ) : null}
                                </div>
                                <div className="space-y-1.5">
                                    <label htmlFor={`branded-subject-${index}`} className="text-sm font-medium">
                                        Subject prefix
                                    </label>
                                    <Input
                                        id={`branded-subject-${index}`}
                                        value={sender.subject}
                                        onChange={e => updateAt(index, { subject: e.target.value })}
                                        disabled={disabled}
                                        placeholder="[Example] %s"
                                        aria-invalid={Boolean(fieldErrors.subject)}
                                        aria-describedby={fieldErrors.subject ? subjectErrorId : undefined}
                                    />
                                    {fieldErrors.subject ? (
                                        <p id={subjectErrorId} className="text-sm text-destructive" role="alert">
                                            {fieldErrors.subject}
                                        </p>
                                    ) : null}
                                </div>
                            </CardContent>
                        </Card>
                    );
                })}
            </section>
        </div>
    );
}
