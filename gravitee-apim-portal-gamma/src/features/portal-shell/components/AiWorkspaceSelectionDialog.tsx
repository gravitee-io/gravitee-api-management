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
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
    Input,
} from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import type { AiWorkspace } from '../../editor/entities/ai-workspace';
import { searchAiWorkspaces } from '../../editor/services/ai-workspace.service';
import styles from './ApiSelectionDialog.module.scss';

interface AiWorkspaceSelectionDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onSelect: (aiWorkspaceId: string, aiWorkspaceName: string) => void | Promise<void>;
}

export function AiWorkspaceSelectionDialog({ open, onOpenChange, onSelect }: AiWorkspaceSelectionDialogProps) {
    const [query, setQuery] = useState('');
    const [workspaces, setWorkspaces] = useState<AiWorkspace[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!open) {
            setQuery('');
            return;
        }

        let cancelled = false;
        setLoading(true);

        void searchAiWorkspaces({ q: query, size: 20 }).then(response => {
            if (!cancelled) {
                setWorkspaces(response.data ?? []);
                setLoading(false);
            }
        });

        return () => {
            cancelled = true;
        };
    }, [open, query]);

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent
                className={styles.content}
                style={{ width: 'min(92vw, 48rem)', maxWidth: 'min(92vw, 48rem)', maxHeight: '65vh' }}
            >
                <DialogHeader>
                    <DialogTitle>Select an AI Workspace</DialogTitle>
                    <DialogDescription>
                        Adds the workspace with auto-generated Getting Started, Models, Snippets, and Usage pages.
                    </DialogDescription>
                </DialogHeader>

                <Input
                    className={styles.search}
                    placeholder="Search AI workspaces…"
                    value={query}
                    onChange={event => setQuery(event.target.value)}
                    aria-label="Search AI workspaces"
                />

                {loading ? (
                    <div className={styles.state}>Loading AI workspaces…</div>
                ) : workspaces.length === 0 ? (
                    <div className={styles.state}>No AI workspaces found.</div>
                ) : (
                    <div className={styles.list} role="listbox" aria-label="AI workspace list">
                        {workspaces.map(workspace => (
                            <button
                                key={workspace.id}
                                type="button"
                                className={styles.apiRow}
                                role="option"
                                onClick={() => void onSelect(workspace.id, workspace.name)}
                            >
                                <span className={styles.apiName}>{workspace.name}</span>
                                <span className={styles.apiDescription}>{workspace.description}</span>
                                <span className={styles.apiMeta}>
                                    <span>{workspace.models.length} models</span>
                                    <span>{workspace.providers.length} providers</span>
                                </span>
                            </button>
                        ))}
                    </div>
                )}
            </DialogContent>
        </Dialog>
    );
}
