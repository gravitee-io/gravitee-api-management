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
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { MatCard, MatCardContent } from '@angular/material/card';

import { ApiCardComponent } from '../../components/api-card/api-card.component';
import { BannerComponent } from '../../components/banner/banner.component';

export interface ApiVM {
  title: string;
  version: string;
  content: string;
  id: number;
}

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [BannerComponent, MatCard, MatCardContent, ApiCardComponent, CommonModule],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
})
export class CatalogComponent {
  apis: ApiVM[] = [
    {
      title: 'Test tile',
      version: 'v.1.2',
      content:
        'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
      id: 1,
    },
  ];

  // TODO: Get banner title + subtitle from configuration
  bannerTitle: string = 'Welcome to Gravitee Developer Portal!';
  bannerSubtitle: string = 'Discover powerful APIs to supercharge your projects.';
}
