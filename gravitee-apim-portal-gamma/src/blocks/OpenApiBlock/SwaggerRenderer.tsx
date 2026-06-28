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
import { useMemo } from 'react';
import SwaggerUI from 'swagger-ui-react';
import 'swagger-ui-react/swagger-ui.css';

import { parseOpenApiSpecObject } from '../../features/editor/utils/parse-openapi-spec';
import styles from './SwaggerRenderer.module.scss';

interface SwaggerRendererProps {
    readonly specContent: string;
}

export function SwaggerRenderer({ specContent }: SwaggerRendererProps) {
    const spec = useMemo(() => parseOpenApiSpecObject(specContent), [specContent]);

    if (!spec) {
        return null;
    }

    return (
        <div className={styles.container} data-testid="swagger-renderer">
            <SwaggerUI spec={spec} docExpansion="list" defaultModelsExpandDepth={1} />
        </div>
    );
}
