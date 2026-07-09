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
import { Input, Label, Switch } from '@gravitee/graphene-core';
import { FileUpIcon } from '@gravitee/graphene-core/icons';
import { useRef } from 'react';

import { SelectionCards } from './SelectionCards';
import { isValidHttpUrl } from '../../../hooks/useImportSourceOptions';
import type { ImportSourceMode, UseImportSourceOptionsResult } from '../../../hooks/useImportSourceOptions';
import type { ApiImportFormat } from '../../../types';

const SOURCE_CARDS: { id: ImportSourceMode; label: string; description: string }[] = [
    { id: 'local', label: 'Local file', description: 'Upload a file from your computer' },
    { id: 'remote', label: 'Remote URL', description: 'Fetch the definition from a URL' },
];

/** Renders the "Configure file source" + "Options" sections, driven entirely by a `useImportSourceOptions` result. */
export function ImportSourceOptionsFields({
    format,
    state,
}: Readonly<{
    format: ApiImportFormat;
    state: UseImportSourceOptionsResult;
}>) {
    const inputRef = useRef<HTMLInputElement>(null);

    return (
        <>
            <div className="space-y-2">
                <p className="text-sm font-medium">Configure file source</p>

                <SelectionCards
                    options={SOURCE_CARDS}
                    activeId={state.sourceMode}
                    onChange={state.handleSourceModeChange}
                    ariaLabel="File source"
                />

                {state.sourceMode === 'remote' ? (
                    <div className="space-y-1">
                        <Label htmlFor="import-remote-url">{state.urlLabel}</Label>
                        <Input
                            id="import-remote-url"
                            value={state.remoteUrl}
                            onChange={e => state.setRemoteUrl(e.target.value)}
                            placeholder={state.urlPlaceholder}
                        />
                        {state.remoteUrl.trim() !== '' && !isValidHttpUrl(state.remoteUrl) && (
                            <p className="text-xs text-destructive">Enter a valid http(s) URL.</p>
                        )}
                    </div>
                ) : (
                    <>
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
                                <p className="text-sm font-medium">{state.fileName ?? 'Drop file here or click to browse'}</p>
                                <p className="text-xs text-muted-foreground">{state.fileHint}</p>
                            </div>
                        </div>
                        {state.parseError && <p className="text-xs text-destructive">{state.parseError}</p>}
                        <input
                            ref={inputRef}
                            type="file"
                            accept={state.fileAccept}
                            className="sr-only"
                            onChange={async e => {
                                const file = e.target.files?.[0];
                                if (file) await state.handleFile(file);
                                e.target.value = '';
                            }}
                        />
                    </>
                )}
            </div>

            {(format === 'openapi' || format === 'wsdl') && (
                <div className="space-y-3">
                    <p className="text-sm font-medium">Options</p>
                    {format === 'wsdl' && state.hasRestToSoapPolicy && (
                        <div className="flex items-center justify-between gap-4 rounded-lg border bg-muted/40 px-4 py-3">
                            <div>
                                <p className="text-sm font-medium">Apply REST to SOAP Transformer policy</p>
                                <p className="text-xs text-muted-foreground">This will overwrite all the existing policy.</p>
                            </div>
                            <Switch checked={state.withRestToSoap} onCheckedChange={state.handleRestToSoapChange} />
                        </div>
                    )}
                    <div className="flex items-center justify-between gap-4 rounded-lg border bg-muted/40 px-4 py-3">
                        <div>
                            <p className="text-sm font-medium">Create documentation page from spec</p>
                            <p className="text-xs text-muted-foreground">
                                Adds a documentation page for this API using the OpenAPI spec used during the import. Published
                                automatically (you can change this later).
                            </p>
                        </div>
                        <Switch checked={state.withDocumentation} onCheckedChange={state.setWithDocumentation} />
                    </div>
                    {state.hasOasValidationPolicy && (
                        <div className="flex items-center justify-between gap-4 rounded-lg border bg-muted/40 px-4 py-3">
                            <div>
                                <p className="text-sm font-medium">Add OpenAPI Specification Validation</p>
                                <p className="text-xs text-muted-foreground">
                                    Adds an OpenAPI Specification validation policy with all options enabled (you can change this later).
                                </p>
                            </div>
                            <Switch checked={state.withOASValidationPolicy} onCheckedChange={state.setWithOASValidationPolicy} />
                        </div>
                    )}
                </div>
            )}
        </>
    );
}
