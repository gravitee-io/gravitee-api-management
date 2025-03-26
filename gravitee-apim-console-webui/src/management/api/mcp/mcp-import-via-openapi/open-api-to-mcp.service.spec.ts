import { TestBed } from '@angular/core/testing';
import {OpenAPIToMCPService} from "./open-api-to-mcp.service";


describe('OpenAPIToMCPService', () => {
  let service: OpenAPIToMCPService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(OpenAPIToMCPService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
