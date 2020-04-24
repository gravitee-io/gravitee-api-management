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
package io.gravitee.management.rest.utils;

import io.gravitee.management.rest.exception.InvalidImageException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ImageUtilsTest {

    @Test(expected = InvalidImageException.class)
    public void shouldNotVerify_svgFormat() throws InvalidImageException {
        ImageUtils.verify("data:image/SVG+xml;base64,PHNWZw0KdmVyc2lvbj0iMS4xIiBiYXNlUHJvZmlsZT0iZnVsbCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4NCiAgIDxyZWN0IHdpZHRoPSIzMDAiIGhlaWdodD0iMTAwIiBzdHlsZT0iZmlsbDpyZ2IoMCwwLDI1NSk7c3Ryb2tlLXdpZHRoOjM7c3Ryb2tlOnJnYigwLDAsMCkiIC8+DQogICA8c2NyaXB0PmFsZXJ0KDEpPC9zY3JpcHQ+DQo8L3NWZz4=");
    }

    @Test(expected = InvalidImageException.class)
    public void shouldNotVerify_invalidBase64Format() throws InvalidImageException {
        ImageUtils.verify("data:image/SVG+xml;base64,invalid_base64");
    }

    @Test
    public void shouldVerify_pngFormat() throws InvalidImageException, IOException {
        InputStream inputStream = this.getClass().getResourceAsStream("/images/valid_png.b64");
        String picture = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        ImageUtils.verify(picture);
    }

}
