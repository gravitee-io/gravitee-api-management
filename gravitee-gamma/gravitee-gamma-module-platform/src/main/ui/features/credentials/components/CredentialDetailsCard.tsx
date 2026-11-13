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
import { Badge, Card, CardContent, CardHeader, CardTitle } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

import type { Credential } from '../types/credential';
import { isCredentialExpired } from '../utils/credentialExpiry';
import { credentialOwnerLabel } from '../utils/credentialOwner';
import { CREDENTIAL_STRATEGY_DEFINITIONS, credentialStrategyLabel, isKnownCredentialStrategy } from '../utils/credentialStrategies';

const DATE_FORMAT = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });

function formatDate(value: string | undefined, fallback = '—'): string {
    return value ? DATE_FORMAT.format(new Date(value)) : fallback;
}

export function CredentialDetailsCard({ credential }: Readonly<{ credential: Credential }>) {
    const definition = isKnownCredentialStrategy(credential.strategy) ? CREDENTIAL_STRATEGY_DEFINITIONS[credential.strategy] : undefined;
    const managed = credential.managed !== false;

    return (
        <Card>
            <CardHeader>
                <CardTitle>Details</CardTitle>
            </CardHeader>
            <CardContent>
                <dl className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                    <Detail label="Service">{credential.serviceId ?? <span className="text-muted-foreground">Not set</span>}</Detail>
                    <Detail label="Type">
                        <Badge variant="outline">{credentialStrategyLabel(credential.strategy)}</Badge>
                    </Detail>
                    <Detail label="Owner">{credentialOwnerLabel(credential) ?? '—'}</Detail>
                    <Detail label="Secret source">{managed ? 'Stored by Gravitee' : 'References existing secrets'}</Detail>
                    {definition?.settings.map(field => {
                        const value = credential.configuration?.[field.key];
                        return value === undefined ? null : (
                            <Detail key={field.key} label={field.label}>
                                {Array.isArray(value) ? value.join(', ') : value}
                            </Detail>
                        );
                    })}
                    {managed
                        ? null
                        : definition?.secrets.map(field => (
                              <Detail key={field.key} label={`${field.label} reference`}>
                                  <code className="break-all text-xs">{credential.secretReferences?.[field.key]?.reference ?? '—'}</code>
                              </Detail>
                          ))}
                    <Detail label="Expires">
                        <span className="flex items-center gap-2">
                            {formatDate(credential.expiresAt)}
                            {isCredentialExpired(credential) ? <Badge variant="destructive">Expired</Badge> : null}
                        </span>
                    </Detail>
                    <Detail label="Created">{formatDate(credential.createdAt)}</Detail>
                    <Detail label="Last updated">{formatDate(credential.updatedAt)}</Detail>
                    <Detail label="Secret last replaced">{formatDate(credential.rotatedAt, 'Never replaced')}</Detail>
                </dl>
            </CardContent>
        </Card>
    );
}

function Detail({ label, children }: Readonly<{ label: string; children: ReactNode }>) {
    return (
        <div className="space-y-1">
            <dt className="text-xs text-muted-foreground">{label}</dt>
            <dd className="text-sm">{children}</dd>
        </div>
    );
}
