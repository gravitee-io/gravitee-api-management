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
import { Dashboard } from '@gravitee/gravitee-dashboard';

import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { switchMap } from 'rxjs/operators';
import { GioActionMenuComponent, GioActionMenuItemComponent } from '@gravitee/ui-particles-angular';
import { MatDivider } from '@angular/material/list';
import { MatTooltip } from '@angular/material/tooltip';

import { GioPermissionModule } from '../../../../shared/components/gio-permission/gio-permission.module';
import { DashboardService } from '../../data-access/dashboard.service';
import { DashboardViewerComponent } from '../ui/dashboard-viewer/dashboard-viewer.component';
import { GioHeaderComponent } from '../../../../shared/components/gio-header/gio-header.component';

@Component({
  selector: 'dashboard-detail',
  imports: [
    DashboardViewerComponent,
    GioActionMenuComponent,
    GioActionMenuItemComponent,
    MatDivider,
    MatTooltip,
    GioPermissionModule,
    GioHeaderComponent,
  ],
  templateUrl: './dashboard-detail.component.html',
  styleUrls: ['./dashboard-detail.component.scss'],
})
export class DashboardDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dashboardService = inject(DashboardService);

  readonly dashboard = toSignal(this.route.params.pipe(switchMap(params => this.dashboardService.getById(params['dashboardId']))));

  public deleteDashboard(dashboard: Dashboard): void {
    this.dashboardService.confirmAndDelete(dashboard).subscribe(() => {
      this.router.navigate(['..'], { relativeTo: this.route });
    });
  }
}
