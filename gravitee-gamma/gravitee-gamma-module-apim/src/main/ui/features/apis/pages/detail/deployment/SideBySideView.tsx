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

import type { SideLine } from './types';

function DiffCell({
    text,
    changed,
    symbol,
    addedColor,
    removedColor,
}: {
    text: string | null;
    changed: boolean;
    symbol: '+' | '-';
    addedColor: boolean;
    removedColor: boolean;
}) {
    return (
        <div className="flex items-start">
            <span
                className={cn(
                    'select-none w-5 shrink-0 text-center leading-5 font-bold',
                    addedColor && 'bg-green-200 text-green-800 dark:bg-green-900/60 dark:text-green-300',
                    removedColor && 'bg-red-200 text-red-700 dark:bg-red-900/60 dark:text-red-300',
                    !changed && 'text-transparent',
                )}
            >
                {symbol}
            </span>
            <pre className="flex-1 px-2 leading-5 overflow-hidden">{text ?? ''}</pre>
        </div>
    );
}

export function SideBySideView({ lines }: { lines: SideLine[] }) {
    return (
        <div className="overflow-x-auto">
            <table className="min-w-full border-collapse font-mono text-xs" style={{ tableLayout: 'fixed', minWidth: '800px' }}>
                <colgroup>
                    <col style={{ width: '50%' }} />
                    <col style={{ width: '50%' }} />
                </colgroup>
                <thead>
                    <tr className="border-b bg-muted/60">
                        <th className="text-left px-3 py-1.5 text-xs font-medium text-muted-foreground border-r">Before</th>
                        <th className="text-left px-3 py-1.5 text-xs font-medium text-muted-foreground">After</th>
                    </tr>
                </thead>
                <tbody>
                    {lines.map((line, i) => {
                        const leftChanged = line.kind !== 'equal' && line.left !== null;
                        const rightChanged = line.kind !== 'equal' && line.right !== null;
                        return (
                            <tr key={i}>
                                <td className={cn('px-0 py-0 align-top border-r', leftChanged && 'bg-red-50 dark:bg-red-950/40')}>
                                    <DiffCell
                                        text={line.left}
                                        changed={leftChanged}
                                        symbol="-"
                                        addedColor={false}
                                        removedColor={leftChanged}
                                    />
                                </td>
                                <td className={cn('px-0 py-0 align-top', rightChanged && 'bg-green-50 dark:bg-green-950/40')}>
                                    <DiffCell
                                        text={line.right}
                                        changed={rightChanged}
                                        symbol="+"
                                        addedColor={rightChanged}
                                        removedColor={false}
                                    />
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
