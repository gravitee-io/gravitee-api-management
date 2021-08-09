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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PageServiceImplTests {

    private PageServiceImpl pageService = new PageServiceImpl();

    @Test
    public void getParentPathFromFilePath_should_return_correct_path() {
        String parentPath = pageService.getParentPathFromFilePath("/folder1/folder.2/folder3/file.txt");
        assertEquals("/folder1/folder.2/folder3", parentPath);
    }

    @Test
    public void getParentPathFromFilePath_with_filename_should_return_empty() {
        String parentPath = pageService.getParentPathFromFilePath("file.txt");
        assertEquals("", parentPath);
    }

    @Test
    public void getParentPathFromFilePath_with_empty_path_should_return_slash() {
        String parentPath = pageService.getParentPathFromFilePath("");
        assertEquals("/", parentPath);
    }
}
