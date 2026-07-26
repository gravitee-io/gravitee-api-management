/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    Button,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldLabel,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import type { PortalAccessGrant } from '../types/permissions.types';
import { loadPlanOptions, type PlanOption } from '../utils/plan-options';

interface AutoProvisionDialogProps {
    readonly open: boolean;
    readonly grant: PortalAccessGrant | null;
    readonly groupName: string;
    readonly assetLabel: string;
    readonly onOpenChange: (open: boolean) => void;
    readonly onConfirm: (defaultPlanId: string) => void;
}

export function AutoProvisionDialog({
    open,
    grant,
    groupName,
    assetLabel,
    onOpenChange,
    onConfirm,
}: AutoProvisionDialogProps) {
    const [plans, setPlans] = useState<PlanOption[]>([]);
    const [planId, setPlanId] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!open || !grant) {
            return;
        }

        let cancelled = false;
        setLoading(true);
        setPlans([]);
        setPlanId('');

        void loadPlanOptions(grant.scopeType, grant.scopeId).then(options => {
            if (cancelled) {
                return;
            }
            setPlans(options);
            setPlanId(options[0]?.id ?? '');
            setLoading(false);
        });

        return () => {
            cancelled = true;
        };
    }, [open, grant]);

    const selectedPlan = plans.find(plan => plan.id === planId);

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="w-full max-w-md sm:max-w-md">
                <DialogHeader>
                    <DialogTitle>Auto-provision access</DialogTitle>
                    <DialogDescription>
                        Choose the default plan for {assetLabel}. Members of {groupName} skip the
                        subscription workflow; each member’s default application is used and
                        credentials are issued on first access.
                    </DialogDescription>
                </DialogHeader>

                <Field>
                    <FieldLabel htmlFor="auto-provision-plan">Default plan</FieldLabel>
                    <Select
                        value={planId || undefined}
                        onValueChange={setPlanId}
                        disabled={loading || plans.length === 0}
                    >
                        <SelectTrigger id="auto-provision-plan" className="w-full">
                            <SelectValue
                                placeholder={loading ? 'Loading plans…' : 'Select a plan'}
                            />
                        </SelectTrigger>
                        <SelectContent position="popper" align="start">
                            {plans.map(plan => (
                                <SelectItem key={plan.id} value={plan.id}>
                                    {plan.name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                    {selectedPlan?.description && (
                        <p className="text-xs text-muted-foreground">{selectedPlan.description}</p>
                    )}
                    {!loading && plans.length === 0 && (
                        <p className="text-xs text-muted-foreground">
                            No plans are available for this asset.
                        </p>
                    )}
                </Field>

                <DialogFooter>
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button
                        type="button"
                        disabled={!planId}
                        onClick={() => {
                            onConfirm(planId);
                            onOpenChange(false);
                        }}
                    >
                        Auto-provision
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
