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
import { FileUpIcon, UploadIcon } from '@gravitee/graphene-core/icons';
import { useRef, useState } from 'react';

export function ImportDialog({
    open,
    onOpenChange,
    onImport,
    isLoading,
    error,
}: Readonly<{
    open: boolean;
    onOpenChange: (v: boolean) => void;
    onImport: (definition: unknown) => void;
    isLoading: boolean;
    error?: string | null;
}>) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [fileName, setFileName] = useState<string | null>(null);
    const [definition, setDefinition] = useState<unknown>(null);
    const [parseError, setParseError] = useState<string | null>(null);

    const handleFile = async (file: File) => {
        setParseError(null);
        setFileName(file.name);
        try {
            setDefinition(JSON.parse(await file.text()) as unknown);
        } catch {
            setParseError('Invalid JSON. Please upload a valid Gravitee API definition file.');
            setDefinition(null);
        }
    };

    const reset = () => {
        setFileName(null);
        setDefinition(null);
        setParseError(null);
    };

    return (
        <Dialog
            open={open}
            onOpenChange={v => {
                onOpenChange(v);
                if (!v) reset();
            }}
        >
            <DialogContent style={{ maxWidth: '512px' }}>
                <DialogHeader>
                    <DialogTitle>Import API Definition</DialogTitle>
                    <DialogDescription>Upload a Gravitee JSON definition to overwrite the current API configuration.</DialogDescription>
                </DialogHeader>
                <div className="py-2 space-y-3">
                    <div
                        role="button"
                        tabIndex={0}
                        onClick={() => inputRef.current?.click()}
                        onKeyDown={e => (e.key === 'Enter' || e.key === ' ') && inputRef.current?.click()}
                        className="flex items-center justify-center rounded-lg border-dashed bg-muted/40 p-6 cursor-pointer hover:border-primary/40 transition-colors"
                        style={{ borderWidth: '2px' }}
                    >
                        <div className="text-center space-y-1">
                            <FileUpIcon className="size-7 text-muted-foreground mx-auto" />
                            <p className="text-sm font-medium">{fileName ?? 'Drop file here or click to browse'}</p>
                            <p className="text-xs text-muted-foreground">Gravitee JSON</p>
                        </div>
                    </div>
                    {parseError && <p className="text-xs text-destructive">{parseError}</p>}
                    {error && <p className="text-sm text-destructive">{error}</p>}
                    <input
                        ref={inputRef}
                        type="file"
                        accept=".json,application/json"
                        className="sr-only"
                        onChange={async e => {
                            const file = e.target.files?.[0];
                            if (file) await handleFile(file);
                            e.target.value = '';
                        }}
                    />
                </div>
                <DialogFooter>
                    <DialogClose asChild>
                        <Button type="button" variant="outline" onClick={reset}>
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" disabled={!definition || isLoading} onClick={() => definition && onImport(definition)}>
                        <UploadIcon className="size-4" /> {isLoading ? 'Importing…' : 'Import'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
