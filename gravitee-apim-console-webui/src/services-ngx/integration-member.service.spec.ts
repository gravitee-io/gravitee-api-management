import { TestBed } from '@angular/core/testing';
import { HttpTestingController } from "@angular/common/http/testing";

import { IntegrationMemberService } from './integration-member.service';

import { CONSTANTS_TESTING, GioTestingModule } from "../shared/testing";
import { AddMember, Member, MembersResponse, UpdateMember } from "../entities/management-api-v2";

describe('IntegrationMemberService', () => {
  let service: IntegrationMemberService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<IntegrationMemberService>(IntegrationMemberService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('get members', () => {
    it('should call the API', (done) => {
      const integrationId = 'fox';
      const mockMembers: Member[] = [
        { id: '1', roles: [{ name: 'king', scope: 'API' }] },
        { id: '1', roles: [{ name: 'prince', scope: 'API' }] },
      ];
      const mockResponse: MembersResponse = {
        data: mockMembers,
      };

      service.getMembers(integrationId).subscribe((response) => {
        expect(response).toMatchObject(mockResponse);
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/members`,
      });

      req.flush(mockResponse);
    });
  });

  describe('add member', () => {
    it('should call the API', (done) => {
      const integrationId = 'fox';
      const membership: AddMember = { userId: '1', roleName: 'king', externalReference: 'ref' };
      service.addMember(integrationId, membership).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/members`,
      });
      req.flush({});
    });
  });

  describe('update member', () => {
    it('should call api', (done) => {
      const integrationId = 'fox';
      const membership: UpdateMember = { memberId: '1', roleName: 'king' };
      service.updateMember(integrationId, membership).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/members/${membership.memberId}`,
      });
      req.flush({});
    });
  });

  describe('delete member', () => {
    it('should call the API', (done) => {
      const integrationId = 'fox';
      const memberId = 'id';
      service.deleteMember(integrationId, memberId).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        method: 'DELETE',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/integrations/${integrationId}/members/${memberId}`,
      });
      req.flush({});
    });
  });
});
