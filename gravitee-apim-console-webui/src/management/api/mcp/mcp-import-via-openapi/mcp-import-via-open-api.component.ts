import {Component, computed, DestroyRef, inject, signal, Signal, WritableSignal} from '@angular/core';
import {GioMonacoEditorModule} from "@gravitee/ui-particles-angular";
import {FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import { MatCardModule} from "@angular/material/card";
import {GioGoBackButtonModule} from "../../../../shared/components/gio-go-back-button/gio-go-back-button.module";
import {ActivatedRoute, Router, RouterLink} from "@angular/router";
import {MCP, MCPTool} from "../../../../entities/management-api-v2";
import {catchError, map, switchMap, tap} from "rxjs/operators";
import {takeUntilDestroyed, toSignal} from "@angular/core/rxjs-interop";
import {McpToolDisplayComponent} from "../mcp-tool-display/mcp-tool-display.component";
import {OpenAPIToMCPService} from "./open-api-to-mcp.service";
import {ApiV2Service} from "../../../../services-ngx/api-v2.service";
import {EMPTY} from "rxjs";

@Component({
  selector: 'mcp-import-via-openapi',
  standalone: true,
  imports: [
    GioMonacoEditorModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    GioGoBackButtonModule,
    RouterLink,
    McpToolDisplayComponent
  ],
  templateUrl: './mcp-import-via-open-api.component.html',
  styleUrl: './mcp-import-via-open-api.component.scss'
})
export class McpImportViaOpenApiComponent {
  form: FormGroup<{
    editor: FormControl<string>
  }> = new FormGroup({
    editor: new FormControl('')
  })

  formIsDirty: WritableSignal<boolean> = signal(false);
  isSubmitting: WritableSignal<boolean> = signal(false);

  editorAsTools: Signal<MCPTool[]> = toSignal(this.form.valueChanges.pipe(
    map(({editor}) => {
      this.formIsDirty.set(true);
      try {
        return this.openAPIToMCPService.parseOpenAPISpecToTools(editor);
      } catch (e) {
        console.error("Oops: ", e)
        return [];
      }
    }))
  );

  error: Signal<string | undefined> = computed(() => {
    if (this.formIsDirty()) {
      if (!this.editorAsTools()?.length) {
        return "Error: Define at least one tool."
      }
    }
    return undefined;
  })

  formIsInvalid = computed(() => !this.editorAsTools()?.length || !this.formIsDirty() || this.isSubmitting());

  private apiId = this.activatedRoute.snapshot.params.apiId;
  private destroyRef = inject(DestroyRef);

  constructor(private readonly openAPIToMCPService: OpenAPIToMCPService, private readonly apiV2Service: ApiV2Service, private readonly activatedRoute: ActivatedRoute,
              private readonly router: Router) {
  }


  createMCPTools() {
    this.isSubmitting.set(true);
    this.apiV2Service.get(this.apiId).pipe(
      switchMap((response) => {
        if (response.definitionVersion === "V4" && response.type === "PROXY") {
          const tools = this.editorAsTools();
          const mcp: MCP = response.mcp ? {...response.mcp, tools} : {enabled: true, tools};

          return this.apiV2Service.update(this.apiId, {...response, mcp })
        }
        throw Error("An API must be a v4 Proxy API in order to include MCP configuration.")
      }),
      tap(() => {
        this.router.navigate(['..'], {relativeTo: this.activatedRoute});
      }),
      catchError((error) => {
        console.error(error);
        this.isSubmitting.set(false);
        return EMPTY;
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }
}
