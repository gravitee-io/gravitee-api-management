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
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, computed, input, OnChanges, SimpleChanges } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

export type GioWidgetLayoutState = 'loading' | 'error' | 'success' | 'empty';

@Component({
  selector: 'gio-widget-layout',
  standalone: true,
  imports: [MatCardModule, MatIconModule, GioLoaderModule, MatTooltipModule],
  templateUrl: './gio-widget-layout.component.html',
  styleUrl: './gio-widget-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GioWidgetLayoutComponent implements OnChanges {
  title = input.required<string>();
  tooltip = input<string>();
  state = input.required<GioWidgetLayoutState>();
  errors = input<string[]>();

  errorMessage = computed(() => {
    const errors = this.errors();
    return errors && errors.length > 0 ? errors.join(', ') : 'An error occurred';
  });

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnChanges(_changes: SimpleChanges): void {
    // Check for changes for ng-content to adjust. Notably used for chart resizing.
    this.cdr.detectChanges();
  }
}
