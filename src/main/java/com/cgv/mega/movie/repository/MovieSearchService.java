package com.cgv.mega.movie.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.cgv.mega.movie.entity.MovieDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<MovieDocument> searchByTitle(String keyword, Pageable pageable) {
        NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q
                            .match(m -> m
                                    .field("title")
                                    .query(keyword)
                                    .operator(Operator.Or)
                            )
                    )
                    .withPageable(pageable)
                    .build();

        SearchHits<MovieDocument> searchHits = elasticsearchOperations.search(query, MovieDocument.class);

        SearchPage<MovieDocument> searchPage = SearchHitSupport.searchPageFor(searchHits, pageable);

        return searchPage.map(SearchHit::getContent);
    }
}
