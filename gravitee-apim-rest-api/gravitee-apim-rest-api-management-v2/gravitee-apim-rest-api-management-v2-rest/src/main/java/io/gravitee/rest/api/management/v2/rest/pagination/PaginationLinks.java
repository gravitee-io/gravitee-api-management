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
package io.gravitee.rest.api.management.v2.rest.pagination;

import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import jakarta.ws.rs.core.MultivaluedMap;
import java.net.URI;

public class PaginationLinks {

    private PaginationLinks() {}

    public static Links computePaginationLinks(
        URI requestUri,
        MultivaluedMap<String, String> queryParameters,
        long totalElements,
        PaginationParam paginationParam
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / paginationParam.getPerPage());
        if (totalElements == 0 || totalPages == 1) {
            return new Links().self(requestUri.toString());
        }

        if (paginationParam.getPage() <= 0 || paginationParam.getPage() > totalPages) {
            return null;
        }

        final String pageToken = "{page}";
        final String perPageToken = "{perPage}";
        String linkTemplate = requestUri.toString();

        if (queryParameters.isEmpty()) {
            linkTemplate += "?" + PaginationParam.PAGE_QUERY_PARAM_NAME + "=" + pageToken;
        } else {
            final String queryPage = queryParameters.getFirst(PaginationParam.PAGE_QUERY_PARAM_NAME);
            final String queryPerPage = queryParameters.getFirst(PaginationParam.PER_PAGE_QUERY_PARAM_NAME);

            if (queryPage != null) {
                linkTemplate = linkTemplate.replaceFirst(
                    PaginationParam.PAGE_QUERY_PARAM_NAME + "=(\\w*)",
                    PaginationParam.PAGE_QUERY_PARAM_NAME + "=" + pageToken
                );
            } else {
                linkTemplate += "&" + PaginationParam.PAGE_QUERY_PARAM_NAME + "=" + pageToken;
            }
            if (queryPerPage != null) {
                linkTemplate = linkTemplate.replaceFirst(
                    PaginationParam.PER_PAGE_QUERY_PARAM_NAME + "=(\\w*)",
                    PaginationParam.PER_PAGE_QUERY_PARAM_NAME + "=" + perPageToken
                );
            }
        }

        Integer firstPage = 1;
        Integer lastPage = totalPages;
        Integer nextPage = Math.min(paginationParam.getPage() + 1, lastPage);
        Integer prevPage = Math.max(firstPage, paginationParam.getPage() - 1);
        String perPageAsString = String.valueOf(paginationParam.getPerPage());
        Links paginatedLinks = new Links()
            .first(linkTemplate.replace(pageToken, String.valueOf(firstPage)).replace(perPageToken, perPageAsString))
            .last(linkTemplate.replace(pageToken, String.valueOf(lastPage)).replace(perPageToken, perPageAsString))
            .next(linkTemplate.replace(pageToken, String.valueOf(nextPage)).replace(perPageToken, perPageAsString))
            .previous(linkTemplate.replace(pageToken, String.valueOf(prevPage)).replace(perPageToken, perPageAsString))
            .self(requestUri.toString());

        if (paginationParam.getPage() == 1) {
            paginatedLinks.setPrevious(null);
        } else if (paginationParam.getPage().equals(totalPages)) {
            paginatedLinks.setNext(null);
        }
        return paginatedLinks;
    }
}
