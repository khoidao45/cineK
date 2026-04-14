package com.codek.movieauthservice.config;

import com.codek.movieauthservice.entity.Movie;
import com.codek.movieauthservice.entity.Role;
import com.codek.movieauthservice.entity.User;
import com.codek.movieauthservice.repository.MovieRepository;
import com.codek.movieauthservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds sample movies and test accounts on first startup.
 * Safe to run multiple times — skips seeding if data already exists.
 *
 * Test accounts:
 *   ADMIN  → admin / Admin@123
 *   USER   → testuser / User@123
 */
@Component
@Order(2) // run after DatabaseSchemaInitializer (Order 1 by default)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final MovieRepository movieRepository;
    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUsers();
        seedMovies();
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("seed.users.skip — already exist");
            return;
        }

        userRepository.saveAll(List.of(
            User.builder()
                .username("admin")
                .email("admin@cinek.dev")
                .password(passwordEncoder.encode("Admin@123"))
                .fullName("Administrator")
                .avatarUrl("")
                .role(Role.ADMIN)
                .active(true)
                .emailVerified(true)
                .provider("LOCAL")
                .build(),

            User.builder()
                .username("testuser")
                .email("user@cinek.dev")
                .password(passwordEncoder.encode("User@123"))
                .fullName("Test User")
                .avatarUrl("")
                .role(Role.USER)
                .active(true)
                .emailVerified(true)
                .provider("LOCAL")
                .build()
        ));
        log.info("seed.users.done — created admin + testuser");
    }

    // ── Movies ────────────────────────────────────────────────────────────────

    private void seedMovies() {
        if (movieRepository.count() > 0) {
            log.info("seed.movies.skip — already exist");
            return;
        }

        List<Movie> movies = List.of(
            movie("Inception",
                "Một tên trộm có khả năng xâm nhập vào giấc mơ của người khác bị giao nhiệm vụ cấy ghép một ý tưởng.",
                "Sci-Fi", 148, 2010,
                "https://m.media-amazon.com/images/M/MV5BMjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwNTI5OTM0Mw@@._V1_SX300.jpg",
                "Christopher Nolan", "Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page"),

            movie("The Dark Knight",
                "Batman đối đầu với Joker — kẻ tội phạm hỗn loạn muốn đẩy Gotham vào tình trạng vô chính phủ.",
                "Action", 152, 2008,
                "https://m.media-amazon.com/images/M/MV5BMTMxNTMwODM0NF5BMl5BanBnXkFtZTcwODAyMTk2Mw@@._V1_SX300.jpg",
                "Christopher Nolan", "Christian Bale, Heath Ledger, Aaron Eckhart"),

            movie("Interstellar",
                "Nhóm phi hành gia du hành qua lỗ sâu đục để tìm kiếm ngôi nhà mới cho nhân loại.",
                "Sci-Fi", 169, 2014,
                "https://m.media-amazon.com/images/M/MV5BZjdkOTU3MDktN2IxOS00OGEyLWFmMjktY2FiMmZkNWIyODZiXkEyXkFqcGdeQXVyMTMxODk2OTU@._V1_SX300.jpg",
                "Christopher Nolan", "Matthew McConaughey, Anne Hathaway, Jessica Chastain"),

            movie("Pulp Fiction",
                "Nhiều câu chuyện tội phạm đan xen nhau tại Los Angeles — bạo lực, hài hước và phi tuyến tính.",
                "Thriller", 154, 1994,
                "https://m.media-amazon.com/images/M/MV5BNGNhMDIzZTUtNTBlZi00MTRlLWFjM2ItYzViMjE3YzI5MjljXkEyXkFqcGdeQXVyNzkwMjQ5NzM@._V1_SX300.jpg",
                "Quentin Tarantino", "John Travolta, Samuel L. Jackson, Uma Thurman"),

            movie("The Shawshank Redemption",
                "Một nhân viên ngân hàng bị kết án oan vào tù và xây dựng tình bạn với một tù nhân kỳ cựu.",
                "Drama", 142, 1994,
                "https://m.media-amazon.com/images/M/MV5BNDE3ODcxYzMtY2YzZC00NiYyLTg3MzMtYTJmNjk3NTQ5MWU3XkEyXkFqcGdeQXVyNjAwNDUxODI@._V1_SX300.jpg",
                "Frank Darabont", "Tim Robbins, Morgan Freeman"),

            movie("Forrest Gump",
                "Cuộc đời kỳ lạ của một người đàn ông IQ thấp nhưng trái tim thuần khiết — nhân chứng của lịch sử Mỹ.",
                "Drama", 142, 1994,
                "https://m.media-amazon.com/images/M/MV5BNWIwODRlZTUtY2U3ZS00Yzg1LWJhNzYtMmZiYmEyNmU1NjMzXkEyXkFqcGdeQXVyMTQxNzMzNDI@._V1_SX300.jpg",
                "Robert Zemeckis", "Tom Hanks, Robin Wright, Gary Sinise"),

            movie("The Matrix",
                "Một hacker khám phá ra rằng thực tại mình đang sống chỉ là một mô phỏng máy tính.",
                "Sci-Fi", 136, 1999,
                "https://m.media-amazon.com/images/M/MV5BNzQzOTk3OTAtNDQ0Zi00ZTVlLTM5YTgtZjNmM2RkNzc2ZjZiXkEyXkFqcGdeQXVyNjU0OTQ0OTY@._V1_SX300.jpg",
                "The Wachowskis", "Keanu Reeves, Laurence Fishburne, Carrie-Anne Moss"),

            movie("Avengers: Endgame",
                "Các siêu anh hùng còn sống tập hợp để đảo ngược hành động của Thanos và khôi phục vũ trụ.",
                "Action", 181, 2019,
                "https://m.media-amazon.com/images/M/MV5BMTc5MDE2ODcwNV5BMl5BanBnXkFtZTgwMzI2NzQ2NzM@._V1_SX300.jpg",
                "Anthony & Joe Russo", "Robert Downey Jr., Chris Evans, Mark Ruffalo"),

            movie("Parasite",
                "Gia đình nghèo dần xâm nhập vào cuộc sống của một gia đình giàu có theo cách bất ngờ.",
                "Thriller", 132, 2019,
                "https://m.media-amazon.com/images/M/MV5BYWZjMjk3ZTItODQ2ZC00NTY5LWE0ZDYtZTI3MjcwN2Q5NTVkXkEyXkFqcGdeQXVyODk4OTc3MTY@._V1_SX300.jpg",
                "Bong Joon-ho", "Song Kang-ho, Lee Sun-kyun, Cho Yeo-jeong"),

            movie("Spirited Away",
                "Cô bé Chihiro bị mắc kẹt trong thế giới thần linh và phải làm việc để giải cứu cha mẹ.",
                "Animation", 125, 2001,
                "https://m.media-amazon.com/images/M/MV5BMjlmZmI5MDctNDE2YS00YWE0LWE5ZWItZDBhYWQ0NTcxNWRhXkEyXkFqcGdeQXVyMTMxODk2OTU@._V1_SX300.jpg",
                "Hayao Miyazaki", "Daveigh Chase, Suzanne Pleshette"),

            movie("Titanic",
                "Chuyện tình lãng mạn bi thảm giữa hai người thuộc tầng lớp khác nhau trên con tàu định mệnh.",
                "Romance", 195, 1997,
                "https://m.media-amazon.com/images/M/MV5BMDdmZGU3NDQtY2E5My00ZTliLWIzOTUtMTY4ZGI1YjdiNjk3XkEyXkFqcGdeQXVyNTA4NzY1MzY@._V1_SX300.jpg",
                "James Cameron", "Leonardo DiCaprio, Kate Winslet"),

            movie("The Godfather",
                "Câu chuyện về gia đình mafia Corleone và sự kế thừa quyền lực đẫm máu.",
                "Drama", 175, 1972,
                "https://m.media-amazon.com/images/M/MV5BM2MyNjYxNmUtYTAwNi00MTYxLWJmNWYtYzZlODY3ZTk3OTFlXkEyXkFqcGdeQXVyNzkwMjQ5NzM@._V1_SX300.jpg",
                "Francis Ford Coppola", "Marlon Brando, Al Pacino, James Caan"),

            movie("Whiplash",
                "Một tay trống trẻ đầy tham vọng chịu đựng huấn luyện khắc nghiệt từ người thầy hà khắc.",
                "Drama", 107, 2014,
                "https://m.media-amazon.com/images/M/MV5BOTA5NDZlZGUtMjAxOS00YTRkLTkwYmMtYWQ0NWEwZDZiNjEzXkEyXkFqcGdeQXVyMTMxODk2OTU@._V1_SX300.jpg",
                "Damien Chazelle", "Miles Teller, J.K. Simmons"),

            movie("The Grand Budapest Hotel",
                "Câu chuyện phiêu lưu hài hước về người quản lý khách sạn nổi tiếng và người học việc của ông.",
                "Comedy", 99, 2014,
                "https://m.media-amazon.com/images/M/MV5BMzM5NjUxOTEyMl5BMl5BanBnXkFtZTgwNjEyMDM0MDE@._V1_SX300.jpg",
                "Wes Anderson", "Ralph Fiennes, F. Murray Abraham, Mathieu Amalric"),

            movie("Joker",
                "Nguồn gốc của gã hề điên loạn — từ một người đàn ông thất bại trở thành biểu tượng hỗn loạn.",
                "Thriller", 122, 2019,
                "https://m.media-amazon.com/images/M/MV5BNGVjNWI4ZGUtNzE0MS00YTJmLWE0ZDctN2ZiYTk2YmI3NTYyXkEyXkFqcGdeQXVyMTkxNjUyNQ@@._V1_SX300.jpg",
                "Todd Phillips", "Joaquin Phoenix, Robert De Niro, Zazie Beetz")
        );

        movieRepository.saveAll(movies);
        log.info("seed.movies.done — created {} movies", movies.size());
    }

    private Movie movie(String title, String description, String genre,
                        int duration, int releaseYear,
                        String posterUrl, String director, String actors) {
        return Movie.builder()
                .title(title)
                .description(description)
                .genre(genre)
                .duration(duration)
                .releaseYear(releaseYear)
                .posterUrl(posterUrl)
                .director(director)
                .actors(actors)
                .views(0L)
                .ratingAvg(0D)
                .ratingCount(0L)
                .deleted(false)
                .build();
    }
}
