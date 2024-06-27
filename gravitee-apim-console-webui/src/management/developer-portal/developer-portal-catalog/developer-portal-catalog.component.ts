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
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioIconsModule, GioLoaderModule } from '@gravitee/ui-particles-angular';

@Component({
  selector: 'developer-portal-catalog',
  templateUrl: './developer-portal-catalog.component.html',
  styleUrls: ['./developer-portal-catalog.component.scss'],
  imports: [CommonModule, GioIconsModule, GioLoaderModule, MatButtonModule, MatTableModule, MatTooltipModule],
  standalone: true,
})
export class DeveloperPortalCatalogComponent {
  public displayedColumns = ['name', 'actions'];
  @Input()
  public dataSource: any[];
  @Input()
  public loading = true;
  @Output()
  public delete = new EventEmitter();
  @Output()
  public edit = new EventEmitter<string>();
}
