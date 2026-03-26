/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { Dashboard, DashboardCapabilities, DASHBOARD_PERSISTENCE, SaveState } from '@gravitee/gravitee-dashboard';

import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { EMPTY } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';
import { DashboardService } from '../../data-access/dashboard.service';
import { DashboardViewerComponent } from '../ui/dashboard-viewer/dashboard-viewer.component';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'dashboard-detail',
  imports: [DashboardViewerComponent],
  providers: [
    {
      provide: DASHBOARD_PERSISTENCE,
      useFactory: (svc: DashboardService, snackBar: SnackBarService) => ({
        update: (d: Dashboard) =>
          svc.update(d).pipe(
            catchError(({ error }) => {
              snackBar.error(error?.message ?? 'Failed to save dashboard.');
              return EMPTY;
            }),
          ),
      }),
      deps: [DashboardService, SnackBarService],
    },
  ],
  templateUrl: './dashboard-detail.component.html',
  styleUrls: ['./dashboard-detail.component.scss'],
})
export class DashboardDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dashboardService = inject(DashboardService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly snackBarService = inject(SnackBarService);

  readonly dashboard = toSignal(this.route.params.pipe(switchMap(params => this.dashboardService.getById(params['dashboardId']))));

  readonly capabilities = computed<DashboardCapabilities>(() => ({
    canEditMetadata: this.permissionService.hasAnyMatching(['environment-dashboard-u']),
    canAddWidget: this.permissionService.hasAnyMatching(['environment-dashboard-u']),
    canEditLayout: this.permissionService.hasAnyMatching(['environment-dashboard-u']),
    canEditWidgetConfig: false,
    canDeleteDashboard: this.permissionService.hasAnyMatching(['environment-dashboard-d']),
  }));

  onDeleteRequested(): void {
    const dashboard = this.dashboard();
    if (!dashboard) return;
    this.dashboardService.confirmAndDelete(dashboard).subscribe(() => {
      this.router.navigate(['..'], { relativeTo: this.route });
    });
  }

  onSaveStateChange(state: SaveState): void {
    if (state === 'saved') {
      this.snackBarService.success('Dashboard updated');
    }
  }
}
