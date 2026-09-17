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
import type { ApiWorkflowState } from '../types';

export function isApiReviewEnabled(config: { apiReview?: { enabled?: boolean } } | null | undefined): boolean {
    return Boolean(config?.apiReview?.enabled);
}

/**
 * Start, stop, and publish stay blocked while a review is pending. An API with no workflow state predates
 * the review feature and is left alone, which is also what the backend checks before starting it.
 */
export function isReviewClearedForLifecycle(reviewEnabled: boolean, workflowState: ApiWorkflowState | undefined): boolean {
    if (!reviewEnabled) return true;
    return workflowState === undefined || workflowState === 'REVIEW_OK';
}

/** Authors ask from a draft, after changes were requested, or on an API that never entered the workflow. */
export function canAskForReview(reviewEnabled: boolean, workflowState: ApiWorkflowState | undefined): boolean {
    if (!reviewEnabled) return false;
    return workflowState === undefined || workflowState === 'DRAFT' || workflowState === 'REQUEST_FOR_CHANGES';
}

/** A reviewer can accept or reject once the author asked, or revisit a rejection. */
export function isAwaitingReviewerDecision(reviewEnabled: boolean, workflowState: ApiWorkflowState | undefined): boolean {
    if (!reviewEnabled) return false;
    return workflowState === 'IN_REVIEW' || workflowState === 'REQUEST_FOR_CHANGES';
}

export type ApiReviewBannerTone = 'info' | 'warning';
export type ApiReviewBannerAction = 'ask' | 'review';

export interface ApiReviewBannerCopy {
    readonly title: string;
    readonly description: string;
    readonly tone: ApiReviewBannerTone;
    readonly action?: ApiReviewBannerAction;
}

const ASK_DESCRIPTION = 'Ask for a review before you can publish or start this API.';
const BLOCKED_DESCRIPTION = 'Start, stop, and publish stay blocked until a reviewer accepts.';

/**
 * Banner shown on the API while review is enabled. Reviewers (`api-reviews-u`) get the decision
 * wording and the "Review changes" action; everyone else sees the author wording. An accepted API
 * shows no banner: it behaves like any other API from then on.
 */
export function apiReviewBannerCopy({
    workflowState,
    isReviewer,
    canAsk,
}: {
    workflowState: ApiWorkflowState | undefined;
    isReviewer: boolean;
    canAsk: boolean;
}): ApiReviewBannerCopy | null {
    const askAction: ApiReviewBannerAction | undefined = canAsk ? 'ask' : undefined;
    switch (workflowState) {
        case 'DRAFT':
            return { title: 'This API is a draft.', description: ASK_DESCRIPTION, tone: 'info', action: askAction };
        case 'IN_REVIEW':
            return isReviewer
                ? {
                      title: 'This API has changes waiting for your review.',
                      description: "Accept or reject them before they're applied.",
                      tone: 'info',
                      action: 'review',
                  }
                : { title: 'The API reviewer has been asked to review the changes.', description: BLOCKED_DESCRIPTION, tone: 'info' };
        case 'REQUEST_FOR_CHANGES':
            return isReviewer
                ? {
                      title: 'As an API reviewer, you have rejected the changes made on this API.',
                      description: 'Open the review to revisit your decision once the author has addressed it.',
                      tone: 'warning',
                      action: 'review',
                  }
                : {
                      title: 'The API reviewer has asked for changes to be made on this API.',
                      description: canAsk ? 'Address the feedback, then ask for a new review.' : BLOCKED_DESCRIPTION,
                      tone: 'warning',
                      action: askAction,
                  };
        default:
            return null;
    }
}
