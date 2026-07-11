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
import type { HtmlPageContent } from '../../portals/types';
import { HtmlContentView } from '../../html/HtmlContentView';
import styles from './HtmlPageViewer.module.scss';

interface HtmlPageViewerProps {
    readonly content: HtmlPageContent;
    readonly scopeId: string;
}

export function HtmlPageViewer({ content, scopeId }: HtmlPageViewerProps) {
    return (
        <div className={styles.viewer}>
            <HtmlContentView
                html={content.html}
                css={content.css ?? ''}
                scopeId={scopeId}
                styleTarget="html-page"
            />
        </div>
    );
}
