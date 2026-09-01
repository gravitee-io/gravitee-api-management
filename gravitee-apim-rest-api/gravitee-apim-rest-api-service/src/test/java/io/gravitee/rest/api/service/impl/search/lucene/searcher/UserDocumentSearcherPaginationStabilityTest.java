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

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_ID;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_LASTNAME_FIRSTNAME;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_TYPE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer.FIELD_TYPE_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.common.SortableImpl;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.UserDocumentTransformer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.Test;

/**
 * Regression test for APIM-15027 (regression of #11311 / #10744): paginating the users
 * search (?q=...) must return a deterministic, complete result set regardless of the order
 * in which the underlying Lucene index happened to be built -- which is what differs between
 * Management API nodes in a clustered deployment, since each node builds its own local index
 * from the same data.
 *
 * @author GraviteeSource Team
 */
class UserDocumentSearcherPaginationStabilityTest {

    private static final int USER_COUNT = 40;
    private static final int PAGE_SIZE = 10;

    /**
     * Builds a Lucene index containing the given users, indexed in the given order, and
     * returns a UserDocumentSearcher backed by it. Two calls to this method with the same
     * users but a different insertion order simulate two Management API nodes that each
     * built their own local index independently from the same underlying data -- exactly
     * the scenario described in APIM-15027.
     */
    private UserDocumentSearcher indexUsersAndBuildSearcher(List<UserEntity> usersInInsertionOrder) throws IOException {
        var directory = new ByteBuffersDirectory();
        var indexWriter = new IndexWriter(directory, new IndexWriterConfig(new StandardAnalyzer()));
        var transformer = new UserDocumentTransformer();

        for (UserEntity user : usersInInsertionOrder) {
            indexWriter.addDocument(transformer.transform(user));
        }
        indexWriter.commit();

        return new UserDocumentSearcher(indexWriter);
    }

    private List<UserEntity> someUsersWithACommonPrefix() {
        return IntStream.range(0, USER_COUNT)
            .mapToObj(i ->
                UserEntity.builder()
                    .id("user-" + i)
                    .lastname("svc-ci-local-" + i)
                    .email("svc-ci-local-" + i + "@example.com")
                    .source("gravitee")
                    .sourceId("svc-ci-local-" + i)
                    .build()
            )
            .collect(Collectors.toList());
    }

    /**
     * Users that all share the exact same lastname/firstname, so the "browse with no search
     * term" sort (lastname_firstname) ties for every one of them -- this is what's needed to
     * exercise the branch of withIdTieBreak that appends the id tie-breaker onto an existing,
     * caller-supplied Sort, rather than building a fresh score-based one.
     */
    private List<UserEntity> someUsersThatAllTieOnTheBrowsingSort() {
        return IntStream.range(0, USER_COUNT)
            .mapToObj(i ->
                UserEntity.builder().id("user-" + i).firstname("John").lastname("Doe").email("john.doe." + i + "@example.com").build()
            )
            .collect(Collectors.toList());
    }

    /** All documents are "type:user" with no other differentiating query term, so they tie on relevance score. */
    private Query matchAllUsersQuery() {
        return new TermQuery(new Term(FIELD_TYPE, FIELD_TYPE_VALUE));
    }

    private List<String> walkAllPages(UserDocumentSearcher searcher, Query query, long totalHits) throws Exception {
        return walkAllPages(searcher, query, null, totalHits);
    }

    private List<String> walkAllPages(UserDocumentSearcher searcher, Query query, Sortable sort, long totalHits) throws Exception {
        List<String> collected = new ArrayList<>();
        int pageCount = (int) Math.ceil((double) totalHits / PAGE_SIZE);
        for (int page = 1; page <= pageCount; page++) {
            SearchResult result = searcher.search(query, sort, new PageableImpl(page, PAGE_SIZE), FIELD_ID);
            collected.addAll(result.getDocuments());
        }
        return collected;
    }

