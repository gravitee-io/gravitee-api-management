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
import { Button, cn } from '@gravitee/graphene-core';
import { CopyIcon } from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';

import { copyTextToClipboardWithNotifyHandler } from '../../../shared/copyToClipboard';

interface CopyableProfileValueProps {
    readonly value: string;
    readonly copyAriaLabel: string;
    readonly children?: ReactNode;
    readonly className?: string;
}

export function CopyableProfileValue({ value, copyAriaLabel, children, className }: CopyableProfileValueProps) {
    return (
        <div className={cn('flex min-w-0 items-center gap-1.5', className)}>
            <span className="min-w-0 [overflow-wrap:anywhere]">{children ?? value}</span>
            <Button
                type="button"
                variant="ghost"
                size="icon"
                className="size-7 shrink-0 text-muted-foreground"
                aria-label={copyAriaLabel}
                onClick={() => copyTextToClipboardWithNotifyHandler(value, 'Copied to clipboard')}
            >
                <CopyIcon className="size-3.5" aria-hidden />
            </Button>
        </div>
    );
}
