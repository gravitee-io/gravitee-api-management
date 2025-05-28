import {Component, input} from '@angular/core';
import {JsonPipe} from "@angular/common";
import {
  MatExpansionModule,
  MatExpansionPanel,
  MatExpansionPanelHeader,
  MatExpansionPanelTitle
} from "@angular/material/expansion";
import {MCPToolDefinition} from "../../../../../entities/entrypoint/mcp";

@Component({
  selector: 'tool-display',
  imports: [
    JsonPipe,
    MatExpansionModule
  ],
  templateUrl: './tool-display.component.html',
  styleUrl: './tool-display.component.scss'
})
export class ToolDisplayComponent {
  tool = input.required<MCPToolDefinition>()
}
