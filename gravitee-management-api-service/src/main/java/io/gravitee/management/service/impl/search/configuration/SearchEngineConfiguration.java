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
package io.gravitee.management.service.impl.search.configuration;

import io.gravitee.management.service.impl.search.lucene.SearchEngineIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class SearchEngineConfiguration {

    @Value("${search.data:${gravitee.home}/data}")
    private String indexDirectory;

    @Bean
    public SearchEngineIndexer searchEngineIndexer() {
        return new SearchEngineIndexer();
    }

    @Bean
    public Directory indexDirectory() throws IOException {
        Path path = Paths.get(indexDirectory);
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
        }

        return FSDirectory.open(path);
    }

    @Bean
    public IndexWriter indexWriter(Directory directory, Analyzer analyzer) throws IOException {
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        return new IndexWriter(directory, iwc);
    }

    @Bean
    public Analyzer analyzer() {
        return new StandardAnalyzer();
    }
}
