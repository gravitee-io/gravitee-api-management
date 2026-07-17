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
import { createReactBlockSpec } from '@blocknote/react';

import { getGmdBlockHooks } from '../../editor/gmd/gmd-block-hooks';
import styles from './InstallMcpBlock.module.scss';

type McpTransport = 'http' | 'sse' | 'stdio';

const TRANSPORTS: McpTransport[] = ['http', 'sse', 'stdio'];
const CLIENT_OPTIONS = ['cursor', 'vscode', 'claude-desktop'];

function buildSnippet(name: string, transport: McpTransport, url: string, command: string): string {
    if (transport === 'stdio') {
        return JSON.stringify(
            {
                mcpServers: {
                    [name || 'mcp-server']: {
                        command: command || 'node',
                        args: [],
                    },
                },
            },
            null,
            2,
        );
    }

    return JSON.stringify(
        {
            mcpServers: {
                [name || 'mcp-server']: {
                    url: url || 'https://example.com/mcp',
                },
            },
        },
        null,
        2,
    );
}

export const InstallMcpBlock = createReactBlockSpec(
    {
        type: 'graviteeInstallMcp' as const,
        propSchema: {
            name: { default: '' },
            transport: { default: 'http' as McpTransport },
            url: { default: '' },
            headers: { default: '' },
            command: { default: '' },
            args: { default: '' },
            env: { default: '' },
            clients: { default: '' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeInstallMcp'),
        render: ({ block, editor }) => {
            const { name, transport, url, headers, command, args, env, clients } = block.props;
            const isEditable = editor.isEditable;
            const snippet = buildSnippet(name, transport as McpTransport, url, command);

            if (!isEditable) {
                return (
                    <div className={styles.preview}>
                        <div className={styles.header}>
                            <span className={styles.badge}>{transport}</span>
                            <strong>{name || 'MCP Server'}</strong>
                        </div>
                        <p className={styles.endpoint}>
                            {transport === 'stdio' ? command || 'Configure stdio command' : url || 'Configure server URL'}
                        </p>
                        {clients && <p className={styles.clients}>Clients: {clients}</p>}
                        <pre className={styles.snippet}>{snippet}</pre>
                    </div>
                );
            }

            const update = (key: string, value: string) => {
                editor.updateBlock(block, { props: { [key]: value } });
            };

            return (
                <div className={styles.edit}>
                    <div className={styles.row}>
                        <label>
                            Name
                            <input value={name} onChange={e => update('name', e.target.value)} />
                        </label>
                        <label>
                            Transport
                            <select
                                value={transport}
                                onChange={e => update('transport', e.target.value)}
                            >
                                {TRANSPORTS.map(option => (
                                    <option key={option} value={option}>
                                        {option}
                                    </option>
                                ))}
                            </select>
                        </label>
                    </div>
                    {transport === 'stdio' ? (
                        <div className={styles.row}>
                            <label>
                                Command
                                <input value={command} onChange={e => update('command', e.target.value)} />
                            </label>
                            <label>
                                Args (JSON)
                                <input value={args} onChange={e => update('args', e.target.value)} />
                            </label>
                        </div>
                    ) : (
                        <label>
                            URL
                            <input value={url} onChange={e => update('url', e.target.value)} />
                        </label>
                    )}
                    <div className={styles.row}>
                        <label>
                            Headers (JSON)
                            <input value={headers} onChange={e => update('headers', e.target.value)} />
                        </label>
                        <label>
                            Env (JSON)
                            <input value={env} onChange={e => update('env', e.target.value)} />
                        </label>
                    </div>
                    <label>
                        Clients
                        <input
                            value={clients}
                            onChange={e => update('clients', e.target.value)}
                            placeholder={CLIENT_OPTIONS.join(', ')}
                        />
                    </label>
                </div>
            );
        },
    },
);
