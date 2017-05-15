package com.example.log2.popmovies.network;

import com.example.log2.popmovies.model.Movie;
import com.example.log2.popmovies.model.MovieCount;
import com.example.log2.popmovies.model.MovieListResponse;
import com.example.log2.popmovies.model.ReviewListResponse;
import com.example.log2.popmovies.model.TrailerListResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TheMovieDbService {

    @SuppressWarnings("HardCodedStringLiteral")
    String BASE_URL = "http://api.themoviedb.org/3/";

    @GET("movie/{sort}/")
    Call<MovieListResponse> getMovies(@Path("sort") String sort, @Query("page") int page, @Query("api_key") String apiKey);

    @GET("movie/{id}")
    Call<Movie> getMovie(@Path("id") String id, @Query("api_key") String apiKey);

    @GET("movie/{sort}/")
    Call<MovieCount> getMoviesCount(@Path("sort") String sort, @Query("api_key") String apiKey);

    @GET("movie/{id}/reviews")
    Call<ReviewListResponse> getReviewsForMovie(@Path("id") String id, @Query("api_key") String apiKey);

    @GET("movie/{id}/videos")
    Call<TrailerListResponse> getTrailersForMovie(@Path("id") String id, @Query("api_key") String apiKey);
}