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
import { useMemo, useState } from 'react';

import { getAiWorkspaceData } from '../../features/editor/services/ai-workspace.service';
import { getGmdBlockHooks } from '../../features/editor/gmd/gmd-block-hooks';
import { HighlightedCodeBlock } from '../OpenApiBlock/gravitee-docs/HighlightedCodeBlock';
import styles from './AiWorkspaceBlocks.module.scss';
import { CopyButton } from './shared';

interface ChatTurn {
    readonly id: string;
    readonly prompt: string;
    readonly model: string;
    readonly reply: string;
    readonly requestJson: string;
    readonly responseJson: string;
}

function buildDummyReply(prompt: string, model: string): string {
    const trimmed = prompt.trim();
    const preview = trimmed.length > 180 ? `${trimmed.slice(0, 177)}…` : trimmed;
    return [
        `(demo) Response from ${model}`,
        '',
        `You said: "${preview}"`,
        '',
        'This is a simulated completion from the Gravitee AI gateway. In production, this request would be routed to the selected model with your AI key and budget limits applied.',
    ].join('\n');
}

function buildRequestJson(model: string, prompt: string): string {
    return JSON.stringify(
        {
            model,
            messages: [{ role: 'user', content: prompt }],
        },
        null,
        2,
    );
}

function buildResponseJson(model: string, prompt: string, reply: string): string {
    return JSON.stringify(
        {
            id: `chatcmpl-demo-${Date.now().toString(36)}`,
            object: 'chat.completion',
            created: Math.floor(Date.now() / 1000),
            model,
            choices: [
                {
                    index: 0,
                    message: { role: 'assistant', content: reply },
                    finish_reason: 'stop',
                },
            ],
            usage: {
                prompt_tokens: Math.max(8, Math.round(prompt.length / 4)),
                completion_tokens: Math.max(12, Math.round(reply.length / 4)),
                total_tokens: Math.max(20, Math.round((prompt.length + reply.length) / 4)),
            },
        },
        null,
        2,
    );
}

