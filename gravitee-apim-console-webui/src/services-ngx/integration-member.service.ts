import { Inject, Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

import { Constants } from "../entities/Constants";
import { AddMember, Member, MembersResponse, UpdateMember } from "../entities/management-api-v2";

@Injectable({
  providedIn: 'root'
})
export class IntegrationMemberService {

  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getMembers(integrationId: string): Observable<MembersResponse> {
    return this.http.get<MembersResponse>(`${this.constants.env.v2BaseURL}/integrations/${integrationId}/members`);
  }

  addMember(integrationId: string, membership: AddMember): Observable<Member> {
    return this.http.post<Member>(`${this.constants.env.v2BaseURL}/integrations/${integrationId}/members`, membership);
  }

  updateMember(integrationId: string, membership: UpdateMember): Observable<Member> {
    return this.http.put<Member>(`${this.constants.env.v2BaseURL}/integrations/${integrationId}/members/${membership.memberId}`, membership);
  }

  deleteMember(integrationId: string, memberId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.v2BaseURL}/integrations/${integrationId}/members/${memberId}`);
  }
}
