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

import { ButtonBlock } from './ButtonBlock';

describe('ButtonBlock', () => {
    const block = {
        props: {
            label: 'Get Started',
            link: '/catalog',
            appearance: 'filled',
        },
    };

    const createEditor = (isEditable: boolean) => ({
        isEditable,
        updateBlock: jest.fn(),
    });

    function renderButton(isEditable: boolean) {
        const { implementation } = ButtonBlock as { implementation: { render: (props: never) => JSX.Element } };

        function ButtonPreview() {
            return implementation.render({ block, editor: createEditor(isEditable) } as never);
        }

        return render(<ButtonPreview />);
    }

    it('should render editable fields in edit mode', () => {
        renderButton(true);

        expect(screen.getByDisplayValue('Get Started')).toBeInTheDocument();
    });

    it('should render a link in read-only mode', () => {
        renderButton(false);

        expect(screen.getByRole('link', { name: 'Get Started' })).toHaveAttribute('href', '/catalog');
    });
});
