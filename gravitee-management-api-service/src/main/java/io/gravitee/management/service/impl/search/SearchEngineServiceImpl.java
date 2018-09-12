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
package io.gravitee.management.service.impl.search;

import io.gravitee.management.model.search.Indexable;
import io.gravitee.management.service.search.SearchEngineService;
import io.gravitee.management.service.impl.search.lucene.DocumentSearcher;
import io.gravitee.management.service.impl.search.lucene.DocumentTransformer;
import io.gravitee.management.service.impl.search.lucene.SearchEngineIndexer;
import io.gravitee.repository.exceptions.TechnicalException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SearchEngineServiceImpl implements SearchEngineService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(SearchEngineServiceImpl.class);

    @Autowired
    private SearchEngineIndexer indexer;

    @Autowired
    private Collection<DocumentTransformer> transformers;

    @Autowired
    private Collection<DocumentSearcher> searchers;

    public static void main(String[] args) throws IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException {
        Directory dir = FSDirectory.open(Paths.get("/Users/david/temp/lucene"));

        System.out.println(dir);

        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

        IndexWriter writer = new IndexWriter(dir, iwc);
        writer.commit();

        // make a new, empty document
        Document apiDoc = new Document();

        Field idField = new StringField("id", "3333-2222-3333-4444", Field.Store.YES);
        apiDoc.add(idField);

        Field nameField = new TextField("name", "My API", Field.Store.YES);
        apiDoc.add(nameField);

        Field textField = new TextField("description", "This is simple description", Field.Store.YES);
        apiDoc.add(textField);

        Field primaryOwner = new TextField("owner", "David BRASSELY", Field.Store.YES);
        apiDoc.add(primaryOwner);

        // labels
        apiDoc.add(new StringField("labels", "label1" , Field.Store.YES));
        apiDoc.add(new StringField("labels", "label2" , Field.Store.YES));
        apiDoc.add(new StringField("labels", "label3" , Field.Store.YES));

        // views
        apiDoc.add(new StringField("views", "view1" , Field.Store.YES));
        apiDoc.add(new StringField("views", "view2" , Field.Store.YES));
        apiDoc.add(new StringField("views", "view3" , Field.Store.YES));

        apiDoc.add(new LongPoint("updatedAt", new Date().getTime()));

        System.out.println(apiDoc.toString());
        if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
            // New index, so we just add the document (no old document can be there):

            writer.addDocument(apiDoc);
        } else {
            // Existing index (an old copy of this document may have been indexed) so
            // we use updateDocument instead to replace the old one matching the exact
            // path, if present:
            writer.updateDocument(new Term("id", "1111-2222-3333-4444"), apiDoc);
        }

        // NOTE: if you want to maximize search performance,
        // you can optionally call forceMerge here.  This can be
        // a terribly costly operation, so generally it's only
        // worth it when your index is relatively static (ie
        // you're done adding documents to it):
        //
        writer.commit();
        //writer.forceMerge(1);
        //writer.close();

        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        QueryParser parser = new MultiFieldQueryParser(new String[]{
                "name",
                "description",
                "owner",
                "labels",
                "views"
        }, analyzer);
        parser.setFuzzyMinSim(0.6f);

        Query query = parser.parse("BRASSELi");

        System.out.println("Searching for: " + query.toString());

        final TopDocs topDocs = searcher.search(query,Integer.MAX_VALUE);
        ScoreDoc[] hits = topDocs.scoreDocs;

        int numTotalHits = Math.toIntExact(topDocs.totalHits);
        System.out.println(numTotalHits + " total matching documents");
    }

    @Async
    @Override
    public void index(Indexable source) {
        transformers.stream()
                .filter(transformer -> transformer.handle(source.getClass()))
                .findFirst()
                .ifPresent(transformer -> {
                    try {
                        indexer.index(transformer.transform(source));
                    } catch (TechnicalException te) {
                        logger.error("Unexpected error while indexing a document", te);
                    }
                });

        /*
        // Get the transformer to the object class
        DocumentTransformer<T> transformer = transformers.get(source.getClass());
        if (transformer != null) {

        } else {
            logger.warn("Object {} type[{}] can not be indexed. No transformer found.",
                    source, source.getClass().getName());
        }
        */
    }

    @Async
    @Override
    public void delete(Indexable source) {
        transformers.stream()
                .filter(transformer -> transformer.handle(source.getClass()))
                .findFirst()
                .ifPresent(transformer -> {
                    try {
                        indexer.remove(transformer.transform(source));
                    } catch (TechnicalException te) {
                        logger.error("Unexpected error while deleting a document", te);
                    }
                });

        /*
        // Get the transformer to the object class
        DocumentTransformer<T> transformer = transformers.get(source.getClass());
        if (transformer != null) {

        } else {
            logger.warn("Object {} type[{}] can not be indexed. No transformer found.",
                    source, source.getClass().getName());
        }
        */
    }

    @Override
    public Collection<String> search(io.gravitee.management.service.search.query.Query<? extends Indexable> query) {
        Optional<Collection<String>> results = searchers.stream()
                .filter(searcher -> searcher.handle(query.getRoot()))
                .findFirst()
                .flatMap(searcher -> {
                    try {
                        List<String> ids = searcher.search(query)
                                .stream()
                                .distinct()
                                .collect(Collectors.toList());
                        return Optional.of(ids);
                    } catch (TechnicalException te) {
                        logger.error("Unexpected error while deleting a document", te);
                        return Optional.of(Collections.emptyList());
                    }
                });

        return results.get();
    }
}
