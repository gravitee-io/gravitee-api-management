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
    Badge,
    Button,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Skeleton,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon, MoreVerticalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState, type ReactNode } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { CredentialConsumerList } from '../components/CredentialConsumerList';
import { DeleteCredentialDialog } from '../components/DeleteCredentialDialog';
import { useCredential, useCredentialConsumers, useCredentialOwnerNames } from '../hooks/useCredentials';
import type { Credential } from '../types/credential';
import { isCredentialExpired } from '../utils/credentialExpiry';
import { ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION, ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION } from '../utils/credentialPermissions';
import { CREDENTIAL_ROTATION_WINDOW_NOTE } from '../utils/credentialRotation';
import { CREDENTIAL_STRATEGY_DEFINITIONS, credentialStrategyLabel } from '../utils/credentialStrategies';

const DATE_FORMAT = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });

function formatDate(value: string | undefined, fallback = '—'): string {
    return value ? DATE_FORMAT.format(new Date(value)) : fallback;
}

export function CredentialDetailPage() {
    const navigate = useNavigate();
    const { credentialId } = useParams<{ credentialId: string }>();
    const { data: credential, isLoading, isError } = useCredential(credentialId);
    const { data: consumers } = useCredentialConsumers(credentialId);
    const ownerNames = useCredentialOwnerNames([credential?.ownerId]);
    const canEdit = useHasEnvironmentPermission([ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION]);
    const canDelete = useHasEnvironmentPermission([ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION]);
    const [deleting, setDeleting] = useState(false);

    if (isLoading) {
        return <Skeleton className="h-40 w-full" />;
    }

    if (isError || !credential) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Failed to load the credential. It may have been deleted.</p>
            </div>
        );
    }

    return (
        <div className="flex flex-col gap-6">
            <div className="space-y-2">
                <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" onClick={() => navigate('..')}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to credentials
                </Button>
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1">
                        <h1 className="text-2xl font-semibold tracking-tight">{credential.name}</h1>
                        <p className="text-sm text-muted-foreground">{credentialStrategyLabel(credential.strategy)}</p>
                    </div>
                    {canEdit || canDelete ? (
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" aria-label="Credential actions">
                                    <MoreVerticalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                {canEdit && (
                                    <DropdownMenuItem onSelect={() => navigate('edit')}>
                                        <PencilIcon className="size-4 mr-2" aria-hidden />
                                        Edit
                                    </DropdownMenuItem>
                                )}
                                {canEdit && canDelete && <DropdownMenuSeparator />}
                                {canDelete && (
                                    <DropdownMenuItem variant="destructive" onSelect={() => setDeleting(true)}>
                                        <Trash2Icon className="size-4 mr-2" aria-hidden />
                                        Delete
                                    </DropdownMenuItem>
                                )}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    ) : null}
                </div>
            </div>

            <CredentialDetailsCard credential={credential} ownerName={credential.ownerId ? ownerNames[credential.ownerId] : undefined} />

            <Card>
                <CardHeader>
                    <CardTitle>Used by</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                    {credential.managed !== false ? (
                        <p className="text-sm text-muted-foreground">{CREDENTIAL_ROTATION_WINDOW_NOTE}</p>
                    ) : null}
                    {consumers === undefined ? (
                        <Skeleton className="h-6 w-48" />
                    ) : consumers.length > 0 ? (
                        <CredentialConsumerList consumers={consumers} />
                    ) : (
                        <p className="text-sm">Nothing uses this credential yet.</p>
                    )}
                </CardContent>
            </Card>

            <DeleteCredentialDialog credential={credential} open={deleting} onOpenChange={setDeleting} onDeleted={() => navigate('..')} />
        </div>
    );
}

function CredentialDetailsCard({ credential, ownerName }: Readonly<{ credential: Credential; ownerName?: string }>) {
    const definition = CREDENTIAL_STRATEGY_DEFINITIONS[credential.strategy];
    const managed = credential.managed !== false;

    return (
        <Card>
            <CardHeader>
                <CardTitle>Details</CardTitle>
            </CardHeader>
            <CardContent>
                <dl className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                    <Detail label="Provider">{credential.serviceId ?? <span className="text-muted-foreground">Not set</span>}</Detail>
                    <Detail label="Type">
                        <Badge variant="outline">{credentialStrategyLabel(credential.strategy)}</Badge>
                    </Detail>
                    <Detail label="Owner">{ownerName ?? '—'}</Detail>
                    <Detail label="Secret source">{managed ? 'Stored by Gravitee' : 'References existing secrets'}</Detail>
                    {definition.settings.map(field => {
                        const value = credential.configuration?.[field.key];
                        return value === undefined ? null : (
                            <Detail key={field.key} label={field.label}>
                                {Array.isArray(value) ? value.join(', ') : value}
                            </Detail>
                        );
                    })}
                    {managed
                        ? null
                        : definition.secrets.map(field => (
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
