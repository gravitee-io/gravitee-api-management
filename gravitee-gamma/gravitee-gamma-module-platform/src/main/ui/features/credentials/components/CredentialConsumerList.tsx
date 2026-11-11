/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Badge } from '@gravitee/graphene-core';
import { Link } from 'react-router-dom';

import { useEnvironment } from '@gravitee/gamma-modules-sdk';

import type { CredentialConsumer } from '../types/credential';
import { credentialConsumerHref, credentialConsumerTypeLabel } from '../utils/credentialConsumers';

export function CredentialConsumerList({ consumers }: Readonly<{ consumers: readonly CredentialConsumer[] }>) {
    const env = useEnvironment();

    return (
        <ul className="space-y-2">
            {consumers.map(consumer => {
                const href = env ? credentialConsumerHref(env.id, consumer) : undefined;
                return (
                    <li key={`${consumer.module}:${consumer.type}:${consumer.id}`} className="flex items-center gap-2 text-sm">
                        {href ? (
                            <Link className="font-medium text-foreground hover:underline" to={href}>
                                {consumer.name}
                            </Link>
                        ) : (
                            <span className="font-medium">{consumer.name}</span>
                        )}
                        <Badge variant="outline">{credentialConsumerTypeLabel(consumer.type)}</Badge>
                    </li>
                );
            })}
        </ul>
    );
}
