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
import { Badge, Button, Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useCallback } from 'react';

import { certificateStatusLabel, certificateStatusVariant } from './certificateUtils';
import type { ClientCertificate } from '../../types/applicationCertificate';
import { formatApplicationDateTime } from '../../utils/applicationFormatters';

export function ApplicationCertificateDetailSheet({
    certificate,
    onClose,
}: Readonly<{
    certificate: ClientCertificate | null;
    onClose: () => void;
}>) {
    const handleOpenChange = useCallback(
        (open: boolean) => {
            if (!open) onClose();
        },
        [onClose],
    );

    return (
        <Sheet open={certificate !== null} onOpenChange={handleOpenChange}>
            <SheetContent side="right" showCloseButton={false} style={{ maxWidth: '480px' }}>
                <SheetHeader className="flex-row items-start justify-between gap-3 space-y-0">
                    <div className="space-y-1.5 text-left">
                        <SheetTitle>{certificate?.name}</SheetTitle>
                        <SheetDescription>Certificate details</SheetDescription>
                    </div>
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-8 shrink-0"
                        aria-label="Close"
                        onClick={() => handleOpenChange(false)}
                    >
                        <XIcon className="size-4" aria-hidden />
                    </Button>
                </SheetHeader>
                {certificate ? (
                    <dl className="space-y-2 px-4 py-2 text-sm">
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
                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
                        Close
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
