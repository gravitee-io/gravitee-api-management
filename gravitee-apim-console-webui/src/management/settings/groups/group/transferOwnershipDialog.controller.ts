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

import { Member } from './membershipState';

export enum ApiOwnershipTransferType {
  DELETE_PRIMARY_OWNER = 'DELETE_PRIMARY_OWNER',
  DEMOTE_PRIMARY_OWNER = 'DEMOTE_PRIMARY_OWNER',
  PROMOTE_NEW_PRIMARY_OWNER = 'PROMOTE_NEW_PRIMARY_OWNER',
}

export interface OwnershipTransferResult {
  primaryOwner: Member;
  newPrimaryOwnerRef?: Partial<Member>;
}

class DialogTransferOwnershipController {
  private readonly usersSelected: Partial<Member>[];

  constructor(
    private $mdDialog: angular.material.IDialogService,
    private primaryOwner: Member,
    private members: Member[],
    private group: any,
    private transferType: ApiOwnershipTransferType,
    private primaryOwnerWithScopes,
  ) {
    this.usersSelected = [];
    this.primaryOwnerWithScopes = primaryOwnerWithScopes;
    this.userFilterFn = this.userFilterFn.bind(this);
  }

  hide() {
    this.$mdDialog.cancel();
  }

  addMembers() {
    const [newPrimaryOwnerRef] = this.usersSelected;
    this.$mdDialog.hide({ newPrimaryOwnerRef, primaryOwner: this.primaryOwner });
  }

  userFilterFn(user) {
    return user.id !== this.primaryOwner.id && this.members.some((member) => member.id === user.id);
  }

  isInvalid(): boolean {
    return !this.isPromotion() && this.usersSelected.length !== 1;
  }

  isRemoval(): boolean {
    return this.transferType === ApiOwnershipTransferType.DELETE_PRIMARY_OWNER;
  }

  isPromotion(): boolean {
    return this.transferType === ApiOwnershipTransferType.PROMOTE_NEW_PRIMARY_OWNER;
  }
}
DialogTransferOwnershipController.$inject = ['$mdDialog', 'primaryOwner', 'members', 'group', 'transferType', 'primaryOwnerWithScopes'];

export default DialogTransferOwnershipController;
