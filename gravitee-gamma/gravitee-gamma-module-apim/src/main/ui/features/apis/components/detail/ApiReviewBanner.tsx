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
import { Alert, AlertAction, AlertDescription, AlertTitle, Button } from '@gravitee/graphene-core';
import { InfoIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';

import type { ApiReviewBannerCopy } from '../../utils/apiReview';

const ACTION_LABEL = {
    ask: 'Ask for a review',
    review: 'Review changes',
} as const;

export function ApiReviewBanner({
    copy,
    isPending,
    onAskForReview,
    onReview,
}: Readonly<{
    copy: ApiReviewBannerCopy;
    isPending: boolean;
    onAskForReview: () => void;
    onReview: () => void;
}>) {
    const isWarning = copy.tone === 'warning';
    const Icon = isWarning ? TriangleAlertIcon : InfoIcon;
    return (
        // A persistent status banner, not an interruption — override Alert's assertive `role="alert"`.
        <Alert
            variant={isWarning ? 'warning' : 'default'}
            role="region"
            aria-label="API review status"
            className="rounded-none border-0 border-b px-6"
        >
            <Icon aria-hidden="true" />
            <AlertTitle>{copy.title}</AlertTitle>
            <AlertDescription>{copy.description}</AlertDescription>
            {copy.action ? (
                <AlertAction align="center">
                    <Button size="sm" onClick={copy.action === 'ask' ? onAskForReview : onReview} disabled={isPending}>
                        {ACTION_LABEL[copy.action]}
                    </Button>
                </AlertAction>
            ) : null}
        </Alert>
    );
}
