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
import { Button } from '@gravitee/graphene-core';
import { CopyIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { copyTextToClipboardWithNotifyHandler } from '../../../shared/copyToClipboard';

export function RulesetPayloadPreview({ payload }: Readonly<{ payload: string }>) {
    const lines = useMemo(() => payload.split('\n'), [payload]);

    return (
        <div
            data-testid="ruleset-payload-preview"
            className="bg-muted/30 overflow-hidden rounded-lg border font-mono text-xs text-foreground"
        >
            <div className="flex justify-end border-b px-2 py-1.5">
                <Button
                    type="button"
                    variant="ghost"
                    size="icon-sm"
                    className="size-8"
                    aria-label="Copy code to clipboard"
                    data-testid="ruleset-payload-copy"
                    onClick={() => copyTextToClipboardWithNotifyHandler(payload, 'Copied to clipboard')}
                >
                    <CopyIcon className="size-4" aria-hidden />
                </Button>
            </div>
            <div className="max-h-[400px] overflow-auto" data-testid="ruleset-payload-preview-scroll">
                <div className="divide-y divide-border">
                    {lines.map((line, index) => (
                        <div key={`${index}-${line.length}`} className="grid grid-cols-[2rem_1fr] gap-3 px-3 py-0.5">
                            <span className="text-muted-foreground select-none text-right tabular-nums">{index + 1}</span>
                            <span className="whitespace-pre-wrap break-all">{line.length > 0 ? line : ' '}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
