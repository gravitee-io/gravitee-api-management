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
package io.gravitee.rest.api.service.impl.search.lucene.searcher;

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_CUSTOM;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_CUSTOM_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_DISPLAYNAME;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_EMAIL;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_ID_SORTED;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_LASTNAME_FIRSTNAME;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_LASTNAME_FIRSTNAME_SORTED;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_REFERENCE;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.search.query.Query;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollectorManager;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.WildcardQuery;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserDocumentSearcher extends AbstractDocumentSearcher {

    protected static final String FIELD_TYPE_VALUE = "user";

    public UserDocumentSearcher(IndexWriter indexWriter) {
        super(indexWriter);
    }

    @Override
    public SearchResult search(ExecutionContext executionContext, Query<?> query) throws TechnicalException {
        QueryParser parser = new MultiFieldQueryParser(
            new String[] {
                FIELD_DISPLAYNAME,
                FIELD_LASTNAME_FIRSTNAME,
                FIELD_LASTNAME_FIRSTNAME_SORTED,
                FIELD_EMAIL,
                FIELD_REFERENCE,
                FIELD_CUSTOM,
                FIELD_CUSTOM_SPLIT,
            },
            analyzer
        );
        parser.setFuzzyMinSim(0.6f);
        parser.setAllowLeadingWildcard(true);

        BooleanQuery.Builder userQuery = new BooleanQuery.Builder();
        BooleanQuery.Builder userFieldsQuery = new BooleanQuery.Builder();

        try {
            final String normalizedQuery = StringUtils.stripAccents(query.getQuery().toLowerCase());

            if (isUserIdFormat(query)) {
                userFieldsQuery.add(new WildcardQuery(new Term("id", normalizedQuery)), BooleanClause.Occur.MUST);
            } else {
                String[] tokens = normalizedQuery.split(" ");
                for (String token : tokens) {
                    userFieldsQuery
                        .add(new WildcardQuery(new Term(FIELD_DISPLAYNAME, '*' + token + '*')), BooleanClause.Occur.SHOULD)
                        .add(new WildcardQuery(new Term(FIELD_LASTNAME_FIRSTNAME, '*' + token + '*')), BooleanClause.Occur.SHOULD)
                        .add(new WildcardQuery(new Term(FIELD_LASTNAME_FIRSTNAME_SORTED, '*' + token + '*')), BooleanClause.Occur.SHOULD)
                        .add(new WildcardQuery(new Term(FIELD_EMAIL, '*' + token + '*')), BooleanClause.Occur.SHOULD)
                        .add(new WildcardQuery(new Term(FIELD_REFERENCE, '*' + token + '*')), BooleanClause.Occur.SHOULD)
                        .add(new WildcardQuery(new Term(FIELD_CUSTOM, '*' + token + '*')), BooleanClause.Occur.SHOULD)
                        .add(new WildcardQuery(new Term(FIELD_CUSTOM_SPLIT, token)), BooleanClause.Occur.SHOULD);
                }
            }

            userQuery.add(userFieldsQuery.build(), BooleanClause.Occur.MUST);
            userQuery.add(parser.parse(QueryParserBase.escape(query.getQuery())), BooleanClause.Occur.SHOULD);
            userQuery.add(new TermQuery(new Term(FIELD_TYPE, FIELD_TYPE_VALUE)), BooleanClause.Occur.MUST);

            BooleanQuery.Builder orgCriteria = new BooleanQuery.Builder();
            orgCriteria.add(
                new TermQuery(new Term(FIELD_REFERENCE_TYPE, ReferenceContext.Type.ORGANIZATION.name())),
                BooleanClause.Occur.MUST
            );
            orgCriteria.add(new TermQuery(new Term(FIELD_REFERENCE_ID, executionContext.getOrganizationId())), BooleanClause.Occur.MUST);

            userQuery.add(orgCriteria.build(), BooleanClause.Occur.FILTER);

            return search(userQuery.build(), query.getSort(), query.getPage());
        } catch (ParseException pe) {
            logger.error("Invalid query to search for user documents", pe);
            throw new TechnicalException("Invalid query to search for user documents", pe);
        }
    }

    @Override
    protected SearchResult search(org.apache.lucene.search.Query query, Sortable sort, Pageable pageable, String fieldReference)
        throws TechnicalException {
        logger.debug("Searching for: {}", query.toString());

        try {
            IndexSearcher searcher = getIndexSearcher();
            TopDocs topDocs;
            LinkedHashSet<ScoreDoc> collectedDocs;
            final Set<String> results = new LinkedHashSet<>();

            if (pageable != null) {
                Sort effectiveSort = withIdTieBreak(sort != null ? convert(sort) : null);
                // page/size are client-controlled (page has no upper bound -- only size is capped
                // at 200), so page * size can overflow int and, worse, TopFieldCollectorManager
                // -- unlike IndexSearcher.search(query, n, sort) -- does not clamp numHits to the
                // index size: it eagerly allocates a priority queue of that size per search slice.
                // An unbounded `to` therefore risks an OutOfMemoryError on a small index rather
                // than the empty/short page IndexSearcher would have returned. Clamp in long
                // arithmetic to the index size (and at least 1, matching IndexSearcher's own
                // handling of an empty index) before it reaches the collector.
                long window = (long) pageable.getPageNumber() * pageable.getPageSize();
                int to = (int) Math.max(1, Math.min(window, searcher.getIndexReader().maxDoc()));
                // Plain searcher.search(query, to, sort) lets Lucene stop early (block-max WAND)
                // once it's confident of the top `to` hits, which turns totalHits into a lower
                // bound past ~1000 matches instead of an exact count -- silently breaking
                // total_elements for large orgs. Disabling the early-termination threshold here
                // keeps the count exact while still only asking for a bounded window of hits.
                var collectorManager = new TopFieldCollectorManager(effectiveSort, to, null, Integer.MAX_VALUE);
                TopFieldDocs allMatchingDocs = searcher.search(query, collectorManager);
                long offset = (long) (pageable.getPageNumber() - 1) * pageable.getPageSize();
                int from = (int) Math.min(offset, allMatchingDocs.scoreDocs.length);
                int end = Math.min(to, allMatchingDocs.scoreDocs.length);

                topDocs = allMatchingDocs;
                collectedDocs = Arrays.stream(allMatchingDocs.scoreDocs, from, end).collect(LinkedHashSet::new, Set::add, Set::addAll);
            } else if (sort != null) {
                topDocs = searcher.search(query, Integer.MAX_VALUE, withIdTieBreak(convert(sort)));
                collectedDocs = Arrays.stream(topDocs.scoreDocs).collect(LinkedHashSet::new, Set::add, Set::addAll);
            } else {
                topDocs = searcher.search(query, Integer.MAX_VALUE, withIdTieBreak(null));
                collectedDocs = Arrays.stream(topDocs.scoreDocs).collect(LinkedHashSet::new, Set::add, Set::addAll);
            }

            logger.debug("Found {} total matching documents", topDocs.totalHits);
            for (ScoreDoc doc : collectedDocs) {
                String reference = searcher.storedFields().document(doc.doc).get(fieldReference);
                results.add(reference);
            }

            return new SearchResult(results, topDocs.totalHits.value());
        } catch (IOException ioe) {
            logger.error("An error occurs while getting documents from search result", ioe);
            throw new TechnicalException("An error occurs while getting documents from search result", ioe);
        }
    }

    /**
     * Appends a deterministic tie-breaker (the user's own id) to a base sort, or builds one from
     * relevance score alone when no explicit sort was requested. See APIM-15027.
     *
     * Always ties on FIELD_ID_SORTED, the doc-values field UserDocumentTransformer indexes for
     * this purpose -- deriving the field name from the caller's fieldReference instead (e.g.
     * FIELD_REFERENCE_ID via the inherited searchReference()) would silently produce a field with
     * no doc values, degrading the tie-break to a no-op instead of failing loudly.
     */
    private Sort withIdTieBreak(Sort baseSort) {
        SortField idTieBreak = new SortField(FIELD_ID_SORTED, SortField.Type.STRING, false);
        idTieBreak.setMissingValue(SortField.STRING_LAST);
        if (baseSort == null) {
            return new Sort(SortField.FIELD_SCORE, idTieBreak);
        }
        SortField[] existing = baseSort.getSort();
        SortField[] combined = Arrays.copyOf(existing, existing.length + 1);
        combined[existing.length] = idTieBreak;
        return new Sort(combined);
    }

    private boolean isUserIdFormat(io.gravitee.rest.api.service.search.query.Query<?> query) {
        try {
            UUID.fromString(query.getQuery());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return source.isAssignableFrom(UserEntity.class);
    }
}
