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
    Input,
    Label,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    Switch,
} from '@gravitee/graphene-core';
import { FileUpIcon, UploadIcon } from '@gravitee/graphene-core/icons';
import { useQuery } from '@tanstack/react-query';
import { useRef, useState } from 'react';

import { SegmentedTabs } from './SegmentedTabs';
import { SelectionCards } from './SelectionCards';
import { policyStudioKeys } from '../../../hooks/usePolicyStudioData';
import { listPolicies } from '../../../services/policyStudioService';
import type { ApiImportFormat, ApiImportSubmission } from '../../../types';

type SourceMode = 'local' | 'remote';

const FORMAT_TABS: { id: ApiImportFormat; label: string }[] = [
    { id: 'gravitee', label: 'Gravitee definition' },
    { id: 'openapi', label: 'OpenAPI specification' },
    { id: 'wsdl', label: 'WSDL' },
];

const SOURCE_CARDS: { id: SourceMode; label: string; description: string }[] = [
    { id: 'local', label: 'Local file', description: 'Upload a file from your computer' },
    { id: 'remote', label: 'Remote URL', description: 'Fetch the definition from a URL' },
];

const OAS_VALIDATION_POLICY_ID = 'oas-validation';
const REST_TO_SOAP_POLICY_ID = 'rest-to-soap';

