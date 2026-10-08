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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ItemAccessBadge, ItemPublishedBadge } from './DocumentationItemBadges';

beforeAll(() => {
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

async function tooltipOf(label: string) {
    await userEvent.setup().hover(screen.getByText(label));
    return (await screen.findByRole('tooltip')).textContent;
}

describe('ItemPublishedBadge', () => {
    it('explains that an unpublished item is not in the developer portal yet', async () => {
        render(<ItemPublishedBadge published={false} itemType="PAGE" />);

        expect(await tooltipOf('Unpublished')).toBe('Not shown in the developer portal until the page is published.');
    });

    it('explains that a published item shows as long as its API is published', async () => {
        render(<ItemPublishedBadge published itemType="FOLDER" />);

        expect(await tooltipOf('Published')).toBe('Shown in the developer portal.');
    });
});

describe('ItemAccessBadge', () => {
    it('explains that a public item needs no sign-in', async () => {
        render(<ItemAccessBadge visibility="PUBLIC" itemType="PAGE" />);

        expect(await tooltipOf('Public')).toBe("Users don't need to sign in to view this page.");
    });

    it('explains that a private item needs a sign-in, naming the kind of item', async () => {
        render(<ItemAccessBadge visibility="PRIVATE" itemType="LINK" />);

        expect(await tooltipOf('Private')).toBe('Only signed-in users can view this link.');
    });
});
