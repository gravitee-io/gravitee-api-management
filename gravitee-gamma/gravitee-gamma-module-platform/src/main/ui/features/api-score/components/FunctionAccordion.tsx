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
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger, Button } from '@gravitee/graphene-core';

import { RulesetPayloadPreview } from './RulesetPayloadPreview';
import type { ScoringFunction } from '../types/rulesets';

export function FunctionAccordion({
    functions,
    onDelete,
}: Readonly<{ functions: ScoringFunction[]; onDelete: (fn: ScoringFunction) => void }>) {
    return (
        <Accordion type="multiple" className="space-y-2" data-testid="function-accordion">
            {functions.map(fn => (
                <AccordionItem key={fn.name} value={fn.name} className="rounded-lg border px-4">
                    <AccordionTrigger>
                        <span className="font-medium">{fn.name}</span>
                    </AccordionTrigger>
                    <AccordionContent>
                        <div className="space-y-4 pb-2">
                            <div className="flex items-start justify-between gap-3">
                                <h4 className="text-sm font-semibold">Details</h4>
                                <Button type="button" variant="outline" size="sm" onClick={() => onDelete(fn)}>
                                    Delete
                                </Button>
                            </div>
                            <RulesetPayloadPreview payload={fn.payload} />
                        </div>
                    </AccordionContent>
                </AccordionItem>
            ))}
        </Accordion>
    );
}
