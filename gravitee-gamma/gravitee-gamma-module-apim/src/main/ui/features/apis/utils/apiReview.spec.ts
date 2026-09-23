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
    apiReviewBannerCopy,
    canAskForReview,
    isApiReviewEnabled,
    isAwaitingReviewerDecision,
    isReviewClearedForLifecycle,
} from './apiReview';

describe('apiReview helpers', () => {
    it('reads the flag from GET /portal', () => {
        expect(isApiReviewEnabled({ apiReview: { enabled: true } })).toBe(true);
        expect(isApiReviewEnabled({ apiReview: { enabled: false } })).toBe(false);
        expect(isApiReviewEnabled({})).toBe(false);
        expect(isApiReviewEnabled(undefined)).toBe(false);
    });

    it('clears the lifecycle when review is off, accepted, or never entered', () => {
        expect(isReviewClearedForLifecycle(false, 'IN_REVIEW')).toBe(true);
        expect(isReviewClearedForLifecycle(true, undefined)).toBe(true);
        expect(isReviewClearedForLifecycle(true, 'REVIEW_OK')).toBe(true);
        expect(isReviewClearedForLifecycle(true, 'DRAFT')).toBe(false);
        expect(isReviewClearedForLifecycle(true, 'IN_REVIEW')).toBe(false);
        expect(isReviewClearedForLifecycle(true, 'REQUEST_FOR_CHANGES')).toBe(false);
    });

    it('lets authors ask from draft, after rejection, or on a legacy API, but not while in review', () => {
        expect(canAskForReview(true, 'DRAFT')).toBe(true);
        expect(canAskForReview(true, 'REQUEST_FOR_CHANGES')).toBe(true);
        expect(canAskForReview(true, undefined)).toBe(true);
        expect(canAskForReview(true, 'IN_REVIEW')).toBe(false);
        expect(canAskForReview(true, 'REVIEW_OK')).toBe(false);
        expect(canAskForReview(false, 'DRAFT')).toBe(false);
    });

    it('awaits a reviewer decision while in review or after a rejection', () => {
        expect(isAwaitingReviewerDecision(true, 'IN_REVIEW')).toBe(true);
        expect(isAwaitingReviewerDecision(true, 'REQUEST_FOR_CHANGES')).toBe(true);
        expect(isAwaitingReviewerDecision(true, 'DRAFT')).toBe(false);
        expect(isAwaitingReviewerDecision(false, 'IN_REVIEW')).toBe(false);
    });

    describe('banner copy', () => {
        it('tells the author a draft needs a review and offers to ask when they may', () => {
            expect(apiReviewBannerCopy({ workflowState: 'DRAFT', isReviewer: false, canAsk: true })).toEqual({
                title: 'This API is a draft.',
                description: 'Ask for a review before you can publish or start this API.',
                tone: 'info',
                action: 'ask',
            });
            expect(apiReviewBannerCopy({ workflowState: 'DRAFT', isReviewer: true, canAsk: false })?.action).toBeUndefined();
        });

        it('offers the reviewer the decision while in review and after a rejection', () => {
            expect(apiReviewBannerCopy({ workflowState: 'IN_REVIEW', isReviewer: true, canAsk: true })).toEqual({
                title: 'This API has changes waiting for your review.',
                description: "Accept or reject them before they're applied.",
                tone: 'info',
                action: 'review',
            });
            expect(apiReviewBannerCopy({ workflowState: 'REQUEST_FOR_CHANGES', isReviewer: true, canAsk: true })).toMatchObject({
                tone: 'warning',
                action: 'review',
            });
        });

        it('keeps the author informed without a decision action', () => {
            expect(apiReviewBannerCopy({ workflowState: 'IN_REVIEW', isReviewer: false, canAsk: true })).toEqual({
                title: 'The API reviewer has been asked to review the changes.',
                description: 'Start, stop, and publish stay blocked until a reviewer accepts.',
                tone: 'info',
            });
            expect(apiReviewBannerCopy({ workflowState: 'REQUEST_FOR_CHANGES', isReviewer: false, canAsk: true })).toEqual({
                title: 'The API reviewer has asked for changes to be made on this API.',
                description: 'Address the feedback, then ask for a new review.',
                tone: 'warning',
                action: 'ask',
            });
            expect(apiReviewBannerCopy({ workflowState: 'REQUEST_FOR_CHANGES', isReviewer: false, canAsk: false })?.action).toBeUndefined();
        });

        it('shows nothing once accepted or when the API never entered the workflow', () => {
            expect(apiReviewBannerCopy({ workflowState: 'REVIEW_OK', isReviewer: true, canAsk: true })).toBeNull();
            expect(apiReviewBannerCopy({ workflowState: undefined, isReviewer: true, canAsk: true })).toBeNull();
        });
    });
});
