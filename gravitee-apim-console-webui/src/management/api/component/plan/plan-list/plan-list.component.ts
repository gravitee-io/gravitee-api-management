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

export interface PlanFilterState {
  statuses: { name: PlanStatus; number: number | null }[];
  selectedStatus: PlanStatus;
}

export type PlanActionType = 'PUBLISH' | 'DEPRECATE' | 'CLOSE' | 'DESIGN';

export interface PlanActionEvent {
  action: PlanActionType;
  plan: PlanDS;
}

export interface PlanListContext {
  isReadOnly?: boolean;
  canAddPlan?: boolean;
  isV2Api?: boolean;
  showDeployOnColumn?: boolean;
}

@Component({
  selector: 'plan-list',
  templateUrl: './plan-list.component.html',
  styleUrls: ['./plan-list.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
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
  readonly isLoadingData = input(false);
  readonly filterState = input<PlanFilterState>();
  readonly context = input<PlanListContext>({});

  readonly actionSelected = output<PlanActionEvent>();
  readonly typeSelected = output<string>();
  readonly planSelected = output<PlanDS>();
  readonly reordered = output<CdkDragDrop<string[]>>();
  readonly statusFilterChanged = output<PlanStatus>();

  protected readonly effectiveContext = computed(() => {
    const ctx = this.context();
    return {
      isReadOnly: ctx.isReadOnly ?? false,
      canAddPlan: ctx.canAddPlan ?? true,
      isV2Api: ctx.isV2Api ?? false,
      showDeployOnColumn: ctx.showDeployOnColumn ?? true,
    };
  });

  protected readonly effectiveFilterState = computed<PlanFilterState>(() => {
    const fs = this.filterState();
    return {
      statuses: fs?.statuses ?? [],
      selectedStatus: (fs?.selectedStatus ?? 'PUBLISHED') as PlanStatus,
    };
  });

  readonly displayedColumns = computed(() => {
    const ctx = this.effectiveContext();
    const cols = ['name', 'type', 'status'];
    if (ctx.showDeployOnColumn) {
      cols.push('deploy-on');
    }
    cols.push('actions');
    if (!ctx.isReadOnly) {
      cols.unshift('drag-icon');
    }
    return cols;
  });
}
