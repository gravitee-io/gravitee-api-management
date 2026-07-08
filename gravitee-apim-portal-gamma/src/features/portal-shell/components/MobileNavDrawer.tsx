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
import type { ReactNode } from 'react';

import styles from './MobileNavDrawer.module.scss';

interface MobileNavDrawerProps {
    readonly open: boolean;
    readonly title?: string;
    readonly onClose: () => void;
    readonly children: ReactNode;
}

export function MobileNavDrawer({ open, title = 'Navigation', onClose, children }: MobileNavDrawerProps) {
    if (!open) {
        return null;
    }

    return (
        <div className={styles.backdrop} onClick={onClose}>
            <nav
                className={styles.panel}
                aria-label={title}
                onClick={event => event.stopPropagation()}
            >
                <div className={styles.header}>
                    <span className={styles.title}>{title}</span>
                    <button type="button" className={styles.closeButton} onClick={onClose} aria-label="Close menu">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                            <path d="M18 6 6 18" />
                            <path d="m6 6 12 12" />
                        </svg>
                    </button>
                </div>
                <div className={styles.content}>{children}</div>
            </nav>
        </div>
    );
}
