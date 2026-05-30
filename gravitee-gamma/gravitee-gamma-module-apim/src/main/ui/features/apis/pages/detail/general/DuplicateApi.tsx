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
import { CopyIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { CheckboxOptionList } from './CheckboxOptionList';
import { useDuplicateEntryValidation } from '../../../hooks/useDuplicateEntryValidation';
import type { DuplicateFilteredField } from '../../../types';
import { DUPLICATE_INCLUDE_OPTIONS, type DuplicateEntryMode, buildDuplicateFilteredFields } from '../../../utils/apiGeneralDuplicate';
import { validateDuplicateVersion } from '../../../utils/duplicateDialogValidation';

export function DuplicateApi({
    open,
    onOpenChange,
    initialVersion,
    entryMode,
    contextPathPlaceholder,
    hostPlaceholder,
    onDuplicate,
    isLoading,
    error,
}: Readonly<{
    open: boolean;
    onOpenChange: (v: boolean) => void;
    initialVersion: string;
    entryMode: DuplicateEntryMode;
    contextPathPlaceholder: string;
    hostPlaceholder: string;
    onDuplicate: (opts: { version: string; contextPath?: string; host?: string; filteredFields: DuplicateFilteredField[] }) => void;
    isLoading: boolean;
    error?: string | null;
}>) {
    const [version, setVersion] = useState('');
    const [contextPath, setContextPath] = useState('');
    const [host, setHost] = useState('');
    const [include, setInclude] = useState<Record<DuplicateFilteredField, boolean>>(
        () => Object.fromEntries(DUPLICATE_INCLUDE_OPTIONS.map(o => [o.id, true])) as Record<DuplicateFilteredField, boolean>,
    );
    const [versionError, setVersionError] = useState<string | null>(null);
    const [showValidation, setShowValidation] = useState(false);
    const [submitInFlight, setSubmitInFlight] = useState(false);
    const [prevOpen, setPrevOpen] = useState(open);

    const {
        contextPathError,
        hostError,
        verifying,
        entryValid,
        resetValidation,
        scheduleContextPathValidation,
        scheduleHostValidation,
        verifyContextPathNow,
        verifyHostNow,
        setContextPathError,
        setHostError,
    } = useDuplicateEntryValidation(entryMode);

    if (prevOpen !== open) {
        setPrevOpen(open);
        if (open) {
            setVersion('');
            setContextPath('');
            setHost('');
            setInclude(Object.fromEntries(DUPLICATE_INCLUDE_OPTIONS.map(o => [o.id, true])) as Record<DuplicateFilteredField, boolean>);
            setVersionError(null);
            setShowValidation(false);
            resetValidation();
        }
    }

    const versionValid = validateDuplicateVersion(version) === null;
    const entryFilled =
        entryMode === 'none' ||
        (entryMode === 'contextPath' && contextPath.trim().length > 0) ||
        (entryMode === 'host' && host.trim().length > 0);
    const canSubmit = versionValid && entryValid && entryFilled && !verifying && !submitInFlight && !isLoading;

    async function handleSubmit() {
        setShowValidation(true);
        const vError = validateDuplicateVersion(version);
        setVersionError(vError);
        if (vError || !entryFilled || isLoading || submitInFlight) return;

        setSubmitInFlight(true);
        try {
            if (entryMode === 'contextPath') {
                const pathErr = await verifyContextPathNow(contextPath);
                setContextPathError(pathErr);
                if (pathErr) return;
            }

            if (entryMode === 'host') {
                const hostErr = await verifyHostNow(host);
                setHostError(hostErr);
                if (hostErr) return;
            }

            onDuplicate({
                version: version.trim(),
                ...(entryMode === 'contextPath' ? { contextPath: contextPath.trim() } : {}),
                ...(entryMode === 'host' ? { host: host.trim() } : {}),
                filteredFields: buildDuplicateFilteredFields(include),
            });
        } finally {
            setSubmitInFlight(false);
        }
    }

    const displayVersionError = showValidation ? versionError : null;
    const displayContextPathError = showValidation || contextPath.length > 0 ? contextPathError : null;
    const displayHostError = showValidation || host.length > 0 ? hostError : null;

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent side="right" style={{ maxWidth: '32rem' }}>
                <SheetHeader>
                    <SheetTitle>Duplicate API</SheetTitle>
                    <SheetDescription>Create a copy of this API with a new context path and version.</SheetDescription>
                </SheetHeader>

                <div className="flex-1 space-y-6 overflow-y-auto px-4">
                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-[1fr_auto] sm:items-start sm:gap-3">
                        {entryMode === 'contextPath' && (
                            <div className="space-y-2 min-w-0">
                                <Label htmlFor="dup-path">
                                    Context path <span className="text-destructive">*</span>
                                </Label>
                                <Input
                                    id="dup-path"
                                    value={contextPath}
                                    onChange={e => {
                                        setContextPath(e.target.value);
                                        scheduleContextPathValidation(e.target.value);
                                    }}
                                    placeholder={contextPathPlaceholder}
                                    aria-invalid={Boolean(displayContextPathError)}
                                />
                                {displayContextPathError && <p className="text-xs text-destructive">{displayContextPathError}</p>}
                                {verifying && !displayContextPathError && (
                                    <p className="text-xs text-muted-foreground">Checking availability…</p>
                                )}
                            </div>
                        )}
                        {entryMode === 'host' && (
                            <div className="space-y-2 min-w-0">
                                <Label htmlFor="dup-host">
                                    Host <span className="text-destructive">*</span>
                                </Label>
                                <Input
                                    id="dup-host"
                                    value={host}
                                    onChange={e => {
                                        setHost(e.target.value);
                                        scheduleHostValidation(e.target.value);
                                    }}
                                    placeholder={hostPlaceholder}
                                    aria-invalid={Boolean(displayHostError)}
                                />
                                {displayHostError && <p className="text-xs text-destructive">{displayHostError}</p>}
                                {verifying && !displayHostError && <p className="text-xs text-muted-foreground">Checking availability…</p>}
                            </div>
                        )}
                        <div className="space-y-2 w-full sm:w-32 shrink-0">
                            <Label htmlFor="dup-version">
                                Version <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="dup-version"
                                value={version}
                                onChange={e => {
                                    setVersion(e.target.value);
                                    if (showValidation) setVersionError(validateDuplicateVersion(e.target.value));
                                }}
                                placeholder={initialVersion}
                                maxLength={32}
                                aria-invalid={Boolean(displayVersionError)}
                            />
                            {displayVersionError && <p className="text-xs text-destructive">{displayVersionError}</p>}
                        </div>
                    </div>

                    <div className="space-y-2">
                        <p className="text-sm font-medium">Include additional data</p>
                        <CheckboxOptionList
                            idPrefix="dup"
                            options={DUPLICATE_INCLUDE_OPTIONS}
                            values={include}
                            onChange={(id, checked) => setInclude(prev => ({ ...prev, [id]: checked }))}
                        />
                    </div>

                    {error && <p className="text-sm text-destructive">{error}</p>}
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={!canSubmit} onClick={() => void handleSubmit()}>
                        <CopyIcon className="size-4" aria-hidden />
                        {isLoading ? 'Duplicating…' : 'Duplicate'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
