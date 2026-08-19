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
import { QuickFiltersStoreService } from './quick-filters-store.service';

import { DEFAULT_PERIOD, NONE_PERIOD } from '../models';

describe('QuickFiltersStoreService', () => {
  let service: QuickFiltersStoreService;

  beforeEach(() => {
    service = new QuickFiltersStoreService();
  });

  describe('toLogFilterQueryParam', () => {
    it('should keep epoch 0 bounds instead of replacing them with the computed period', () => {
      const queryParam = service.toLogFilterQueryParam({ period: DEFAULT_PERIOD, from: 0, to: 0 }, 1, 10);

      expect(queryParam.from).toBe(0);
      expect(queryParam.to).toBe(0);
      expect(queryParam.period).toBe('-5m');
    });

    it('should use a frozen time range as-is', () => {
      const queryParam = service.toLogFilterQueryParam({ period: DEFAULT_PERIOD }, 2, 25, { from: 100, to: 200 });

      expect(queryParam).toEqual(
        expect.objectContaining({
          page: 2,
          perPage: 25,
          period: '-5m',
          from: 100,
          to: 200,
        }),
      );
    });

    it('should keep a frozen unbounded range unbounded', () => {
      const queryParam = service.toLogFilterQueryParam({ period: DEFAULT_PERIOD }, 2, 10, { from: null, to: null });

      expect(queryParam.from).toBeNull();
      expect(queryParam.to).toBeNull();
    });

    it('should compute a relative period when no frozen range is provided', () => {
      const queryParam = service.toLogFilterQueryParam({ period: DEFAULT_PERIOD }, 1, 10);

      expect(queryParam.period).toBe('-5m');
      expect(queryParam.to - queryParam.from).toBe(5 * 60 * 1000);
    });

    it('should omit dates when the period is None', () => {
      const queryParam = service.toLogFilterQueryParam({ period: NONE_PERIOD }, 1, 10);

      expect(queryParam.period).toBe('0');
      expect(queryParam.from).toBeNull();
      expect(queryParam.to).toBeNull();
    });
  });
});
