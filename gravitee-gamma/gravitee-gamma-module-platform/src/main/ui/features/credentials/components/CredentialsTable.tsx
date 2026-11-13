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
import { Button, DataTable, DataTableEmptyState, SearchField } from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import type { Ref } from 'react';

import { credentialColumns } from './credentialColumns';
import type { Credential } from '../types/credential';

interface CredentialsTableProps {
    readonly credentials: Credential[];
    readonly totalCount: number;
    readonly loading: boolean;
    readonly searchRef: Ref<HTMLInputElement>;
    readonly search: string;
    readonly hasCriteria: boolean;
    readonly page: number;
    readonly pageSize: number;
    readonly pageSizeOptions: readonly number[];
    readonly onSearchChange: (value: string) => void;
    readonly onReset: () => void;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (size: number) => void;
}

export function CredentialsTable({
    credentials,
    totalCount,
    loading,
    searchRef,
    search,
    hasCriteria,
    page,
    pageSize,
    pageSizeOptions,
    onSearchChange,
    onReset,
    onPageChange,
    onPageSizeChange,
}: CredentialsTableProps) {
    return (
        <DataTable
            aria-label="Credentials"
            columns={credentialColumns}
            data={credentials}
            serverSide
            enableColumnVisibility
            loading={loading}
            skeletonCount={pageSize}
            tableOptions={{ getRowId: credential => credential.id }}
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...pageSizeOptions],
                onPageChange,
                onPageSizeChange,
            }}
            emptyMessage={
                <DataTableEmptyState
                    variant="no-results"
                    icon={<SearchIcon />}
                    title="No credential matches your search"
                    description="Try another search."
                />
            }
            toolbar={
                <div className="flex min-w-0 flex-wrap items-center gap-2">
                    <SearchField
                        ref={searchRef}
                        aria-label="Search credentials"
                        placeholder="Search credentials..."
                        value={search}
                        onValueChange={onSearchChange}
                        className="w-64"
                    />
                    {hasCriteria && (
                        <Button variant="ghost" size="sm" onClick={onReset}>
                            Reset
                        </Button>
                    )}
                </div>
            }
        />
    );
}
