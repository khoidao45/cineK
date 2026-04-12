package com.codek.movieauthservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    private String description;

    @NotBlank(message = "Thể loại không được để trống")
    @Size(max = 100, message = "Thể loại tối đa 100 ký tự")
    private String genre;

    @Min(value = 1, message = "Thời lượng phải lớn hơn 0")
    private int duration;

    @Min(value = 1888, message = "Năm phát hành không hợp lệ")
    private int releaseYear;

    @Size(max = 500, message = "posterUrl tối đa 500 ký tự")
    private String posterUrl;

    @Size(max = 500, message = "thumbnailUrl tối đa 500 ký tự")
    private String thumbnailUrl;

    @Size(max = 1000, message = "videoUrl tối đa 1000 ký tự")
    private String videoUrl;

    @Size(max = 255, message = "director tối đa 255 ký tự")
    private String director;

    private String actors;
}
