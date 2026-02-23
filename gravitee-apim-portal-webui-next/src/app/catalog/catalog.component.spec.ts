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

import { CatalogComponent } from './catalog.component';
import { ApiCardHarness } from '../../components/api-card/api-card.harness';
import { PaginationHarness } from '../../components/pagination/pagination.harness';
import { fakeApi, fakeApisResponse } from '../../entities/api/api.fixtures';
import { ApisResponse } from '../../entities/api/apis-response';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('CatalogComponent', () => {
  let fixture: ComponentFixture<CatalogComponent>;
  let harnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const initBase = async () => {
    await TestBed.configureTestingModule({
      imports: [CatalogComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogComponent);

    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    fixture.detectChanges();
  };

  const init = async (
    params: Partial<{
      apisResponse: ApisResponse;
      page: number;
      size: number;
    }> = {
      apisResponse: fakeApisResponse(),
      page: 1,
      size: 20,
    },
  ) => {
    await initBase();
    expectApiList(params.apisResponse, params.page, params.size);
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('populated api list', () => {
    beforeEach(async () => {
      await init({
        apisResponse: fakeApisResponse({
          data: [
            fakeApi({
              id: '1',
              name: 'Test title',
              version: 'v.1.2',
              description:
                'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
            }),
            fakeApi({
              id: '2',
              name: 'MCP Server API',
              version: 'v.2.0',
              description:
                'Access enterprise-level financial data, reports, and analytics to empower your applications with financial insights.',
              mcp: {
                mcpPath: '/mcp',
                tools: [{ toolDefinition: { name: 'MCP Tool', description: 'MCP Tool Description', inputSchema: {} } }],
              },
            }),
          ],
          metadata: {
            pagination: {
              current_page: 1,
              total_pages: 2,
            },
          },
        }),
      });
    });

    it('should show API list', async () => {
      const apiCard = await harnessLoader.getHarness(ApiCardHarness);
      expect(apiCard).toBeDefined();
      expect(await apiCard.getTitle()).toEqual('Test title');
      expect(await apiCard.getDescription()).toEqual(
        'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
      );
      expect(await apiCard.getVersion()).toEqual('v.1.2');
    });

        it('should call API list with search query', async () => {
      const apiCard = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(apiCard).toBeDefined();
      expect(apiCard.length).toEqual(2);
      expect(await apiCard[0].getTitle()).toEqual('Test title');

      document.getElementsByClassName('api-list__container')[0].dispatchEvent(new Event('scrolled'));
      fixture.detectChanges();

      expectApiList(
        fakeApisResponse({
          data: [fakeApi({ id: 'second-page-api', name: 'second page api', version: '24' })],
          metadata: {
            pagination: {
              current_page: 3,
              total_pages: 5,
            },
          },
        }),
        3,
        9,
      );
      fixture.detectChanges();
    });

    it('should call second page when pagination changes', async () => {
      const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(apiCards.length).toEqual(2);
      expect(await apiCards[0].getTitle()).toEqual('Test title');

      const pagination = await harnessLoader.getHarness(PaginationHarness);
      const nextButton = await pagination.getNextPageButton();
      await nextButton.click();
      fixture.detectChanges();

      expectApiList(
        fakeApisResponse({
          data: [fakeApi({ id: 'second-page-api', name: 'second page api', version: '24' })],
          metadata: {
            pagination: {
              current_page: 2,
              total_pages: 2,
            },
          },
        }),
        2,
        20,
      );
      fixture.detectChanges();

      const allHarnesses = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(allHarnesses.length).toEqual(1);
      expect(await allHarnesses[0].getTitle()).toEqual('second page api');
    });

    it('should show MCP server chip', async () => {
      const apiCards = await harnessLoader.getAllHarnesses(ApiCardHarness);
      expect(await apiCards[1].isMcpServer()).toBeTruthy();
    });
  });

  describe('empty component', () => {
    describe('when no results', () => {
      beforeEach(async () => {
        await init({ apisResponse: fakeApisResponse({ data: [] }) });
      });

      it('should show empty API list', () => {
        const emptyState = fixture.nativeElement.querySelector('.api-list__empty-state');
        expect(emptyState).toBeTruthy();
        expect(emptyState.textContent).toContain('No APIs available yet');
      });
    });

    describe('when error occurs', () => {
      it('should show empty API list', async () => {
        await initBase();
        httpTestingController
          .expectOne(`${TESTING_BASE_URL}/apis/_search?page=1&category=all&size=20&q=`)
          .flush({ error: { message: 'Error occurred' } }, { status: 500, statusText: 'Internal Error' });
        fixture.detectChanges();

        const emptyState = fixture.nativeElement.querySelector('.api-list__empty-state');
        expect(emptyState).toBeTruthy();
        expect(emptyState.textContent).toContain('No APIs available yet');
      });
    });
  });

  function expectApiList(apisResponse: ApisResponse = fakeApisResponse(), page: number = 1, size: number = 20) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/_search?page=${page}&category=all&size=${size}&q=`).flush(apisResponse);
  }
});
