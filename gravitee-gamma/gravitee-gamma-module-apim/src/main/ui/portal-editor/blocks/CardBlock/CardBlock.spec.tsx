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

import { CardBlock } from './CardBlock';

describe('CardBlock', () => {
    const block = {
        props: {
            title: 'Feature Card',
            subtitle: 'Describe your feature',
            icon: 'book',
            color: 'white',
        },
    };

    const createEditor = (isEditable: boolean) => ({
        isEditable,
        updateBlock: jest.fn(),
    });

    function renderCard(isEditable: boolean) {
        const { implementation } = CardBlock as { implementation: { render: (props: never) => JSX.Element } };

        function CardPreview() {
            return implementation.render({ block, editor: createEditor(isEditable) } as never);
        }

        return render(<CardPreview />);
    }

    it('should render editable fields in edit mode', () => {
        renderCard(true);

        expect(screen.getByDisplayValue('Feature Card')).toBeInTheDocument();
    });

    it('should render static content in read-only mode', () => {
        renderCard(false);

        expect(screen.getByText('Feature Card')).toBeInTheDocument();
        expect(screen.getByText('Describe your feature')).toBeInTheDocument();
    });
});
