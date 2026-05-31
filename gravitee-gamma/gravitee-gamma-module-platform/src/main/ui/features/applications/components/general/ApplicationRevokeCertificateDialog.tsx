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
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';

import type { ClientCertificate } from '../../types/applicationCertificate';

export function ApplicationRevokeCertificateDialog({
    certificate,
    onClose,
    onConfirm,
    isLoading,
}: Readonly<{
    certificate: ClientCertificate | null;
    onClose: () => void;
    onConfirm: () => void;
    isLoading: boolean;
}>) {
    return (
        <Dialog open={certificate !== null} onOpenChange={open => !open && onClose()}>
            <DialogContent className="w-full max-w-sm sm:max-w-md" showCloseButton={false}>
                <DialogHeader>
                    <DialogTitle>Revoke certificate?</DialogTitle>
                    <DialogDescription>
                        Revoking <strong>{certificate?.name}</strong> cannot be undone. Active mTLS connections using this certificate will
                        fail.
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter className="sm:justify-end">
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" disabled={isLoading} onClick={onConfirm}>
                        {isLoading ? 'Revoking…' : 'Revoke'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
