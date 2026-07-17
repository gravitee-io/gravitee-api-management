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

import type { Api } from '../../editor/entities/api';
import { searchApis } from '../../editor/services/api.service';
import styles from './ApiSelectionDialog.module.scss';

interface ApiSelectionDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onSelect: (apiId: string, apiName: string) => void | Promise<void>;
}

export function ApiSelectionDialog({ open, onOpenChange, onSelect }: ApiSelectionDialogProps) {
    const [query, setQuery] = useState('');
    const [apis, setApis] = useState<Api[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!open) {
            setQuery('');
            return;
        }

        let cancelled = false;
        setLoading(true);

        void searchApis({ q: query, size: 20 }).then(response => {
            if (!cancelled) {
                setApis(response.data ?? []);
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
                    <DialogTitle>Select an API</DialogTitle>
                    <DialogDescription>Choose an API to add to the navigation tree.</DialogDescription>
                </DialogHeader>

                <Input
                    className={styles.search}
                    placeholder="Search APIs…"
                    value={query}
                    onChange={event => setQuery(event.target.value)}
                    aria-label="Search APIs"
                />

                {loading ? (
                    <div className={styles.state}>Loading APIs…</div>
                ) : apis.length === 0 ? (
                    <div className={styles.state}>No APIs found.</div>
                ) : (
                    <div className={styles.list} role="listbox" aria-label="API list">
                        {apis.map(api => (
                            <button
                                key={api.id}
                                type="button"
                                className={styles.apiRow}
                                role="option"
                                onClick={() => void onSelect(api.id, api.name)}
                            >
                                <span className={styles.apiName}>{api.name}</span>
                                <span className={styles.apiDescription}>{api.description}</span>
                                <span className={styles.apiMeta}>
                                    <span>v{api.version}</span>
                                    {api.categories?.[0] ? <span>{api.categories[0]}</span> : null}
                                </span>
                            </button>
                        ))}
                    </div>
                )}
            </DialogContent>
        </Dialog>
    );
}
