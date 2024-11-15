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
package assertions;

import io.gravitee.apim.core.scoring.model.ScoreRequest;
import java.util.List;
import org.assertj.core.api.AbstractObjectAssert;

public class ScoreRequestAssert extends AbstractObjectAssert<ScoreRequestAssert, ScoreRequest> {

    public ScoreRequestAssert(ScoreRequest scoreRequest) {
        super(scoreRequest, ScoreRequestAssert.class);
    }

    public ScoreRequestAssert hasJobId(String jobId) {
        isNotNull();
        if (!actual.jobId().equals(jobId)) {
            failWithMessage("Expected jobId to be <%s> but was <%s>", jobId, actual.jobId());
        }
        return this;
    }

    public ScoreRequestAssert hasOrganizationId(String organizationId) {
        isNotNull();
        if (!actual.organizationId().equals(organizationId)) {
            failWithMessage("Expected organizationId to be <%s> but was <%s>", organizationId, actual.organizationId());
        }
        return this;
    }

    public ScoreRequestAssert hasEnvironmentId(String environmentId) {
        isNotNull();
        if (!actual.environmentId().equals(environmentId)) {
            failWithMessage("Expected environmentId to be <%s> but was <%s>", environmentId, actual.environmentId());
        }
        return this;
    }

    public ScoreRequestAssert hasApiId(String apiId) {
        isNotNull();
        if (!actual.apiId().equals(apiId)) {
            failWithMessage("Expected apiId to be <%s> but was <%s>", apiId, actual.apiId());
        }
        return this;
    }

    public ScoreRequestAssert hasAssetsContaining(ScoreRequest.AssetToScore... assets) {
        isNotNull();
        if (!actual.assets().containsAll(List.of(assets))) {
            failWithMessage("Assets <%s> not found in <%s>", assets, actual.assets());
        }
        return this;
    }

    public ScoreRequestAssert hasOnlyAssets(ScoreRequest.AssetToScore... assets) {
        isNotNull();
        if (!actual.assets().equals(List.of(assets))) {
            failWithMessage("Expected Assets to be <%s> but was <%s>", assets, actual.assets());
        }
        return this;
    }

    public ScoreRequestAssert hasCustomRulesets(ScoreRequest.CustomRuleset... customRulesets) {
        isNotNull();
        if (!actual.customRulesets().containsAll(List.of(customRulesets))) {
            failWithMessage("CustomRulesets <%s> not found in <%s>", customRulesets, actual.customRulesets());
        }
        return this;
    }
}
