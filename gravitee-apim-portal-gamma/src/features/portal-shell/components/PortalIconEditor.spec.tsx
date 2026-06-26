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
import { renderPortalUi } from '../../../testing/render-portal-ui';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { PortalIconEditor } from './PortalIconEditor';

describe('PortalIconEditor', () => {
    it('should open file picker when clicked in edit mode', async () => {
        const user = userEvent.setup();
        const clickSpy = jest.spyOn(HTMLInputElement.prototype, 'click').mockImplementation(() => undefined);

        renderPortalUi(
            <PortalIconEditor portalIconUrl="" editable onChange={jest.fn()} />,
        );

        await user.click(screen.getByLabelText('Change portal icon'));
        expect(clickSpy).toHaveBeenCalled();

        clickSpy.mockRestore();
    });
});
