import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {PortalNavigationItemType} from "../../../entities/management-api-v2/portalNavigationItem";

export interface AddSectionDialogData {
  type: PortalNavigationItemType;
}

export interface AddSectionDialogResult {
  id: string;
}

@Component({
  selector: 'add-section-dialog',
  imports: [],
  templateUrl: './add-section-dialog.component.html',
  styleUrl: './add-section-dialog.component.scss'
})
export class AddSectionDialogComponent {

  constructor(
    private readonly dialogRef: MatDialogRef<AddSectionDialogData>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AddSectionDialogData,
  ) {
  }


}
