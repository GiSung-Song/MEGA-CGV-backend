package com.cgv.mega.movie.repository;

import com.cgv.mega.movie.entity.MovieDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieSearchRepository extends ElasticsearchRepository<MovieDocument, Long> {

    @Query("""
    {
        "match": {
            "title": {
                "query": "?0",
                "operator": "and"
            }
        }
    }
    """)
    Page<MovieDocument> searchByTitle(String keyword, Pageable pageable);
}