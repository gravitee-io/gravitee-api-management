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
import { ClockIcon } from '@gravitee/graphene-core/icons';

export function SharedPolicyGroupHistoryPage() {
    return (
        <Empty className="rounded-lg border py-10" data-testid="shared-policy-group-history-empty">
            <EmptyHeader>
                <EmptyMedia variant="icon">
                    <ClockIcon aria-hidden />
                </EmptyMedia>
                <EmptyTitle>Version history is not available yet</EmptyTitle>
                <EmptyDescription>Version history, comparison, and restore are not available in this view.</EmptyDescription>
            </EmptyHeader>
        </Empty>
    );
}
