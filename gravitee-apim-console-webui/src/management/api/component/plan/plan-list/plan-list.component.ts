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
import { Component, computed, input, output } from '@angular/core';
import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CommonModule } from '@angular/common';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { Plan, PlanStatus } from '../../../../../entities/management-api-v2';
import { PlanMenuItemVM } from '../../../../../services-ngx/constants.service';

export type PlanDS = Plan & { securityTypeLabel: string };

@Component({
  selector: 'plan-list',
  templateUrl: './plan-list.component.html',
  styleUrls: ['./plan-list.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    DragDropModule,
    MatTableModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    GioIconsModule,
  ],
})
export class PlanListComponent {
  readonly plans = input<PlanDS[]>([]);
  readonly planMenuItems = input<PlanMenuItemVM[]>([]);
  readonly isReadOnly = input(false);
  readonly canAddPlan = input(true);
  readonly isV2Api = input(false);
  readonly showDeployOnColumn = input(true);
  readonly planStatuses = input<{ name: PlanStatus; number: number | null }[]>([]);
  readonly selectedStatus = input<PlanStatus>('PUBLISHED');
  readonly isLoadingData = input(false);

  readonly planTypeSelected = output<string>();
  readonly planReordered = output<CdkDragDrop<string[]>>();
  readonly planPublish = output<Plan>();
  readonly planDeprecate = output<Plan>();
  readonly planClose = output<Plan>();
  readonly planDesign = output<string>();
  readonly statusFilterChanged = output<PlanStatus>();

  readonly displayedColumns = computed(() => {
    const cols = ['name', 'type', 'status'];
    if (this.showDeployOnColumn()) {
      cols.push('deploy-on');
    }
    cols.push('actions');
    if (!this.isReadOnly()) {
      cols.unshift('drag-icon');
    }
    return cols;
  });
}
