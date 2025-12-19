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

import { extractWebhookAdditionalMetrics } from './webhook-metrics.utils';

describe('extractWebhookAdditionalMetrics', () => {
  describe('bool_webhook_dlq parsing', () => {
    it('should parse boolean true to true', () => {
      const input = {
        bool_webhook_dlq: true,
        'int_webhook_resp-status': '400',
      };

      const result = extractWebhookAdditionalMetrics(input);

      expect(result.bool_webhook_dlq).toBe(true);
    });

    it('should parse boolean false to false', () => {
      const input = {
        bool_webhook_dlq: false,
        'int_webhook_resp-status': '400',
      };

      const result = extractWebhookAdditionalMetrics(input);

      expect(result.bool_webhook_dlq).toBe(false);
    });

    it('should ignore non-boolean values (e.g., strings)', () => {
      const input = {
        bool_webhook_dlq: 'true' as any, // Backend never sends strings, but test defensive behavior
        'int_webhook_resp-status': '400',
      };

      const result = extractWebhookAdditionalMetrics(input);

      // Should be undefined since it's not a boolean
      expect(result.bool_webhook_dlq).toBeUndefined();
    });

    it('should not set bool_webhook_dlq when field is undefined', () => {
      const input = {
        'int_webhook_resp-status': '400',
        // bool_webhook_dlq is missing
      };

      const result = extractWebhookAdditionalMetrics(input);

      expect(result.bool_webhook_dlq).toBeUndefined();
    });

    it('should not set bool_webhook_dlq when field is null', () => {
      const input = {
        bool_webhook_dlq: null as any,
        'int_webhook_resp-status': '400',
      };

      const result = extractWebhookAdditionalMetrics(input);

      expect(result.bool_webhook_dlq).toBeUndefined();
    });

    it('should handle case where bool_webhook_dlq is the only field', () => {
      const input = {
        bool_webhook_dlq: true,
      };

      const result = extractWebhookAdditionalMetrics(input);

      expect(result.bool_webhook_dlq).toBe(true);
    });

    it('should handle empty additionalMetrics object', () => {
      const input = {};

      const result = extractWebhookAdditionalMetrics(input);

      expect(result.bool_webhook_dlq).toBeUndefined();
    });
  });

  describe('other fields parsing', () => {
    it('should parse all string fields correctly', () => {
      const input = {
        'string_webhook_req-method': 'POST',
        string_webhook_url: 'https://example.com',
        'keyword_webhook_app-id': 'app-123',
        'keyword_webhook_sub-id': 'sub-456',
        'int_webhook_resp-status': '200',
      };

      const result = extractWebhookAdditionalMetrics(input);

      expect(result['string_webhook_req-method']).toBe('POST');
      expect(result.string_webhook_url).toBe('https://example.com');
      expect(result['keyword_webhook_app-id']).toBe('app-123');
      expect(result['keyword_webhook_sub-id']).toBe('sub-456');
    });

    it('should parse numeric fields correctly', () => {
      const input = {
        'long_webhook_req-timestamp': '1234567890',
        'int_webhook_resp-status': '400',
        'int_webhook_retry-count': '3',
        'long_webhook_resp-time': '1500',
        'int_webhook_resp-body-size': '1024',
      };

      const result = extractWebhookAdditionalMetrics(input);

      expect(result['long_webhook_req-timestamp']).toBe(1234567890);
      expect(result['int_webhook_resp-status']).toBe(400);
      expect(result['int_webhook_retry-count']).toBe(3);
      expect(result['long_webhook_resp-time']).toBe(1500);
      expect(result['int_webhook_resp-body-size']).toBe(1024);
    });

    it('should handle optional numeric fields as undefined when missing', () => {
      const input = {
        'int_webhook_resp-status': '200',
      };

      const result = extractWebhookAdditionalMetrics(input);

      expect(result['long_webhook_resp-time']).toBeUndefined();
      expect(result['int_webhook_resp-body-size']).toBeUndefined();
    });
  });

  describe('integration scenarios', () => {
    it('should handle complete webhook log with bool_webhook_dlq as boolean true', () => {
      const input = {
        'string_webhook_req-method': 'POST',
        string_webhook_url: 'https://webhook.site/test',
        'keyword_webhook_app-id': 'app-123',
        'keyword_webhook_sub-id': 'sub-456',
        'int_webhook_retry-count': '0',
        'long_webhook_req-timestamp': '1766139427978',
        'int_webhook_resp-status': '400',
        'long_webhook_resp-time': '87490',
        bool_webhook_dlq: true, // Boolean from backend
      };

      const result = extractWebhookAdditionalMetrics(input);

      expect(result.bool_webhook_dlq).toBe(true);
      expect(result['int_webhook_resp-status']).toBe(400);
      expect(result.string_webhook_url).toBe('https://webhook.site/test');
    });

    it('should handle complete webhook log with bool_webhook_dlq as boolean false', () => {
      const input = {
        'string_webhook_req-method': 'POST',
        string_webhook_url: 'https://webhook.site/test',
        'keyword_webhook_app-id': 'app-123',
        'keyword_webhook_sub-id': 'sub-456',
        'int_webhook_retry-count': '0',
        'long_webhook_req-timestamp': '1766139427978',
        'int_webhook_resp-status': '400',
        'long_webhook_resp-time': '87490',
        bool_webhook_dlq: false, // Boolean from backend
      };

      const result = extractWebhookAdditionalMetrics(input);

      expect(result.bool_webhook_dlq).toBe(false);
      expect(result['int_webhook_resp-status']).toBe(400);
    });

    it('should handle webhook log without bool_webhook_dlq field', () => {
      const input = {
        'string_webhook_req-method': 'POST',
        string_webhook_url: 'https://webhook.site/test',
        'int_webhook_resp-status': '400',
        // bool_webhook_dlq is missing
      };

      const result = extractWebhookAdditionalMetrics(input);

      expect(result.bool_webhook_dlq).toBeUndefined();
      expect(result['int_webhook_resp-status']).toBe(400);
    });
  });
});
