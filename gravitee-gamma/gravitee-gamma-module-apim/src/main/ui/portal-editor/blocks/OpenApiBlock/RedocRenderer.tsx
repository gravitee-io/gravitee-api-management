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
import { RedocStandalone } from 'redoc';

import { parseOpenApiSpecObject } from '../../editor/utils/parse-openapi-spec';
import styles from './RedocRenderer.module.scss';

interface RedocRendererProps {
    readonly specContent: string;
}

export function RedocRenderer({ specContent }: RedocRendererProps) {
    const spec = parseOpenApiSpecObject(specContent);

    if (!spec) {
        return <p className={styles.empty}>Unable to render spec with Redoc.</p>;
    }

    return (
        <div className={styles.container} data-testid="redoc-renderer">
            <RedocStandalone
                spec={spec}
                options={{
                    scrollYOffset: 0,
                    hideDownloadButton: true,
                }}
            />
        </div>
    );
}
