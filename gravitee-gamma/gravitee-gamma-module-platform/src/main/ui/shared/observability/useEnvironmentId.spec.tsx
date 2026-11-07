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
import { useEnvironmentId } from '@gravitee/gamma-ui-shared/observability';
import { renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    useEnvironment: jest.fn(),
}));

const mockUseEnvironment = jest.mocked(useEnvironment);

function renderAt(pathname: string) {
    function Wrapper({ children }: { children: ReactNode }) {
        return <MemoryRouter initialEntries={[pathname]}>{children}</MemoryRouter>;
    }
    return renderHook(() => useEnvironmentId(), { wrapper: Wrapper });
}

describe('useEnvironmentId', () => {
    it('prefers the environment the host reports', () => {
        mockUseEnvironment.mockReturnValue({ id: 'from-host' });
        expect(renderAt('/environments/from-url/platform/applications').result.current).toBe('from-host');
    });

    it('falls back to the environment segment of the URL before the host reports one', () => {
        mockUseEnvironment.mockReturnValue(undefined);
        expect(renderAt('/environments/from-url/platform/applications').result.current).toBe('from-url');
    });

    it('falls back to DEFAULT when neither is known', () => {
        mockUseEnvironment.mockReturnValue(undefined);
        expect(renderAt('/platform/applications').result.current).toBe('DEFAULT');
    });
});
