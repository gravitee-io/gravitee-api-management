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
import { CircleCheckIcon, CopyIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

export function CopyButton({ value }: { readonly value: string }) {
    const [copied, setCopied] = useState(false);

    function handleCopy() {
        void navigator.clipboard.writeText(value).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
        });
    }

    return (
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
    );
}
