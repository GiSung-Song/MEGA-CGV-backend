package com.cgv.mega.movie.repository;

import com.cgv.mega.movie.entity.MovieDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieSearchRepository extends ElasticsearchRepository<MovieDocument, Long> {

}