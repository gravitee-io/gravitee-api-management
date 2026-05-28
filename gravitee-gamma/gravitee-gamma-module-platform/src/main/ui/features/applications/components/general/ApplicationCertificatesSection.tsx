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
    Badge,
    Button,
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
    DataTable,
    Skeleton,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { FileUpIcon, PlusIcon } from '@gravitee/graphene-core/icons';
import type { UseMutationResult } from '@tanstack/react-query';
import { useMemo, useState } from 'react';

import { ApplicationAddCertificateDialog, type AddCertificateSubmit } from './ApplicationAddCertificateDialog';
import { ApplicationCertificateDetailDialog } from './ApplicationCertificateDetailDialog';
import { ApplicationRevokeCertificateDialog } from './ApplicationRevokeCertificateDialog';
import { certificateStatusLabel, certificateStatusVariant } from './certificateUtils';
import { useApplicationCertificates } from '../../hooks/useApplicationCertificates';
import {
    toGracePeriodUpdateFailure,
    type AddCertificateWithGraceInput,
    type GracePeriodUpdateFailure,
} from '../../hooks/useApplicationGeneralMutations';
import type { ClientCertificate, UpdateClientCertificate } from '../../types/applicationCertificate';
import { formatApplicationDateTime } from '../../utils/applicationFormatters';
import { NON_SORTABLE_COLUMN } from '../../utils/dataTableHeaders';
import type { ColCell } from '../../utils/dataTableTypes';

export interface ApplicationCertificatesSectionProps {
    readonly applicationId: string;
    readonly canManageCertificates: boolean;
    readonly isMutating: boolean;
    readonly addCertificateWithGraceMutation: UseMutationResult<ClientCertificate, Error, AddCertificateWithGraceInput, unknown>;
    readonly updateCertificateMutation: UseMutationResult<
        ClientCertificate,
        Error,
        { certificateId: string; update: UpdateClientCertificate },
        unknown
    >;
}

