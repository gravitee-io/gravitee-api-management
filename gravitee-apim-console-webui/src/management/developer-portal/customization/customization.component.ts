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
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable,
} from '@angular/material/table';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { NgForOf, NgIf } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatTab, MatTabGroup, MatTabLink, MatTabNav, MatTabNavPanel } from '@angular/material/tabs';

import { MapProviderNamePipe } from '../../integrations/pipes/map-provider-name.pipe';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { ApiHealthCheckV4FormModule } from '../../api/component/health-check-v4-form/api-health-check-v4-form.module';

@Component({
  selector: 'customization',
  standalone: true,
  imports: [
    GioBannerModule,
    GioPermissionModule,
    GioTableWrapperModule,
    MapProviderNamePipe,
    MatButton,
    MatCard,
    MatCardContent,
    MatCell,
    MatCellDef,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderRow,
    MatHeaderRowDef,
    MatIcon,
    MatIconButton,
    MatRow,
    MatRowDef,
    MatTable,
    MatTooltip,
    NgIf,
    RouterLink,
    MatTabLink,
    MatTabNav,
    MatTabNavPanel,
    NgForOf,
    RouterLinkActive,
    ApiHealthCheckV4FormModule,
    MatTab,
    MatTabGroup,
  ],
  templateUrl: './customization.component.html',
  styleUrl: './customization.component.scss',
})
export class CustomizationComponent {}
