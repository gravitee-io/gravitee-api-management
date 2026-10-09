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
import { Button, Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@gravitee/graphene-core';
import { UploadIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { ImportSourceOptionsFields } from './ImportSourceOptionsFields';
import { SegmentedTabs } from './SegmentedTabs';
import { useImportSourceOptions } from '../../../hooks/useImportSourceOptions';
import type { ApiImportFormat, ApiImportSubmission } from '../../../types';
import { readableImportError } from '../../../utils/importFileValidation';

const FORMAT_TABS: { id: ApiImportFormat; label: string }[] = [
    { id: 'gravitee', label: 'Gravitee definition' },
    { id: 'openapi', label: 'OpenAPI specification' },
    { id: 'wsdl', label: 'WSDL' },
];

export function ImportApiSheet({
    open,
    onOpenChange,
    onImport,
    isImporting,
    error,
}: Readonly<{
    open: boolean;
    onOpenChange: (v: boolean) => void;
    onImport: (submission: ApiImportSubmission) => void;
    isImporting: boolean;
    error?: string | null;
}>) {
    const [format, setFormat] = useState<ApiImportFormat>('gravitee');
    const source = useImportSourceOptions(format, open);

    const [prevOpen, setPrevOpen] = useState(open);
    if (prevOpen !== open) {
        setPrevOpen(open);
        if (open) {
            setFormat('gravitee');
            source.reset();
        }
    }

    const canSubmit = !isImporting && source.canSubmit;
    const importError = readableImportError(error);

    const handleSubmit = () => {
        if (!canSubmit) return;
        onImport(source.buildSubmission());
    };

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent side="right" style={{ maxWidth: '32rem' }}>
                <SheetHeader>
                    <SheetTitle>Import API Definition</SheetTitle>
                    <SheetDescription>
                        Update this API by importing a Gravitee definition, an OpenAPI specification, or a WSDL.
                    </SheetDescription>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto px-4">
                    <div className="space-y-2">
                        <p className="text-sm font-medium">API format</p>
                        <SegmentedTabs tabs={FORMAT_TABS} activeId={format} onChange={setFormat} ariaLabel="API format" />
                    </div>

                    <ImportSourceOptionsFields format={format} state={source} />

                    {importError && <p className="text-sm text-destructive">{importError}</p>}
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={isImporting}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={!canSubmit} onClick={handleSubmit}>
                        <UploadIcon className="size-4" aria-hidden /> {isImporting ? 'Importing…' : 'Import'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
