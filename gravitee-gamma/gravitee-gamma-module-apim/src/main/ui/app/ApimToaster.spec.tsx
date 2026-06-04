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

import { ApimToaster } from './ApimToaster';

const mockToaster = jest.fn(({ position, richColors, closeButton }: Record<string, unknown>) => (
    <div
        data-testid="graphene-toaster"
        data-position={String(position)}
        data-rich-colors={String(richColors)}
        data-close-button={String(closeButton)}
    />
));

jest.mock('@gravitee/graphene-core', () => ({
    Toaster: (props: Record<string, unknown>) => mockToaster(props),
}));

describe('ApimToaster', () => {
    beforeEach(() => {
        mockToaster.mockClear();
    });

    it('renders the graphene Toaster with apim defaults', () => {
        render(<ApimToaster />);

        expect(screen.getByTestId('graphene-toaster')).not.toBeNull();
        expect(mockToaster).toHaveBeenCalledWith(
            expect.objectContaining({
                position: 'bottom-right',
                richColors: true,
                closeButton: true,
            }),
        );
    });
});
