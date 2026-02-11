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
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface Breadcrumb {
  id: string;
  label: string;
  url?: string;
}

@Component({
  selector: 'app-breadcrumbs-component',
  template: `
    @for (breadcrumb of breadcrumbs(); track breadcrumb.id; let i = $index) {
      @if (i > 0) {
        <span class="breadcrumb-separator">/</span>
      }
      @if (breadcrumb.url) {
        <a class="internal-link" [routerLink]="breadcrumb.url">{{ breadcrumb.label }}</a>
      } @else {
        <span class="breadcrumb-item">{{ breadcrumb.label }}</span>
      }
    }
  `,
  styles: [
    `
      :host {
        display: flex;
        align-items: center;
        gap: 15px;
      }
    `,
  ],
  imports: [RouterLink],
})
export class BreadcrumbsComponent {
  breadcrumbs = input.required<Breadcrumb[]>();
}
