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
package io.gravitee.gateway.reactive.flow;

import io.gravitee.definition.model.flow.Operator;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.runners.Parameterized;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class FlowBaseTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Parameterized.Parameter(0)
    public List<String> flowPaths;

    @Parameterized.Parameter(1)
    public Operator operator;

    @Parameterized.Parameter(2)
    public String expectedBestMatchResult;

    @Parameterized.Parameter(3)
    public String requestPath;

    /**
     * Build list of parameters for test case.
     * @return Tests parameter objects with this structure:
     * { list of flow paths, expected path result, path used by request}
     */
    @Parameterized.Parameters(name = "{index}: Configured flows={0}, Request={3}, Operator={1}, Expected BestMatch={2}")
    public static Iterable<Object> data() {
        return Arrays.asList(
            new Object[][] {
                { List.of(), Operator.STARTS_WITH, null, "/path/55" },
                { List.of("/"), Operator.STARTS_WITH, "/", "" },
                { List.of("/"), Operator.STARTS_WITH, "/", "/" },
                { List.of("/"), Operator.STARTS_WITH, "/", "/path/55" },
                { List.of("/"), Operator.EQUALS, "/", "" },
                { List.of("/"), Operator.EQUALS, "/", "/" },
                { List.of("/"), Operator.EQUALS, null, "/path/55" },
                { List.of("/", "/path"), Operator.STARTS_WITH, "/", "/random" },
                { List.of("/", "/path"), Operator.STARTS_WITH, "/path", "/path/55" },
                { List.of("/path/:id"), Operator.STARTS_WITH, "/path/:id", "/path/55" },
                { List.of("/path/:id"), Operator.STARTS_WITH, "/path/:id", "/path/55" },
                { List.of("/path/:id"), Operator.EQUALS, "/path/:id", "/path/55" },
                { List.of("/path/:id", "/path/staticId"), Operator.STARTS_WITH, "/path/:id", "/path/55" },
                { List.of("/path/:id", "/path/staticId"), Operator.EQUALS, "/path/:id", "/path/55" },
                { List.of("/path/:id", "/path/staticId"), Operator.STARTS_WITH, "/path/staticId", "/path/staticId" },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/staticId",
                    "/path/staticId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.EQUALS,
                    "/path/staticId",
                    "/path/staticId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/staticId",
                    "/path/staticId/secondId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.EQUALS,
                    "/path/:id/secondId",
                    "/path/staticId/secondId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/:id/secondId",
                    "/path/5555/secondId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.EQUALS,
                    "/path/:id/secondId",
                    "/path/5555/secondId",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/:id",
                    "/path/5555",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.EQUALS,
                    "/path/:id",
                    "/path/5555",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/:id/:id2",
                    "/path/5555/5959",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.EQUALS,
                    "/path/:id/:id2",
                    "/path/5555/5959",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2"),
                    Operator.STARTS_WITH,
                    "/path/:id/:id2",
                    "/path/5555/5559/5553",
                },
                { List.of("/path/:id", "/path/:id/secondId"), Operator.EQUALS, "/path/:id/secondId", "/path/5555/secondId" },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2", "/path/:id/subResource/:id2"),
                    Operator.STARTS_WITH,
                    "/path/:id/subResource/:id2",
                    "/path/5555/subResource/5553",
                },
                {
                    List.of("/path/:id", "/path/staticId", "/path/:id/secondId", "/path/:id/:id2", "/path/:id/subResource/:id2"),
                    Operator.EQUALS,
                    "/path/:id/subResource/:id2",
                    "/path/5555/subResource/5553",
                },
                {
                    List.of(
                        "/",
                        "/book",
                        "/book/9999/chapter/145/page/200/line",
                        "/book/9999/chapter/145/page",
                        "/city/washington/street/first/library/amazon/book/9999/chapter/145",
                        "/book/7777/chapter/145",
                        "/book/9999/chapter/147",
                        "/book/9999/chapter/145",
                        "/book/9999/chapter/148",
                        "/book/9999/chapter"
                    ),
                    Operator.STARTS_WITH,
                    "/book/9999/chapter/145",
                    "/book/9999/chapter/145",
                },
                {
                    List.of(
                        "/",
                        "/book",
                        "/book/9999/chapter/145/page/200/line",
                        "/book/9999/chapter/145/page",
                        "/city/washington/street/first/library/amazon/book/9999/chapter/145",
                        "/book/7777/chapter/145",
                        "/book/9999/chapter/147",
                        "/book/9999/chapter/145",
                        "/book/9999/chapter/148",
                        "/book/9999/chapter"
                    ),
                    Operator.EQUALS,
                    "/book/9999/chapter/145",
                    "/book/9999/chapter/145",
                },
                {
                    List.of(
                        "/",
                        "/book",
                        "/book/9999/chapter/145/page/200/line",
                        "/book/9999/chapter/145/page",
                        "/city/washington/street/first/library/amazon/book/9999/chapter/145",
                        "/book/7777/chapter/145",
                        "/book/9999/chapter/147",
                        "/book/9999/chapter/145",
                        "/book/9999/chapter/148",
                        "/book/9999/chapter"
                    ),
                    Operator.STARTS_WITH,
                    "/",
                    "/food",
                },
                {
                    List.of(
                        "/book",
                        "/book/:bookId/chapter/:chapterId/page/:pageId",
                        "/book/:bookId",
                        "/book/:bookId/chapter/:chapterId",
                        "/book/9999/chapter"
                    ),
                    Operator.STARTS_WITH,
                    "/book/9999/chapter",
                    "/book/9999/chapter/145",
                },
                {
                    List.of(
                        "/book",
                        "/book/:bookId/chapter/:chapterId/page/:pageId",
                        "/book/:bookId",
                        "/book/:bookId/chapter/:chapterId",
                        "/book/9999/chapter"
                    ),
                    Operator.EQUALS,
                    "/book/:bookId/chapter/:chapterId",
                    "/book/9999/chapter/145",
                },
                {
                    List.of("/book", "/book/:bookId/chapter/:chapterId/page/:pageId", "/book/:bookId", "/book/:bookId/chapter/:chapterId"),
                    Operator.STARTS_WITH,
                    "/book/:bookId/chapter/:chapterId",
                    "/book/9999/chapter/145",
                },
                {
                    List.of("/book", "/book/:bookId/chapter/:chapterId/page/:pageId", "/book/:bookId", "/book/:bookId/chapter/:chapterId"),
                    Operator.EQUALS,
                    "/book/:bookId/chapter/:chapterId",
                    "/book/9999/chapter/145",
                },
                {
                    List.of("/", "/api_entrypoint/some_path/:test", "/api_entrypoint/some_path/sub_path"),
                    Operator.STARTS_WITH,
                    "/api_entrypoint/some_path/sub_path",
                    "/api_entrypoint/some_path/sub_path",
                },
                {
                    List.of("/", "/api_entrypoint/some_path/:test", "/api_entrypoint/some_path/sub_path"),
                    Operator.STARTS_WITH,
                    "/api_entrypoint/some_path/sub_path",
                    "/api_entrypoint/some_path/sub_path/sub_sub_path",
                },
                {
                    List.of("/", "/api_entrypoint/some_path/:test", "/api_entrypoint/some_path/sub_path"),
                    Operator.EQUALS,
                    "/api_entrypoint/some_path/sub_path",
                    "/api_entrypoint/some_path/sub_path",
                },
                {
                    List.of("/", "/api_entrypoint/some_path/:test", "/api_entrypoint/some_path/sub_path"),
                    Operator.STARTS_WITH,
                    "/api_entrypoint/some_path/:test",
                    "/api_entrypoint/some_path/145",
                },
                {
                    List.of("/", "/api_entrypoint/some_path/:test", "/api_entrypoint/some_path/sub_path"),
                    Operator.STARTS_WITH,
                    "/api_entrypoint/some_path/:test",
                    "/api_entrypoint/some_path/145/sub_sub_path",
                },
                {
                    List.of("/", "/api_entrypoint/some_path/:test", "/api_entrypoint/some_path/sub_path"),
                    Operator.EQUALS,
                    "/api_entrypoint/some_path/:test",
                    "/api_entrypoint/some_path/145",
                },
            }
        );
    }
}
