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
package io.gravitee.apim.infra.query_service.cluster;

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_CLUSTERS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_ID;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_ID_SORTED;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_NAME_LOWERCASE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_NAME_SORTED;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TYPE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TYPE_VALUE;

import io.gravitee.apim.core.cluster.query_service.ClusterApiSearchQueryService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Service;

/**
 * Pages in the API search index: Lucene keeps only the hits up to the end of the requested page, and counts the rest,
 * so a page never loads the ids of every matching API.
 */
@Service
public class ClusterApiSearchQueryServiceImpl implements ClusterApiSearchQueryService {

    private static final Sort BY_NAME_THEN_ID = new Sort(
        new SortField(FIELD_NAME_SORTED, SortField.Type.STRING),
        new SortField(FIELD_ID_SORTED, SortField.Type.STRING)
    );

    private final IndexWriter indexWriter;

    public ClusterApiSearchQueryServiceImpl(IndexWriter indexWriter) {
        this.indexWriter = indexWriter;
    }

    @Override
    public Page<String> search(String environmentId, Collection<String> clusterCrossIds, String name, Pageable pageable) {
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        if (clusterCrossIds.isEmpty() || pageSize <= 0) {
            return new Page<>(List.of(), pageNumber, 0, 0);
        }
        Query query = query(environmentId, clusterCrossIds, name);
        try (DirectoryReader reader = DirectoryReader.open(indexWriter)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            int total = searcher.count(query);
            long from = (long) (pageNumber - 1) * pageSize;
            if (from >= total) {
                return new Page<>(List.of(), pageNumber, 0, total);
            }
            ScoreDoc[] hits = searcher.search(query, (int) from + pageSize, BY_NAME_THEN_ID).scoreDocs;
            StoredFields storedFields = searcher.storedFields();
            List<String> ids = new ArrayList<>();
            for (int i = (int) from; i < hits.length; i++) {
                ids.add(storedFields.document(hits[i].doc).get(FIELD_ID));
            }
            return new Page<>(ids, pageNumber, ids.size(), total);
        } catch (IOException e) {
            throw new TechnicalDomainException("An error occurred while searching the APIs using clusters " + clusterCrossIds, e);
        }
    }

    private static Query query(String environmentId, Collection<String> clusterCrossIds, String name) {
        // A term set, not a SHOULD clause per cross id: no limit on the number of virtual clusters.
        Query clusters = new TermInSetQuery(FIELD_CLUSTERS, clusterCrossIds.stream().map(BytesRef::new).toList());
        BooleanQuery.Builder query = new BooleanQuery.Builder()
            .add(new TermQuery(new Term(FIELD_TYPE, FIELD_TYPE_VALUE)), BooleanClause.Occur.FILTER)
            .add(
                new TermQuery(new Term(DocumentTransformer.FIELD_REFERENCE_TYPE, ReferenceContext.Type.ENVIRONMENT.name())),
                BooleanClause.Occur.FILTER
            )
            .add(new TermQuery(new Term(DocumentTransformer.FIELD_REFERENCE_ID, environmentId)), BooleanClause.Occur.FILTER)
            .add(clusters, BooleanClause.Occur.FILTER);
        if (name != null && !name.isBlank()) {
            // Lower-cased as the index lower-cases name_lowercase.
            String contains = "*" + escapeWildcards(name.trim().toLowerCase()) + "*";
            query.add(new WildcardQuery(new Term(FIELD_NAME_LOWERCASE, contains)), BooleanClause.Occur.FILTER);
        }
        return query.build();
    }

    private static String escapeWildcards(String value) {
        StringBuilder escaped = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c == '*' || c == '?' || c == WildcardQuery.WILDCARD_ESCAPE) {
                escaped.append(WildcardQuery.WILDCARD_ESCAPE);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
}
