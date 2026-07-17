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
import { getGmdBlockHooks } from '../../editor/gmd/gmd-block-hooks';

import { ApiTryItView } from './ApiTryItView';
import styles from './ApiTryItBlock.module.scss';

export const ApiTryItBlock = createReactBlockSpec(
    {
        type: 'graviteeApiTryIt' as const,
        propSchema: {
            tag: { default: '' },
            operationId: { default: '' },
            serverUrl: { default: '' },
            authType: { default: 'none' },
            authValue: { default: '' },
        },
        content: 'none',
    },
    {
        ...getGmdBlockHooks('graviteeApiTryIt'),
        render: ({ block, editor }) => {
            const isEditable = editor.isEditable;

            return (
                <div className={`${styles.blockWrapper} ${isEditable ? styles.editable : ''}`}>
                    <ApiTryItView
                        tag={block.props.tag}
                        operationId={block.props.operationId || undefined}
                        serverUrlOverride={block.props.serverUrl || undefined}
                        authType={block.props.authType}
                        authValue={block.props.authValue}
                        isEditable={isEditable}
                    />
                </div>
            );
        },
    },
);
