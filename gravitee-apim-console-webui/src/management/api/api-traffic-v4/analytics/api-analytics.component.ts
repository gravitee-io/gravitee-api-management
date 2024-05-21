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
import { GioCardEmptyStateModule } from '@gravitee/ui-particles-angular';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle } from '@angular/material/card';

@Component({
  selector: 'api-analytics',
  standalone: true,
  imports: [GioCardEmptyStateModule, MatButton, MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle],
  templateUrl: './api-analytics.component.html',
  styleUrl: './api-analytics.component.scss',
})
export class ApiAnalyticsComponent {
  isEmptyAnalytics = true;
}
