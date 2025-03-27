import {Component, signal, WritableSignal} from '@angular/core';
import {MatCard, MatCardModule} from "@angular/material/card";
import {JsonPipe} from "@angular/common";
import {CopyCodeIconComponent} from "../copy-code/copy-code-icon/copy-code-icon/copy-code-icon.component";

interface ClaudeNpxConfig {
  mcpServers: {
    gravitee: {
      command: string;
      args: string[];
    }
  }
}

@Component({
  selector: 'app-mcp-use-with-claude',
  standalone: true,
  imports: [
    MatCardModule,
    JsonPipe,
    CopyCodeIconComponent
  ],
  templateUrl: './mcp-use-with-claude.component.html',
  styleUrl: './mcp-use-with-claude.component.scss'
})
export class McpUseWithClaudeComponent {
    npxConfig: WritableSignal<ClaudeNpxConfig> = signal({mcpServers: {
      gravitee: {
        command: 'npx',
        args: ['mcp-remote', 'http://localhost:18082/node/mcp/sse']
      }
      }})
}
