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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import {
    Button,
    Checkbox,
    Label,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    Skeleton,
    Textarea,
} from '@gravitee/graphene-core';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useId, useMemo, useState } from 'react';

import { notify } from '../../../../shared/notify';
import { useSubmitApiReview } from '../../hooks/useApiReviewMutations';
import { listApiQualityRuleChecks, listQualityRules, type ApiReviewDecision } from '../../services/apiReview';
import { apiReviewKeys } from '../../utils/queryKeys';

/** Classic caps the review comment at 500 characters. */
const REVIEW_COMMENT_MAX_LENGTH = 500;

/**
 * Reviewer side sheet: tick the environment's manual rules, leave a comment, then accept or reject.
 * Rule checks are stored per API, so a rule the reviewer already answered is updated rather than re-created.
 */
export function ApiReviewSheet({
    apiId,
    open,
    onOpenChange,
}: Readonly<{
    apiId: string;
    open: boolean;
    onOpenChange: (open: boolean) => void;
}>) {
    const env = useEnvironment();
    const commentId = useId();
    const [comment, setComment] = useState('');
    const [checked, setChecked] = useState<Record<string, boolean>>({});

    const rulesQuery = useQuery({
        queryKey: apiReviewKeys.qualityRules(env?.id ?? ''),
        queryFn: () => listQualityRules(env!.id),
        enabled: open && Boolean(env?.id),
    });
    const checksQuery = useQuery({
        queryKey: apiReviewKeys.apiQualityRules(env?.id ?? '', apiId),
        queryFn: () => listApiQualityRuleChecks(env!.id, apiId),
        enabled: open && Boolean(env?.id),
    });

    const rules = useMemo(() => rulesQuery.data ?? [], [rulesQuery.data]);
    const recordedChecks = useMemo(
        () => new Map((checksQuery.data ?? []).map(check => [check.quality_rule, check.checked])),
        [checksQuery.data],
    );

    // Reset the form each time the sheet opens, seeding the boxes from what the reviewer recorded last time.
    useEffect(() => {
        if (!open) return;
        setComment('');
        setChecked(Object.fromEntries(rules.map(rule => [rule.id, recordedChecks.get(rule.id) ?? false])));
    }, [open, rules, recordedChecks]);

    const submitMutation = useSubmitApiReview(apiId);
    const isLoading = rulesQuery.isLoading || checksQuery.isLoading;
    const isBusy = isLoading || submitMutation.isPending;

    function decide(decision: ApiReviewDecision) {
        submitMutation.mutate(
            {
                decision,
                message: comment.trim() || undefined,
                checks: rules.map(rule => ({
                    qualityRuleId: rule.id,
                    checked: checked[rule.id] ?? false,
                    exists: recordedChecks.has(rule.id),
                })),
            },
            {
                onSuccess: () => {
                    notify.success('API review saved.');
                    onOpenChange(false);
                },
                onError: error => notify.error(error, 'An error occurred while saving API review.'),
            },
        );
    }

    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent side="right" className="flex flex-col" style={{ maxWidth: '30rem' }}>
                <SheetHeader>
                    <SheetTitle>API Review</SheetTitle>
                    <SheetDescription>Accept the changes, or reject them and ask the author for updates.</SheetDescription>
                </SheetHeader>

                <div className="flex-1 min-h-0 space-y-6 overflow-y-auto px-1 py-4">
                    {isLoading ? (
                        <Skeleton className="h-28 w-full rounded-lg" />
                    ) : (
                        <>
                            {rulesQuery.isError || checksQuery.isError ? (
                                <p className="text-sm text-destructive">Could not load the manual rules. You can still accept or reject.</p>
                            ) : null}
                            {rules.length > 0 ? (
                                <div className="space-y-3">
                                    <p className="text-sm font-medium">Manual rules</p>
                                    <div className="space-y-1">
                                        {rules.map(rule => (
                                            <label
                                                key={rule.id}
                                                htmlFor={`review-rule-${rule.id}`}
                                                className="flex cursor-pointer items-start gap-3 rounded-lg p-2 hover:bg-muted/40"
                                            >
                                                <Checkbox
                                                    id={`review-rule-${rule.id}`}
                                                    className="mt-0.5"
                                                    checked={checked[rule.id] ?? false}
                                                    disabled={isBusy}
                                                    aria-label={rule.name}
                                                    onCheckedChange={value => setChecked(prev => ({ ...prev, [rule.id]: value === true }))}
                                                />
                                                <span className="min-w-0 flex-1">
                                                    <span className="block text-sm font-medium">{rule.name}</span>
                                                    <span className="text-xs text-muted-foreground">{rule.description}</span>
                                                </span>
                                            </label>
                                        ))}
                                    </div>
                                </div>
                            ) : null}
                            <div className="space-y-2">
                                <Label htmlFor={commentId}>Review comments</Label>
                                <Textarea
                                    id={commentId}
                                    value={comment}
                                    maxLength={REVIEW_COMMENT_MAX_LENGTH}
                                    rows={3}
                                    disabled={isBusy}
                                    onChange={event => setComment(event.target.value)}
                                />
                                <p className="text-right text-xs text-muted-foreground">
                                    {comment.length}/{REVIEW_COMMENT_MAX_LENGTH}
                                </p>
                            </div>
                        </>
                    )}
                </div>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)} disabled={submitMutation.isPending}>
                        Cancel
                    </Button>
                    <Button type="button" variant="destructive" disabled={isBusy} onClick={() => decide('reject')}>
                        Reject
                    </Button>
                    <Button type="button" disabled={isBusy} onClick={() => decide('accept')}>
                        Accept
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
