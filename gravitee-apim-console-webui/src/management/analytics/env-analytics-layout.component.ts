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
import { Component } from '@angular/core';

@Component({
  selector: 'env-alerts-layout',
  template: `
    <nav mat-tab-nav-bar class="navigation-tabs" [tabPanel]="tabPanel">
      <a mat-tab-link routerLinkActive #rla1="routerLinkActive" [active]="rla1.isActive" routerLink="dashboard"
        ><mat-icon class="navigation-tabs__icon" svgIcon="gio:dashboard-dots"></mat-icon> Dashboard</a
      >
      <a mat-tab-link routerLinkActive #rla2="routerLinkActive" [active]="rla2.isActive" routerLink="logs"
        ><mat-icon class="navigation-tabs__icon" svgIcon="gio:table-rows"></mat-icon> Logs</a
      >
    </nav>
    <mat-tab-nav-panel #tabPanel>
      <router-outlet></router-outlet>
    </mat-tab-nav-panel>
  `,
  styles: [
    `
      .navigation-tabs {
        display: flex;
        align-items: center;
        width: 100%;
      }
      .navigation-tabs__icon {
        margin-right: 8px;
      }
    `,
  ],
  standalone: false,
})
export class EnvAnalyticsLayoutComponent {}
