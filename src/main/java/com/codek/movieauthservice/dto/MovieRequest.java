package com.codek.movieauthservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String description;

    @NotBlank(message = "Thể loại không được để trống")
    private String genre;

    @Min(value = 1, message = "Thời lượng phải lớn hơn 0")
    private int duration;

    @Min(value = 1888, message = "Năm phát hành không hợp lệ")
    private int releaseYear;

    private String posterUrl;
}
