package com.codek.movieauthservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistoryRequest {

    @NotNull(message = "Movie ID không được để trống")
    private Long movieId;

    @Min(value = 0, message = "Tiến độ không được nhỏ hơn 0")
    @Max(value = 100, message = "Tiến độ không được lớn hơn 100")
    private int progress;
}
