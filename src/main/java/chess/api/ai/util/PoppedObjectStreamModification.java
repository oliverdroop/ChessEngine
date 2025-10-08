package chess.api.ai.util;

import java.io.ByteArrayOutputStream;

public record PoppedObjectStreamModification<T>(ByteArrayOutputStream stream, T poppedObject) {
}
