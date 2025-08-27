import { Component, Input, OnInit } from "@angular/core";
import { isEqual } from "lodash";
import { GroupData } from "src/permissions/model/group-data";
import { MemberDataSource } from "src/permissions/model/member-data-source";
import { GioTableWrapperFilters } from "src/shared/components/gio-table-wrapper/gio-table-wrapper.component";

@Component({
    selector: 'cluster-group-members',
    templateUrl: './cluster-group-members.component.html',
    styleUrls: ['./cluster-group-members.component.scss'],
    standalone: false
  })
  export class ClusterGroupMembersComponent implements OnInit {
    @Input()
  groupData: GroupData;
  
  dataSourceGroupVM: {
    memberTotalCount: number;
    membersPageResult: MemberDataSource[];
    isLoading: boolean;
    canViewGroupMembers: boolean;
  };
  
  filters: GioTableWrapperFilters = {
    pagination: { index: 1, size: 10 },
    searchTerm: '',
  };

  displayedColumns = ['picture', 'displayName', 'role'];
  
  ngOnInit(): void {
    this.getGroupMembersPage();
}
    getGroupMembersPage(page = 1, perPage = 10): void {
        this.dataSourceGroupVM = {
            // isLoading: true,
            isLoading: false,
            canViewGroupMembers: true,
            memberTotalCount: 0,
            membersPageResult: [],
          }
    }

    onFiltersChanged(filters: GioTableWrapperFilters) {
      if (!isEqual(this.filters, filters)) { 
      this.filters = filters;
      this.getGroupMembersPage(filters.pagination.index, filters.pagination.size);
      }
    }
  }
