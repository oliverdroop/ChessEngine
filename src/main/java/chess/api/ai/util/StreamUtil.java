package chess.api.ai.util;

import chess.api.PieceConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;

public class StreamUtil {

    public static void writeIntToOutputStream(int value, ByteArrayOutputStream outputStream) {
        outputStream.write((value & 0b11111111000000000000000000000000) >> 24);
        outputStream.write((value & 0b00000000111111110000000000000000) >> 16);
        outputStream.write((value & 0b00000000000000001111111100000000) >> 8);
        outputStream.write(value & 0b00000000000000000000000011111111);
    }

    public static int readIntFromInputStream(ByteArrayInputStream inputStream) throws IOException {
        final byte[] bytes = inputStream.readNBytes(4);
        return (((int)bytes[0]) << 24)
            | (((int)bytes[1]) << 16)
            | (((int)bytes[2]) << 8)
            | (((int)bytes[3]));
    }

    public static  PieceConfiguration[] readObjectsFromStream(
            ByteArrayOutputStream outputStream,
            Function<ByteArrayInputStream, PieceConfiguration> readFunction,
            int bytesPerObject) {
        final int lengthInBytes = outputStream.size();
        final int objectCount = lengthInBytes / bytesPerObject;
        final PieceConfiguration[] objects = new PieceConfiguration[objectCount];
        try(final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            for (int i = 0; i < objectCount; i++) {
                objects[i] = readFunction.apply(inputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException("Reading Objects from ByteArrayInputStream failed", e);
        }
        return objects;
    }

    public static <T> PoppedObjectStreamModification<T> popObjectFromStream(
            ByteArrayOutputStream outputStream,
            Function<ByteArrayInputStream, T> readFunction,
            int bytesPerObject) {
        final int newLength = outputStream.size() - bytesPerObject;
        final ByteArrayOutputStream newOutputStream = new ByteArrayOutputStream(newLength);
        final byte[] bytes = outputStream.toByteArray();
        newOutputStream.writeBytes(Arrays.copyOfRange(bytes, 0, newLength));
        final ByteArrayInputStream poppedObjectInputStream = new ByteArrayInputStream(
            Arrays.copyOfRange(bytes, newLength, newLength + bytesPerObject));
        final T poppedObject = readFunction.apply(poppedObjectInputStream);
        return new PoppedObjectStreamModification<>(newOutputStream, poppedObject);
    }
}
