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
import { fireEvent, render, screen } from '@testing-library/react';

import { ApiReviewBanner } from './ApiReviewBanner';
import type { ApiReviewBannerCopy } from '../../utils/apiReview';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

const ASK_COPY: ApiReviewBannerCopy = {
    title: 'This API is a draft.',
    description: 'Ask for a review before you can publish or start this API.',
    tone: 'info',
    action: 'ask',
};

function renderBanner(copy: ApiReviewBannerCopy, isPending = false) {
    const onAskForReview = jest.fn();
    const onReview = jest.fn();
    render(<ApiReviewBanner copy={copy} isPending={isPending} onAskForReview={onAskForReview} onReview={onReview} />);
    return { onAskForReview, onReview };
}

describe('ApiReviewBanner', () => {
    it('renders the copy as a status region and routes the ask action', () => {
        const { onAskForReview, onReview } = renderBanner(ASK_COPY);
        expect(screen.getByRole('region', { name: 'API review status' })).toBeInTheDocument();
        expect(screen.getByText('This API is a draft.')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Ask for a review' }));
        expect(onAskForReview).toHaveBeenCalled();
        expect(onReview).not.toHaveBeenCalled();
    });

    it('routes the review action', () => {
        const { onReview } = renderBanner({ ...ASK_COPY, action: 'review' });
        fireEvent.click(screen.getByRole('button', { name: 'Review changes' }));
        expect(onReview).toHaveBeenCalled();
    });

    it('renders no button when the copy has no action', () => {
        renderBanner({ ...ASK_COPY, action: undefined });
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('disables the action while a request is pending', () => {
        renderBanner(ASK_COPY, true);
        expect(screen.getByRole('button', { name: 'Ask for a review' })).toBeDisabled();
    });
});
