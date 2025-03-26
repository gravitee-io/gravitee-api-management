import {Component, input, signal, Signal} from '@angular/core';
import {MCPTool} from "../../../../entities/management-api-v2";
import {MatExpansionModule, MatExpansionPanel} from "@angular/material/expansion";
import {GioMonacoEditorModule} from "@gravitee/ui-particles-angular";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {JsonPipe} from "@angular/common";

@Component({
  selector: 'mcp-tool-read',
  standalone: true,
  imports: [
    MatExpansionModule,
    GioMonacoEditorModule,
    ReactiveFormsModule,
    FormsModule,
    JsonPipe
  ],
  templateUrl: './mcp-tool-display.component.html',
  styleUrl: './mcp-tool-display.component.scss'
})
export class McpToolDisplayComponent {
  tool = input.required<MCPTool>();
}
