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
import type { Api } from '../../features/editor/entities/api';
import styles from './CatalogListRow.module.scss';

interface CatalogListRowProps {
    readonly api: Api;
    readonly clickable?: boolean;
    readonly onClick?: () => void;
}

export function CatalogListRow({ api, clickable = false, onClick }: CatalogListRowProps) {
    const handleClick = (event: React.MouseEvent) => {
        if (!clickable) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        onClick?.();
    };

    return (
        <div className={`${styles.row} ${clickable ? styles.clickable : ''}`} onClick={handleClick}>
            <span className={styles.name}>{api.name}</span>
            <span className={styles.description}>
                {api.description || 'Description for this API is missing.'}
            </span>
            <div className={styles.meta}>
                {api.labels?.includes('MCP') ? <span className={styles.mcpBadge}>MCP</span> : null}
                <span className={styles.version}>v{api.version}</span>
            </div>
        </div>
    );
}
