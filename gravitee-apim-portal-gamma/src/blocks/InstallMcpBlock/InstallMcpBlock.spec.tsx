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

import { InstallMcpBlock } from './InstallMcpBlock';

describe('InstallMcpBlock', () => {
    const block = {
        props: {
            name: 'My MCP Server',
            transport: 'http',
            url: 'https://gateway.example.com/mcp',
            headers: '',
            command: '',
            args: '',
            env: '',
            clients: 'cursor,vscode',
        },
    };

    const createEditor = (isEditable: boolean) => ({
        isEditable,
        updateBlock: jest.fn(),
    });

    function renderBlock(isEditable: boolean) {
        const { implementation } = InstallMcpBlock as { implementation: { render: (props: never) => JSX.Element } };

        function Preview() {
            return implementation.render({ block, editor: createEditor(isEditable) } as never);
        }

        return render(<Preview />);
    }

    it('should render edit fields in edit mode', () => {
        renderBlock(true);

        expect(screen.getByDisplayValue('My MCP Server')).toBeInTheDocument();
        expect(screen.getByDisplayValue('https://gateway.example.com/mcp')).toBeInTheDocument();
    });

    it('should render preview in read-only mode', () => {
        renderBlock(false);

        expect(screen.getByText('My MCP Server')).toBeInTheDocument();
        expect(screen.getByText('https://gateway.example.com/mcp')).toBeInTheDocument();
        expect(screen.getByText('Clients: cursor,vscode')).toBeInTheDocument();
    });
});
