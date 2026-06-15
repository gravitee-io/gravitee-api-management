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
import { cn } from '@gravitee/graphene-core';
import { CircleCheckIcon, CopyIcon, EyeIcon, EyeOffIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

interface CopyFieldProps {
    value: string;
    /** Render as a secret with a reveal toggle (masked by default). */
    secret?: boolean;
    mono?: boolean;
}

export function CopyField({ value, secret = false, mono = false }: CopyFieldProps) {
    const [copied, setCopied] = useState(false);
    const [revealed, setRevealed] = useState(!secret);

    function handleCopy() {
        void navigator.clipboard.writeText(value).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
        });
    }

    const display = revealed ? value : '•'.repeat(Math.min(value.length, 24));

    return (
        <div className="flex items-center gap-1 rounded-md border bg-muted/40 px-2.5 py-1.5">
            <span className={cn('flex-1 truncate text-sm', mono && 'font-mono')} title={revealed ? value : undefined}>
                {display}
            </span>
            {secret ? (
                <button
                    type="button"
                    onClick={() => setRevealed(v => !v)}
                    aria-label={revealed ? 'Hide' : 'Reveal'}
                    className="p-1.5 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
                >
                    {revealed ? <EyeOffIcon className="size-4" /> : <EyeIcon className="size-4" />}
                </button>
            ) : null}
            <button
                type="button"
                onClick={handleCopy}
                aria-label={`Copy ${value}`}
                className={cn(
                    'p-1.5 rounded-md transition-colors',
                    copied ? 'text-success' : 'text-muted-foreground hover:text-foreground hover:bg-muted',
                )}
            >
                {copied ? <CircleCheckIcon className="size-4" /> : <CopyIcon className="size-4" />}
            </button>
        </div>
    );
}
