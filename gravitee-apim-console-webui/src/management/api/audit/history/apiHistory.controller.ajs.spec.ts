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
import { of } from 'rxjs';

import ApiHistoryControllerAjs from './apiHistory.controller.ajs';

describe('ApiHistoryControllerAjs', () => {
  let $mdDialogMock;
  let $scopeMock;
  let $rootScopeMock;
  let $stateMock;
  let ApiServiceMock;
  let NotificationServiceMock;
  let AuditServiceMock;
  let PolicyServiceMock;
  let ResourceServiceMock;
  let FlowServiceMock;
  let ngApiV2ServiceMock;
  let ngGroupV2ServiceMock;
  let controller;

  enum Modes {
    Diff = 'Diff',
    DiffWithMaster = 'DiffWithMaster',
    Payload = 'Payload',
    Design = 'Design',
  }

  beforeEach(() => {
    $mdDialogMock = jest.fn();
    $scopeMock = { $emit: jest.fn(), $on: jest.fn() };
    $rootScopeMock = { $emit: jest.fn() };
    $stateMock = { current: { name: 'apiHistory' }, params: { apiId: '123' } };
    ApiServiceMock = {
      getFlowSchemaForm: jest.fn(),
      get: jest.fn().mockResolvedValue({ data: { id: '123', name: 'test-api' } }),
      isAPISynchronized: jest.fn().mockResolvedValue({ data: { is_synchronized: true } }),
      searchApiEvents: jest.fn().mockResolvedValue({
        data: {
          content: [
            { type: 'PUBLISH_API', created_at: '2024-09-26', user: 'admin', properties: { deployment_label: 'v1', deployment_number: 1 } },
          ],
          totalElements: 10,
          pageNumber: 0,
          pageElements: 5,
        },
      }),
    };
    NotificationServiceMock = jest.fn();
    AuditServiceMock = jest.fn();
    PolicyServiceMock = { list: jest.fn().mockResolvedValue({ data: [] }) };
    ResourceServiceMock = { list: jest.fn().mockResolvedValue({ data: [] }) };
    FlowServiceMock = { getConfigurationSchema: jest.fn().mockResolvedValue({ data: [] }) };
    ngApiV2ServiceMock = jest.fn();
    ngGroupV2ServiceMock = { list: jest.fn().mockReturnValue(of({ data: [] })) };

    controller = new ApiHistoryControllerAjs(
      $mdDialogMock,
      $scopeMock,
      $rootScopeMock,
      $stateMock,
      ApiServiceMock,
      NotificationServiceMock,
      AuditServiceMock,
      PolicyServiceMock,
      ResourceServiceMock,
      FlowServiceMock,
      ngApiV2ServiceMock,
      ngGroupV2ServiceMock,
    );
  });

  test('should initialize with default values', () => {
    expect(controller.eventsSelected).toEqual([]);
    expect(controller.eventsTimeline).toEqual([]);
    expect(controller.eventsToCompare).toEqual([]);
    expect(controller.eventSelected).toEqual({});
    expect(controller.mode).toBe('Payload');
  });

  test('should trigger $onInit and load API data', async () => {
    await controller.$onInit();
    expect(ApiServiceMock.get).toHaveBeenCalledWith('123');
    expect(ngGroupV2ServiceMock.list).toHaveBeenCalled();

    await Promise.all([
      ngGroupV2ServiceMock.list(1, 99999).toPromise(),
      ApiServiceMock.get('123'),
      ApiServiceMock.isAPISynchronized('123'),
    ]);

    expect(controller.api).toEqual({ id: '123', name: 'test-api' });
  });

  test('should append events in appendNextPage', async () => {
    controller.api = { id: '123', name: 'test-api' };
    controller.eventPage = -1;
    await controller.appendNextPage();
    expect(ApiServiceMock.searchApiEvents).toHaveBeenCalledWith('PUBLISH_API', '123', undefined, undefined, 0, 100, true);
    expect(controller.eventsTimeline.length).toBeGreaterThan(0);
    expect(controller.hasNextEventPageToLoad).toBe(true);
  });

  test('should subscribe to apiChangeSuccess event in constructor', () => {
    expect($scopeMock.$on).toHaveBeenCalledWith('apiChangeSuccess', expect.any(Function));
  });

  test('should add an event to eventsToCompare', () => {
    const eventTimeline = { id: 'event1' };
    controller.selectEventToCompare(eventTimeline);

    expect(controller.eventsToCompare).toContain(eventTimeline);
  });

  test('should allow adding multiple events to eventsToCompare', () => {
    const eventTimeline1 = { id: 'event1' };
    const eventTimeline2 = { id: 'event2' };

    controller.selectEventToCompare(eventTimeline1);
    controller.selectEventToCompare(eventTimeline2);

    expect(controller.eventsToCompare).toContain(eventTimeline1);
    expect(controller.eventsToCompare).toContain(eventTimeline2);
  });

  test('should clear eventsToCompare', () => {
    const eventTimeline1 = { id: 'event1' };
    const eventTimeline2 = { id: 'event2' };

    controller.selectEventToCompare(eventTimeline1);
    controller.selectEventToCompare(eventTimeline2);
    controller.clearDataToCompare();

    expect(controller.eventsToCompare).toEqual([]);
  });

  test('should clear eventsSelected', () => {
    controller.eventsSelected = [{ id: 'event1' }, { id: 'event2' }];
    controller.clearDataSelected();

    expect(controller.eventsSelected).toEqual([]);
  });

  test('should check if an event is selected for comparison', () => {
    const eventTimeline = { id: 'event1' };

    controller.selectEventToCompare(eventTimeline);
    const isSelected = controller.isEventSelectedForComparaison(eventTimeline);

    expect(isSelected).toBe(true);
  });

  test('should return false for an event not selected for comparison', () => {
    const eventTimeline1 = { id: 'event1' };
    const eventTimeline2 = { id: 'event2' };

    controller.selectEventToCompare(eventTimeline1);
    const isSelected = controller.isEventSelectedForComparaison(eventTimeline2);

    expect(isSelected).toBe(false);
  });

  test('should clear data and set to Design mode when detail is false', () => {
    controller.eventToCompareRequired = true;
    controller.mode = Modes.Payload;
    const eventDetail = { detail: false };
    controller.eventsToCompare = [{ id: 'event1' }, { id: 'event2' }];

    controller.toggleMode(eventDetail);

    expect(controller.eventsToCompare).toEqual([]);
    expect(controller.eventToCompareRequired).toBe(false);
    expect(controller.mode).toBe(Modes.Design);
  });

  it('should set mode to Payload when detail is true', () => {
    controller.mode = Modes.Design;
    controller.eventToCompareRequired = true;
    const event = { detail: true };

    const clearDataToCompareSpy = jest.spyOn(controller, 'clearDataToCompare');

    controller.toggleMode(event);

    expect(clearDataToCompareSpy).not.toHaveBeenCalled();
    expect(controller.eventToCompareRequired).toBe(true);
    expect(controller.mode).toBe(Modes.Payload);

    clearDataToCompareSpy.mockRestore();
  });

  test('should unshift toDeployEventTimeline into eventsTimeline when it exists', async () => {
    controller.api = { id: '123', name: 'test-api' };
    const toDeployEventTimeline = {
      event: {
        payload: 'example-payload',
      },
      badgeClass: 'warning',
      badgeIconClass: 'notification:sync',
      title: 'TO_DEPLOY',
      isCurrentAPI: true,
    };
    await controller.appendNextPage(toDeployEventTimeline);
    expect(controller.eventsTimeline[0]).toEqual(toDeployEventTimeline);
  });
});