    @Test
    void should_return_the_same_page_1_regardless_of_the_order_the_index_was_built_in() throws Exception {
        // Given the same users, but indexed in two different orders -- simulating two
        // Management API nodes that each built their own local index independently.
        List<UserEntity> users = someUsersWithACommonPrefix();
        List<UserEntity> shuffledUsers = new ArrayList<>(users);
        Collections.reverse(shuffledUsers);

        UserDocumentSearcher nodeA = indexUsersAndBuildSearcher(users);
        UserDocumentSearcher nodeB = indexUsersAndBuildSearcher(shuffledUsers);

        Query query = matchAllUsersQuery();

        // When asking both nodes for "page 1" of the exact same search
        SearchResult page1FromNodeA = nodeA.search(query, null, new PageableImpl(1, PAGE_SIZE), FIELD_ID);
        SearchResult page1FromNodeB = nodeB.search(query, null, new PageableImpl(1, PAGE_SIZE), FIELD_ID);

        // Then both nodes must agree on exactly who is on page 1 -- before the fix, this
        // failed because relevance ties were broken by each index's own internal document
        // order, which differs when documents were added in a different order.
        assertThat(page1FromNodeA.getDocuments()).containsExactlyElementsOf(page1FromNodeB.getDocuments());
    }

    @Test
    void should_return_every_user_exactly_once_when_walking_all_pages() throws Exception {
        List<UserEntity> users = someUsersWithACommonPrefix();
        UserDocumentSearcher searcher = indexUsersAndBuildSearcher(users);
        Query query = matchAllUsersQuery();

        SearchResult firstPage = searcher.search(query, null, new PageableImpl(1, PAGE_SIZE), FIELD_ID);
        List<String> allIdsAcrossAllPages = walkAllPages(searcher, query, firstPage.getHits());

        // total_elements must be honoured, but more importantly: no user should be
        // duplicated across pages, and none should be missing.
        assertThat(firstPage.getHits()).isEqualTo(USER_COUNT);
        assertThat(allIdsAcrossAllPages).hasSize(USER_COUNT);
        assertThat(allIdsAcrossAllPages).doesNotHaveDuplicates();
        assertThat(allIdsAcrossAllPages).containsExactlyInAnyOrderElementsOf(
            users.stream().map(UserEntity::getId).collect(Collectors.toList())
        );
    }

    @Test
    void should_return_identical_sweeps_across_repeated_full_pagination_walks() throws Exception {
        // Regression detail from the original report: the *set* of missing/duplicated users
        // shifted on every sweep. Walking all pages three times in a row must yield the
        // exact same result every time.
        List<UserEntity> users = someUsersWithACommonPrefix();
        UserDocumentSearcher searcher = indexUsersAndBuildSearcher(users);
        Query query = matchAllUsersQuery();

        List<String> sweep1 = walkAllPages(searcher, query, USER_COUNT);
        List<String> sweep2 = walkAllPages(searcher, query, USER_COUNT);
        List<String> sweep3 = walkAllPages(searcher, query, USER_COUNT);

        assertThat(sweep1).containsExactlyElementsOf(sweep2);
        assertThat(sweep2).containsExactlyElementsOf(sweep3);
    }

    @Test
    void should_break_ties_by_id_when_an_explicit_sort_is_provided() throws Exception {
        List<UserEntity> users = someUsersThatAllTieOnTheBrowsingSort();
        List<UserEntity> shuffledUsers = new ArrayList<>(users);
        Collections.reverse(shuffledUsers);

        UserDocumentSearcher nodeA = indexUsersAndBuildSearcher(users);
        UserDocumentSearcher nodeB = indexUsersAndBuildSearcher(shuffledUsers);

        Query query = matchAllUsersQuery();
        Sortable browsingSort = new SortableImpl(UserDocumentTransformer.FIELD_LASTNAME_FIRSTNAME, true);

        // Both nodes must agree on page 1 despite the index being built in a different order.
        SearchResult page1FromNodeA = nodeA.search(query, browsingSort, new PageableImpl(1, PAGE_SIZE), FIELD_ID);
        SearchResult page1FromNodeB = nodeB.search(query, browsingSort, new PageableImpl(1, PAGE_SIZE), FIELD_ID);
        assertThat(page1FromNodeA.getDocuments()).containsExactlyElementsOf(page1FromNodeB.getDocuments());

        // And walking every page must still return every user exactly once.
        List<String> allIdsAcrossAllPages = walkAllPages(nodeA, query, browsingSort, USER_COUNT);
        assertThat(allIdsAcrossAllPages).hasSize(USER_COUNT);
        assertThat(allIdsAcrossAllPages).doesNotHaveDuplicates();
        assertThat(allIdsAcrossAllPages).containsExactlyInAnyOrderElementsOf(
            users.stream().map(UserEntity::getId).collect(Collectors.toList())
        );
    }

