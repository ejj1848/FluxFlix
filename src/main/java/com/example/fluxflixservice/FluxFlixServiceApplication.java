package com.example.fluxflixservice;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;


interface MovieRepository extends ReactiveMongoRepository<Movie, String> {

}
@Document
@Data
@NoArgsConstructor
@ToString
class Movie {

    @Id
    private String id;

    private String title;
    private String genre;


    public Movie(String id, String title, String genre) {
        this.id = id;
        this.title = title;
        this.genre = genre;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }
}

@Document
@Data
@NoArgsConstructor
@ToString
class MovieEvent {

    private Movie movie;
    private Date when;
    private String user;

    public MovieEvent(Movie movie, Date when, String user) {
        this.movie = movie;
        this.when = when;
        this.user = user;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Date getWhen() {
        return when;
    }

    public void setWhen(Date when) {
        this.when = when;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
@Service
class FluxFlixService {

    private final MovieRepository movieRepository;

    FluxFlixService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Flux<MovieEvent> streamStreams(Movie movie) {
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));

        Flux<MovieEvent> events = Flux.fromStream(Stream.generate(() -> new MovieEvent(movie, new Date(), randomUser())));

        return Flux.zip(interval, events).map(Tuple2::getT2);

    }

    private String randomUser() {
        String[] users = "Eric,Tony,Bipin,Josh,Louie,Dexter,KittyCat,DoggyDog".split(",");
        return users[new Random().nextInt(users.length)];
    }

    public Flux<Movie> all() {
        return movieRepository.findAll();
    }

    public Mono<Movie> byId(String id) {
        return movieRepository.findById(id);
    }
}

//@RestController
//@RequestMapping("/movie")
//class MovieRestController {
//    private final FluxFlixService fluxFlixService;
//
//    MovieRestController(FluxFlixService fluxFlixService) {
//        this.fluxFlixService = fluxFlixService;
//    }
//
//    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<MovieEvent> event(@PathVariable String id) {
//        return fluxFlixService.byId(id)
//                .flatMapMany(fluxFlixService::streamStreams);
//    }
//
//    @GetMapping
//    public Flux<Movie> all() {
//        return fluxFlixService.all();
//    }
//
//    @GetMapping("/{id}")
//    public Mono<Movie> byId(@PathVariable String id) {
//        return fluxFlixService.byId(id);
//    }
//}

@SpringBootApplication
public class FluxFlixServiceApplication {


    public static void main(String[] args) {
        SpringApplication.run(FluxFlixServiceApplication.class, args);
    }

    @Bean
    RouterFunction<?> routes(FluxFlixService service) {
        return route(RequestPredicates.GET("/movies"),
                serverRequest -> ok().body(service.all(), Movie.class))
                .andRoute(GET("/movies/{id}"), serverRequest -> ok()
                        .body(service.byId(serverRequest.pathVariable("id")), Movie.class))
                .andRoute(GET("/movies/{id}/events"), serverRequest -> ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(service.byId(serverRequest.pathVariable("id"))
                                .flatMapMany(service::streamStreams), MovieEvent.class));

    }

    @Bean
    CommandLineRunner demo(MovieRepository movieRepository) {
        return args -> {

            movieRepository.deleteAll()
                    .subscribe(null, null, () -> Stream.of("Aeon Flux", "Enter the Mono<Void>", "The Fluxinator", "Silence of the Lambdas", "Reactive Mongos on a Plane", "Y tu mono Tambien", "Attack of the Fluxes")
                            .map(title -> new Movie(UUID.randomUUID().toString(), title, randomGenre()))
                            .forEach(movie -> movieRepository.save(movie).subscribe(System.out::println)));

        };
    }

    private String randomGenre() {
        String[] genres = "comdey,horror,romcom,documentary, actrion, drama".split(",");
        return genres[new Random().nextInt(genres.length)];
    }

}



