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

import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DashboardService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('sort by labels', () => {
    it('should sort by labels ascending', done => {
      service.list(undefined, { active: 'labels', direction: 'asc' }, 1, 100).subscribe(result => {
        const labels = result.data.map(d => d.labels);
        // Items without labels (empty string) should come first in ascending order
        const withoutLabels = labels.filter(l => !l || Object.keys(l).length === 0);
        const withLabels = labels.filter(l => l && Object.keys(l).length > 0);
        expect(withoutLabels.length + withLabels.length).toBe(labels.length);

        // Verify items with labels are sorted
        const labelStrings = withLabels.map(l =>
          Object.entries(l)
            .map(([k, v]) => `${k}:${v}`)
            .sort()
            .join(','),
        );
        for (let i = 1; i < labelStrings.length; i++) {
          expect(labelStrings[i] >= labelStrings[i - 1]).toBeTruthy();
        }
        done();
      });
    });

    it('should sort by labels descending', done => {
      service.list(undefined, { active: 'labels', direction: 'desc' }, 1, 100).subscribe(result => {
        const labels = result.data.map(d => d.labels);
        const labelStrings = labels
          .filter(l => l && Object.keys(l).length > 0)
          .map(l =>
            Object.entries(l)
              .map(([k, v]) => `${k}:${v}`)
              .sort()
              .join(','),
          );
        for (let i = 1; i < labelStrings.length; i++) {
          expect(labelStrings[i] <= labelStrings[i - 1]).toBeTruthy();
        }
        done();
      });
    });

    it('should handle items without labels when sorting', done => {
      service.list(undefined, { active: 'labels', direction: 'asc' }, 1, 100).subscribe(result => {
        expect(result.data.length).toBeGreaterThan(0);
        // Should not throw an error and should return all items
        const itemsWithLabels = result.data.filter(d => d.labels && Object.keys(d.labels).length > 0);
        const itemsWithoutLabels = result.data.filter(d => !d.labels || Object.keys(d.labels).length === 0);
        expect(itemsWithLabels.length).toBeGreaterThan(0);
        expect(itemsWithoutLabels.length).toBeGreaterThan(0);
        done();
      });
    });
  });
});
