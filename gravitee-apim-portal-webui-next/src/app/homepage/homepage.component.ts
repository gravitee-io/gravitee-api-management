/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
/*
 * Copyright (C) 2025 The Gravitee team
 */
import { AsyncPipe, NgIf } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { map, of } from 'rxjs';

import { PageComponent } from '../../components/page/page.component';
import { Page } from '../../entities/page/page';
import { PortalHomepage } from '../../entities/portal/portal-homepage';
import { PortalService } from '../../services/portal.service';

@Component({
  selector: 'app-homepage',
  standalone: true,
  imports: [NgIf, AsyncPipe, PageComponent],
  template: `
    <ng-container *ngIf="page$ | async as page">
      <app-page [page]="page" />
    </ng-container>
  `,
})
export class HomepageComponent {
  page$ = of(null as Page | null);

  constructor(
    private readonly router: Router,
    private readonly portalService: PortalService,
  ) {
    const state = this.router.getCurrentNavigation()?.extras?.state as { homepage?: PortalHomepage } | undefined;
    if (state?.homepage) {
      this.page$ = of(this.mapPortalHomepageToPage(state.homepage));
    } else {
      this.page$ = this.portalService.getPortalHomepage(true).pipe(map(h => this.mapPortalHomepageToPage(h)));
    }
  }

  private mapPortalHomepageToPage(h: PortalHomepage): Page {
    return {
      id: h.id,
      name: h.name ?? 'Homepage',
      type: h.type === 'GRAVITEE_MARKDOWN' ? 'GRAVITEE_MARKDOWN' : 'MARKDOWN',
      order: 0,
      content: h.content ?? '',
    };
  }
}
