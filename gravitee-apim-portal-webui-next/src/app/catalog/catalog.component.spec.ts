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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatCardHarness } from '@angular/material/card/testing';

import { CatalogComponent } from './catalog.component';
import { ApiCardHarness } from '../../components/api-card/api-card.harness';
import { fakeApi, fakeApisResponse } from '../../entities/api/api.fixtures';
import { ApisResponse } from '../../entities/api/apis-response';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('CatalogComponent', () => {
  let fixture: ComponentFixture<CatalogComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CatalogComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  });

  it('should render banner text', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Welcome to Gravitee Developer Portal!');
  });

  it('should show empty API list', async () => {
    expectApiList(fakeApisResponse({ data: [] }));
    const noApiCard = await harnessLoader.getHarness(MatCardHarness.with({ selector: '#no-apis' }));
    expect(noApiCard).toBeTruthy();
    expect(await noApiCard.getText()).toContain('Sorry, there are no APIs listed yet.');
  });

  it('should show API list', async () => {
    expectApiList(
      fakeApisResponse({
        data: [
          fakeApi({
            id: '1',
            name: 'Test title',
            version: 'v.1.2',
            description:
              'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
          }),
        ],
      }),
    );
    const apiCard = await harnessLoader.getHarness(ApiCardHarness);
    expect(apiCard).toBeDefined();
    expect(await apiCard.getTitle()).toEqual('Test title');
    expect(await apiCard.getDescription()).toEqual(
      'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
    );
    expect(await apiCard.getVersion()).toEqual('v.1.2');
  });

  function expectApiList(apisResponse: ApisResponse = fakeApisResponse()) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis?page=1&size=9`).flush(apisResponse);
  }
});
