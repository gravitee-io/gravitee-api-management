/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Directive, inject, input, TemplateRef } from '@angular/core';

/**
 * Projects a custom cell template into `PaginatedTableComponent`, matched against a column id.
 *
 * Usage:
 * ```html
 * <app-paginated-table [columns]="columns" [rows]="rows" ...>
 *   <ng-template appTableCell="name" let-row>
 *     <app-user-cell [user]="row.user" />
 *   </ng-template>
 * </app-paginated-table>
 * ```
 *
 * The context exposes the row as `$implicit` (so `let-row` works) and as `row`.
 */
@Directive({
  selector: '[appTableCell]',
  standalone: true,
})
export class TableCellDirective<T = unknown> {
  /** Column id to bind this template to (must match `TableColumn.id`). */
  appTableCell = input.required<string>();

  readonly templateRef = inject(TemplateRef<TableCellContext<T>>);
}

export interface TableCellContext<T> {
  $implicit: T;
  row: T;
}
