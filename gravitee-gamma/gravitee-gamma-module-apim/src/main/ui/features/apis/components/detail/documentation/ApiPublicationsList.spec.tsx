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
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ApiPublicationsList } from './ApiPublicationsList';
import type { ApiPortalPublication, ApiPortalPublishPortal } from '../../../types/apiDocumentation';
import type { PortalPublication } from '../../../utils/documentationTree';

const DEVELOPER_PORTAL: ApiPortalPublishPortal = { id: 'DEFAULT', name: 'Developer Portal', default: true };
const PARTNERS_PORTAL: ApiPortalPublishPortal = { id: 'partners-portal', name: 'Partners', default: false };

function publishedIn(portal: ApiPortalPublishPortal, sectionName: string): PortalPublication {
    const publication: ApiPortalPublication = {
        portalId: portal.id,
        sectionName,
        portalNavigationItem: {
            id: `listing-${portal.id}`,
            organizationId: 'DEFAULT',
            environmentId: 'DEFAULT',
            title: 'Payment API',
            type: 'API',
            apiId: 'api-1',
            area: 'TOP_NAVBAR',
            parentId: 'section',
            rootId: 'section',
            order: 0,
            published: true,
            visibility: 'PUBLIC',
        },
    };
    return { portal, publication };
}

const PUBLICATIONS = [publishedIn(DEVELOPER_PORTAL, 'APIs'), publishedIn(PARTNERS_PORTAL, 'Partner APIs')];

function renderList({ canUnpublish = true }: { canUnpublish?: boolean } = {}) {
    const onUnpublish = jest.fn();
    render(<ApiPublicationsList publications={PUBLICATIONS} canUnpublish={canUnpublish} onUnpublish={onUnpublish} />);
    return { onUnpublish };
}

function rows() {
    return within(screen.getByRole('list', { name: 'Published in' })).getAllByRole('listitem');
}

describe('ApiPublicationsList', () => {
    it('lists each portal the API is published in, with its section', () => {
        renderList();

        expect(rows()).toHaveLength(2);
        expect(rows()[0]).toHaveTextContent('Developer Portal');
        expect(rows()[0]).toHaveTextContent('Section: APIs');
        expect(rows()[1]).toHaveTextContent('Partners');
        expect(rows()[1]).toHaveTextContent('Section: Partner APIs');
    });

    it('marks the default portal, and only that one', () => {
        renderList();

        expect(within(rows()[0] as HTMLElement).getByText('Default')).toBeInTheDocument();
        expect(within(rows()[1] as HTMLElement).queryByText('Default')).not.toBeInTheDocument();
    });

    it('asks to unpublish from the portal of the row', async () => {
        const { onUnpublish } = renderList();

        await userEvent.click(screen.getByRole('button', { name: 'Unpublish from Partners' }));

        expect(onUnpublish).toHaveBeenCalledTimes(1);
        expect(onUnpublish).toHaveBeenCalledWith(PARTNERS_PORTAL);
    });

    it('offers no Unpublish without the permission to edit', () => {
        renderList({ canUnpublish: false });

        expect(rows()).toHaveLength(2);
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });
});
