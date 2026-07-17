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

import type { ApiProduct } from '../../editor/entities/api-product';
import { searchApiProducts } from '../../editor/services/api-product.service';
import styles from './ApiSelectionDialog.module.scss';

interface ApiProductSelectionDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onSelect: (apiProductId: string, apiProductName: string) => void | Promise<void>;
}

export function ApiProductSelectionDialog({ open, onOpenChange, onSelect }: ApiProductSelectionDialogProps) {
    const [query, setQuery] = useState('');
    const [products, setProducts] = useState<ApiProduct[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!open) {
            setQuery('');
            return;
        }

        let cancelled = false;
        setLoading(true);

        void searchApiProducts({ q: query, size: 20 }).then(response => {
            if (!cancelled) {
                setProducts(response.data ?? []);
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
                    <DialogTitle>Select an API Product</DialogTitle>
                    <DialogDescription>Choose an API product to add to the navigation tree.</DialogDescription>
                </DialogHeader>

                <Input
                    className={styles.search}
                    placeholder="Search API products…"
                    value={query}
                    onChange={event => setQuery(event.target.value)}
                    aria-label="Search API products"
                />

                {loading ? (
                    <div className={styles.state}>Loading API products…</div>
                ) : products.length === 0 ? (
                    <div className={styles.state}>No API products found.</div>
                ) : (
                    <div className={styles.list} role="listbox" aria-label="API product list">
                        {products.map(product => (
                            <button
                                key={product.id}
                                type="button"
                                className={styles.apiRow}
                                role="option"
                                onClick={() => void onSelect(product.id, product.name)}
                            >
                                <span className={styles.apiName}>{product.name}</span>
                                <span className={styles.apiDescription}>{product.description}</span>
                                <span className={styles.apiMeta}>
                                    <span>v{product.version}</span>
                                    {product.apiIds?.length ? <span>{product.apiIds.length} APIs</span> : null}
                                </span>
                            </button>
                        ))}
                    </div>
                )}
            </DialogContent>
        </Dialog>
    );
}
