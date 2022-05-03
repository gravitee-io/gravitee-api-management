/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.elasticsearch.index;

import io.gravitee.elasticsearch.utils.Type;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ILMIndexNameGeneratorTest {

    private final ILMIndexNameGenerator generator = new ILMIndexNameGenerator("gravitee");

    @Test
    public void shouldGenerateIndexName() {
        String indexName = generator.getIndexName(Type.REQUEST, 0, 0, null);
        Assert.assertEquals("gravitee-request", indexName);
    }

    @Test
    public void shouldGenerateIndexName_withClusters() {
        String indexName = generator.getIndexName(Type.REQUEST, 0, 0, new String [] {"europe", "asia"});
        Assert.assertEquals("europe:gravitee-request,asia:gravitee-request", indexName);
    }
}
