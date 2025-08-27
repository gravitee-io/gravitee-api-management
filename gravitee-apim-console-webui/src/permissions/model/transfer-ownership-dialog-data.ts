import { Group, Member } from '../../entities/management-api-v2';
import { Role } from '../../entities/role/role';

export interface TransferOwnershipDialogData {
  groups: Group[];
  roles: Role[];
  members: Member[];
}
