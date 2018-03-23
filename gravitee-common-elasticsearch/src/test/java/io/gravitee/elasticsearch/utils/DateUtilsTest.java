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
package io.gravitee.elasticsearch.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DateUtilsTest {

    @Test
    public void shouldReturnSingleDate() {
        List<String> indices = DateUtils.rangedIndices(1474581851724l, 1474582151724l);
        Assert.assertEquals(1, indices.size());
    }

    @Test
    public void shouldReturnMultipleDate() {
        List<String> indices = DateUtils.rangedIndices(1473459236000l, 1474582436000l);
        Assert.assertEquals(14, indices.size());
    }
}
