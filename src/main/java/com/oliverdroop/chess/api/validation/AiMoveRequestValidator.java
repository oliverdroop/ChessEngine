package com.oliverdroop.chess.api.validation;

import com.oliverdroop.chess.api.FENReader;
import com.oliverdroop.chess.api.FENWriter;
import com.oliverdroop.chess.api.configuration.IntsPieceConfiguration;
import com.oliverdroop.chess.api.configuration.PieceConfiguration;
import com.oliverdroop.chess.api.dto.AiMoveRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.regex.Pattern;

import static com.oliverdroop.chess.api.MoveDescriber.getMoveFromAlgebraicNotation;
import static com.oliverdroop.chess.api.configuration.PieceConfiguration.toNewConfigurationFromMove;

public class AiMoveRequestValidator implements ConstraintValidator<AiMoveRequestValidation, AiMoveRequestDto> {

    public static final String ALGEBRAIC_NOTATION_REGEX = "^[a-h][1-8]x?[a-h][1-8][qrbn]?$";

    @Override
    public void initialize(AiMoveRequestValidation constraintAnnotation) {}

    @Override
    public boolean isValid(AiMoveRequestDto aiMoveRequestDto, ConstraintValidatorContext constraintValidatorContext) {

        final String fen = aiMoveRequestDto.getFen();
        if (fen == null) {
            return false;
        }
        final List<String> moveHistory = aiMoveRequestDto.getMoveHistory();
        if (moveHistory == null) {
            return true;
        }
        if (moveHistory.stream().anyMatch(this::isInvalidAlgebraicNotation)) {
            return false;
        }
        PieceConfiguration currentConfiguration = FENReader.read(
            FENWriter.STARTING_POSITION, IntsPieceConfiguration.class);
        for (final String moveAlgebraicNotation : moveHistory) {
            final short moveDescription = getMoveFromAlgebraicNotation(moveAlgebraicNotation);
            currentConfiguration = toNewConfigurationFromMove(currentConfiguration, moveDescription);
        }
        return fen.equals(currentConfiguration.toString());
    }

    private boolean isInvalidAlgebraicNotation(String moveAlgebraicNotation) {
        return !Pattern.matches(ALGEBRAIC_NOTATION_REGEX, moveAlgebraicNotation);
    }
}
