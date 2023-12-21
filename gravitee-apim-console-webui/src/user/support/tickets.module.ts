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
import { MatCardModule } from '@angular/material/card';
import { GioClipboardModule } from '@gravitee/ui-particles-angular';

import { TicketComponent } from './ticket/ticket.component';

import { GioGoBackButtonModule } from '../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { NewTicketComponent } from './new-ticket/new-ticket.component';

@NgModule({
  imports: [CommonModule, RouterModule, MatCardModule, GioGoBackButtonModule, GioClipboardModule],
  declarations: [TicketComponent, NewTicketComponent],
})
export class TicketsModule {}
