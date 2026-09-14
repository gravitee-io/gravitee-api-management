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
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger, Badge, Button } from '@gravitee/graphene-core';
import { Link } from 'react-router-dom';

import { RulesetPayloadPreview } from './RulesetPayloadPreview';
import type { ScoringRuleset } from '../types/rulesets';
import { rulesetFormatLabel } from '../utils/rulesetFormat';

export function RulesetAccordion({
    rulesets,
    onDelete,
}: Readonly<{ rulesets: ScoringRuleset[]; onDelete: (ruleset: ScoringRuleset) => void }>) {
    return (
        <Accordion type="multiple" className="space-y-2" data-testid="ruleset-accordion">
            {rulesets.map(ruleset => (
                <AccordionItem key={ruleset.id} value={ruleset.id} className="rounded-lg border px-4">
                    <AccordionTrigger>
                        <span className="flex flex-wrap items-center gap-2 text-left">
                            <span className="font-medium">{ruleset.name}</span>
                            {ruleset.format ? (
                                <Badge variant="secondary" className="font-normal">
                                    {rulesetFormatLabel(ruleset.format)}
                                </Badge>
                            ) : null}
                        </span>
                    </AccordionTrigger>
                    <AccordionContent>
                        <div className="space-y-4 pb-2">
                            <div className="flex flex-wrap items-start justify-between gap-3">
                                <div>
                                    <h4 className="text-sm font-semibold">Details</h4>
                                    {ruleset.description ? (
                                        <p className="text-muted-foreground mt-1 text-sm">{ruleset.description}</p>
                                    ) : null}
                                </div>
                                <div className="flex gap-2">
                                    <Button asChild variant="outline" size="sm">
                                        <Link to={`${encodeURIComponent(ruleset.id)}/edit`} relative="path">
                                            Edit
                                        </Link>
                                    </Button>
                                    <Button type="button" variant="outline" size="sm" onClick={() => onDelete(ruleset)}>
                                        Delete
                                    </Button>
                                </div>
                            </div>
                            <RulesetPayloadPreview payload={ruleset.payload} />
                        </div>
                    </AccordionContent>
                </AccordionItem>
            ))}
        </Accordion>
    );
}
