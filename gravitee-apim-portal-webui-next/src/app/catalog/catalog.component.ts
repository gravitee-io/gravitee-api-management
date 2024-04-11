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

import { Component, OnInit } from '@angular/core';
import { MatCard, MatCardContent } from '@angular/material/card';

import { ApiCardComponent } from '../../components/api-card/api-card.component';
import { BannerComponent } from '../../components/banner/banner.component';
import { ApiService } from '../../services/api.service';

export interface ApiVM {
  id: string;
  title: string;
  version: string;
  content: string;
  picture?: string;
}

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [BannerComponent, MatCard, MatCardContent, ApiCardComponent],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
})
export class CatalogComponent implements OnInit {
  apis: ApiVM[] = [];

  // TODO: Get banner title + subtitle from configuration
  bannerTitle: string = 'Welcome to Gravitee Developer Portal!';
  bannerSubtitle: string = 'Discover powerful APIs to supercharge your projects.';

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.apiService.list().subscribe(resp => {
      if (resp.data) {
        this.apis = resp.data.map(api => ({
          id: api.id,
          content: api.description,
          version: api.version,
          title: api.name,
          picture: api._links?.picture,
        }));
      }
    });
  }
}
