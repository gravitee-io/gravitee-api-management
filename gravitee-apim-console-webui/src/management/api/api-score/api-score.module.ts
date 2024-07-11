import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle } from '@angular/material/card';

import { ApiScoreComponent } from "./api-score.component";
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import {
  MatAccordion,
  MatExpansionModule,
  MatExpansionPanel,
  MatExpansionPanelTitle
} from '@angular/material/expansion';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { MapProviderNamePipe } from '../../integrations/pipes/map-provider-name.pipe';
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell, MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef, MatNoDataRow, MatRow, MatRowDef, MatTable
} from '@angular/material/table';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';



@NgModule({
  declarations: [
    ApiScoreComponent
  ],
  imports: [
    CommonModule,
    MatCard,
    MatCardContent,
    MatCardHeader,
    GioPermissionModule,
    MatButton,
    MatCardTitle,
    MatCardSubtitle,
    MatButtonToggleGroup,
    MatButtonToggle,
    MatExpansionModule,
    GioTableWrapperModule,
    MapProviderNamePipe,
    MatCell,
    MatCellDef,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderRow,
    MatHeaderRowDef,
    MatIcon,
    MatIconButton,
    MatRow,
    MatRowDef,
    MatTable,
    MatTooltip,
    MatNoDataRow,
    MatHeaderCellDef
  ],
  exports: [ApiScoreComponent]
})
export class ApiScoreModule { }
