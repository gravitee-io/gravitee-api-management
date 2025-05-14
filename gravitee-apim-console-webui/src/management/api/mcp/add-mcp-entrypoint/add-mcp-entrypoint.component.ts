import {Component, inject} from '@angular/core';
import {MatStepperModule} from "@angular/material/stepper";
import {FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatCardModule} from "@angular/material/card";
import {GioFormJsonSchemaModule, GioJsonSchema} from "@gravitee/ui-particles-angular";
import {AsyncPipe} from "@angular/common";
import {Observable, of} from "rxjs";
import {ConnectorService} from "../../../../services-ngx/connector.service";
import {ConnectorPluginsV2Service} from "../../../../services-ngx/connector-plugins-v2.service";
import {fakePolicySchema} from "../../../../entities/policy";

@Component({
  selector: 'add-mcp-entrypoint',
  imports: [MatCardModule, MatStepperModule, MatButtonModule, ReactiveFormsModule, GioFormJsonSchemaModule, AsyncPipe],
  templateUrl: './add-mcp-entrypoint.component.html',
  styleUrl: './add-mcp-entrypoint.component.scss'
})
export class AddMcpEntrypointComponent {
  firstFormGroup: FormGroup<{configuration: FormControl}> = new FormGroup({
    configuration: new FormControl(null, {nonNullable: true}),
  }, {
    validators: [],
    asyncValidators: [],
  });
  secondFormGroup: FormGroup<{}> = new FormGroup({});
  // mcpEntrypointSchema$: Observable<GioJsonSchema> = inject(ConnectorPluginsV2Service).getEntrypointPluginSchema('MCP');
  mcpEntrypointSchema$: Observable<GioJsonSchema> = of({
      "$schema": "http://json-schema.org/draft-07/schema#",
      "type": "object",
      "properties": {
        "messagesLimitCount": {
          "type": "integer",
          "title": "Limit messages count",
          "description": "Maximum number of messages to retrieve.",
          "default": 500
        },
        "messagesLimitDurationMs": {
          "type": "number",
          "title": "Limit messages duration (in ms)",
          "default": 5000,
          "gioConfig": {
            "banner": {
              "title": "Limit messages duration",
              "text": "Maximum duration in milliseconds to wait to retrieve the expected number of messages (See Limit messages count). The effective number of retrieved messages could be less than expected it maximum duration is reached."
            }
          }
        },
        "headersInPayload": {
          "type": "boolean",
          "default": false,
          "title": "Allow sending messages headers to client in payload",
          "description": "Default is false.",
          "gioConfig": {
            "banner": {
              "title": "Allow sending messages headers to client in payload",
              "text": "Each header will be sent as extra field in payload. For JSON and XML, in a dedicated headers object. For plain text, following 'key=value' format. Default is false."
            }
          }
        },
        "metadataInPayload": {
          "type": "boolean",
          "default": false,
          "title": "Allow sending messages metadata to client in payload",
          "description": "Default is false.",
          "gioConfig": {
            "banner": {
              "title": "Allow sending messages metadata to client in payload",
              "text": "Allow sending messages metadata to client in payload. Each metadata will be sent as extra field in the payload. For JSON and XML, in a dedicated metadata object. For plain text, following 'key=value' format. Default is false."
            }
          }
        },
        "statusOverride": {
          "type": "integer",
          "title": "Response Status Code",
          "minimum": 200,
          "maximum": 299,
          "default": 200,
          "description": "Enter an HTTP status code between 200 and 299 to override the default success response status (200)."
        }
      },
      "additionalProperties": false
    }
  );
}
