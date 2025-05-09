/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, Input } from '@angular/core';
import { MatCard, MatCardContent } from '@angular/material/card';

export type BannerType = 'default' | 'error';

@Component({
  selector: 'app-banner',
  imports: [MatCard, MatCardContent],
  template: `
    <mat-card [class]="bannerStyles[type]">
      <mat-card-content>
        <ng-content />
      </mat-card-content>
    </mat-card>
  `,
  styleUrl: './banner.component.scss',
})
export class BannerComponent {
  @Input() type: BannerType = 'default';
  public bannerStyles = {
    default: 'banner',
    error: 'banner-error',
  };
}
