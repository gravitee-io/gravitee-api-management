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
import type { DataTableColumnHeaderProps } from '@gravitee/graphene-core';

/** Column header render props for Graphene `DataTable` column definitions. */
export type ColHeader<T> = { column: DataTableColumnHeaderProps<T, unknown>['column'] };

/** Column cell render props for Graphene `DataTable` column definitions. */
export type ColCell<T> = { row: { original: T } };
