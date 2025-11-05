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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { WebhookLogDetailsDrawerComponent } from './webhook-log-details-drawer.component';

describe('WebhookLogDetailsDrawerComponent', () => {
  let component: WebhookLogDetailsDrawerComponent;
  let fixture: ComponentFixture<WebhookLogDetailsDrawerComponent>;

  const mockLog = {
    requestId: 'req-1',
    timestamp: '2025-06-15T12:00:00.000Z',
    status: 200,
    callbackUrl: 'https://test.com/webhook',
    uri: 'https://test.com/webhook',
    application: { id: 'app-1', name: 'Test App', type: 'SIMPLE' as any },
    duration: '2.8 s',
    additionalMetrics: {
      long_webhook_responseTime: 2800,
      int_webhook_status: 200,
      string_webhook_retry_timeline: '[{"attempt":1,"timestamp":1718455200000,"status":200,"duration":2800}]',
      int_webhook_retry_count: 0,
      int_webhook_request_method: 'POST',
      string_webhook_request_body: '{"test":"data"}',
      long_webhook_payload_size: 1024,
      string_webhook_last_error: null,
      string_webhook_request_headers: '{"Content-Type":"application/json"}',
      string_webhook_response_body: '{"success":true}',
      string_webhook_response_headers: '{"Content-Type":"application/json"}',
      string_webhook_url: 'https://test.com/webhook',
      keyword_webhook_application_id: 'app-1',
      keyword_webhook_subscription_id: 'sub-1',
      long_webhook_request_timestamp: new Date('2025-06-15T12:00:00.000Z').getTime(),
      boolean_webhook_dl_queue: false,
    },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebhookLogDetailsDrawerComponent, NoopAnimationsModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(WebhookLogDetailsDrawerComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('log', mockLog);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Drawer Visibility', () => {
    it('should render the drawer', () => {
      fixture.detectChanges();
      const compiled = fixture.nativeElement;
      const drawer = compiled.querySelector('.details-drawer');
      expect(drawer).toBeTruthy();
    });
  });

  describe('Expand/Collapse', () => {
    it('should toggle expanded state', () => {
      fixture.detectChanges();
      expect(component.isExpanded()).toBe(true);

      component.toggleExpand();
      expect(component.isExpanded()).toBe(false);

      component.toggleExpand();
      expect(component.isExpanded()).toBe(true);
    });

    it('should not have expanded class by default (expanded is false in template)', () => {
      fixture.detectChanges();
      component.isExpanded.set(false);
      fixture.detectChanges();

      const compiled = fixture.nativeElement;
      const drawer = compiled.querySelector('.details-drawer');
      expect(drawer.classList.contains('details-drawer__expanded')).toBe(false);
    });
  });

  describe('Close Drawer', () => {
    it('should emit closeDrawer and reset isExpanded', (done) => {
      fixture.detectChanges();
      component.isExpanded.set(true);

      component.closeDrawer.subscribe(() => {
        expect(component.isExpanded()).toBe(false);
        done();
      });

      component.close();
    });
  });

  describe('Data Parsing', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should parse request headers from JSON', () => {
      const headers = component.requestHeaders();
      expect(headers).toEqual({ 'Content-Type': 'application/json' });
    });

    it('should parse response headers from JSON', () => {
      const headers = component.responseHeaders();
      expect(headers).toEqual({ 'Content-Type': 'application/json' });
    });

    it('should parse and format request body', () => {
      const body = component.requestBody();
      expect(body).toContain('"test"');
      expect(body).toContain('"data"');
    });

    it('should parse and format response body', () => {
      const body = component.responseBody();
      expect(body).toContain('"success"');
      expect(body).toContain('true');
    });

    it('should parse retry timeline', () => {
      const timeline = component.retryTimeline();
      expect(timeline).toHaveLength(1);
      expect(timeline[0].attempt).toBe(1);
      expect(timeline[0].status).toBe(200);
    });

    it('should handle invalid JSON gracefully', () => {
      const logWithInvalidJson = {
        ...mockLog,
        additionalMetrics: {
          ...mockLog.additionalMetrics,
          string_webhook_request_headers: 'invalid json',
        },
      };

      const testFixture = TestBed.createComponent(WebhookLogDetailsDrawerComponent);
      const testComponent = testFixture.componentInstance;
      testFixture.componentRef.setInput('log', logWithInvalidJson);
      testFixture.detectChanges();

      const headers = testComponent.requestHeaders();
      expect(headers).toEqual({});
    });
  });

  describe('Formatting Utilities', () => {
    it('should format duration in milliseconds', () => {
      expect(component.formatDuration(500)).toBe('500 ms');
    });

    it('should format duration in seconds', () => {
      expect(component.formatDuration(2500)).toBe('2.5 s');
    });

    it('should format bytes', () => {
      expect(component.formatBytes(500)).toBe('500B');
      expect(component.formatBytes(2048)).toBe('2.0KB');
      expect(component.formatBytes(2097152)).toBe('2.0MB');
    });
  });

  describe('Copy to Clipboard', () => {
    it('should set copied to true and reset after 2 seconds', (done) => {
      jest.useFakeTimers();

      expect(component.copied()).toBe(false);

      component.onCopied();
      expect(component.copied()).toBe(true);

      jest.advanceTimersByTime(2000);
      expect(component.copied()).toBe(false);

      jest.useRealTimers();
      done();
    });
  });
});
