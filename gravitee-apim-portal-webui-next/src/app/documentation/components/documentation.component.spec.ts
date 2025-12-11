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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { provideRouter, Router } from '@angular/router';

import { DocumentationComponent } from './documentation.component';
import { DocumentationComponentHarness } from './documentation.component.harness';
import { PortalNavigationItem } from '../../../entities/portal-navigation/portal-navigation-item';
import { fakePortalNavigationFolder } from '../../../entities/portal-navigation/portal-navigation-item.fixture';
import { NotFoundComponent } from '../../not-found/not-found.component';

describe('DocumentationComponent', () => {
  let fixture: ComponentFixture<DocumentationComponent>;
  let harness: DocumentationComponentHarness;
  let routerSpy: jest.SpyInstance;

  const init = async (navItem: PortalNavigationItem | null) => {
    await TestBed.configureTestingModule({
      imports: [DocumentationComponent, MatIconTestingModule, HttpClientTestingModule],
      providers: [provideRouter([{ path: '404', component: NotFoundComponent }])],
    }).compileComponents();

    routerSpy = jest.spyOn(TestBed.inject(Router), 'navigate');

    fixture = TestBed.createComponent(DocumentationComponent);
    fixture.componentRef.setInput('navItem', navItem);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, DocumentationComponentHarness);
  };

  describe('initial load', () => {
    it('should display folder', async () => {
      await init(fakePortalNavigationFolder());
      const folder = await harness.getFolder();
      expect(folder).not.toBeNull();

      const page = await harness.getPage();
      expect(page).toBeNull();
    });

    it('should redirect to 404', async () => {
      await init(null);

      expect(routerSpy).toHaveBeenCalledWith(['/404']);
    });
  });
});