export function ApplicationCertificatesSection({
    applicationId,
    canManageCertificates,
    isMutating,
    addCertificateWithGraceMutation,
    updateCertificateMutation,
}: ApplicationCertificatesSectionProps) {
    const { data: certificates = [], isLoading: certificatesLoading } = useApplicationCertificates(applicationId);

    const [certDialogOpen, setCertDialogOpen] = useState(false);
    const [viewCert, setViewCert] = useState<ClientCertificate | null>(null);
    const [revokeCert, setRevokeCert] = useState<ClientCertificate | null>(null);
    const [certError, setCertError] = useState<string | null>(null);
    const [revokeError, setRevokeError] = useState<string | null>(null);
    const [gracePeriodWarning, setGracePeriodWarning] = useState<GracePeriodUpdateFailure | null>(null);
    const actionsDisabled = isMutating || !canManageCertificates;

    const certificateColumns = useMemo((): DataTableProps<ClientCertificate>['columns'] => {
        return [
            {
                accessorKey: 'name',
                header: 'Name',
                ...NON_SORTABLE_COLUMN,
                cell: ({ row }: ColCell<ClientCertificate>) => <span className="font-medium">{row.original.name}</span>,
            },
            {
                accessorKey: 'createdAt',
                header: 'Uploaded',
                ...NON_SORTABLE_COLUMN,
                cell: ({ row }: ColCell<ClientCertificate>) => (
                    <span className="text-sm text-muted-foreground">{formatApplicationDateTime(row.original.createdAt)}</span>
                ),
            },
            {
                accessorKey: 'endsAt',
                header: 'Expiry date and time',
                ...NON_SORTABLE_COLUMN,
                cell: ({ row }: ColCell<ClientCertificate>) => (
                    <span className="text-sm text-muted-foreground">
                        {row.original.endsAt ? formatApplicationDateTime(row.original.endsAt) : '—'}
                    </span>
                ),
            },
            {
                accessorKey: 'status',
                header: 'Status',
                ...NON_SORTABLE_COLUMN,
                cell: ({ row }: ColCell<ClientCertificate>) => (
                    <Badge variant={certificateStatusVariant(row.original.status)} className="text-xs">
                        {certificateStatusLabel(row.original.status)}
                    </Badge>
                ),
            },
            {
                id: 'actions',
                header: () => <div className="text-right">Actions</div>,
                cell: ({ row }: ColCell<ClientCertificate>) => {
                    const cert = row.original;
                    return (
                        <div className="flex justify-end gap-1">
                            <Button type="button" variant="ghost" size="sm" onClick={() => setViewCert(cert)}>
                                View
                            </Button>
                            {canManageCertificates && cert.status !== 'REVOKED' ? (
                                <Button
                                    type="button"
                                    variant="ghost"
                                    size="sm"
                                    className="text-destructive hover:text-destructive"
                                    disabled={actionsDisabled}
                                    onClick={() => {
                                        setRevokeError(null);
                                        setRevokeCert(cert);
                                    }}
                                >
                                    Revoke
                                </Button>
                            ) : null}
                        </div>
                    );
                },
                enableSorting: false,
                enableHiding: false,
            },
        ];
    }, [actionsDisabled, canManageCertificates]);

    const openAddDialog = () => {
        setCertError(null);
        setCertDialogOpen(true);
    };

    const handleAddCertificate = (submit: AddCertificateSubmit) => {
        setCertError(null);
        const activeCert = submit.activeCertificateId ? certificates.find(c => c.id === submit.activeCertificateId) : undefined;
        addCertificateWithGraceMutation.mutate(
            {
                create: {
                    name: submit.name,
                    certificate: submit.certificate,
                    endsAt: submit.endsAt,
                },
                gracePeriod:
                    submit.gracePeriodEnd && submit.activeCertificateId && activeCert
                        ? {
                              certificateId: submit.activeCertificateId,
                              name: activeCert.name,
                              endsAt: submit.gracePeriodEnd,
                          }
                        : undefined,
            },
            {
                onSuccess: () => {
                    setCertDialogOpen(false);
                    setGracePeriodWarning(null);
                },
                onError: (e: unknown) => {
                    const graceFailure = toGracePeriodUpdateFailure(e);
                    if (graceFailure) {
                        setGracePeriodWarning(graceFailure);
                        setCertDialogOpen(false);
                        return;
                    }
                    setCertError(e instanceof Error ? e.message : 'Failed to add certificate.');
                },
            },
        );
    };

    const handleRetryGracePeriod = () => {
        if (!gracePeriodWarning) return;
        updateCertificateMutation.mutate(
            {
                certificateId: gracePeriodWarning.certificateId,
                update: { name: gracePeriodWarning.name, endsAt: gracePeriodWarning.endsAt },
            },
            {
                onSuccess: () => setGracePeriodWarning(null),
                onError: (e: unknown) =>
                    setGracePeriodWarning(prev =>
                        prev
                            ? {
                                  ...prev,
                                  message: e instanceof Error ? e.message : 'Failed to update grace period.',
                              }
                            : prev,
                    ),
            },
        );
    };

    const handleRevokeCertificate = () => {
        if (!revokeCert) return;
        setRevokeError(null);
        const endsAt = new Date(Date.now() - 1000).toISOString();
        updateCertificateMutation.mutate(
            {
                certificateId: revokeCert.id,
                update: { name: revokeCert.name, endsAt },
            },
            {
                onSuccess: () => {
                    setRevokeCert(null);
                    setRevokeError(null);
                },
                onError: (e: unknown) => setRevokeError(e instanceof Error ? e.message : 'Failed to revoke certificate.'),
            },
        );
    };

    return (
        <>
            {gracePeriodWarning ? (
                <Alert variant="default" className="border-warning/40 bg-warning/10">
                    <AlertDescription className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                        <span className="text-sm text-foreground">{gracePeriodWarning.message}</span>
                        <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            className="shrink-0"
                            disabled={updateCertificateMutation.isPending}
                            onClick={handleRetryGracePeriod}
                        >
                            {updateCertificateMutation.isPending ? 'Retrying…' : 'Retry grace period update'}
                        </Button>
                    </AlertDescription>
                </Alert>
            ) : null}

            <Card>
                <CardHeader className="pb-3">
                    <div className="flex items-center justify-between gap-4">
                        <div className="space-y-1">
                            <CardTitle className="text-sm">Certificates</CardTitle>
                            <CardDescription className="text-xs">
                                mTLS client certificates for authenticating this application to the gateway.
                            </CardDescription>
                        </div>
                        {canManageCertificates ? (
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                className="shrink-0"
                                disabled={actionsDisabled}
                                onClick={openAddDialog}
                            >
                                <PlusIcon className="size-3.5" aria-hidden />
                                Add certificate
                            </Button>
                        ) : null}
                    </div>
                </CardHeader>
                <CardContent>
                    {certificatesLoading ? (
                        <Skeleton className="h-24 w-full" />
                    ) : certificates.length === 0 ? (
                        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed bg-muted/20 px-4 py-10 text-center">
                            <FileUpIcon className="mb-2 size-8 text-muted-foreground/40" aria-hidden />
                            <h3 className="text-sm font-medium">No mTLS certificates added</h3>
                            <p className="mt-1 max-w-sm text-xs text-muted-foreground">
                                Add your first certificate to secure this application
                            </p>
                        </div>
                    ) : (
                        <DataTable columns={certificateColumns} data={certificates} emptyMessage="No certificates" />
                    )}
                </CardContent>
            </Card>

            <ApplicationAddCertificateDialog
                open={certDialogOpen}
                onOpenChange={open => {
                    setCertDialogOpen(open);
                    if (!open) setCertError(null);
                }}
                applicationId={applicationId}
                certificates={certificates}
                onSubmit={handleAddCertificate}
                isSubmitting={addCertificateWithGraceMutation.isPending}
                error={certError}
            />

            <ApplicationCertificateDetailDialog certificate={viewCert} onClose={() => setViewCert(null)} />

            <ApplicationRevokeCertificateDialog
                certificate={revokeCert}
                onClose={() => {
                    setRevokeCert(null);
                    setRevokeError(null);
                }}
                onConfirm={handleRevokeCertificate}
                isLoading={updateCertificateMutation.isPending}
                error={revokeError}
            />
        </>
    );
}
