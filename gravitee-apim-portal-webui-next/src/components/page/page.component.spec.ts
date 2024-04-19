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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageSwaggerHarness } from './page-swagger/page-swagger.harness';
import { PageComponent } from './page.component';
import { fakePage } from '../../entities/page/page.fixtures';

describe('PageComponent', () => {
  let component: PageComponent;
  let fixture: ComponentFixture<PageComponent>;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PageComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    component = fixture.componentInstance;
  });

  describe('swagger', () => {
    beforeEach(() => {
      component.page = fakePage({ type: 'SWAGGER' });
      fixture.detectChanges();
    });

    it('should show swagger content', async () => {
      const swagger = await harnessLoader.getHarnessOrNull(PageSwaggerHarness);
      expect(swagger).toBeTruthy();
      expect(await swagger?.getSwagger()).toBeTruthy();
    });
  });
});