function isValidHttpUrl(value: string): boolean {
    try {
        const url = new URL(value.trim());
        return url.protocol === 'http:' || url.protocol === 'https:';
    } catch {
        return false;
    }
}

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
    const inputRef = useRef<HTMLInputElement>(null);

    const [format, setFormat] = useState<ApiImportFormat>('gravitee');
    const [sourceMode, setSourceMode] = useState<SourceMode>('local');
    const [fileName, setFileName] = useState<string | null>(null);
    const [fileText, setFileText] = useState<string | null>(null);
    const [definition, setDefinition] = useState<unknown>(null);
    const [parseError, setParseError] = useState<string | null>(null);
    const [remoteUrl, setRemoteUrl] = useState('');
    const [withDocumentation, setWithDocumentation] = useState(true);
    const [withOASValidationPolicy, setWithOASValidationPolicy] = useState(false);
    const [withRestToSoap, setWithRestToSoap] = useState(false);

    // Options (and the policies they depend on) only apply to OpenAPI/WSDL, and only once the sheet is open.
    const { data: policies, isError: policiesError } = useQuery({
        queryKey: policyStudioKeys.policies(),
        queryFn: listPolicies,
        enabled: open && (format === 'openapi' || format === 'wsdl'),
    });
    const hasOasValidationPolicy = (policies ?? []).some(p => p.id === OAS_VALIDATION_POLICY_ID);
    const hasRestToSoapPolicy = (policies ?? []).some(p => p.id === REST_TO_SOAP_POLICY_ID);

    // Seed withOASValidationPolicy's default once per (format, policy-availability) pair —
    // mirrors classic console, which defaults the toggle on only when the policy is installed.
    const [seededOasKey, setSeededOasKey] = useState<string | null>(null);
    const oasKey = `openapi:${hasOasValidationPolicy}`;
    if (format === 'openapi' && oasKey !== seededOasKey) {
        setSeededOasKey(oasKey);
        setWithOASValidationPolicy(hasOasValidationPolicy);
    }

    // Seed WSDL's options once per (rest-to-soap-availability, oas-availability) pair — mirrors classic
    // console: REST-to-SOAP defaults on only if installed, and enabling it defaults documentation + OAS
    // validation on too.
    const [seededWsdlKey, setSeededWsdlKey] = useState<string | null>(null);
    const wsdlKey = `wsdl:${hasRestToSoapPolicy}:${hasOasValidationPolicy}`;
    if (format === 'wsdl' && wsdlKey !== seededWsdlKey) {
        setSeededWsdlKey(wsdlKey);
        setWithRestToSoap(hasRestToSoapPolicy);
        setWithDocumentation(hasRestToSoapPolicy);
        setWithOASValidationPolicy(hasRestToSoapPolicy && hasOasValidationPolicy);
    }

    const resetSourceState = () => {
        setSourceMode('local');
        setFileName(null);
        setFileText(null);
        setDefinition(null);
        setParseError(null);
        setRemoteUrl('');
    };

    // Resets Options to the defaults for `nextFormat` — only OpenAPI starts with documentation on;
    // WSDL's options are seeded once its policies are known (see the wsdlKey effect above).
    const resetOptionsState = (nextFormat: ApiImportFormat) => {
        setWithDocumentation(nextFormat === 'openapi');
        setWithOASValidationPolicy(false);
        setWithRestToSoap(false);
        setSeededOasKey(null);
        setSeededWsdlKey(null);
    };

    const [prevOpen, setPrevOpen] = useState(open);
    if (prevOpen !== open) {
        setPrevOpen(open);
        if (open) {
            setFormat('gravitee');
            resetSourceState();
            resetOptionsState('gravitee');
        }
    }

    const handleFormatChange = (next: ApiImportFormat) => {
        setFormat(next);
        resetSourceState();
        resetOptionsState(next);
    };

    const handleSourceModeChange = (next: SourceMode) => {
        setSourceMode(next);
        setFileName(null);
        setFileText(null);
        setDefinition(null);
        setParseError(null);
        setRemoteUrl('');
    };

    // Mirrors classic console's syncRestToSoapDependentsEffect: for WSDL, documentation + OAS
    // validation are entirely driven by REST-to-SOAP (they operate on the OpenAPI spec derived from
    // the WSDL via that transformer, so they're meaningless — and disabled — without it).
    const handleRestToSoapChange = (checked: boolean) => {
        setWithRestToSoap(checked);
        setWithDocumentation(checked);
        setWithOASValidationPolicy(checked && hasOasValidationPolicy);
    };

    const handleFile = async (file: File) => {
        setParseError(null);
        setFileName(file.name);
        const text = await file.text();
        if (format === 'gravitee') {
            try {
                setDefinition(JSON.parse(text) as unknown);
                setFileText(text);
            } catch {
                setParseError('Invalid JSON. Please upload a valid Gravitee API definition file.');
                setDefinition(null);
                setFileText(null);
            }
        } else {
            setDefinition(null);
            setFileText(text);
        }
    };

    const fileAccept =
        format === 'gravitee'
            ? '.json,application/json'
            : format === 'openapi'
              ? '.json,.yml,.yaml,application/json,text/yaml,text/x-yaml'
              : '.wsdl,.xml,application/xml,text/xml';
    const fileHint = format === 'gravitee' ? 'Gravitee JSON' : format === 'openapi' ? 'OpenAPI JSON or YAML' : 'WSDL XML';
    const urlLabel = format === 'gravitee' ? 'Definition URL' : format === 'openapi' ? 'Specification URL' : 'WSDL URL';
    const urlPlaceholder =
        format === 'gravitee'
            ? 'https://example.com/api-definition.json'
            : format === 'openapi'
              ? 'https://example.com/openapi.yaml'
              : 'https://example.com/service.wsdl';

    const canSubmit =
        !isImporting &&
        (sourceMode === 'remote'
            ? isValidHttpUrl(remoteUrl)
            : format === 'gravitee'
              ? definition !== null && !parseError
              : fileText !== null && fileText.trim() !== '');

    const handleSubmit = () => {
        if (!canSubmit) return;
        if (format === 'gravitee') {
            onImport(
                sourceMode === 'remote'
                    ? { format: 'gravitee', source: 'remote', url: remoteUrl.trim() }
                    : { format: 'gravitee', source: 'local', definition },
            );
            return;
        }
        const payload = sourceMode === 'remote' ? remoteUrl.trim() : (fileText as string);
        if (format === 'wsdl') {
            onImport({
                format: 'wsdl',
                descriptor: {
                    payload,
                    type: sourceMode === 'remote' ? 'URL' : 'INLINE',
                    withDocumentation,
                    ...(hasOasValidationPolicy ? { withOASValidationPolicy } : {}),
                    // Always send withPolicies (never omit it): the backend treats a missing/null
                    // value as "skip nothing" but an empty array as "skip flow regeneration"
                    // (WsdlToUpdateApiUseCase#skipFlows) — omitting it when REST-to-SOAP is off would
                    // silently regenerate flows instead of preserving them, unlike classic console.
                    withPolicies: withRestToSoap ? [REST_TO_SOAP_POLICY_ID] : [],
                },
            });
            return;
        }
        onImport({
            format: 'openapi',
            descriptor: {
                payload,
                withDocumentation,
                ...(hasOasValidationPolicy ? { withOASValidationPolicy } : {}),
            },
        });
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
                        <SegmentedTabs tabs={FORMAT_TABS} activeId={format} onChange={handleFormatChange} ariaLabel="API format" />
                    </div>

                    <div className="space-y-2">
                        <p className="text-sm font-medium">Configure file source</p>

                        <SelectionCards
                            options={SOURCE_CARDS}
                            activeId={sourceMode}
                            onChange={handleSourceModeChange}
                            ariaLabel="File source"
                        />

                        {sourceMode === 'remote' ? (
                            <div className="space-y-1">
                                <Label htmlFor="import-remote-url">{urlLabel}</Label>
                                <Input
                                    id="import-remote-url"
                                    value={remoteUrl}
                                    onChange={e => setRemoteUrl(e.target.value)}
                                    placeholder={urlPlaceholder}
                                />
                                {remoteUrl.trim() !== '' && !isValidHttpUrl(remoteUrl) && (
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
                                        <p className="text-sm font-medium">{fileName ?? 'Drop file here or click to browse'}</p>
                                        <p className="text-xs text-muted-foreground">{fileHint}</p>
                                    </div>
                                </div>
                                {parseError && <p className="text-xs text-destructive">{parseError}</p>}
                                <input
                                    ref={inputRef}
                                    type="file"
                                    accept={fileAccept}
                                    className="sr-only"
                                    onChange={async e => {
                                        const file = e.target.files?.[0];
                                        if (file) await handleFile(file);
                                        e.target.value = '';
                                    }}
                                />
                            </>
                        )}
                    </div>

                    {(format === 'openapi' || format === 'wsdl') && (
                        <div className="space-y-3">
                            <p className="text-sm font-medium">Options</p>
                            {policiesError && (
                                <p className="text-xs text-destructive">
                                    Could not check which policies are installed — some options may be unavailable.
                                </p>
                            )}
                            {format === 'wsdl' && hasRestToSoapPolicy && (
                                <div className="flex items-center justify-between gap-4 rounded-lg border bg-muted/40 px-4 py-3">
                                    <div>
                                        <p className="text-sm font-medium">Apply REST to SOAP Transformer policy</p>
                                        <p className="text-xs text-muted-foreground">This will overwrite all the existing policy.</p>
                                    </div>
                                    <Switch checked={withRestToSoap} onCheckedChange={handleRestToSoapChange} />
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
                                <Switch
                                    checked={withDocumentation}
                                    onCheckedChange={setWithDocumentation}
                                    disabled={format === 'wsdl' && !withRestToSoap}
                                />
                            </div>
                            {hasOasValidationPolicy && (
                                <div className="flex items-center justify-between gap-4 rounded-lg border bg-muted/40 px-4 py-3">
                                    <div>
                                        <p className="text-sm font-medium">Add OpenAPI Specification Validation</p>
                                        <p className="text-xs text-muted-foreground">
                                            Adds an OpenAPI Specification validation policy with all options enabled (you can change this
                                            later).
                                        </p>
                                    </div>
                                    <Switch
                                        checked={withOASValidationPolicy}
                                        onCheckedChange={setWithOASValidationPolicy}
                                        disabled={format === 'wsdl' && !withRestToSoap}
                                    />
                                </div>
                            )}
                        </div>
                    )}

                    {error && <p className="text-sm text-destructive">{error}</p>}
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
