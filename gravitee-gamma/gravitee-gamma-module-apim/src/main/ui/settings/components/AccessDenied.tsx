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
import { Empty, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@gravitee/graphene-core';
import { LockIcon } from 'lucide-react';

interface AccessDeniedProps {
    readonly itemLabel: string;
    readonly envName: string;
}

export function AccessDenied({ itemLabel, envName }: AccessDeniedProps) {
    return (
        <Empty className="py-16">
            <EmptyHeader>
                <EmptyMedia variant="icon">
                    <LockIcon />
                </EmptyMedia>
                <EmptyTitle>Access Restricted</EmptyTitle>
                <EmptyDescription>
                    You do not have permission to view <strong>{itemLabel}</strong> in the <strong>{envName}</strong>{' '}
                    environment.
                </EmptyDescription>
            </EmptyHeader>
        </Empty>
    );
}
