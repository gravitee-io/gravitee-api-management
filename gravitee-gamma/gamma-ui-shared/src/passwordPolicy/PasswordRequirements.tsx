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

import type { PasswordPolicy } from './types';
import {
    assessPassword,
    describablePasswordRules,
    evaluatePasswordPolicyRule,
    resolvePasswordStrengthLabel,
    resolvePasswordStrengthLevel,
} from './passwordPolicyRules';

interface PasswordRequirementsProps {
    /**
     * The whole policy, not only its rules, so the checklist cannot contradict the verdict that gates
     * submission. Its description is shown where no rule can be listed, and where the pattern refuses
     * a password every listed rule accepts; anywhere else a checklist says the same thing item by item.
     */
    readonly policy: PasswordPolicy;
    readonly password?: string;
    readonly showStrengthMeter?: boolean;
    readonly className?: string;
}

/*
 * Only classes Graphene's pre-built `/styles` bundle emits: the consoles load that bundle instead of
 * compiling their own, so any other utility renders as nothing. `graphene-check-classes` enforces it.
 * The number of filled bars already tells "good" from "strong"; a paler green is not needed.
 */
const STRENGTH_BAR_CLASS: Record<ReturnType<typeof resolvePasswordStrengthLevel>, string> = {
    weak: 'bg-destructive',
    fair: 'bg-warning',
    good: 'bg-success',
    strong: 'bg-success',
};

const STRENGTH_TEXT_CLASS: Record<ReturnType<typeof resolvePasswordStrengthLevel>, string> = {
    weak: 'text-destructive',
    fair: 'text-warning',
    good: 'text-success',
    strong: 'text-success',
};

const STRENGTH_FILLED_BARS: Record<ReturnType<typeof resolvePasswordStrengthLevel>, number> = {
    weak: 1,
    fair: 2,
    good: 3,
    strong: 4,
};

const UNLISTED_REQUIREMENT = "This password doesn't meet the full policy yet. One of its requirements isn't listed here.";

export function PasswordRequirements({ policy, password = '', showStrengthMeter = false, className }: PasswordRequirementsProps) {
    // Nothing listable means either the policy could not be loaded or its pattern defeated the
    // parser. Nothing here can say anything about the password in that state: a heading over an
    // empty list reads as "there are no requirements", and a meter scored against no rules reports
    // "Weak" for every password ever typed.
    const listedRules = describablePasswordRules(policy.rules);
    // Every box ticked and still refused: what the password lacks is a requirement no rule could list.
    const refusedDespiteChecklist =
        listedRules.length > 0 &&
        listedRules.every(rule => evaluatePasswordPolicyRule(rule, password)) &&
        assessPassword(password, policy) === 'unsatisfied';
    // "Strong" tells the reader they are done, which the policy says they are not.
    const strengthLevel = refusedDespiteChecklist ? 'good' : resolvePasswordStrengthLevel(password, listedRules);
    const strengthLabel = resolvePasswordStrengthLabel(strengthLevel);
    const filledBars = showStrengthMeter ? STRENGTH_FILLED_BARS[strengthLevel] : 0;
    const showsStrength = showStrengthMeter && Boolean(password) && listedRules.length > 0;

    return (
        <div className={className}>
            <div className="space-y-3">
                {showsStrength ? (
                    <div className="space-y-1" aria-hidden>
                        <div className="flex gap-1">
                            {Array.from({ length: 4 }, (_, index) => (
                                <span
                                    key={index}
                                    className={cn(
                                        'h-1 flex-1 rounded-full bg-muted',
                                        index < filledBars && STRENGTH_BAR_CLASS[strengthLevel],
                                    )}
                                />
                            ))}
                        </div>
                        <p className={cn('text-sm font-medium', STRENGTH_TEXT_CLASS[strengthLevel])}>{strengthLabel}</p>
                    </div>
                ) : null}

                {/* Where the operator wrote a sentence of their own, it is the only guidance left. */}
                {listedRules.length === 0 && policy.description ? (
                    <p className="text-sm text-muted-foreground">{policy.description}</p>
                ) : null}

                {listedRules.length > 0 ? (
                    <div className="space-y-2">
                        <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Requirements</p>
                        <ul className="space-y-2">
                            {listedRules.map(rule => {
                                const satisfied = password ? evaluatePasswordPolicyRule(rule, password) : false;
                                return (
                                    <li key={rule.id} className="flex items-start gap-2 text-sm">
                                        {satisfied ? (
                                            <CircleCheckIcon className="mt-0.5 size-4 shrink-0 text-success" aria-hidden />
                                        ) : (
                                            <span
                                                className="mt-0.5 flex size-4 shrink-0 items-center justify-center rounded-full border border-muted-foreground/40"
                                                aria-hidden
                                            />
                                        )}
                                        <span className={cn(satisfied && password ? 'text-foreground' : 'text-muted-foreground')}>
                                            {rule.label}
                                        </span>
                                    </li>
                                );
                            })}
                        </ul>
                    </div>
                ) : null}

                {refusedDespiteChecklist ? (
                    <div className="space-y-1">
                        <p className="text-sm text-destructive">{UNLISTED_REQUIREMENT}</p>
                        {policy.description ? <p className="text-sm text-muted-foreground">{policy.description}</p> : null}
                    </div>
                ) : null}
            </div>
            {/* The meter is only seen, so its verdict is spoken here. The region stays mounted, empty
                until there is something to say: one that appears together with its first message is
                usually not announced at all. Outside the spacing stack so the empty node adds no gap. */}
            {showStrengthMeter ? (
                <p aria-live="polite" className="sr-only">
                    {showsStrength ? `Password strength: ${strengthLabel}` : ''}
                </p>
            ) : null}
        </div>
    );
}
