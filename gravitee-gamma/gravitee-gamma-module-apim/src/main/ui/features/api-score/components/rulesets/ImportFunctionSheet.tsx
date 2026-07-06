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
import { Button, Input, Label, Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@gravitee/graphene-core';
import { UploadIcon } from '@gravitee/graphene-core/icons';
import { useId, useState } from 'react';

import { FileDropField } from './FileDropField';
import type { ImportFunctionRequest } from '../../types/apiScore';
import { FUNCTION_FILE_EXTENSIONS, FUNCTION_NAME_MAX, FUNCTION_NAME_PATTERN } from '../../utils/scoreFormat';

interface ImportFunctionSheetProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    existingNames: string[];
    onImport: (request: ImportFunctionRequest) => void;
    isSubmitting?: boolean;
}

export function ImportFunctionSheet({ open, onOpenChange, existingNames, onImport, isSubmitting }: ImportFunctionSheetProps) {
    const nameId = useId();

    const [name, setName] = useState('');
    const [fileName, setFileName] = useState<string | null>(null);
    const [fileContent, setFileContent] = useState<string | null>(null);
    const [prevOpen, setPrevOpen] = useState(open);

    if (prevOpen !== open) {
        setPrevOpen(open);
        if (open) {
            setName('');
            setFileName(null);
            setFileContent(null);
        }
    }

    const nameValid = FUNCTION_NAME_PATTERN.test(name) && name.length <= FUNCTION_NAME_MAX;
    const willOverwrite = nameValid && existingNames.includes(name);
    const canSubmit = nameValid && Boolean(fileContent) && !isSubmitting;

    const handleSubmit = () => {
        if (!fileContent || !nameValid) return;
        onImport({ name, payload: fileContent });
    };

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent side="right" className="max-w-lg">
                <SheetHeader>
                    <SheetTitle>Import a Function</SheetTitle>
                    <SheetDescription>
                        Custom functions let you define specific logic or operations that extend the rulesets.
                    </SheetDescription>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-5 overflow-y-auto px-4">
                    <div className="space-y-2">
                        <Label>File</Label>
                        <FileDropField
                            extensions={FUNCTION_FILE_EXTENSIONS}
                            fileName={fileName}
                            onFileRead={(pickedName, content) => {
                                setFileName(pickedName);
                                setFileContent(content);
                                setName(pickedName);
                            }}
                        />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor={nameId}>Name</Label>
                        <Input
                            id={nameId}
                            value={name}
                            onChange={e => setName(e.target.value)}
                            placeholder="my-function.js"
                            maxLength={FUNCTION_NAME_MAX}
                        />
                        {name.length > 0 && !nameValid && (
                            <p className="text-xs text-destructive">
                                The name must be a valid file name ending in .js (no slashes), max {FUNCTION_NAME_MAX} characters.
                            </p>
                        )}
                        {willOverwrite && (
                            <p className="text-xs text-warning">A function with this name already exists and will be overwritten.</p>
                        )}
                    </div>
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={!canSubmit} onClick={handleSubmit}>
                        <UploadIcon className="size-4" aria-hidden />
                        {isSubmitting ? 'Importing…' : 'Import'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
