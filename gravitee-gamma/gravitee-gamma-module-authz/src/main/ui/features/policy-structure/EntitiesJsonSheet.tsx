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
import { Button, Sheet, SheetContent, SheetTitle } from '@gravitee/graphene-core';
import { CheckIcon, CopyIcon, DownloadIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';
import { buildEntitiesJson } from '../../shared/entities-json';
import type { EntityInstance } from '../../shared/entity.types';

export interface EntitiesJsonSheetProps {
    readonly entities: readonly EntityInstance[];
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
}

export function EntitiesJsonSheet({ entities, open, onOpenChange }: EntitiesJsonSheetProps) {
    const json = useMemo(() => buildEntitiesJson(entities), [entities]);
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        if (!copied) return;
        const timer = setTimeout(() => setCopied(false), 1500);
        return () => clearTimeout(timer);
    }, [copied]);

    async function copy() {
        try {
            await navigator.clipboard?.writeText(json);
            setCopied(true);
        } catch {
            // clipboard unavailable — the JSON stays visible for manual copy
        }
    }

    function download() {
        const url = URL.createObjectURL?.(new Blob([json], { type: 'application/json' }));
        if (!url) return;
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = 'entities.json';
        anchor.click();
        setTimeout(() => URL.revokeObjectURL(url), 10_000);
    }

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent
                side="right"
                showCloseButton
                aria-label="entities.json"
                style={{ width: 'min(640px, 100vw)', maxWidth: 'min(640px, 100vw)' }}
                className="flex h-full flex-col gap-0 p-0"
            >
                <div className="flex flex-col gap-2 border-b px-6 py-4">
                    <SheetTitle className="font-mono text-lg font-semibold">entities.json</SheetTitle>
                    <p className="text-sm text-muted-foreground">
                        The canonical GAPL shape of all {entities.length} principals and resources the Policy Decision Point evaluates against.
                    </p>
                    <div className="flex items-center gap-2">
                        <Button type="button" variant="outline" size="sm" onClick={copy}>
                            {copied ? <CheckIcon className="mr-2 size-4" aria-hidden /> : <CopyIcon className="mr-2 size-4" aria-hidden />}
                            {copied ? 'Copied' : 'Copy JSON'}
                        </Button>
                        <Button type="button" variant="outline" size="sm" onClick={download}>
                            <DownloadIcon className="mr-2 size-4" aria-hidden />
                            Download
                        </Button>
                    </div>
                </div>
                <div className="min-h-0 flex-1 overflow-y-auto px-6 py-4">
                    <pre
                        data-testid="entities-json"
                        className="overflow-auto rounded-md border bg-muted/40 p-3 font-mono text-xs text-foreground"
                    >
                        {json}
                    </pre>
                </div>
            </SheetContent>
        </Sheet>
    );
}
