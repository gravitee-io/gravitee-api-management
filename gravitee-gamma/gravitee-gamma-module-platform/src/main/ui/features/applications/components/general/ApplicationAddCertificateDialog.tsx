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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import {
    Button,
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    cn,
} from '@gravitee/graphene-core';
import { FileUpIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useRef, useState } from 'react';

import { findActiveCertificate, readFileAsText } from './certificateUtils';
import { validateApplicationCertificate } from '../../services/applicationDetail';
import type { ClientCertificate } from '../../types/applicationCertificate';

export interface AddCertificateSubmit {
    name: string;
    certificate: string;
    endsAt?: string;
    gracePeriodEnd?: string;
    activeCertificateId?: string;
}

export function ApplicationAddCertificateDialog({
    open,
    onOpenChange,
    applicationId,
    certificates,
    onSubmit,
    isSubmitting,
    error,
}: Readonly<{
    open: boolean;
    onOpenChange: (open: boolean) => void;
    applicationId: string;
    certificates: ClientCertificate[];
    onSubmit: (payload: AddCertificateSubmit) => void;
    isSubmitting: boolean;
    error?: string | null;
}>) {
    const env = useEnvironment();
    const fileRef = useRef<HTMLInputElement>(null);
    const [name, setName] = useState('');
    const [certificate, setCertificate] = useState('');
    const [endsAt, setEndsAt] = useState('');
    const [gracePeriodEnd, setGracePeriodEnd] = useState('');
    const [fileError, setFileError] = useState<string | null>(null);
    const [validating, setValidating] = useState(false);

    const activeCert = findActiveCertificate(certificates);
    const hasActive = Boolean(activeCert);

    useEffect(() => {
        if (!open) {
            setName('');
            setCertificate('');
            setEndsAt('');
            setGracePeriodEnd('');
            setFileError(null);
        }
    }, [open]);

    const handleFile = useCallback(
        async (file: File | undefined) => {
            if (!file || !env) return;
            setFileError(null);
            try {
                const content = await readFileAsText(file);
                setValidating(true);
                await validateApplicationCertificate(env.id, applicationId, content);
                setCertificate(content);
            } catch (e) {
                setCertificate('');
                setFileError(e instanceof Error ? e.message : 'Invalid certificate.');
            } finally {
                setValidating(false);
            }
        },
        [applicationId, env],
    );

    const handleAdd = () => {
        if (!name.trim() || !certificate) return;
        const endsAtIso = endsAt ? new Date(endsAt).toISOString() : undefined;
        const graceIso = gracePeriodEnd ? new Date(gracePeriodEnd).toISOString() : undefined;
        onSubmit({
            name: name.trim(),
            certificate,
            endsAt: endsAtIso,
            ...(hasActive && graceIso && activeCert ? { gracePeriodEnd: graceIso, activeCertificateId: activeCert.id } : {}),
        });
    };

    const canSubmit =
        Boolean(name.trim()) && Boolean(certificate) && !isSubmitting && !validating && (!hasActive || Boolean(gracePeriodEnd));

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="w-[clamp(20rem,33vw,36rem)] max-w-[clamp(20rem,33vw,36rem)]" showCloseButton={false}>
                <DialogHeader className="flex-row items-start justify-between gap-3 space-y-0">
                    <div className="space-y-1.5 text-left">
                        <DialogTitle>Add certificate</DialogTitle>
                        <DialogDescription>Upload a PEM client certificate for mutual TLS authentication.</DialogDescription>
                    </div>
                    <DialogClose asChild>
                        <Button type="button" variant="ghost" size="icon" className="size-8 shrink-0" aria-label="Close">
                            <XIcon className="size-4" aria-hidden />
                        </Button>
                    </DialogClose>
                </DialogHeader>

                <div className="space-y-4 py-2">
                    <div className="space-y-2">
                        <Label htmlFor="cert-name">Certificate name</Label>
                        <Input id="cert-name" value={name} onChange={e => setName(e.target.value)} placeholder="e.g. my-app-mtls-2026" />
                    </div>
                    <div className="space-y-2">
                        <Label htmlFor="cert-expiry">Expiry date (optional)</Label>
                        <Input id="cert-expiry" type="date" value={endsAt} onChange={e => setEndsAt(e.target.value)} />
                    </div>
                    {hasActive ? (
                        <div className="space-y-2">
                            <Label htmlFor="cert-grace">Grace period end (required)</Label>
                            <Input id="cert-grace" type="date" value={gracePeriodEnd} onChange={e => setGracePeriodEnd(e.target.value)} />
                            <p className="text-xs text-muted-foreground">
                                Sets when the current active certificate ({activeCert?.name}) stops being used.
                            </p>
                        </div>
                    ) : null}
                    <button
                        type="button"
                        onClick={() => fileRef.current?.click()}
                        disabled={validating}
                        className={cn(
                            'flex min-h-36 w-full flex-col items-center justify-center rounded-lg border-2 border-dashed border-border bg-muted/30 p-6 transition-colors',
                            validating ? 'opacity-60' : 'cursor-pointer hover:border-primary',
                        )}
                    >
                        <FileUpIcon className="size-7 text-muted-foreground/50" aria-hidden />
                        <p className="mt-2 text-sm font-medium">{validating ? 'Validating…' : 'Drop PEM file or click to browse'}</p>
                        {certificate ? <p className="mt-1 text-xs text-success">Certificate loaded</p> : null}
                        {fileError ? <p className="mt-1 text-xs text-destructive">{fileError}</p> : null}
                    </button>
                    <input
                        ref={fileRef}
                        type="file"
                        accept=".pem,.crt,.cer,text/plain"
                        className="sr-only"
                        onChange={e => {
                            void handleFile(e.target.files?.[0]);
                            e.target.value = '';
                        }}
                    />
                    {error ? <p className="text-sm text-destructive">{error}</p> : null}
                </div>

                <DialogFooter className="sm:justify-end">
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" onClick={handleAdd} disabled={!canSubmit}>
                        {isSubmitting ? 'Adding…' : 'Add certificate'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
