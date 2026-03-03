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
import { Widget } from '@gravitee/gravitee-dashboard';

import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { DashboardService } from '../data-access/dashboard.service';
import { DashboardViewerComponent } from '../dashboards/ui/dashboard-viewer/dashboard-viewer.component';

@Component({
  selector: 'overview',
  imports: [DashboardViewerComponent, RouterModule, MatIconModule, GioIconsModule],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.scss',
})
export class OverviewComponent {
  widgets: Widget[] = inject(DashboardService).overviewDashboard().widgets;
}
