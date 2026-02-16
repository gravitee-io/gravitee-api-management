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
import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { combineLatest } from 'rxjs';
import { filter } from 'rxjs/operators';

import { ApiAnalyticsWidgetService } from './api-analytics-widget.service';
import { ApiAnalyticsDashboardWidgetConfig } from './api-analytics-proxy/api-analytics-proxy.component';

import { fakeGroupByResponse } from '../../../../entities/management-api-v2/analytics/analyticsGroupBy.fixture';
import { fakeAnalyticsHistogram } from '../../../../entities/management-api-v2/analytics/analyticsHistogram.fixture';
import { GroupByResponse } from '../../../../entities/management-api-v2/analytics/analyticsGroupBy';
import {
  AggregationFields,
  AggregationTypes,
  HistogramAnalyticsResponse,
} from '../../../../entities/management-api-v2/analytics/analyticsHistogram';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { AnalyticsStatsResponse } from '../../../../entities/management-api-v2/analytics/analyticsStats';
import { fakeAnalyticsStatsResponse } from '../../../../entities/management-api-v2/analytics/analyticsStats.fixture';

describe('ApiAnalyticsWidgetService', () => {
  let service: ApiAnalyticsWidgetService;
  let httpTestingController: HttpTestingController;

  const API_ID = 'api-123';
  const TIME_RANGE_PARAMS = { from: 1000, to: 2000, interval: 10 };

  const baseUrlParams = {
    timeRangeParams: null,
    plans: null,
    httpStatuses: null,
    applications: null,
  };

  function expectGroupByRequest(
    field: string,
    response: GroupByResponse,
    options: {
      ranges?: string;
      order?: string;
    } = {},
  ): void {
    const { ranges, order } = options;
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics?type=GROUP_BY&from=${TIME_RANGE_PARAMS.from}&to=${TIME_RANGE_PARAMS.to}&interval=${TIME_RANGE_PARAMS.interval}&field=${field}${ranges ? `&ranges=${ranges}` : ''}${order ? `&order=${order}` : ''}`,
      method: 'GET',
    });
    req.flush(response);
  }

  function expectStatsRequest(field: string, response: AnalyticsStatsResponse): void {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics?type=STATS&from=${TIME_RANGE_PARAMS.from}&to=${TIME_RANGE_PARAMS.to}&interval=${TIME_RANGE_PARAMS.interval}&field=${field}`,
      method: 'GET',
    });
    req.flush(response);
  }

  function expectHistogramRequest(aggregations: string, response: HistogramAnalyticsResponse): void {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${API_ID}/analytics?type=HISTOGRAM&from=${TIME_RANGE_PARAMS.from}&to=${TIME_RANGE_PARAMS.to}&interval=${TIME_RANGE_PARAMS.interval}&aggregations=${aggregations}`,
      method: 'GET',
    });
    req.flush(response);
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);

    service = TestBed.inject<ApiAnalyticsWidgetService>(ApiAnalyticsWidgetService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('getApiAnalyticsWidgetConfig$', () => {
    describe('when no time range is selected', () => {
      beforeEach(() => {
        service.setUrlParamsData({ ...baseUrlParams, timeRangeParams: null });
      });

      it('should return error config', done => {
        const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
          type: 'pie',
          apiId: API_ID,
          title: 'Test Widget',
          tooltip: 'Test tooltip',
          analyticsType: 'GROUP_BY',
          groupByField: 'status',
        };

        service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
          // Skip loading state and wait for the actual result
          if (result.state === 'loading') {
            return;
          }

          expect(result.state).toBe('error');
          expect(result.errors).toContain('No time range selected');
          expect(result.title).toBe('Test Widget');
          expect(result.tooltip).toBe('Test tooltip');
          done();
        });
      });
    });

    describe('STATS', () => {
      describe('stats widget', () => {
        beforeEach(() => {
          service.setUrlParamsData({ ...baseUrlParams, timeRangeParams: { from: 1000, to: 2000, interval: 10 } });
        });

        it('should transform STATS response to stats chart config', done => {
          const fakeWidgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'stats',
            apiId: API_ID,
            title: 'Total Requests',
            statsKey: 'count',
            statsUnit: 'ms',
            tooltip: '',
            shouldSortBuckets: false,
            statsField: 'gateway-response-time-ms',
            analyticsType: 'STATS',
          };

          const mockedStatsResponse: AnalyticsStatsResponse = fakeAnalyticsStatsResponse();

          service.getApiAnalyticsWidgetConfig$(fakeWidgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            expect(result.state).toBe('success');
            expect(result.title).toBe('Total Requests');
            expect(result.tooltip).toBe('');
            expect(result.widgetType).toBe('stats');
            expect(result.widgetData).toEqual({ stats: 100, statsUnit: 'ms' });

            service.clearStatsCache();
            done();
          });
          expectStatsRequest('gateway-response-time-ms', mockedStatsResponse);
        });

        it('should call only once for multiple STATS widgets', done => {
          const fakeWidgetConfigs: ApiAnalyticsDashboardWidgetConfig[] = [
            {
              type: 'stats',
              apiId: API_ID,
              title: 'Total Requests',
              statsKey: 'count',
              statsUnit: 'ms',
              tooltip: '',
              shouldSortBuckets: false,
              statsField: 'gateway-response-time-ms',
              analyticsType: 'STATS',
            },
            {
              type: 'stats',
              apiId: API_ID,
              title: 'Test',
              statsKey: 'count',
              tooltip: '',
              shouldSortBuckets: false,
              statsField: 'gateway-response-time-ms',
              analyticsType: 'STATS',
            },
          ];

          const mockedStatsResponse: AnalyticsStatsResponse = fakeAnalyticsStatsResponse();

          const obs1$ = service.getApiAnalyticsWidgetConfig$(fakeWidgetConfigs[0]);
          const obs2$ = service.getApiAnalyticsWidgetConfig$(fakeWidgetConfigs[1]);

          combineLatest([
            obs1$.pipe(filter(result => result.state !== 'loading')),
            obs2$.pipe(filter(result => result.state !== 'loading')),
          ]).subscribe(([result1, result2]) => {
            // Assertions for the first result
            expect(result1.state).toBe('success');
            expect(result1.widgetData).toEqual({ stats: 100, statsUnit: 'ms' });

            // Assertions for the second result
            expect(result2.state).toBe('success');
            expect(result2.widgetData).toEqual({ stats: 100, statsUnit: undefined });

            done();
          });

          // Expect and flush ONE single request, proving the caching works.
          // The httpTestingController will see two subscriptions to the sharedReplay observable,
          // but only one of those will result in an actual HTTP call to the mocked backend.
          expectStatsRequest('gateway-response-time-ms', mockedStatsResponse);
        });
      });
    });

    describe('GROUP_BY analytics', () => {
      beforeEach(() => {
        service.setUrlParamsData({ ...baseUrlParams, timeRangeParams: { from: 1000, to: 2000, interval: 10 } });
      });

      describe('pie chart widget', () => {
        it('should transform GROUP_BY response to pie chart config', done => {
          const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'pie',
            apiId: API_ID,
            title: 'HTTP Status Repartition',
            tooltip: 'Distribution of HTTP status codes',
            analyticsType: 'GROUP_BY',
            groupByField: 'status',
            ranges: [
              { label: '100-199', value: '100:199', color: '#2B72FB' },
              { label: '200-299', value: '200:299', color: '#64BDC6' },
              { label: '300-399', value: '300:399', color: '#EECA34' },
              { label: '400-499', value: '400:499', color: '#FA4B42' },
              { label: '500-599', value: '500:599', color: '#FE6A35' },
            ],
          };

          const mockGroupByResponse: GroupByResponse = fakeGroupByResponse({
            values: {
              '100-199': 111,
              '400-499': 500,
              '200-299': 33333,
              '500-599': 0, // This should be filtered out
              '300-399': 0, // This should be filtered out
            },
            metadata: {
              '100-199': { name: '100-199', order: 0 },
              '400-499': { name: '400-499', order: 3 },
              '200-299': { name: '200-299', order: 1 },
              '500-599': { name: '500-599', order: 4 }, // This will not be used since value is 0
              '300-399': { name: '300-399', order: 2 }, // This will not be used since value is 0
            },
          });

          service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            expect(result.state).toBe('success');
            expect(result.title).toBe('HTTP Status Repartition');
            expect(result.tooltip).toBe('Distribution of HTTP status codes');
            expect(result.widgetType).toBe('pie');
            if (result.widgetType === 'pie') {
              expect(result.widgetData).toHaveLength(3);
              // Data is sorted alphabetically by label
              expect(result.widgetData[0]).toEqual({
                label: '100-199',
                value: 111,
                color: '#2B72FB',
              });
              expect(result.widgetData[1]).toEqual({
                label: '200-299',
                value: 33333,
                color: '#64BDC6',
              });
              expect(result.widgetData[2]).toEqual({
                label: '400-499',
                value: 500,
                color: '#FA4B42',
              });
            }
            done();
          });

          expectGroupByRequest('status', mockGroupByResponse, { ranges: '100:199;200:299;300:399;400:499;500:599' });
        });

        it('should handle status ranges with predefined labels', done => {
          const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'pie',
            apiId: API_ID,
            title: 'Status Ranges',
            tooltip: 'Status ranges',
            analyticsType: 'GROUP_BY',
            groupByField: 'status',
            ranges: [
              { label: '100-199', value: '1', color: '#2B72FB' },
              { label: '200-299', value: '2', color: '#64BDC6' },
              { label: '300-399', value: '3', color: '#EECA34' },
            ],
          };

          const mockGroupByResponse: GroupByResponse = fakeGroupByResponse({
            values: {
              '1': 100,
              '2': 200,
              '3': 300,
            },
          });

          service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            expect(result.state).toBe('success');
            expect(result.widgetType).toBe('pie');
            if (result.widgetType === 'pie') {
              expect(result.widgetData).toHaveLength(3);
              expect(result.widgetData[0]).toEqual({
                label: '100-199',
                value: 100,
                color: '#2B72FB',
              });
              expect(result.widgetData[1]).toEqual({
                label: '200-299',
                value: 200,
                color: '#64BDC6',
              });
              expect(result.widgetData[2]).toEqual({
                label: '300-399',
                value: 300,
                color: '#EECA34',
              });
            }
            done();
          });

          expectGroupByRequest('status', mockGroupByResponse, { ranges: '1;2;3' });
        });

        it('should fallback to metadata or original labels when ranges are not provided', done => {
          const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'pie',
            apiId: API_ID,
            title: 'Status Ranges',
            tooltip: 'Status ranges',
            analyticsType: 'GROUP_BY',
            groupByField: 'status',
          };

          const mockGroupByResponse: GroupByResponse = fakeGroupByResponse({
            values: {
              '100-199': 100,
              '200-299': 200,
              '400-499': 400,
            },
            metadata: {
              '100-199': { name: 'Informational', order: 0 },
              '200-299': { name: 'Success', order: 1 },
              '400-499': { name: 'Client Error', order: 2 },
            },
          });

          service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            expect(result.state).toBe('success');
            expect(result.widgetType).toBe('pie');
            if (result.widgetType === 'pie') {
              expect(result.widgetData).toHaveLength(3);
              // Data is sorted alphabetically by label
              expect(result.widgetData[0]).toEqual({
                label: 'Client Error',
                value: 400,
                color: '#EECA34', // index 2 % 5 = 2, colors[2] = '#EECA34'
              });
              expect(result.widgetData[1]).toEqual({
                label: 'Informational',
                value: 100,
                color: '#2B72FB', // index 0 % 5 = 0, colors[0] = '#2B72FB'
              });
              expect(result.widgetData[2]).toEqual({
                label: 'Success',
                value: 200,
                color: '#64BDC6', // index 1 % 5 = 1, colors[1] = '#64BDC6'
              });
            }
            done();
          });

          expectGroupByRequest('status', mockGroupByResponse);
        });

        it('should filter out zero values', done => {
          const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'pie',
            apiId: API_ID,
            title: 'Test Widget',
            tooltip: 'Test tooltip',
            analyticsType: 'GROUP_BY',
            groupByField: 'status',
          };

          const mockGroupByResponse: GroupByResponse = fakeGroupByResponse({
            values: {
              '100-199': 111,
              '200-299': 0,
              '400-499': 500,
            },
          });

          service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            if (result.widgetType === 'pie') {
              expect(result.widgetData).toHaveLength(2);
              expect(result.widgetData.find(d => d.value === 0)).toBeUndefined();
            }
            done();
          });

          expectGroupByRequest('status', mockGroupByResponse);
        });

        it('should fallback to original label when range value does not match', done => {
          const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'pie',
            apiId: API_ID,
            title: 'Status Ranges',
            tooltip: 'Status ranges',
            analyticsType: 'GROUP_BY',
            groupByField: 'status',
            ranges: [
              { label: '200-299', value: '200:299', color: '#4CAF50' },
              { label: '400-499', value: '400:499', color: '#FF9800' },
            ],
          };

          const mockGroupByResponse: GroupByResponse = fakeGroupByResponse({
            values: {
              '200-299': 200,
              '400-499': 400,
            },
          });

          service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            expect(result.state).toBe('success');
            expect(result.widgetType).toBe('pie');
            expect(result.widgetData).toHaveLength(2);
            // Data is sorted alphabetically by label
            expect(result.widgetData[0]).toEqual({
              label: '200-299',
              value: 200,
              color: '#4CAF50', // Custom color from ranges
            });
            expect(result.widgetData[1]).toEqual({
              label: '400-499',
              value: 400,
              color: '#FF9800', // Custom color from ranges
            });

            done();
          });

          expectGroupByRequest('status', mockGroupByResponse, { ranges: '200:299;400:499' });
        });
      });

      describe('table widget', () => {
        it('should transform GROUP_BY response to table config', done => {
          const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'table',
            apiId: API_ID,
            title: 'Top Applications',
            tooltip: 'Applications ranked by total API calls',
            analyticsType: 'GROUP_BY',
            groupByField: 'application-id',
            shouldSortBuckets: false,
            orderBy: '-count:_count',
            tableData: {
              columns: [
                { label: 'Host', dataType: 'string' },
                { label: 'count', dataType: 'number' },
              ],
            },
          };

          const mockGroupByResponse: GroupByResponse = fakeGroupByResponse({
            values: {
              'app-1': 100,
              'app-2': 200,
              'app-3': 50,
            },
            metadata: {
              'app-1': { name: 'Application 1', order: 1 },
              'app-2': { name: 'Application 2', order: 0 },
              'app-3': { name: 'Application 3', order: 2 },
            },
          });

          service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            expect(result.state).toBe('success');
            expect(result.title).toBe('Top Applications');
            expect(result.tooltip).toBe('Applications ranked by total API calls');
            expect(result.widgetType).toBe('table');
            if (result.widgetType === 'table') {
              expect(result.widgetData.columns).toHaveLength(2);
              expect(result.widgetData.columns[0]).toEqual({
                name: 'col-0',
                label: 'Host',
                dataType: 'string',
              });
              expect(result.widgetData.columns[1]).toEqual({
                name: 'col-1',
                label: 'count',
                dataType: 'number',
              });
              expect(result.widgetData.data).toHaveLength(3);
              expect(result.widgetData.data[0]).toEqual({
                'col-0': 'Application 2',
                'col-1': 200,
                __key: 'app-2',
                unknown: false,
              });
              expect(result.widgetData.data[1]).toEqual({
                'col-0': 'Application 1',
                'col-1': 100,
                __key: 'app-1',
                unknown: false,
              });
              expect(result.widgetData.data[2]).toEqual({
                'col-0': 'Application 3',
                'col-1': 50,
                __key: 'app-3',
                unknown: false,
              });
            }
            done();
          });

          expectGroupByRequest('application-id', mockGroupByResponse, { order: '-count:_count' });
        });

        it('should not sort when shouldSortBuckets is false', done => {
          const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'table',
            apiId: API_ID,
            title: 'Test Widget',
            tooltip: 'Test tooltip',
            analyticsType: 'GROUP_BY',
            groupByField: 'application-id',
            shouldSortBuckets: false,
            tableData: {
              columns: [
                { label: 'app-id', dataType: 'string' },
                { label: 'count', dataType: 'number' },
              ],
            },
          };

          const mockGroupByResponse: GroupByResponse = fakeGroupByResponse({
            values: {
              'app-1': 100,
              'app-2': 200,
              'app-3': 50,
            },
          });

          service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            if (result.widgetType === 'table') {
              expect(result.widgetData.data[0]['col-1']).toBe(100);
              expect(result.widgetData.data[1]['col-1']).toBe(200);
              expect(result.widgetData.data[2]['col-1']).toBe(50);
            }
            done();
          });

          expectGroupByRequest('application-id', mockGroupByResponse);
        });

        it('should use default column names when no columns are provided for table widget', done => {
          const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'table',
            apiId: API_ID,
            title: 'Test Widget',
            tooltip: 'Test tooltip',
            analyticsType: 'GROUP_BY',
            groupByField: 'application-id',
            shouldSortBuckets: false,
            // No tableData.columns provided
          };

          const mockGroupByResponse: GroupByResponse = fakeGroupByResponse({
            values: {
              'app-1': 100,
              'app-2': 200,
              'app-3': 50,
            },
            metadata: {
              'app-1': { name: 'Application 1', order: 1 },
              'app-2': { name: 'Application 2', order: 0 },
              'app-3': { name: 'Application 3', order: 2 },
            },
          });

          service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            expect(result.state).toBe('success');
            expect(result.widgetType).toBe('table');
            if (result.widgetType === 'table') {
              // Should have default columns
              expect(result.widgetData.columns).toHaveLength(2);
              expect(result.widgetData.columns[0]).toEqual({
                name: 'col-0',
                label: 'Name',
                dataType: 'string',
              });
              expect(result.widgetData.columns[1]).toEqual({
                name: 'col-1',
                label: 'Value',
                dataType: 'number',
              });

              // Data should be properly formatted with default column names
              expect(result.widgetData.data).toHaveLength(3);
            }
            done();
          });

          expectGroupByRequest('application-id', mockGroupByResponse);
        });
      });
    });

    describe('HISTOGRAM analytics', () => {
      beforeEach(() => {
        service.setUrlParamsData({ ...baseUrlParams, timeRangeParams: { from: 1000, to: 2000, interval: 10 } });
      });

      describe('line chart widget', () => {
        it('should transform HISTOGRAM response to line chart config', done => {
          const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'line',
            apiId: API_ID,
            title: 'Response Time Over Time',
            tooltip: 'Measures response time for gateway and endpoint',
            analyticsType: 'HISTOGRAM',
            aggregations: [
              {
                type: AggregationTypes.AVG,
                field: AggregationFields.GATEWAY_RESPONSE_TIME_MS,
                label: 'Gateway Response Time',
              },
              {
                type: AggregationTypes.AVG,
                field: AggregationFields.ENDPOINT_RESPONSE_TIME_MS,
                label: 'Endpoint Response Time',
              },
            ],
          };

          const mockHistogramResponse: HistogramAnalyticsResponse = fakeAnalyticsHistogram({
            timestamp: {
              from: 1000,
              to: 2000,
              interval: 100,
            },
            values: [
              {
                name: 'avg_gateway-response-time-ms',
                field: 'gateway-response-time-ms',
                buckets: [
                  {
                    name: 'Gateway Response Time',
                    data: [10, 20, 30, 40, 50],
                  },
                ],
              },
              {
                name: 'avg_endpoint-response-time-ms',
                field: 'endpoint-response-time-ms',
                buckets: [
                  {
                    name: 'Endpoint Response Time',
                    data: [15, 25, 35, 45, 55],
                  },
                ],
              },
            ],
          });

          service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            expect(result.state).toBe('success');
            expect(result.title).toBe('Response Time Over Time');
            expect(result.tooltip).toBe('Measures response time for gateway and endpoint');
            expect(result.widgetType).toBe('line');
            if (result.widgetType === 'line') {
              expect(result.widgetData.data).toHaveLength(2);
              expect(result.widgetData.data[0]).toEqual({
                name: 'Gateway Response Time',
                values: [10, 20, 30, 40, 50],
              });
              expect(result.widgetData.data[1]).toEqual({
                name: 'Endpoint Response Time',
                values: [15, 25, 35, 45, 55],
              });
              expect(result.widgetData.options).toEqual({
                pointStart: 1000,
                pointInterval: 100,
                enableMarkers: false,
              });
            }
            done();
          });

          expectHistogramRequest('AVG:gateway-response-time-ms,AVG:endpoint-response-time-ms', mockHistogramResponse);
        });

        it('should transform HISTOGRAM response with single aggregation using bucket names', done => {
          const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'line',
            apiId: API_ID,
            title: 'Status Over Time',
            tooltip: 'Status codes over time',
            analyticsType: 'HISTOGRAM',
            aggregations: [
              {
                type: AggregationTypes.FIELD,
                field: AggregationFields.STATUS,
              },
            ],
          };

          const mockHistogramResponse: HistogramAnalyticsResponse = fakeAnalyticsHistogram({
            timestamp: {
              from: 1000,
              to: 2000,
              interval: 100,
            },
            values: [
              {
                name: 'status',
                field: 'status',
                buckets: [
                  {
                    name: '200',
                    data: [10, 20, 30, 40, 50],
                  },
                  {
                    name: '400',
                    data: [5, 15, 25, 35, 45],
                  },
                  {
                    name: '500',
                    data: [1, 2, 3, 4, 5],
                  },
                ],
              },
            ],
          });

          service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            expect(result.state).toBe('success');
            expect(result.title).toBe('Status Over Time');
            expect(result.tooltip).toBe('Status codes over time');
            expect(result.widgetType).toBe('line');
            if (result.widgetType === 'line') {
              expect(result.widgetData.data).toHaveLength(3);
              expect(result.widgetData.data[0]).toEqual({
                name: '200',
                values: [10, 20, 30, 40, 50],
              });
              expect(result.widgetData.data[1]).toEqual({
                name: '400',
                values: [5, 15, 25, 35, 45],
              });
              expect(result.widgetData.data[2]).toEqual({
                name: '500',
                values: [1, 2, 3, 4, 5],
              });
              expect(result.widgetData.options).toEqual({
                pointStart: 1000,
                pointInterval: 100,
                enableMarkers: false,
              });
            }
            done();
          });

          expectHistogramRequest('FIELD:status', mockHistogramResponse);
        });

        it('should handle empty aggregations', done => {
          const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
            type: 'line',
            apiId: API_ID,
            title: 'Test Widget',
            tooltip: 'Test tooltip',
            analyticsType: 'HISTOGRAM',
            aggregations: [],
          };

          service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
            // Skip loading state and wait for the actual result
            if (result.state === 'loading') {
              return;
            }

            expect(result.state).toBe('error');
            expect(result.errors).toContain('No aggregations specified for histogram');
            done();
          });
        });
      });

      it('should transform HISTOGRAM response with bucket metadata to line chart config', done => {
        const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
          type: 'line',
          apiId: API_ID,
          title: 'Applications Requests Over Time',
          tooltip: 'Application requests over time',
          analyticsType: 'HISTOGRAM',
          aggregations: [
            {
              type: AggregationTypes.FIELD,
              field: AggregationFields.APPLICATION_ID,
            },
          ],
        };

        const mockHistogramResponse: HistogramAnalyticsResponse = fakeAnalyticsHistogram({
          timestamp: {
            from: 1000,
            to: 2000,
            interval: 100,
          },
          values: [
            {
              field: 'by_application-id',
              name: 'application-id',
              buckets: [
                {
                  name: 'app-1',
                  data: [10, 20, 30, 40, 50],
                },
                {
                  name: 'app-2',
                  data: [15, 25, 35, 45, 55],
                },
              ],
              metadata: {
                'app-1': { name: 'Application 1' },
                'app-2': { name: 'Application 2' },
              },
            },
          ],
        });

        service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
          // Skip loading state and wait for the actual result
          if (result.state === 'loading') {
            return;
          }

          expect(result.state).toBe('success');
          expect(result.title).toBe('Applications Requests Over Time');
          expect(result.tooltip).toBe('Application requests over time');
          expect(result.widgetType).toBe('line');
          if (result.widgetType === 'line') {
            expect(result.widgetData.data).toHaveLength(2);
            expect(result.widgetData.data[0]).toEqual({
              name: 'Application 1',
              values: [10, 20, 30, 40, 50],
            });
            expect(result.widgetData.data[1]).toEqual({
              name: 'Application 2',
              values: [15, 25, 35, 45, 55],
            });
            expect(result.widgetData.options).toEqual({
              pointStart: 1000,
              pointInterval: 100,
              enableMarkers: false,
            });
          }
          done();
        });

        expectHistogramRequest('FIELD:application-id', mockHistogramResponse);
      });

      it('should transform authentication histogram data into combined bar chart format', done => {
        const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
          type: 'bar',
          apiId: API_ID,
          title: 'Authentication Success vs Failure',
          tooltip: 'Authentication success and failure over time',
          analyticsType: 'HISTOGRAM',
          aggregations: [
            {
              type: AggregationTypes.TREND,
              field: AggregationFields.DOWNSTREAM_AUTHENTICATION_SUCCESSES_COUNT_INCREMENT,
              label: 'Downstream Success',
            },
            {
              type: AggregationTypes.TREND,
              field: AggregationFields.DOWNSTREAM_AUTHENTICATION_FAILURES_COUNT_INCREMENT,
              label: 'Downstream Failure',
            },
            {
              type: AggregationTypes.TREND,
              field: AggregationFields.UPSTREAM_AUTHENTICATION_SUCCESSES_COUNT_INCREMENT,
              label: 'Upstream Success',
            },
            {
              type: AggregationTypes.TREND,
              field: AggregationFields.UPSTREAM_AUTHENTICATION_FAILURES_COUNT_INCREMENT,
              label: 'Upstream Failure',
            },
          ],
        };

        const mockHistogramResponse: HistogramAnalyticsResponse = {
          analyticsType: 'HISTOGRAM',
          timestamp: { from: 1000, to: 2000, interval: 100 },
          values: [
            {
              field: 'downstream-authentication-failures-count-increment',
              name: 'downstream-authentication-failures-count-increment',
              buckets: [{ name: 'all', data: [2, 4, 3] }],
            },
            {
              field: 'upstream-authentication-failures-count-increment',
              name: 'upstream-authentication-failures-count-increment',
              buckets: [{ name: 'all', data: [1, 2, 1] }],
            },
            {
              field: 'upstream-authentication-successes-count-increment',
              name: 'upstream-authentication-successes-count-increment',
              buckets: [{ name: 'all', data: [5, 8, 6] }],
            },
            {
              field: 'downstream-authentication-successes-count-increment',
              name: 'downstream-authentication-successes-count-increment',
              buckets: [{ name: 'all', data: [10, 20, 15] }],
            },
          ],
        };

        service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
          if (result.state === 'loading') return;
          if (result.widgetType === 'bar') {
            expect(result.state).toBe('success');
            expect(result.widgetType).toBe('bar');
            expect(result.title).toBe('Authentication Success vs Failure');

            const barData = result.widgetData.data;
            expect(barData).toHaveLength(2); // Success and Failure

            // Verify combined data: Success = Downstream + Upstream
            expect(barData[0].name).toBe('Success');
            expect(barData[0].values).toEqual([15, 28, 21]);
            expect(barData[0].color).toBeDefined();

            // Verify combined data: Failure = Downstream + Upstream
            expect(barData[1].name).toBe('Failure');
            expect(barData[1].values).toEqual([3, 6, 4]);
            expect(barData[1].color).toBeDefined();

            // Verify chart options
            const options = result.widgetData.options;
            expect(options.stacked).toBe(true);
            expect(options.reverseStack).toBe(true);
            expect(options.categories).toHaveLength(3);
          }

          done();
        });

        expectHistogramRequest(
          'TREND:downstream-authentication-successes-count-increment,TREND:downstream-authentication-failures-count-increment,TREND:upstream-authentication-successes-count-increment,TREND:upstream-authentication-failures-count-increment',
          mockHistogramResponse,
        );
      });
    });

    describe('error handling', () => {
      beforeEach(() => {
        service.setUrlParamsData({ ...baseUrlParams, timeRangeParams: { from: 1000, to: 2000, interval: 10 } });
      });

      it('should handle unsupported analytics type', done => {
        const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
          type: 'pie',
          apiId: API_ID,
          title: 'Test Widget',
          tooltip: 'Test tooltip',
          analyticsType: 'UNSUPPORTED' as any,
        };

        service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
          // Skip loading state and wait for the actual result
          if (result.state === 'loading') {
            return;
          }

          expect(result.state).toBe('error');
          expect(result.errors).toContain('Unsupported analytics type');
          done();
        });
      });

      it('should handle unsupported widget type for GROUP_BY', done => {
        const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
          type: 'line',
          apiId: API_ID,
          title: 'Test Widget',
          tooltip: 'Test tooltip',
          analyticsType: 'GROUP_BY',
          groupByField: 'status',
        };

        const mockGroupByResponse: GroupByResponse = fakeGroupByResponse();

        service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
          // Skip loading state and wait for the actual result
          if (result.state === 'loading') {
            return;
          }

          expect(result.state).toBe('error');
          expect(result.errors).toContain('Unsupported widget type for GROUP_BY analytics');
          done();
        });

        expectGroupByRequest('status', mockGroupByResponse);
      });

      it('should handle unsupported widget type for HISTOGRAM', done => {
        const widgetConfig: ApiAnalyticsDashboardWidgetConfig = {
          type: 'pie',
          apiId: API_ID,
          title: 'Test Widget',
          tooltip: 'Test tooltip',
          analyticsType: 'HISTOGRAM',
          aggregations: [
            {
              type: AggregationTypes.AVG,
              field: AggregationFields.GATEWAY_RESPONSE_TIME_MS,
            },
          ],
        };

        const mockHistogramResponse: HistogramAnalyticsResponse = fakeAnalyticsHistogram();

        service.getApiAnalyticsWidgetConfig$(widgetConfig).subscribe(result => {
          // Skip loading state and wait for the actual result
          if (result.state === 'loading') {
            return;
          }

          expect(result.state).toBe('error');
          expect(result.errors).toContain('Unsupported widget type for HISTOGRAM analytics');
          done();
        });

        expectHistogramRequest('AVG:gateway-response-time-ms', mockHistogramResponse);
      });
    });

    describe('queryOf', () => {
      it('should return null when all params are empty or undefined', () => {
        expect(
          service['queryOf']({
            httpStatuses: [],
            plans: [],
            applications: [],
            timeRangeParams: undefined,
          }),
        ).toBe(null);
        expect(
          service['queryOf']({
            httpStatuses: [],
            plans: [],
            applications: [],
            timeRangeParams: undefined,
          }),
        ).toBe(null);
      });

      it('should build query for httpStatuses', () => {
        expect(
          service['queryOf']({
            httpStatuses: ['200', '404'],
            plans: [],
            applications: [],
            timeRangeParams: undefined,
          }),
        ).toBe('status:("200" OR "404")');
      });

      it('should build query for plans', () => {
        expect(
          service['queryOf']({
            httpStatuses: [],
            plans: ['planA', 'planB'],
            applications: [],
            timeRangeParams: undefined,
          }),
        ).toBe('plan-id:("planA" OR "planB")');
      });

      it('should build query for applications', () => {
        expect(
          service['queryOf']({
            httpStatuses: [],
            plans: [],
            applications: ['app1', 'app2'],
            timeRangeParams: undefined,
          }),
        ).toBe('application-id:("app1" OR "app2")');
      });

      it('should build query for multiple filters', () => {
        expect(
          service['queryOf']({
            httpStatuses: ['200'],
            plans: ['planA'],
            applications: ['app1'],
            timeRangeParams: undefined,
          }),
        ).toBe('status:("200") AND plan-id:("planA") AND application-id:("app1")');
      });

      it('should ignore empty arrays', () => {
        expect(
          service['queryOf']({
            httpStatuses: [],
            plans: ['plan1'],
            applications: [],
            timeRangeParams: undefined,
          }),
        ).toBe('plan-id:("plan1")');
      });
    });
  });
});
