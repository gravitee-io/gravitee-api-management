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
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { GioClipboardModule, GioFormFocusInvalidModule, GioFormSlideToggleModule, GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { MatLegacyFormFieldModule as MatFormFieldModule } from '@angular/material/legacy-form-field';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { ReactiveFormsModule } from '@angular/forms';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { MatLegacyOptionModule as MatOptionModule } from '@angular/material/legacy-core';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatSortModule } from '@angular/material/sort';

import { TicketComponent } from './ticket/ticket.component';
import { NewTicketComponent } from './new-ticket/new-ticket.component';
import { TicketsComponent } from './tickets/tickets.component';
import { TicketsRoutingModule } from './tickets-routing.module';

import { GioGoBackButtonModule } from '../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { GioTableWrapperModule } from '../../shared/components/gio-table-wrapper/gio-table-wrapper.module';

@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,

    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatOptionModule,
    MatSelectModule,
    MatSnackBarModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatSortModule,

    GioGoBackButtonModule,
    GioClipboardModule,
    GioSaveBarModule,
    GioFormFocusInvalidModule,
    GioFormSlideToggleModule,
    GioTableWrapperModule,

    TicketsRoutingModule,
  ],
  declarations: [TicketComponent, NewTicketComponent, TicketsComponent],
})
export class TicketsModule {}
