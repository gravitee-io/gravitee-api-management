import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";

@Component({
    selector: 'cluster-manage-groups',
    templateUrl: './cluster-manage-groups.component.html',
    styleUrls: ['./cluster-manage-groups.component.scss'],
    standalone: false
  })
  export class ClusterManageGroupsComponent {

    groupsControl = new FormControl();

    groups = [
        {
            id: 1,
            name: 'g1'
        },
        {
            id: 2,
            name: 'g2'
        },
        {
            id: 3,
            name: 'g3'
        },
    ]

    save() {}
  }
  