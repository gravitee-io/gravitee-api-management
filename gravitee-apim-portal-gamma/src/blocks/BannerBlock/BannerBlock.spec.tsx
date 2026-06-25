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
jest.mock('@blocknote/react', () => ({
    createReactBlockSpec: (config: Record<string, unknown>, implementation: Record<string, unknown>) => ({
        ...config,
        implementation,
    }),
}));

import { render, screen } from '@testing-library/react';

import { BannerBlock } from './BannerBlock';

describe('BannerBlock', () => {
    const block = {
        props: {
            title: 'Welcome',
            subtitle: 'Build your portal',
            variant: 'indigo',
            buttons: '[]',
            backgroundImage: '',
            height: '0',
        },
    };

    const createEditor = (isEditable: boolean) => ({
        isEditable,
        updateBlock: jest.fn(),
    });

    function renderBanner(isEditable: boolean) {
        const { implementation } = BannerBlock as { implementation: { render: (props: never) => JSX.Element } };

        function BannerPreview() {
            return implementation.render({ block, editor: createEditor(isEditable) } as never);
        }

        return render(<BannerPreview />);
    }

    it('should render editable fields in edit mode', () => {
        renderBanner(true);

        expect(screen.getByDisplayValue('Welcome')).toBeInTheDocument();
    });

    it('should render static content in read-only mode', () => {
        renderBanner(false);

        expect(screen.getByText('Welcome')).toBeInTheDocument();
        expect(screen.getByText('Build your portal')).toBeInTheDocument();
    });
});
