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
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';

import { certificateStatusLabel, certificateStatusVariant } from './certificateUtils';
import type { ClientCertificate } from '../../types/applicationCertificate';
import { formatApplicationDateTime } from '../../utils/applicationFormatters';

export function ApplicationCertificateDetailDialog({
    certificate,
    onClose,
}: Readonly<{
    certificate: ClientCertificate | null;
    onClose: () => void;
}>) {
    return (
        <Dialog open={certificate !== null} onOpenChange={open => !open && onClose()}>
            <DialogContent className="w-full max-w-sm sm:max-w-md" showCloseButton={false}>
                <DialogHeader className="flex-row items-start justify-between gap-3 space-y-0">
                    <div className="space-y-1.5 text-left">
                        <DialogTitle>{certificate?.name}</DialogTitle>
                        <DialogDescription>Certificate details</DialogDescription>
                    </div>
                    <DialogClose asChild>
                        <Button type="button" variant="ghost" size="icon" className="size-8 shrink-0" aria-label="Close">
                            <XIcon className="size-4" aria-hidden />
                        </Button>
                    </DialogClose>
                </DialogHeader>
                {certificate ? (
                    <dl className="space-y-2 py-2 text-sm">
                        <div className="flex justify-between gap-4">
                            <dt className="text-muted-foreground">Uploaded</dt>
                            <dd>{formatApplicationDateTime(certificate.createdAt)}</dd>
                        </div>
                        <div className="flex justify-between gap-4">
                            <dt className="text-muted-foreground">Expiry</dt>
                            <dd>{certificate.endsAt ? formatApplicationDateTime(certificate.endsAt) : '—'}</dd>
                        </div>
                        <div className="flex justify-between gap-4">
                            <dt className="text-muted-foreground">Status</dt>
                            <dd>
                                <Badge variant={certificateStatusVariant(certificate.status)} className="text-xs">
                                    {certificateStatusLabel(certificate.status)}
                                </Badge>
                            </dd>
                        </div>
                    </dl>
                ) : null}
                <DialogFooter className="sm:justify-end">
                    <Button type="button" variant="outline" onClick={onClose}>
                        Close
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
