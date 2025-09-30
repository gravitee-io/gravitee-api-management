import { TestBed } from '@angular/core/testing';

import { GraviteeDashboardService } from './gravitee-dashboard.service';

describe('GraviteeDashboardService', () => {
  let service: GraviteeDashboardService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GraviteeDashboardService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
