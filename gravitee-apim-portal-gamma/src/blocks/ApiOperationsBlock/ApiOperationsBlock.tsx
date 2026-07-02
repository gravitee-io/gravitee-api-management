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
import { createReactBlockSpec } from '@blocknote/react';
import { getGmdBlockHooks } from '../../features/editor/gmd/gmd-block-hooks';

import { ApiOperationsView } from './ApiOperationsView';
import styles from './ApiOperationsBlock.module.scss';

export const ApiOperationsBlock = createReactBlockSpec(
    {
        type: 'graviteeApiOperations' as const,
        propSchema: {
            tag: { default: '' },
            operationId: { default: '' },
            showResponses: { default: 'true' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeApiOperations'),
        render: ({ block, editor }) => {
            const isEditable = editor.isEditable;
            const tag = block.props.tag;
            const operationId = block.props.operationId;
            const showResponses = block.props.showResponses !== 'false';

            return (
                <div className={`${styles.blockWrapper} ${isEditable ? styles.editable : ''}`}>
                    <ApiOperationsView
                        tag={tag}
                        operationId={operationId || undefined}
                        showResponses={showResponses}
                        isEditable={isEditable}
                    />
                </div>
            );
        },
    },
);
