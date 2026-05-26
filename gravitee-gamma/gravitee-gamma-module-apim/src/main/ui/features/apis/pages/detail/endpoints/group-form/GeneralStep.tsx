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
import { Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

import type { EndpointGroupFormState, LoadBalancerType } from '../types';
import { validateGroupName } from '../types';

const LB_OPTIONS: { value: LoadBalancerType; label: string; description: string }[] = [
    { value: 'ROUND_ROBIN', label: 'Round robin', description: 'Distributes requests evenly across all endpoints.' },
    { value: 'RANDOM', label: 'Random', description: 'Picks an endpoint at random for each request.' },
    { value: 'WEIGHTED_ROUND_ROBIN', label: 'Weighted round robin', description: 'Round robin respecting endpoint weights.' },
    { value: 'WEIGHTED_RANDOM', label: 'Weighted random', description: 'Random selection respecting endpoint weights.' },
];

interface GeneralStepProps {
    form: EndpointGroupFormState;
    existingGroupNames: string[];
    onFormChange: (patch: Partial<EndpointGroupFormState>) => void;
}

export function GeneralStep({ form, existingGroupNames, onFormChange }: Readonly<GeneralStepProps>) {
    const nameError = (() => {
        const base = validateGroupName(form.name);
        if (base) return base;
        const lower = form.name.trim().toLowerCase();
        if (existingGroupNames.some(n => n.toLowerCase() === lower)) return 'Name must be unique.';
        return null;
    })();

    return (
        <div className="space-y-6">
            {/* Group name */}
            <div className="space-y-2">
                <Label htmlFor="group-name" className="text-sm">
                    Group name <span className="text-destructive">*</span>
                </Label>
                <Input
                    id="group-name"
                    value={form.name}
                    onChange={e => onFormChange({ name: e.target.value })}
                    placeholder="default-group"
                />
                {nameError && <p className="text-xs text-destructive">{nameError}</p>}
                <p className="text-xs text-muted-foreground">Must be unique. Colons are not allowed.</p>
            </div>

            {/* Load balancer */}
            <div className="space-y-2">
                <Label htmlFor="lb-type" className="text-sm">
                    Load balancer
                </Label>
                <Select value={form.loadBalancerType} onValueChange={v => onFormChange({ loadBalancerType: v as LoadBalancerType })}>
                    <SelectTrigger id="lb-type" className="w-full">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        {LB_OPTIONS.map(opt => (
                            <SelectItem key={opt.value} value={opt.value}>
                                {opt.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">{LB_OPTIONS.find(o => o.value === form.loadBalancerType)?.description}</p>
            </div>
        </div>
    );
}
