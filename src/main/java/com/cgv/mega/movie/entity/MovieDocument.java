package com.cgv.mega.movie.entity;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.Set;
import java.util.stream.Collectors;

@Document(indexName = "movies")
@Setting(settingPath = "/elastic/settings/movie-analyzer.json")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
public class MovieDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "nori_edge_ngram_analyzer")
    private String title;

    @Field(type = FieldType.Keyword)
    private Set<String> genres;

    @Field(type = FieldType.Keyword)
    private Set<String> types;

    @Field(type = FieldType.Keyword)
    private String posterUrl;

    public static MovieDocument from(Movie movie) {
        return MovieDocument.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .genres(movie.getMovieGenres().stream()
                        .map(mg -> mg.getGenre().getName())
                        .collect(Collectors.toSet()))
                .types(movie.getMovieTypes().stream()
                        .map(mt -> mt.getType().name())
                        .collect(Collectors.toSet()))
                .posterUrl(movie.getPosterUrl())
                .build();
    }
}
