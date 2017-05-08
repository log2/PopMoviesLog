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

    String BASE_URL = "http://api.themoviedb.org/3/";

    @GET("movie/popular/")
    Call<MovieListResponse> getPopularMovies(@Query("page") int page, @Query("api_key") String apiKey);

    @GET("movie/top_rated/")
    Call<MovieListResponse> getTopRatedMovies(@Query("page") int page, @Query("api_key") String apiKey);

    @GET("movie/{id}")
    Call<Movie> getMovie(@Path("id") String id, @Query("api_key") String apiKey);

    @GET("movie/popular/")
    Call<MovieCount> getPopularMoviesCount(@Query("api_key") String apiKey);

    @GET("movie/top_rated/")
    Call<MovieCount> getTopRatedMoviesCount(@Query("api_key") String apiKey);

    @GET("movie/{id}/reviews")
    Call<ReviewListResponse> getReviewsForMovie(@Path("id") String id, @Query("api_key") String apiKey);

    @GET("movie/{id}/videos")
    Call<TrailerListResponse> getTrailersForMovie(@Path("id") String id, @Query("api_key") String apiKey);
}