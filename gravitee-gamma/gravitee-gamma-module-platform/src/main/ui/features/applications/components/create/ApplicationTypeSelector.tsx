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
import { CircleCheckIcon } from '@gravitee/graphene-core/icons';

import type { ApplicationTypeConfig } from '../../types/applicationCreate';
import { applicationTypeIcon } from '../../utils/applicationTypeIcons';
import { isSameApplicationType } from '../../utils/applicationTypeLabels';

interface ApplicationTypeSelectorProps {
    readonly types: ApplicationTypeConfig[];
    readonly selectedTypeId: string;
    readonly onTypeChange: (typeId: string) => void;
    /** When only one type is enabled, show it selected but not changeable (console hides selector; gamma keeps card visible). */
    readonly readOnly?: boolean;
}

export function ApplicationTypeSelector({ types, selectedTypeId, onTypeChange, readOnly = false }: ApplicationTypeSelectorProps) {
    return (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {types.map(type => {
                const isSelected = isSameApplicationType(selectedTypeId, type.id);
                const Icon = applicationTypeIcon(type.id);

                return (
                    <button
                        key={type.id}
                        type="button"
                        className={cn(
                            'relative flex items-start gap-3 rounded-xl border-2 p-4 text-left transition-all outline-none',
                            readOnly ? 'cursor-default' : undefined,
                            isSelected
                                ? 'border-primary bg-primary/10 focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/40'
                                : 'border-transparent bg-muted focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/40',
                            !readOnly && !isSelected && 'hover:border-border',
                        )}
                        onClick={() => {
                            if (!readOnly) {
                                onTypeChange(type.id);
                            }
                        }}
                        aria-pressed={isSelected}
                        aria-disabled={readOnly || undefined}
                    >
                        <Icon className={cn('mt-0.5 size-5 shrink-0', isSelected ? 'text-primary' : 'text-muted-foreground')} aria-hidden />
                        <div className="min-w-0">
                            <span className="text-sm font-medium">{type.name}</span>
                            {type.description && <p className="mt-0.5 text-xs leading-relaxed text-muted-foreground">{type.description}</p>}
                        </div>
                        {isSelected && <CircleCheckIcon className="absolute top-3 right-3 size-4 shrink-0 text-primary" aria-hidden />}
                    </button>
                );
            })}
        </div>
    );
}
