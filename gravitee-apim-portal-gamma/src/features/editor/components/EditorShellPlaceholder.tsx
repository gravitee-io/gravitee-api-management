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
import type { PortalLayout } from '../../portals/types';
import type { EditorMode } from '../stores/editor.store';
import styles from './EditorShellPlaceholder.module.scss';

interface EditorShellPlaceholderProps {
    readonly layout: PortalLayout;
    readonly mode: EditorMode;
    readonly children: React.ReactNode;
}

export function EditorShellPlaceholder({ layout, mode, children }: EditorShellPlaceholderProps) {
    const isPreview = mode === 'preview';
    const shellClassName = [
        styles.shell,
        layout === 'header-content-footer' ? styles.headerLayout : styles.sidebarLayout,
        isPreview ? styles.preview : styles.edit,
    ].join(' ');

    if (layout === 'header-content-footer') {
        return (
            <div className={shellClassName} data-layout={layout} data-mode={mode}>
                <div className={styles.portalHeader} aria-hidden={isPreview}>
                    <span className={styles.placeholderBar} />
                </div>
                <main className={styles.content}>{children}</main>
                <div className={styles.portalFooter} aria-hidden={isPreview}>
                    <span className={styles.placeholderBar} />
                </div>
            </div>
        );
    }

    return (
        <div className={shellClassName} data-layout={layout} data-mode={mode}>
            <aside className={styles.sidebar} aria-hidden={isPreview}>
                <span className={styles.placeholderBar} />
            </aside>
            <main className={styles.content}>{children}</main>
        </div>
    );
}
