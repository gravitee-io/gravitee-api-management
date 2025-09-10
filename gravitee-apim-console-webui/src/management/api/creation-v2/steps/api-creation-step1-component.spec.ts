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
import ApiCreationStep1Component from './api-creation-step1.component';

// Extract the controller class from the component definition
const ControllerClass = ApiCreationStep1Component.controller[1];

describe('ApiCreationStep1Component Scroll Events', () => {
  let instance: any;
  let mockParent: any;

  beforeEach(() => {
    mockParent = {
      hasMoreGroups: true,
      loadMoreGroups: jest.fn(),
    };

    instance = new ControllerClass({ isHybrid: () => true });
    instance.parent = mockParent;
    instance.hasLoadedOnce = false;
    instance.isLoading = false;
  });

  it('should call loadMoreGroups on first scroll when threshold is met', () => {
    const fakeTarget = {
      scrollTop: 700,
      clientHeight: 300,
      scrollHeight: 1000,
    };

    const event = { target: fakeTarget } as unknown as Event;

    instance.onScroll(event);

    expect(mockParent.loadMoreGroups).toHaveBeenCalled();
    expect(instance.hasLoadedOnce).toBe(true);
  });

  it('should throttle subsequent scrolls with delay', () => {
    jest.useFakeTimers();

    instance.hasLoadedOnce = true;
    instance.isLoading = false;

    const fakeTarget = {
      scrollTop: 700,
      clientHeight: 300,
      scrollHeight: 1000,
    };

    const event = { target: fakeTarget } as unknown as Event;

    instance.onScroll(event);
    expect(instance.isLoading).toBe(true);
    expect(mockParent.loadMoreGroups).not.toHaveBeenCalled();

    jest.advanceTimersByTime(1000);
    expect(mockParent.loadMoreGroups).toHaveBeenCalled();
    expect(instance.isLoading).toBe(false);

    jest.useRealTimers();
  });

  it('should not call loadMoreGroups if threshold not met', () => {
    const fakeTarget = {
      scrollTop: 100,
      clientHeight: 300,
      scrollHeight: 1000,
    };

    const event = { target: fakeTarget } as unknown as Event;

    instance.onScroll(event);
    expect(mockParent.loadMoreGroups).not.toHaveBeenCalled();
  });

  it('should clean up scroll listener on destroy', () => {
    const removeListener = jest.fn();
    instance.scrollListener = removeListener;
    instance.scrollContainer = {} as HTMLElement;

    instance.$onDestroy();

    expect(removeListener).toHaveBeenCalled();
    expect(instance.scrollListener).toBeNull();
    expect(instance.scrollContainer).toBeNull();
  });

  it('should set useGroupAsPrimaryOwner to true if isGroupOnly returns true', () => {
    const ctrl = new ControllerClass({ isHybrid: () => false, isGroupOnly: () => true });
    expect(ctrl.useGroupAsPrimaryOwner).toBe(false); // default
    ctrl.$onInit();
    expect(ctrl.useGroupAsPrimaryOwner).toBe(true); // should be enabled
  });

  it('should keep useGroupAsPrimaryOwner false if isGroupOnly returns false', () => {
    const ctrl = new ControllerClass({ isHybrid: () => false, isGroupOnly: () => false });
    expect(ctrl.useGroupAsPrimaryOwner).toBe(false); // default
    ctrl.$onInit();
    expect(ctrl.useGroupAsPrimaryOwner).toBe(false); // should remain false
  });
});
