import {Component, input, signal, Signal} from '@angular/core';
import {MCPTool} from "../../../../entities/management-api-v2";

@Component({
  selector: 'mcp-tool-read',
  standalone: true,
  imports: [],
  templateUrl: './mcp-tool-read.component.html',
  styleUrl: './mcp-tool-read.component.scss'
})
export class McpToolReadComponent {
  tool = input.required<MCPTool>();
}
