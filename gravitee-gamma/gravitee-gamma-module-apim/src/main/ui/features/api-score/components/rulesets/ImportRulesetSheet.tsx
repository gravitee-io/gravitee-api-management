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
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    Textarea,
} from '@gravitee/graphene-core';
import { UploadIcon } from '@gravitee/graphene-core/icons';
import { useId, useState } from 'react';

import { FileDropField } from './FileDropField';
import type { ImportRulesetRequest, ScoringAssetFormat } from '../../types/apiScore';
import {
    GRAVITEE_FORMAT_OPTIONS,
    resolveImportFormat,
    RULESET_DESCRIPTION_MAX,
    RULESET_FILE_EXTENSIONS,
    RULESET_IMPORT_KIND_OPTIONS,
    RULESET_NAME_MAX,
    type RulesetImportKind,
} from '../../utils/scoreFormat';

interface ImportRulesetSheetProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    onImport: (request: ImportRulesetRequest) => void;
    isSubmitting?: boolean;
}

export function ImportRulesetSheet({ open, onOpenChange, onImport, isSubmitting }: ImportRulesetSheetProps) {
    const nameId = useId();
    const descriptionId = useId();

    const [kind, setKind] = useState<RulesetImportKind | ''>('');
    const [graviteeFormat, setGraviteeFormat] = useState<ScoringAssetFormat | null>(null);
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [fileName, setFileName] = useState<string | null>(null);
    const [fileContent, setFileContent] = useState<string | null>(null);
    const [prevOpen, setPrevOpen] = useState(open);

    if (prevOpen !== open) {
        setPrevOpen(open);
        if (open) {
            setKind('');
            setGraviteeFormat(null);
            setName('');
            setDescription('');
            setFileName(null);
            setFileContent(null);
        }
    }

    const format = kind === '' ? null : resolveImportFormat(kind, graviteeFormat);
    const nameValid = name.trim().length > 0 && name.length <= RULESET_NAME_MAX;
    const descriptionValid = description.length <= RULESET_DESCRIPTION_MAX;
    const canSubmit = Boolean(format) && nameValid && descriptionValid && Boolean(fileContent) && !isSubmitting;

    const handleSubmit = () => {
        if (!format || !fileContent) return;
        onImport({ name: name.trim(), description: description.trim(), payload: fileContent, format });
    };

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent side="right" className="max-w-lg">
                <SheetHeader>
                    <SheetTitle>Import a Ruleset</SheetTitle>
                    <SheetDescription>
                        Custom rulesets allow you to enforce your organization&apos;s API design, quality and security standards.
                    </SheetDescription>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-5 overflow-y-auto px-4">
                    <div className="space-y-2">
                        <Label>Asset Format</Label>
                        <Select
                            value={kind}
                            onValueChange={value => {
                                setKind(value as RulesetImportKind);
                                setGraviteeFormat(null);
                            }}
                        >
                            <SelectTrigger className="w-full">
                                <SelectValue placeholder="Choose the format the ruleset applies to…" />
                            </SelectTrigger>
                            <SelectContent>
                                {RULESET_IMPORT_KIND_OPTIONS.map(option => (
                                    <SelectItem key={option.value} value={option.value}>
                                        {option.label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    {kind === 'GRAVITEE' && (
                        <div className="space-y-2">
                            <Label>Gravitee API Format</Label>
                            <Select value={graviteeFormat ?? ''} onValueChange={value => setGraviteeFormat(value as ScoringAssetFormat)}>
                                <SelectTrigger className="w-full">
                                    <SelectValue placeholder="Select a Gravitee API format…" />
                                </SelectTrigger>
                                <SelectContent>
                                    {GRAVITEE_FORMAT_OPTIONS.map(option => (
                                        <SelectItem key={option.value} value={option.value}>
                                            {option.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    )}

                    <div className="space-y-2">
                        <Label htmlFor={nameId}>Name</Label>
                        <Input
                            id={nameId}
                            value={name}
                            onChange={e => setName(e.target.value)}
                            placeholder="Set your custom name"
                            maxLength={RULESET_NAME_MAX}
                        />
                        <p className="text-xs text-muted-foreground">
                            Use this custom name to organize and identify the ruleset more easily.
                        </p>
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor={descriptionId}>Description</Label>
                        <Textarea
                            id={descriptionId}
                            value={description}
                            onChange={e => setDescription(e.target.value)}
                            maxLength={RULESET_DESCRIPTION_MAX}
                            rows={3}
                        />
                        <p className="text-right text-xs text-muted-foreground">
                            {description.length}/{RULESET_DESCRIPTION_MAX}
                        </p>
                    </div>

                    <div className="space-y-2">
                        <Label>File</Label>
                        <FileDropField
                            extensions={RULESET_FILE_EXTENSIONS}
                            fileName={fileName}
                            onFileRead={(name, content) => {
                                setFileName(name);
                                setFileContent(content);
                            }}
                        />
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
