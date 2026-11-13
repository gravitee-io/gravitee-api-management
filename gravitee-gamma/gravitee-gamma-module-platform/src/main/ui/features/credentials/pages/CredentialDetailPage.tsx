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
    PageHeader,
    Skeleton,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon, MoreVerticalIcon, PencilIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { CredentialConsumerList } from '../components/CredentialConsumerList';
import { CredentialDetailsCard } from '../components/CredentialDetailsCard';
import { CredentialLoadError } from '../components/CredentialLoadError';
import { DeleteCredentialDialog } from '../components/DeleteCredentialDialog';
import { useCredentialActions } from '../hooks/useCredentialActions';
import { useCredential, useCredentialConsumers } from '../hooks/useCredentials';
import { CREDENTIAL_ROTATION_WINDOW_NOTE } from '../utils/credentialRotation';
import { credentialStrategyLabel } from '../utils/credentialStrategies';

export function CredentialDetailPage() {
    const navigate = useNavigate();
    const { credentialId } = useParams<{ credentialId: string }>();
    const { data: credential, isLoading, error, isFetching, refetch } = useCredential(credentialId);
    const { data: consumers, isError: consumersFailed } = useCredentialConsumers(credentialId);
    const actions = useCredentialActions();
    const [deleting, setDeleting] = useState(false);

    const backButton = (
        <div>
            <Button type="button" variant="ghost" size="sm" onClick={() => navigate('..')}>
                <ArrowLeftIcon aria-hidden />
                Back to credentials
            </Button>
        </div>
    );

    if (isLoading) {
        return <Skeleton className="h-40 w-full" />;
    }

    if (!credential) {
        return (
            <div className="flex flex-col gap-6">
                {backButton}
                <CredentialLoadError error={error} retrying={isFetching} onRetry={() => void refetch()} />
            </div>
        );
    }

    const canEdit = actions.canEdit(credential);
    const canDelete = actions.canDelete(credential);

    return (
        <div className="flex flex-col gap-6">
            {backButton}
            <PageHeader
                title={credential.name}
                description={credentialStrategyLabel(credential.strategy)}
                actions={
                    (canEdit || canDelete) && (
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" aria-label={`Actions for ${credential.name}`}>
                                    <MoreVerticalIcon aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                {canEdit && (
                                    <DropdownMenuItem asChild>
                                        <Link to="edit">
                                            <PencilIcon aria-hidden />
                                            Edit
                                        </Link>
                                    </DropdownMenuItem>
                                )}
                                {canEdit && canDelete && <DropdownMenuSeparator />}
                                {canDelete && (
                                    <DropdownMenuItem variant="destructive" onSelect={() => setDeleting(true)}>
                                        <Trash2Icon aria-hidden />
                                        Delete
                                    </DropdownMenuItem>
                                )}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    )
                }
            />

            <CredentialDetailsCard credential={credential} />

            <Card>
                <CardHeader>
                    <CardTitle>Used by</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                    {credential.managed !== false ? (
                        <p className="text-sm text-muted-foreground">{CREDENTIAL_ROTATION_WINDOW_NOTE}</p>
                    ) : null}
                    {consumersFailed ? (
                        <p className="text-sm text-muted-foreground">Could not load what uses this credential. Refresh to try again.</p>
                    ) : consumers === undefined ? (
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
