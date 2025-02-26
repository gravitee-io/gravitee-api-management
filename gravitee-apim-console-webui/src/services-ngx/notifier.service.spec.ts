import { TestBed } from '@angular/core/testing';

import { NotifierService } from './notifier.service';

describe('NotifierService', () => {
  let service: NotifierService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotifierService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
