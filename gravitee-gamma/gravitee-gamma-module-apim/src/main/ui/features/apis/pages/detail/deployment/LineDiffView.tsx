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

import type { UnifiedLine } from './types';

export function LineDiffView({ lines }: { lines: UnifiedLine[] }) {
    return (
        <div className="overflow-x-auto">
            <table className="min-w-full border-collapse font-mono text-xs">
                <tbody>
                    {lines.map((line, i) => {
                        const added = line.type === '+';
                        const removed = line.type === '-';
                        return (
                            <tr key={i}>
                                <td
                                    className={cn(
                                        'select-none w-6 text-center px-2 border-r align-top leading-5 font-bold',
                                        added && 'bg-green-200 text-green-800 dark:bg-green-900/60 dark:text-green-300',
                                        removed && 'bg-red-200 text-red-700 dark:bg-red-900/60 dark:text-red-300',
                                        !added && !removed && 'text-muted-foreground/30',
                                    )}
                                >
                                    {added ? '+' : removed ? '-' : ''}
                                </td>
                                <td
                                    className={cn(
                                        'px-3 py-0 leading-5 whitespace-pre align-top',
                                        added && 'bg-green-50 text-green-900 dark:bg-green-950/40 dark:text-green-200',
                                        removed && 'bg-red-50 text-red-900 dark:bg-red-950/40 dark:text-red-200',
                                    )}
                                >
                                    {line.text}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
