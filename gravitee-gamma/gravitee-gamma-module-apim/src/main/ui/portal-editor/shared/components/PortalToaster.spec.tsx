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
import { render } from '@testing-library/react';

import { PortalToaster } from './PortalToaster';

const mockToaster = jest.fn(() => null);

jest.mock('@gravitee/graphene-core', () => ({
    Toaster: (props: Record<string, unknown>) => {
        mockToaster(props);
        return null;
    },
    useTheme: () => ({ resolvedTheme: 'dark' }),
}));

describe('PortalToaster', () => {
    beforeEach(() => {
        mockToaster.mockClear();
    });

    it('should pass resolved theme to Toaster', () => {
        render(<PortalToaster />);

        expect(mockToaster).toHaveBeenCalledWith(
            expect.objectContaining({
                position: 'bottom-right',
                richColors: true,
                closeButton: true,
                theme: 'dark',
            }),
        );
    });
});
