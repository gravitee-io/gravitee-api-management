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

import { GRAVITEE_API_DEFINITION, type DefinitionFormatSelection, type RulesetFormat } from '../types/rulesets';
import { GRAVITEE_API_FORMATS } from '../utils/rulesetFormat';

const PARENT_CARDS: ReadonlyArray<{ value: DefinitionFormatSelection; title: string }> = [
    { value: 'OPENAPI', title: 'OpenAPI' },
    { value: 'ASYNCAPI', title: 'AsyncAPI' },
    { value: GRAVITEE_API_DEFINITION, title: 'Gravitee API' },
];

function SelectableCard({
    selected,
    title,
    subtitle,
    onClick,
    testId,
}: Readonly<{ selected: boolean; title: string; subtitle?: string; onClick: () => void; testId: string }>) {
    return (
        <button
            type="button"
            data-testid={testId}
            aria-pressed={selected}
            onClick={onClick}
            className={cn(
                'h-auto rounded-lg border p-4 text-left transition-colors',
                selected ? 'border-primary bg-primary/5' : 'hover:border-foreground/20',
            )}
        >
            <p className="font-medium">{title}</p>
            {subtitle ? <p className="text-muted-foreground mt-1 text-sm">{subtitle}</p> : null}
        </button>
    );
}

export function RulesetFormatCards({
    definitionFormat,
    graviteeApiFormat,
    onDefinitionFormatChange,
    onGraviteeApiFormatChange,
}: Readonly<{
    definitionFormat: DefinitionFormatSelection | '';
    graviteeApiFormat: RulesetFormat | '';
    onDefinitionFormatChange: (value: DefinitionFormatSelection) => void;
    onGraviteeApiFormatChange: (value: RulesetFormat) => void;
}>) {
    return (
        <div className="space-y-4">
            <div>
                <h3 className="text-base font-semibold">Asset Format</h3>
                <p className="text-muted-foreground mb-3 text-sm">Choose the format for which the ruleset will apply to.</p>
                <div className="grid gap-3 sm:grid-cols-3" data-testid="definition-format-selection">
                    {PARENT_CARDS.map(card => (
                        <SelectableCard
                            key={card.value}
                            title={card.title}
                            selected={definitionFormat === card.value}
                            onClick={() => onDefinitionFormatChange(card.value)}
                            testId={`definition-format-${card.value}`}
                        />
                    ))}
                </div>
            </div>
            {definitionFormat === GRAVITEE_API_DEFINITION ? (
                <div className="rounded-lg border p-4" data-testid="gravitee-api-format-selection">
                    <h3 className="mb-3 text-base font-semibold">Gravitee API Formats</h3>
                    <div className="grid gap-3">
                        {GRAVITEE_API_FORMATS.map(item => (
                            <SelectableCard
                                key={item.value}
                                title={item.title}
                                subtitle={item.subtitle}
                                selected={graviteeApiFormat === item.value}
                                onClick={() => onGraviteeApiFormatChange(item.value)}
                                testId={`gravitee-api-format-${item.value}`}
                            />
                        ))}
                    </div>
                </div>
            ) : null}
        </div>
    );
}
