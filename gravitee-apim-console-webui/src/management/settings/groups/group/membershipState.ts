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
import { cloneDeep } from 'lodash';

export enum RoleName {
  PRIMARY_OWNER = 'PRIMARY_OWNER',
  OWNER = 'OWNER',
  USER = 'USER',
}

export enum RoleScope {
  API = 'API',
  APPLICATION = 'APPLICATION',
  INTEGRATION = 'INTEGRATION',
}

export type Roles = {
  [scope in RoleScope]: RoleName;
};

export interface Member {
  id: string;
  displayName: string;
  roles: Roles;
}

export class MemberState {
  constructor(
    private member,
    private previousMemberState,
  ) {}

  wasPrimaryOwner(scope: RoleScope): boolean {
    return !this.isNewMember() && this.previousMemberState.roles[scope] === RoleName.PRIMARY_OWNER;
  }

  isPrimaryOwner(scope: RoleScope): boolean {
    return this.member.roles[scope] === RoleName.PRIMARY_OWNER;
  }

  isRoleUpdate(scope: RoleScope): boolean {
    return this.isNewMember() || this.previousMemberState.roles[scope] !== this.member.roles[scope];
  }

  isNewMember(): boolean {
    return !this.previousMemberState;
  }

  getLastState(): Member {
    return this.previousMemberState;
  }

  getCurrentState(): Member {
    return this.member;
  }

  getPrimaryOwnerScopes() {
    return Object.entries(this.member.roles)
      .filter(([, roleName]) => roleName === RoleName.PRIMARY_OWNER)
      .map(([roleScope]) => roleScope);
  }
}

export class MembershipState {
  private readonly members: Member[];

  constructor(members: Member[]) {
    this.members = cloneDeep(members);
  }

  stateOf(member: Member): MemberState {
    return new MemberState(cloneDeep(member), this.findByRef(member));
  }

  getPrimaryOwner(scope: RoleScope): Member {
    return this.members.find((member) => member.roles[scope] === RoleName.PRIMARY_OWNER);
  }

  hasPrimaryOwner(scope: RoleScope): boolean {
    return this.members.some((member) => member.roles[scope] === RoleName.PRIMARY_OWNER);
  }

  findByRef(memberRef): Member | undefined {
    return this.members.find((member) => member.id === memberRef.id);
  }

  findAll(): Member[] {
    return this.members;
  }

  add(member: Member): void {
    this.members.push(cloneDeep(member));
  }

  isPrimaryOwnerDemotion(member: Member, scope: RoleScope): boolean {
    const memberState = this.stateOf(member);
    return memberState.isRoleUpdate(scope) && memberState.wasPrimaryOwner(scope);
  }

  isPrimaryOwnerPromotion(member: Member, scope: RoleScope): boolean {
    const memberState = this.stateOf(member);
    return memberState.isRoleUpdate(scope) && memberState.isPrimaryOwner(scope) && this.hasPrimaryOwner(scope);
  }

  isNewMember(member: Member): boolean {
    return !this.findByRef(member);
  }

  primaryOwnerWithScopes(scopes: RoleScope[]) {
    return scopes.map((scope) => ({
      member: this.getPrimaryOwner(scope),
      roleScope: scope,
    }));
  }

  sync(members: Member[]): void {
    for (const member of members) {
      this.syncOne(member);
    }
  }

  private syncOne(member: Member): void {
    const previousMemberState = this.findByRef(member);

    if (previousMemberState) {
      Object.assign(previousMemberState, cloneDeep(member));
    } else {
      this.add(member);
    }
  }
}
