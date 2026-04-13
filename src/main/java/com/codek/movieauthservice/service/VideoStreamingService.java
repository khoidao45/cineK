package com.codek.movieauthservice.service;

import com.codek.movieauthservice.entity.Movie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoStreamingService {

    private static final int BUFFER_SIZE = 64 * 1024;
    private static final long DEFAULT_CHUNK_SIZE = 2L * 1024 * 1024;

    @Value("${app.video.local-root:videos}")
    private String videoLocalRoot;

    private final MovieService movieService;

    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> streamVideo(Long movieId, String rangeHeader) {
        Movie movie = movieService.findMovieEntityById(movieId);
        String videoUrl = movie.getVideoUrl();

        if (!StringUtils.hasText(videoUrl)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Phim chưa có video");
        }

        log.info("stream.request movieId={} range={} source={}", movieId, rangeHeader, videoUrl);

        if (isHttpUrl(videoUrl)) {
            return streamRemote(videoUrl, rangeHeader);
        }

        return streamLocal(videoUrl, rangeHeader);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> streamVideoUrl(String videoUrl, String rangeHeader) {
        if (!StringUtils.hasText(videoUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "videoUrl không được để trống");
        }

        log.info("stream.request range={} source={}", rangeHeader, videoUrl);

        if (isHttpUrl(videoUrl)) {
            return streamRemote(videoUrl, rangeHeader);
        }

        return streamLocal(videoUrl, rangeHeader);
    }

    private ResponseEntity<StreamingResponseBody> streamLocal(String videoPathValue, String rangeHeader) {
        try {
            Path filePath = resolveLocalPath(videoPathValue);
            if (!Files.exists(filePath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy file video");
            }

            long fileSize = Files.size(filePath);
            ByteRange range = parseRange(rangeHeader, fileSize);
            String contentType = detectContentType(filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(range.length());

            if (range.partial()) {
                headers.set(HttpHeaders.CONTENT_RANGE, contentRangeValue(range.start(), range.end(), fileSize));
            }

            StreamingResponseBody body = outputStream -> {
                try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
                    raf.seek(range.start());
                    long remaining = range.length();
                    byte[] buffer = new byte[BUFFER_SIZE];

                    while (remaining > 0) {
                        int bytesToRead = (int) Math.min(buffer.length, remaining);
                        int read = raf.read(buffer, 0, bytesToRead);
                        if (read == -1) {
                            break;
                        }
                        outputStream.write(buffer, 0, read);
                        remaining -= read;
                    }
                    outputStream.flush();
                }
            };

            return new ResponseEntity<>(body, headers, range.partial() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("stream.local.error path={} message={}", videoPathValue, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi stream video local", ex);
        }
    }

    private ResponseEntity<StreamingResponseBody> streamRemote(String videoUrl, String rangeHeader) {
        HttpURLConnection connection = null;
        try {
            URL url = parseRemoteUrl(videoUrl);
            log.info("stream.remote.connect url={} range={}", videoUrl, rangeHeader);

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(30_000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty(HttpHeaders.ACCEPT, "video/*");
            connection.setRequestProperty(HttpHeaders.ACCEPT_ENCODING, "identity");
            if (StringUtils.hasText(rangeHeader)) {
                connection.setRequestProperty(HttpHeaders.RANGE, rangeHeader);
            }
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy video ở upstream storage");
            }
            if (responseCode >= 500) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upstream video service bị lỗi");
            }
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Upstream trả về mã không hợp lệ: " + responseCode);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT_RANGES, connection.getHeaderField(HttpHeaders.ACCEPT_RANGES) != null
                    ? connection.getHeaderField(HttpHeaders.ACCEPT_RANGES)
                    : "bytes");

            String contentType = connection.getContentType();
            headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "video/mp4"));

            String contentLength = connection.getHeaderField(HttpHeaders.CONTENT_LENGTH);
            if (contentLength != null) {
                headers.set(HttpHeaders.CONTENT_LENGTH, contentLength);
            }

            String contentRange = connection.getHeaderField(HttpHeaders.CONTENT_RANGE);
            if (contentRange != null) {
                headers.set(HttpHeaders.CONTENT_RANGE, contentRange);
            }

            final HttpURLConnection activeConnection = connection;

            StreamingResponseBody body = outputStream -> {
                try (InputStream inputStream = activeConnection.getInputStream()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.flush();
                } finally {
                    activeConnection.disconnect();
                }
            };

            HttpStatus status = responseCode == HttpURLConnection.HTTP_PARTIAL
                    ? HttpStatus.PARTIAL_CONTENT
                    : HttpStatus.OK;

            return new ResponseEntity<>(body, headers, status);
        } catch (ResponseStatusException ex) {
            log.warn("stream.remote.failed url={} status={} message={}", videoUrl, ex.getStatusCode(), ex.getReason());
            if (connection != null) {
                connection.disconnect();
            }
            throw ex;
        } catch (IOException ex) {
            log.error("stream.remote.io-error url={} message={}", videoUrl, ex.getMessage());
            if (connection != null) {
                connection.disconnect();
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không kết nối được upstream video", ex);
        }
    }

    private URL parseRemoteUrl(String videoUrl) {
        try {
            URI uri = URI.create(videoUrl);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "videoUrl phải là http/https");
            }
            return uri.toURL();
        } catch (IllegalArgumentException | MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "videoUrl không hợp lệ", ex);
        }
    }

    private Path resolveLocalPath(String videoPathValue) {
        Path source = Paths.get(videoPathValue);
        if (source.isAbsolute()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đường dẫn video không hợp lệ");
        }
        Path root = Paths.get(videoLocalRoot).toAbsolutePath().normalize();
        Path resolved = root.resolve(videoPathValue).normalize();
        if (!resolved.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đường dẫn video không hợp lệ");
        }
        return resolved;
    }

    private String detectContentType(Path filePath) throws IOException {
        String detected = Files.probeContentType(filePath);
        return detected != null ? detected : "video/mp4";
    }

    private boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private String contentRangeValue(long start, long end, long total) {
        return "bytes " + start + "-" + end + "/" + total;
    }

    private ByteRange parseRange(String rangeHeader, long fileSize) {
        if (!StringUtils.hasText(rangeHeader) || !rangeHeader.startsWith("bytes=")) {
            return new ByteRange(0, fileSize - 1, false);
        }

        String[] parts = rangeHeader.substring("bytes=".length()).split("-", 2);
        try {
            long start;
            long end;

            if (parts[0].isBlank()) {
                long suffixLength = Long.parseLong(parts[1]);
                if (suffixLength <= 0) {
                    throw new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "Range header không hợp lệ");
                }
                start = Math.max(fileSize - suffixLength, 0);
                end = fileSize - 1;
            } else {
                start = Long.parseLong(parts[0]);
                if (parts.length < 2 || parts[1].isBlank()) {
                    end = Math.min(start + DEFAULT_CHUNK_SIZE - 1, fileSize - 1);
                } else {
                    end = Long.parseLong(parts[1]);
                }
            }

            if (start < 0 || end < start || end >= fileSize) {
                throw new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "Range header không hợp lệ");
            }

            return new ByteRange(start, end, true);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "Range header không hợp lệ", ex);
        }
    }

    private record ByteRange(long start, long end, boolean partial) {
        private long length() {
            return end - start + 1;
        }
    }
}
