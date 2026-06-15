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
import { Button, Input, Label, Sheet, SheetContent, SheetFooter, SheetHeader, SheetTitle } from '@gravitee/graphene-core';
import { SlidersHorizontalIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { notify } from '../../../../shared/notify';
import type { Subscription } from '../../../apis/types/subscription';
import { DEVELOPER_RATE_LIMIT_METADATA_KEY, DEVELOPER_TOKEN_LIMIT_METADATA_KEY } from '../../../apis/utils/planTransformers';
import { useUpdateDeveloperLimits } from '../../hooks/useAiProductHooks';

interface EditLimitsSheetProps {
    open: boolean;
    productId: string;
    subscription: Subscription | null;
    onClose: () => void;
}

function metaNumber(sub: Subscription | null, key: string, fallback: number): number {
    const raw = sub?.metadata?.[key];
    const value = raw ? Number(raw) : NaN;
    return Number.isFinite(value) && value > 0 ? value : fallback;
}

export function EditLimitsSheet({ open, productId, subscription, onClose }: EditLimitsSheetProps) {
    const [tokenLimit, setTokenLimit] = useState<number | ''>(0);
    const [rateLimit, setRateLimit] = useState<number | ''>(0);
    const { mutate: updateLimits, isPending } = useUpdateDeveloperLimits();

    // Re-seed the inputs from the subscription each time the sheet is (re)opened for a row.
    const [seededFor, setSeededFor] = useState<string | null>(null);
    if (open && subscription && seededFor !== subscription.id) {
        setSeededFor(subscription.id);
        setTokenLimit(metaNumber(subscription, DEVELOPER_TOKEN_LIMIT_METADATA_KEY, 100000));
        setRateLimit(metaNumber(subscription, DEVELOPER_RATE_LIMIT_METADATA_KEY, 60));
    }
    if (!open && seededFor !== null) setSeededFor(null);

    const tokenLimitValid = typeof tokenLimit === 'number' && tokenLimit > 0;
    const rateLimitValid = typeof rateLimit === 'number' && rateLimit > 0;
    const canSubmit = Boolean(subscription) && tokenLimitValid && rateLimitValid && !isPending;

    function handleSubmit() {
        if (!subscription || !tokenLimitValid || !rateLimitValid) return;
        updateLimits(
            {
                productId,
                subscriptionId: subscription.id,
                tokenLimit: tokenLimit as number,
                rateLimit: rateLimit as number,
                existingMetadata: subscription.metadata,
            },
            {
                onSuccess: () => {
                    notify.success('Limits updated.');
                    onClose();
                },
                onError: error => notify.error(error, 'Failed to update the limits.'),
            },
        );
    }

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <SheetContent side="right" aria-describedby={undefined} style={{ maxWidth: '460px' }}>
                <SheetHeader>
                    <SheetTitle>Edit limits</SheetTitle>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-4 px-4">
                    <div className="flex items-start gap-2 rounded-lg border bg-muted/40 px-3 py-2.5">
                        <SlidersHorizontalIcon className="size-4 text-muted-foreground shrink-0 mt-0.5" aria-hidden />
                        <p className="text-xs text-muted-foreground">
                            Updating <span className="font-medium">{subscription?.application.name}</span>&apos;s personal limits. Only this
                            user changes — their key, plan, and everyone else stay the same. The reset window is set by the plan; to change
                            it, re-add the user on a different window.
                        </p>
                    </div>

                    <div className="space-y-1.5">
                        <Label htmlFor="edit-token-limit">Token budget</Label>
                        <Input
                            id="edit-token-limit"
                            type="number"
                            min={1}
                            value={tokenLimit}
                            onChange={e => setTokenLimit(e.target.value === '' ? '' : Math.max(1, Number(e.target.value)))}
                            aria-invalid={!tokenLimitValid}
                        />
                    </div>

                    <div className="space-y-1.5">
                        <Label htmlFor="edit-rate-limit">Rate limit (requests per minute)</Label>
                        <Input
                            id="edit-rate-limit"
                            type="number"
                            min={1}
                            value={rateLimit}
                            onChange={e => setRateLimit(e.target.value === '' ? '' : Math.max(1, Number(e.target.value)))}
                            aria-invalid={!rateLimitValid}
                        />
                    </div>
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleSubmit} disabled={!canSubmit}>
                        {isPending ? 'Saving…' : 'Save limits'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