export function AiTryItView({ workspaceId }: { workspaceId: string }) {
    const workspace = getAiWorkspaceData(workspaceId);
    const models = workspace.models.filter(model => model.capabilities.includes('chat'));
    const selectableModels = models.length > 0 ? models : workspace.models;

    const [modelId, setModelId] = useState(selectableModels[0]?.id ?? '');
    const [prompt, setPrompt] = useState('');
    const [sending, setSending] = useState(false);
    const [turns, setTurns] = useState<ChatTurn[]>([]);
    const [codeTab, setCodeTab] = useState<'request' | 'response'>('request');

    const activeModel = selectableModels.find(model => model.id === modelId) ?? selectableModels[0];
    const latestTurn = turns[turns.length - 1];

    const codePayload = useMemo(() => {
        if (latestTurn) {
            return {
                request: latestTurn.requestJson,
                response: latestTurn.responseJson,
            };
        }
        const placeholderPrompt = prompt.trim() || 'Your prompt will appear here';
        const model = activeModel?.id ?? 'gpt-4o';
        return {
            request: buildRequestJson(model, placeholderPrompt),
            response: '// Send a message to see the response payload',
        };
    }, [activeModel?.id, latestTurn, prompt]);

    const handleSend = () => {
        const text = prompt.trim();
        if (!text || !activeModel || sending) {
            return;
        }

        setSending(true);
        window.setTimeout(() => {
            const reply = buildDummyReply(text, activeModel.name);
            const turn: ChatTurn = {
                id: `turn-${Date.now()}`,
                prompt: text,
                model: activeModel.id,
                reply,
                requestJson: buildRequestJson(activeModel.id, text),
                responseJson: buildResponseJson(activeModel.id, text, reply),
            };
            setTurns(prev => [...prev, turn]);
            setPrompt('');
            setCodeTab('response');
            setSending(false);
        }, 450);
    };

    return (
        <div className={`${styles.block} ${styles.tryItRoot}`}>
            <div className={styles.tryItLayout}>
                <div className={styles.tryItChat}>
                    <div className={styles.tryItToolbar}>
                        <label className={styles.tryItModelField}>
                            Model
                            <select
                                value={activeModel?.id ?? ''}
                                onChange={e => setModelId(e.target.value)}
                                disabled={selectableModels.length === 0}
                            >
                                {selectableModels.map(model => (
                                    <option key={model.id} value={model.id}>
                                        {model.name}
                                    </option>
                                ))}
                            </select>
                        </label>
                        <span className={styles.tryItHint}>Demo responses — no live gateway call</span>
                    </div>

                    <div className={styles.tryItMessages}>
                        {turns.length === 0 ? (
                            <div className={styles.tryItEmpty}>
                                Pick a model, write a prompt, and send it to preview the chat flow.
                            </div>
                        ) : (
                            turns.map(turn => (
                                <div key={turn.id} className={styles.tryItTurn}>
                                    <div className={`${styles.tryItBubble} ${styles.tryItUser}`}>
                                        <span className={styles.tryItRole}>You</span>
                                        <p>{turn.prompt}</p>
                                    </div>
                                    <div className={`${styles.tryItBubble} ${styles.tryItAssistant}`}>
                                        <span className={styles.tryItRole}>{turn.model}</span>
                                        <p>{turn.reply}</p>
                                    </div>
                                </div>
                            ))
                        )}
                        {sending ? (
                            <div className={`${styles.tryItBubble} ${styles.tryItAssistant}`}>
                                <span className={styles.tryItRole}>{activeModel?.id}</span>
                                <p className={styles.tryItTyping}>Thinking…</p>
                            </div>
                        ) : null}
                    </div>

                    <div className={styles.tryItComposer}>
                        <textarea
                            value={prompt}
                            onChange={e => setPrompt(e.target.value)}
                            placeholder="Ask something…"
                            rows={3}
                            onKeyDown={e => {
                                if (e.key === 'Enter' && !e.shiftKey) {
                                    e.preventDefault();
                                    handleSend();
                                }
                            }}
                        />
                        <button
                            type="button"
                            className={styles.tryItSend}
                            onClick={handleSend}
                            disabled={!prompt.trim() || sending || !activeModel}
                        >
                            {sending ? 'Sending…' : 'Send'}
                        </button>
                    </div>
                </div>

                <div className={styles.tryItCode}>
                    <div className={styles.tryItCodeHeader}>
                        <div className={styles.tabs}>
                            <button
                                type="button"
                                className={codeTab === 'request' ? styles.tabActive : styles.tab}
                                onClick={() => setCodeTab('request')}
                            >
                                Request
                            </button>
                            <button
                                type="button"
                                className={codeTab === 'response' ? styles.tabActive : styles.tab}
                                onClick={() => setCodeTab('response')}
                            >
                                Response
                            </button>
                        </div>
                        <CopyButton
                            value={codeTab === 'request' ? codePayload.request : codePayload.response}
                            className={styles.snippetCopy}
                        />
                    </div>
                    <div className={`${styles.snippetWrap} ${styles.tryItSnippetWrap}`}>
                        <HighlightedCodeBlock
                            code={codeTab === 'request' ? codePayload.request : codePayload.response}
                            language="json"
                            className={`${styles.snippet} ${styles.tryItSnippet}`}
                        />
                    </div>
                </div>
            </div>
        </div>
    );
}

export const AiTryItBlock = createReactBlockSpec(
    {
        type: 'graviteeAiTryIt' as const,
        propSchema: {
            workspaceId: { default: '' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeAiTryIt'),
        render: ({ block, editor }) => {
            const { workspaceId } = block.props;
            const view = <AiTryItView workspaceId={workspaceId} />;

            if (!editor.isEditable) {
                return view;
            }

            const update = (key: string, value: string) =>
                editor.updateBlock(block, { props: { [key]: value } });

            return (
                <div className={`${styles.block} ${styles.editable} ${styles.tryItRoot}`}>
                    <div className={styles.editHeader}>AI Try It</div>
                    <div className={styles.editGrid}>
                        <label className={styles.editField}>
                            Workspace ID
                            <input value={workspaceId} onChange={e => update('workspaceId', e.target.value)} />
                        </label>
                    </div>
                    <div className={styles.editPreview}>{view}</div>
                </div>
            );
        },
    },
);
