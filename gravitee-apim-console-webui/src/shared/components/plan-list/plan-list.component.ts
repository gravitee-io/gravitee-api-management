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
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';
import { Component, input, output, TemplateRef } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { Plan } from '../../../entities/management-api-v2';

/** Plan row for the table: Plan plus required display label for a security type. */
export type PlanListTableRow = Plan & { securityTypeLabel: string };

@Component({
  selector: 'plan-list',
  templateUrl: './plan-list.component.html',
  styleUrls: ['./plan-list.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    DragDropModule,
    GioIconsModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    RouterModule,
  ],
})
export class PlanListComponent {
  dataSource = input<PlanListTableRow[]>([]);
  displayedColumns = input<string[]>([]);
  isLoading = input(false);
  isReadOnly = input(false);
  dragEnabled = input(false);
  planDetailLink = input<string | null>(null);
  showDesignButton = input(false);
  actionsTemplate = input<TemplateRef<{ $implicit: PlanListTableRow }> | null>(null);
  ariaLabel = input('Plans table');
  emptyMessage = input('There is no plan (yet).');
  loadingMessage = input('Loading...');
  tableId = input<string | null>(null);
  testIdPrefix = input<string | null>(null);

  dropRow = output<CdkDragDrop<PlanListTableRow[]>>();
  designPlan = output<string>();
  publishPlan = output<PlanListTableRow>();
  deprecatePlan = output<PlanListTableRow>();
  closePlan = output<PlanListTableRow>();
}
