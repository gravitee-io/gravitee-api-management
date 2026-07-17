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
} from '@gravitee/graphene-core';

import type { PageContentType } from '../../portals/types';
import { getPageContentTypeIcon } from '../utils/nav-type-icons';
import { PAGE_TYPE_OPTIONS, type PageTypeOption } from '../utils/page-type-options';
import styles from './PageTypeDialog.module.scss';

interface PageTypeDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onSelect: (contentType: PageContentType) => void;
    readonly options?: ReadonlyArray<PageTypeOption>;
}

export function PageTypeDialog({ open, onOpenChange, onSelect, options = PAGE_TYPE_OPTIONS }: PageTypeDialogProps) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className={styles.content} style={{ width: 'min(92vw, 36rem)', maxWidth: 'min(92vw, 36rem)' }}>
                <DialogHeader>
                    <DialogTitle>Choose page type</DialogTitle>
                    <DialogDescription>Select the kind of content this page will display.</DialogDescription>
                </DialogHeader>

                <div className={styles.grid} role="listbox" aria-label="Page types">
                    {options.map(option => (
                        <button
                            key={option.contentType}
                            type="button"
                            className={styles.option}
                            role="option"
                            onClick={() => onSelect(option.contentType)}
                        >
                            <span className={styles.icon} aria-hidden="true">
                                {getPageContentTypeIcon(option.contentType)}
                            </span>
                            <span className={styles.label}>{option.label}</span>
                            <span className={styles.description}>{option.description}</span>
                        </button>
                    ))}
                </div>
            </DialogContent>
        </Dialog>
    );
}
