import { TestBed } from '@angular/core/testing';

import { CurrentUserService } from './current-user.service';

describe('CurrentUserService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: CurrentUserService = TestBed.get(CurrentUserService);
    expect(service).toBeTruthy();
  });
});
