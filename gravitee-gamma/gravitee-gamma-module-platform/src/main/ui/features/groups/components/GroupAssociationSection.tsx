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

import { GroupMembershipTable } from './GroupMembershipTable';
import { SectionError } from './SectionError';
import type { GroupMembershipItem } from '../types/group';

interface GroupAssociationSectionProps {
    readonly title: string;
    readonly error: boolean;
    readonly errorMessage: string;
    readonly items: GroupMembershipItem[];
    readonly loading: boolean;
    readonly ariaLabel: string;
    readonly searchPlaceholder: string;
    readonly emptyTitle: string;
    readonly showVersionColumn?: boolean;
}

export function GroupAssociationSection({
    title,
    error,
    errorMessage,
    items,
    loading,
    ariaLabel,
    searchPlaceholder,
    emptyTitle,
    showVersionColumn,
}: GroupAssociationSectionProps) {
    return (
        <section className="space-y-4 rounded-xl border bg-card p-5">
            <h2 className="text-base font-semibold">{title}</h2>
            {error ? (
                <SectionError message={errorMessage} />
            ) : (
                <GroupMembershipTable
                    items={items}
                    loading={loading}
                    ariaLabel={ariaLabel}
                    searchPlaceholder={searchPlaceholder}
                    emptyTitle={emptyTitle}
                    showVersionColumn={showVersionColumn}
                />
            )}
        </section>
    );
}