    /**
     * A bare type:user TermQuery makes every document score identically, so block-max WAND has
     * nothing non-competitive to skip and totalHits comes back exact even without the fix --
     * this cannot distinguish pre-fix from post-fix behaviour. Giving a minority of users an
     * extra matching token, and querying with SHOULD clauses over it, produces the varying
     * per-document scores the real user query builds (a BooleanQuery of SHOULD wildcard
     * clauses), which is what actually triggers early termination.
     */
    @Test
    void should_report_the_exact_total_when_more_than_1000_users_match() throws Exception {
        // Lucene can stop counting early (block-max WAND) once it's confident of the top N hits
        // for a small, bounded, score-sorted request -- turning totalHits into a lower bound
        // rather than an exact count once matches exceed roughly 1000. This must not regress
        // total_elements for large organisations, even though we now ask for a bounded window of
        // hits (page * size) instead of Integer.MAX_VALUE.
        int largeUserCount = 2500;
        List<UserEntity> users = IntStream.range(0, largeUserCount)
            .mapToObj(i -> {
                // Every 250th user gets an extra "admin" token, so scores vary across the
                // index instead of tying -- see the class-level javadoc above for why.
                // firstname must be set too: UserDocumentTransformer only indexes
                // FIELD_LASTNAME_FIRSTNAME (what the query below matches on) when both
                // lastname and firstname are non-empty.
                String lastname = (i % 250 == 0) ? "svc-ci-local-admin-" + i : "svc-ci-local-" + i;
                return UserEntity.builder()
                    .id("user-" + i)
                    .firstname("ci")
                    .lastname(lastname)
                    .email("svc-ci-local-" + i + "@example.com")
                    .source("gravitee")
                    .sourceId("svc-ci-local-" + i)
                    .build();
            })
            .collect(Collectors.toList());
        UserDocumentSearcher searcher = indexUsersAndBuildSearcher(users);

        // FIELD_LASTNAME_FIRSTNAME is indexed as a single un-split token (SingleTokenTokenizerFactory,
        // mirroring how the real user query matches it), so it must be queried with wildcards
        // rather than TermQuery, exactly as UserDocumentSearcher.searchQuery() itself does.
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        queryBuilder.add(new TermQuery(new Term(FIELD_TYPE, FIELD_TYPE_VALUE)), BooleanClause.Occur.MUST);
        queryBuilder.add(new WildcardQuery(new Term(FIELD_LASTNAME_FIRSTNAME, "*local*")), BooleanClause.Occur.SHOULD);
        queryBuilder.add(new WildcardQuery(new Term(FIELD_LASTNAME_FIRSTNAME, "*admin*")), BooleanClause.Occur.SHOULD);
        Query query = queryBuilder.build();

        // A small, bounded page request -- exactly the shape that triggers early termination.
        SearchResult firstPage = searcher.search(query, null, new PageableImpl(1, PAGE_SIZE), FIELD_ID);
        assertThat(firstPage.getHits()).isEqualTo(largeUserCount);
        assertThat(firstPage.getDocuments()).hasSize(PAGE_SIZE);

        // A deep page request, which would also have caught an unclamped/overflowing `to`.
        SearchResult deepPage = searcher.search(query, null, new PageableImpl(200, PAGE_SIZE), FIELD_ID);
        assertThat(deepPage.getHits()).isEqualTo(largeUserCount);
    }
}
