import { TestBed } from '@angular/core/testing';

import { CustomUserFieldsService } from './custom-user-fields.service';

describe('CustomUserFieldsService', () => {
  let service: CustomUserFieldsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CustomUserFieldsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
