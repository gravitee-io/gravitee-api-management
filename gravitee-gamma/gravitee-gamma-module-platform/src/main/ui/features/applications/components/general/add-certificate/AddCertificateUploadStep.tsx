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
    FileUpload,
    Input,
    Label,
    Textarea,
    type FileRejection,
    type FileUploadItemValue,
} from '@gravitee/graphene-core';
import { useCallback, useEffect, useMemo, useState } from 'react';

import { ADD_CERTIFICATE_TEST_IDS } from './addCertificateTestIds';
import { PEM_PLACEHOLDER } from './addCertificateTypes';

const CERTIFICATE_ACCEPT = {
    'application/x-pem-file': ['.pem'],
    'application/x-x509-ca-cert': ['.crt', '.cer'],
    'text/plain': ['.pem', '.crt', '.cer'],
} as const;

function rejectionMessages(rejections: FileRejection[]): string[] {
    return rejections.flatMap(rejection =>
        rejection.errors.map(error => {
            if (error.code === 'file-invalid-type') {
                return 'Supported file formats: .pem, .crt';
            }
            return error.message;
        }),
    );
}

export function AddCertificateUploadStep({
    name,
    certificate,
    fileError,
    validationError,
    uploadTouched,
    isValidating,
    onNameChange,
    onCertificateChange,
    onFileSelected,
}: Readonly<{
    name: string;
    certificate: string;
    fileError: string | null;
    validationError: string | null;
    uploadTouched: boolean;
    isValidating: boolean;
    onNameChange: (value: string) => void;
    onCertificateChange: (value: string) => void;
    onFileSelected: (file: File) => void;
}>) {
    const [items, setItems] = useState<FileUploadItemValue[]>([]);
    const [rejectErrors, setRejectErrors] = useState<string[]>([]);
    const showNameRequired = uploadTouched && !name.trim();
    const showCertificateRequired = uploadTouched && !certificate.trim();

    useEffect(() => {
        if (!certificate.trim()) {
            setItems([]);
        }
    }, [certificate]);

    useEffect(() => {
        if (!fileError) return;
        setItems(previous =>
            previous.map(item => ({
                ...item,
                error: fileError,
                status: 'error' as const,
            })),
        );
    }, [fileError]);

    const uploadErrors = useMemo(() => {
        const errors = [...rejectErrors];
        if (fileError && !errors.includes(fileError)) {
            errors.push(fileError);
        }
        return errors;
    }, [fileError, rejectErrors]);

    const handleFilesAdd = useCallback(
        (files: File[]) => {
            const file = files[0];
            if (!file) return;

            setRejectErrors([]);
            setItems([
                {
                    id: `${file.name}-${file.size}-${file.lastModified}`,
                    file,
                    status: 'idle',
                },
            ]);
            onFileSelected(file);
        },
        [onFileSelected],
    );

    const handleFilesReject = useCallback((rejections: FileRejection[]) => {
        setRejectErrors(rejectionMessages(rejections));
    }, []);

    const handleFileRemove = useCallback((id: string) => {
        setItems(previous => previous.filter(item => item.id !== id));
        setRejectErrors([]);
    }, []);

    useEffect(() => {
        if (items.length !== 1 || !certificate.trim() || fileError) return;
        setItems(previous =>
            previous.map(item => ({
                ...item,
                status: 'success' as const,
                error: undefined,
            })),
        );
    }, [certificate, fileError, items.length]);

    return (
        <div className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="cert-name">Certificate Name*</Label>
                <Input
                    id="cert-name"
                    data-testid={ADD_CERTIFICATE_TEST_IDS.nameInput}
                    value={name}
                    onChange={e => onNameChange(e.target.value)}
                    maxLength={256}
                    aria-invalid={showNameRequired}
                />
                {showNameRequired ? <p className="text-xs text-destructive">Certificate name is required.</p> : null}
            </div>

            <div className="space-y-3 rounded-lg border border-border p-3">
                <p className="text-sm font-medium">Paste certificate</p>
                <div className="space-y-2">
                    <Label htmlFor="cert-pem">Certificate*</Label>
                    <Textarea
                        id="cert-pem"
                        data-testid={ADD_CERTIFICATE_TEST_IDS.pemInput}
                        value={certificate}
                        onChange={e => onCertificateChange(e.target.value)}
                        rows={3}
                        placeholder={PEM_PLACEHOLDER}
                        className="field-sizing-fixed min-h-[4.5rem] resize-y overflow-y-auto font-mono text-xs"
                        aria-invalid={showCertificateRequired}
                    />
                    <p className="text-xs text-muted-foreground">
                        {isValidating ? 'Validating certificate...' : 'Paste the PEM-encoded client certificate'}
                    </p>
                    {showCertificateRequired ? <p className="text-xs text-destructive">Certificate is required.</p> : null}
                </div>
            </div>

            <div className="space-y-3 rounded-lg border border-border p-3">
                <p className="text-sm font-medium">Upload file</p>
                <FileUpload
                    accept={CERTIFICATE_ACCEPT}
                    disabled={isValidating}
                    errors={uploadErrors}
                    hint="Supported file formats: .pem, .crt"
                    invalid={uploadErrors.length > 0}
                    items={items}
                    label="Drag and drop a file or click to browse"
                    maxFiles={1}
                    onFileRemove={handleFileRemove}
                    onFilesAdd={handleFilesAdd}
                    onFilesReject={handleFilesReject}
                />
            </div>

            {validationError ? (
                <Alert variant="destructive" data-testid={ADD_CERTIFICATE_TEST_IDS.validationErrorBanner}>
                    <AlertDescription>{validationError}</AlertDescription>
                </Alert>
            ) : null}
        </div>
    );
}
