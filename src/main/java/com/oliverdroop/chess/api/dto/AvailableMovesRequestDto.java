package com.oliverdroop.chess.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class AvailableMovesRequestDto extends FENRequestDto {

    @NotNull
    @Pattern(regexp = "^[a-h][1-8]$")
    private String from;

    public AvailableMovesRequestDto(){}

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
