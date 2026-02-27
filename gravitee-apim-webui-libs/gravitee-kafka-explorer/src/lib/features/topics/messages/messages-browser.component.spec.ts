/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { MessagesBrowserComponent } from './messages-browser.component';
import { MessagesBrowserHarness } from './messages-browser.harness';
import { fakeKafkaMessage } from '../../../models/kafka-cluster.fixture';

describe('MessagesBrowserComponent', () => {
  let fixture: ComponentFixture<MessagesBrowserComponent>;
  let harness: MessagesBrowserHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MessagesBrowserComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(MessagesBrowserComponent);
    fixture.componentRef.setInput('topicName', 'test-topic');
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, MessagesBrowserHarness);
  });

  it('should display topic name', async () => {
    expect(await harness.getTopicName()).toContain('test-topic');
  });

  it('should show empty message when no messages', async () => {
    expect(await harness.hasEmptyMessage()).toBe(true);
  });

  it('should display messages table when messages are provided', async () => {
    fixture.componentRef.setInput('messages', [
      fakeKafkaMessage({ partition: 0, offset: 42, key: 'key-1' }),
      fakeKafkaMessage({ partition: 1, offset: 10, key: 'key-2' }),
    ]);
    fixture.componentRef.setInput('totalFetched', 2);
    fixture.detectChanges();

    const rows = await harness.getMessagesRows();
    expect(rows.length).toBe(2);
    expect(rows[0]['partition']).toBe('0');
    expect(rows[0]['offset']).toBe('42');
    expect(rows[0]['key']).toContain('key-1');
  });

  it('should emit back event on back button click', async () => {
    const spy = jest.fn();
    fixture.componentInstance.back.subscribe(spy);

    await harness.clickBack();

    expect(spy).toHaveBeenCalled();
  });

  it('should emit search event on fetch button click', async () => {
    const spy = jest.fn();
    fixture.componentInstance.search.subscribe(spy);

    await harness.clickFetch();

    expect(spy).toHaveBeenCalledWith(
      expect.objectContaining({
        offsetMode: 'NEWEST',
        limit: 50,
      }),
    );
  });

  it('should show loading bar when loading', async () => {
    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    expect(await harness.isLoading()).toBe(true);
  });

  it('should not show loading bar when not loading', async () => {
    fixture.componentRef.setInput('loading', false);
    fixture.detectChanges();

    expect(await harness.isLoading()).toBe(false);
  });

  it('should compute partitionOptions from partitionCount', () => {
    fixture.componentRef.setInput('partitionCount', 3);
    fixture.detectChanges();

    expect(fixture.componentInstance.partitionOptions).toEqual([undefined, 0, 1, 2]);
  });

  it('should return only undefined when partitionCount is 0', () => {
    fixture.componentRef.setInput('partitionCount', 0);
    fixture.detectChanges();

    expect(fixture.componentInstance.partitionOptions).toEqual([undefined]);
  });

  it('should toggle expanded message on row click', () => {
    const msg1 = fakeKafkaMessage({ partition: 0, offset: 42 });
    const msg2 = fakeKafkaMessage({ partition: 1, offset: 10 });

    fixture.componentInstance.onRowClick(msg1);
    expect(fixture.componentInstance.expandedMessage()).toBe(msg1);

    fixture.componentInstance.onRowClick(msg1);
    expect(fixture.componentInstance.expandedMessage()).toBeNull();

    fixture.componentInstance.onRowClick(msg2);
    expect(fixture.componentInstance.expandedMessage()).toBe(msg2);
  });

  describe('truncate', () => {
    it('should return empty string for null', () => {
      expect(fixture.componentInstance.truncate(null)).toBe('');
    });

    it('should return value if shorter than maxLength', () => {
      expect(fixture.componentInstance.truncate('hello', 10)).toBe('hello');
    });

    it('should truncate and add ellipsis if longer than maxLength', () => {
      expect(fixture.componentInstance.truncate('hello world', 5)).toBe('hello...');
    });
  });

  describe('formatJson', () => {
    it('should return empty string for null', () => {
      expect(fixture.componentInstance.formatJson(null)).toBe('');
    });

    it('should pretty-print valid JSON', () => {
      expect(fixture.componentInstance.formatJson('{"a":1}')).toBe('{\n  "a": 1\n}');
    });

    it('should return original value for invalid JSON', () => {
      expect(fixture.componentInstance.formatJson('not json')).toBe('not json');
    });
  });

  describe('isJson', () => {
    it('should return false for null', () => {
      expect(fixture.componentInstance.isJson(null)).toBe(false);
    });

    it('should return true for valid JSON', () => {
      expect(fixture.componentInstance.isJson('{"a":1}')).toBe(true);
    });

    it('should return false for invalid JSON', () => {
      expect(fixture.componentInstance.isJson('not json')).toBe(false);
    });
  });
});
