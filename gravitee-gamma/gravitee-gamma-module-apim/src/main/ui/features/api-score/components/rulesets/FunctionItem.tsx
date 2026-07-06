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
import { ChevronDownIcon, ChevronUpIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { PayloadPreview } from './PayloadPreview';
import type { ScoringFunction } from '../../types/apiScore';

interface FunctionItemProps {
    fn: ScoringFunction;
    canDelete: boolean;
    onDelete: (fn: ScoringFunction) => void;
}

export function FunctionItem({ fn, canDelete, onDelete }: FunctionItemProps) {
    const [open, setOpen] = useState(false);

    return (
        <div className="rounded-lg border">
            <button
                type="button"
                className="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-accent/50 transition-colors rounded-lg"
                onClick={() => setOpen(o => !o)}
                aria-expanded={open}
            >
                <span className="font-medium font-mono text-sm">{fn.name}</span>
                <span className="ml-auto shrink-0 text-muted-foreground">
                    {open ? <ChevronUpIcon className="size-4" aria-hidden /> : <ChevronDownIcon className="size-4" aria-hidden />}
                </span>
            </button>
            {open && (
                <div className="space-y-3 border-t px-4 pt-3 pb-3">
                    {canDelete ? (
                        <Button type="button" variant="outline" size="sm" className="text-destructive" onClick={() => onDelete(fn)}>
                            <Trash2Icon className="size-4" aria-hidden />
                            Delete
                        </Button>
                    ) : null}
                    <PayloadPreview payload={fn.payload} />
                </div>
            )}
        </div>
    );
}
