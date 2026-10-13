/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.rest.api.mcp.core.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.rest.api.mcp.core.EnvironmentResolver;
import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.Change;
import io.gravitee.apim.rest.api.mcp.core.model.EnvRef;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.ChangeLog;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.apim.rest.api.mcp.spi.McpCaller;
import io.gravitee.common.utils.TimeProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What was changed on an API in the control plane: deployments, and the audit trail of its plans, subscriptions and keys.
 */
@UseCase
public class ListChangesUseCase {

    private final EnvironmentDirectory environments;
    private final TriageAccess access;
    private final ApiDirectory apis;
    private final ChangeLog changeLog;

    public ListChangesUseCase(EnvironmentDirectory environments, TriageAccess access, ApiDirectory apis, ChangeLog changeLog) {
        this.environments = environments;
        this.access = access;
        this.apis = apis;
        this.changeLog = changeLog;
    }

    public record Input(McpCaller caller, String environment, String api, String window, String until) {}

    /**
     * @param omittedSources sources the caller may not read: an empty {@code changes} does not mean nothing changed there
     */
    public record Output(
        String summary,
        EnvRef environment,
        ApiRef api,
        TimeWindow window,
        List<Change> changes,
        List<Change.Source> omittedSources
    ) {}

    public Output execute(Input input) {
        if (input.api() == null || input.api().isBlank()) {
            throw new TriageException("api is required: pass the id or the name of the API to list changes for.");
        }
        var scope = EnvironmentResolver.resolve(environments, apis, input.caller(), input.environment(), input.api());
        var auditInfo = scope.auditInfo();
        var api = scope.api();
        var window = TimeWindow.parse(input.window(), input.until(), TimeProvider.instantNow());

        var changes = new ArrayList<Change>();
        var omitted = new ArrayList<Change.Source>();
        if (access.canReadApiEvents(auditInfo, api.id())) {
            changes.addAll(changeLog.deployments(auditInfo, api.id(), window));
        } else {
            omitted.add(Change.Source.DEPLOYMENT);
        }
        if (access.canReadApiAudit(auditInfo, api.id())) {
            changes.addAll(changeLog.audit(auditInfo, api.id(), window));
        } else {
            omitted.add(Change.Source.AUDIT);
        }
        changes.sort(Comparator.comparing(Change::at).reversed());

        var summary = changes.size() + " change(s) in the window, newest first.";
        if (!omitted.isEmpty()) {
            summary += " Not allowed to read: " + omitted + ".";
        }
        return new Output(summary, scope.environment(), api, window, changes, omitted);
    }
}
