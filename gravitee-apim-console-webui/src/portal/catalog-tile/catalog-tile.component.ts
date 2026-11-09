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
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { CATALOG_CARD_TEMPLATE_NAV_ID } from './catalog-card-template.constants';

import { HasUnsavedChanges } from '../../shared/guards/has-unsaved-changes.guard';

/**
 * Legacy Catalog settings route — redirects into Navigation's Catalog folder
 * so old bookmarks to /api-catalog still open the editor.
 */
@Component({
  selector: 'portal-catalog-tile',
  template: '',
})
export class CatalogTileComponent implements OnInit, HasUnsavedChanges {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  ngOnInit(): void {
    void this.router.navigate(['../navigation'], {
      relativeTo: this.route,
      queryParams: { navId: CATALOG_CARD_TEMPLATE_NAV_ID },
      replaceUrl: true,
    });
  }

  hasUnsavedChanges(): boolean {
    return false;
  }
}
