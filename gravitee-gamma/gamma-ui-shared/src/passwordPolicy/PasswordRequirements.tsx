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

import type { PasswordPolicyRule } from './types';
import { evaluatePasswordPolicyRule, resolvePasswordStrengthLabel, resolvePasswordStrengthLevel } from './passwordPolicyRules';

interface PasswordRequirementsProps {
    readonly rules: PasswordPolicyRule[];
    readonly password?: string;
    readonly showStrengthMeter?: boolean;
    readonly className?: string;
}

const STRENGTH_BAR_CLASS: Record<ReturnType<typeof resolvePasswordStrengthLevel>, string> = {
    weak: 'bg-destructive',
    fair: 'bg-warning',
    good: 'bg-success/70',
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

export function PasswordRequirements({ rules, password = '', showStrengthMeter = false, className }: PasswordRequirementsProps) {
    const strengthLevel = resolvePasswordStrengthLevel(password, rules);
    const strengthLabel = resolvePasswordStrengthLabel(strengthLevel);
    const filledBars = showStrengthMeter ? STRENGTH_FILLED_BARS[strengthLevel] : 0;

    return (
        <div className={cn('space-y-3', className)}>
            {showStrengthMeter && password ? (
                <div className="space-y-1">
                    <div className="flex gap-1" aria-hidden>
                        {Array.from({ length: 4 }, (_, index) => (
                            <span
                                key={index}
                                className={cn(
                                    'h-1.5 flex-1 rounded-full bg-muted',
                                    index < filledBars && STRENGTH_BAR_CLASS[strengthLevel],
                                )}
                            />
                        ))}
                    </div>
                    <p className={cn('text-sm font-medium', STRENGTH_TEXT_CLASS[strengthLevel])}>{strengthLabel}</p>
                </div>
            ) : null}

            <div className="space-y-2">
                <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Requirements</p>
                <ul className="space-y-1.5">
                    {rules.map(rule => {
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
        </div>
    );
}
