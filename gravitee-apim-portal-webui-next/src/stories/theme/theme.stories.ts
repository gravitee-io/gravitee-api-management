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
import { MatButtonModule } from '@angular/material/button';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { action } from '@storybook/addon-actions';
import { Args, Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { of } from 'rxjs/internal/observable/of';
import { BreadcrumbService } from 'xng-breadcrumb';

import { computeAndInjectThemeForStory } from './theme.util';
import { ApiDetailsComponent } from '../../app/api/api-details/api-details.component';
import { ApiCardComponent } from '../../components/api-card/api-card.component';
import { BannerComponent } from '../../components/banner/banner.component';
import { CompanyTitleComponent } from '../../components/company-title/company-title.component';
import { NavBarComponent } from '../../components/nav-bar/nav-bar.component';
import { PageTreeComponent } from '../../components/page-tree/page-tree.component';
import { fakeApi } from '../../entities/api/api.fixtures';
import { fakePage } from '../../entities/page/page.fixtures';
import { fakeUser } from '../../entities/user/user.fixtures';
import { TESTING_ACTIVATED_ROUTE } from '../../testing/app-testing.module';

export default {
  title: 'Theme',
  decorators: [
    moduleMetadata({
      imports: [
        CompanyTitleComponent,
        NavBarComponent,
        BannerComponent,
        NoopAnimationsModule,
        MatButtonModule,
        ApiCardComponent,
        ApiDetailsComponent,
        PageTreeComponent,
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: TESTING_ACTIVATED_ROUTE,
        },
        {
          provide: BreadcrumbService,
          useValue: {
            activatedRoute: TESTING_ACTIVATED_ROUTE,
            set: (_key: string, _breadcrumb: string) => {},
            breadcrumbs$: of([]),
          },
        },
      ],
    }),
  ],
  render: () => ({}),
} as Meta;

export const Default: StoryObj = {
  argTypes: {
    primary: {
      control: { type: 'color' },
    },
    secondary: {
      control: { type: 'color' },
    },
    tertiary: {
      control: { type: 'color' },
    },
    error: {
      control: { type: 'color' },
    },
    background: {
      control: { type: 'color' },
    },
    bannerBackground: {
      control: { type: 'color' },
    },
    bannerText: {
      control: { type: 'color' },
    },
  },
  render: (args: Args) => {
    computeAndInjectThemeForStory(args);
    return {
      template: `
<div style="display: flex; justify-content: center;">
        <div style="display:flex; flex-flow: column; gap: 35px; width: 1040px;">
         <app-nav-bar [currentUser]="user"> </app-nav-bar>
          <div style="display: flex; flex-flow: column; gap: 32px">
                      <div style="width: 360px">
                <h2>API Card</h2>
                <app-api-card [id]="api.id" [picture]="api.picture" [content]="api.description" [title]="api.name" [version]="api.version" />
            </div>
                        <div>
             <h2>API Details</h2>
             <app-api-details [api]="api" />
</div>
          <div>
                    <h2>Banner</h2>
            <app-banner>
              <h1>My really nice title!</h1>
              <h5>And a good subtitle :)</h5>
            </app-banner>
</div>

            <div>
              <h2>Buttons</h2>
              <div>
                  <h4>Raised</h4>
                  <div class="button-container">
                      <button mat-raised-button>Basic</button>
                      <button mat-raised-button color="primary">Primary</button>
                      <button mat-raised-button class="secondary-button">Secondary</button>
                      <button mat-raised-button color="accent">Tertiary</button>
                      <button mat-raised-button color="warn">Error</button>
                      <button mat-raised-button disabled>Disabled</button>
                  </div>
              </div>

              <div>
                  <h4>Flat</h4>
                  <div class="button-container">
                      <button mat-flat-button>Basic</button>
                      <button mat-flat-button color="primary">Primary</button>
                      <button mat-flat-button class="secondary-button">Secondary</button>
                      <button mat-flat-button color="accent">Tertiary</button>
                      <button mat-flat-button color="warn">Error</button>
                      <button mat-flat-button disabled>Disabled</button>
                  </div>
              </div>

              <div>
                  <h4>Stroked</h4>
                  <div class="button-container">
                      <button mat-stroked-button>Basic</button>
                      <button mat-stroked-button color="primary">Primary</button>
                      <button mat-stroked-button class="secondary-button">Secondary</button>
                      <button mat-stroked-button color="accent">Tertiary</button>
                      <button mat-stroked-button color="warn">Error</button>
                      <button mat-stroked-button disabled>Disabled</button>
                  </div>
              </div>
          </div>


<div style="width: 300px">
<h2>Page Tree</h2>
          <app-page-tree [activePage]="page" [pages]="pages" (openFile)="onOpenFile($event)"/>

</div>
          </div>

        </div>
        </div>
        `,
      props: {
        user: fakeUser(),
        api: fakeApi(),
        pages: [
          fakePage({ id: 'page', name: 'Page' }),
          fakePage({ id: 'page2', name: 'Page 2' }),
          fakePage({ id: 'page3', name: 'Page 3' }),
        ],
        onOpenFile: (id: string) => action('File selected')(id),
      },
      styles: [
        `
       `,
      ],
    };
  },
};
