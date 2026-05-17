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
    CardDescription,
    CardHeader,
    CardTitle,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { FileUpIcon, PlusIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { ApplicationAddCertificateDialog, type AddCertificateSubmit } from './ApplicationAddCertificateDialog';
import { ApplicationCertificateDetailDialog } from './ApplicationCertificateDetailDialog';
import { ApplicationRevokeCertificateDialog } from './ApplicationRevokeCertificateDialog';
import { certificateStatusLabel, certificateStatusVariant } from './certificateUtils';
import { useApplicationCertificates } from '../../hooks/useApplicationCertificates';
import { useApplicationGeneralMutations } from '../../hooks/useApplicationGeneralMutations';
import type { ApplicationListItem } from '../../types/application';
import type { ClientCertificate } from '../../types/applicationCertificate';
import { formatApplicationDateTime } from '../../utils/applicationFormatters';

export interface ApplicationCertificatesSectionProps {
    readonly application: ApplicationListItem;
    readonly applicationId: string;
    readonly canManageCertificates: boolean;
}

export function ApplicationCertificatesSection({ application, applicationId, canManageCertificates }: ApplicationCertificatesSectionProps) {
    const { data: certificates = [], isLoading: certificatesLoading } = useApplicationCertificates(applicationId);
    const { createCertificateMutation, updateCertificateMutation } = useApplicationGeneralMutations(application, applicationId);

    const [certDialogOpen, setCertDialogOpen] = useState(false);
    const [viewCert, setViewCert] = useState<ClientCertificate | null>(null);
    const [revokeCert, setRevokeCert] = useState<ClientCertificate | null>(null);
    const [certError, setCertError] = useState<string | null>(null);
    const [revokeError, setRevokeError] = useState<string | null>(null);

    const openAddDialog = () => setCertDialogOpen(true);

    const handleAddCertificate = (submit: AddCertificateSubmit) => {
        setCertError(null);
        const activeCert = submit.activeCertificateId ? certificates.find(c => c.id === submit.activeCertificateId) : undefined;
        createCertificateMutation.mutate(
            {
                name: submit.name,
                certificate: submit.certificate,
                endsAt: submit.endsAt,
            },
            {
                onSuccess: () => {
                    if (submit.gracePeriodEnd && submit.activeCertificateId && activeCert) {
                        updateCertificateMutation.mutate(
                            {
                                certificateId: submit.activeCertificateId,
                                update: { name: activeCert.name, endsAt: submit.gracePeriodEnd },
                            },
                            {
                                onSuccess: () => setCertDialogOpen(false),
                                onError: (e: unknown) =>
                                    setCertError(e instanceof Error ? e.message : 'Certificate added but grace period update failed.'),
                            },
                        );
                    } else {
                        setCertDialogOpen(false);
                    }
                },
                onError: (e: unknown) => setCertError(e instanceof Error ? e.message : 'Failed to add certificate.'),
            },
        );
    };

    const handleRevokeCertificate = () => {
        if (!revokeCert) return;
        setRevokeError(null);
        const revokedAt = new Date(Date.now() - 1000).toISOString();
        updateCertificateMutation.mutate(
            {
                certificateId: revokeCert.id,
                update: { name: revokeCert.name, endsAt: revokedAt },
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
                            <Button type="button" size="sm" variant="outline" className="shrink-0" onClick={openAddDialog}>
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
                            <p className="text-sm font-medium">No certificates</p>
                            <p className="mt-1 max-w-sm text-xs text-muted-foreground">
                                Upload a client certificate to enable mutual TLS for this application.
                            </p>
                            {canManageCertificates ? (
                                <Button type="button" size="sm" variant="outline" className="mt-4" onClick={openAddDialog}>
                                    <PlusIcon className="size-3.5" aria-hidden />
                                    Add certificate
                                </Button>
                            ) : null}
                        </div>
                    ) : (
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Name</TableHead>
                                    <TableHead>Uploaded</TableHead>
                                    <TableHead>Expiry</TableHead>
                                    <TableHead>Status</TableHead>
                                    <TableHead className="text-right">Actions</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {certificates.map(cert => (
                                    <TableRow key={cert.id}>
                                        <TableCell className="font-medium">{cert.name}</TableCell>
                                        <TableCell className="text-sm text-muted-foreground">
                                            {formatApplicationDateTime(cert.createdAt)}
                                        </TableCell>
                                        <TableCell className="text-sm text-muted-foreground">
                                            {cert.endsAt ? formatApplicationDateTime(cert.endsAt) : '—'}
                                        </TableCell>
                                        <TableCell>
                                            <Badge variant={certificateStatusVariant(cert.status)} className="text-xs">
                                                {certificateStatusLabel(cert.status)}
                                            </Badge>
                                        </TableCell>
                                        <TableCell className="text-right">
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
                                                        onClick={() => {
                                                            setRevokeError(null);
                                                            setRevokeCert(cert);
                                                        }}
                                                    >
                                                        Revoke
                                                    </Button>
                                                ) : null}
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
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
                isSubmitting={createCertificateMutation.isPending || updateCertificateMutation.isPending}
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
