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
import {
    Button,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Sheet,
    SheetContent,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { CircleCheckIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { notify } from '../../../../shared/notify';
import type { Subscription } from '../../../apis/types/subscription';
import { useApproveDeveloper } from '../../hooks/useAiProductHooks';
import { type BudgetWindow, WINDOW_LABEL } from '../../services/aiProduct';

const WINDOW_OPTIONS: BudgetWindow[] = ['MINUTE', 'HOUR', 'DAY', 'WEEK', 'MONTH'];

interface ApproveRequestSheetProps {
    open: boolean;
    productId: string;
    subscription: Subscription | null;
    onClose: () => void;
}

const DEFAULT_TOKEN_LIMIT = 100000;
const DEFAULT_RATE_LIMIT = 60;
const DEFAULT_WINDOW: BudgetWindow = 'MONTH';

export function ApproveRequestSheet({ open, productId, subscription, onClose }: ApproveRequestSheetProps) {
    const [tokenLimit, setTokenLimit] = useState<number | ''>(DEFAULT_TOKEN_LIMIT);
    const [rateLimit, setRateLimit] = useState<number | ''>(DEFAULT_RATE_LIMIT);
    const [window, setWindow] = useState<BudgetWindow>(DEFAULT_WINDOW);
    const { mutate: approve, isPending } = useApproveDeveloper();

    // Reset the form each time the sheet is opened for a request.
    const [prevOpen, setPrevOpen] = useState(open);
    if (prevOpen !== open) {
        setPrevOpen(open);
        if (open) {
            setTokenLimit(DEFAULT_TOKEN_LIMIT);
            setRateLimit(DEFAULT_RATE_LIMIT);
            setWindow(DEFAULT_WINDOW);
        }
    }

    const tokenLimitValid = typeof tokenLimit === 'number' && tokenLimit > 0;
    const rateLimitValid = typeof rateLimit === 'number' && rateLimit > 0;
    const canSubmit = Boolean(subscription) && tokenLimitValid && rateLimitValid && !isPending;

    function handleSubmit() {
        if (!subscription || !tokenLimitValid || !rateLimitValid) return;
        approve(
            {
                productId,
                subscriptionId: subscription.id,
                tokenLimit: tokenLimit as number,
                rateLimit: rateLimit as number,
                window,
                currentPlanId: subscription.plan?.id,
                existingMetadata: subscription.metadata,
            },
            {
                onSuccess: () => {
                    notify.success('Request approved — the user can now use the product.');
                    onClose();
                },
                onError: error => notify.error(error, 'Failed to approve the request.'),
            },
        );
    }

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <SheetContent side="right" aria-describedby={undefined} style={{ maxWidth: '460px' }}>
                <SheetHeader>
                    <SheetTitle>Approve access request</SheetTitle>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-4 px-4">
                    <div className="flex items-start gap-2 rounded-lg border bg-muted/40 px-3 py-2.5">
                        <CircleCheckIcon className="size-4 text-muted-foreground shrink-0 mt-0.5" aria-hidden />
                        <p className="text-xs text-muted-foreground">
                            Approving <span className="font-medium">{subscription?.application.name}</span>. Set their personal limits —
                            these apply only to this user. On approval they get their own key and can start calling the product&apos;s
                            models from the portal.
                        </p>
                    </div>

                    <div className="space-y-1.5">
                        <Label htmlFor="approve-token-limit">Token budget</Label>
                        <Input
                            id="approve-token-limit"
                            type="number"
                            min={1}
                            value={tokenLimit}
                            onChange={e => setTokenLimit(e.target.value === '' ? '' : Math.max(1, Number(e.target.value)))}
                            aria-invalid={!tokenLimitValid}
                        />
                        <p className="text-xs text-muted-foreground">Total tokens this user may consume per the plan&apos;s window.</p>
                    </div>

                    <div className="space-y-1.5">
                        <Label htmlFor="approve-rate-limit">Rate limit (requests per minute)</Label>
                        <Input
                            id="approve-rate-limit"
                            type="number"
                            min={1}
                            value={rateLimit}
                            onChange={e => setRateLimit(e.target.value === '' ? '' : Math.max(1, Number(e.target.value)))}
                            aria-invalid={!rateLimitValid}
                        />
                        <p className="text-xs text-muted-foreground">Exceeding either limit returns HTTP 429 until the window resets.</p>
                    </div>

                    <div className="space-y-1.5">
                        <Label htmlFor="approve-window">Reset window</Label>
                        <Select value={window} onValueChange={v => setWindow(v as BudgetWindow)}>
                            <SelectTrigger id="approve-window">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {WINDOW_OPTIONS.map(w => (
                                    <SelectItem key={w} value={w}>
                                        {WINDOW_LABEL[w]}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <p className="text-xs text-muted-foreground">
                            How often this user&apos;s token budget resets. We place them on the matching plan automatically.
                        </p>
                    </div>
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleSubmit} disabled={!canSubmit}>
                        {isPending ? 'Approving…' : 'Approve & set limits'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
