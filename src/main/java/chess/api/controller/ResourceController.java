package chess.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.lang.String.format;

@RestController
public class ResourceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceController.class);

    @GetMapping("/{path}")
    public ResponseEntity<byte[]> getResource(@PathVariable String path) {
        try(final InputStream inputStream = ResourceController.class.getClassLoader().getResourceAsStream(path)) {
            return ResponseEntity.ok(inputStream.readAllBytes());
        } catch (Throwable e) {
            LOGGER.error("Unable to return {}", path, e);
            final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            final String message = format("%d : %s", status.value(), status.getReasonPhrase());
            return ResponseEntity.status(status).body(message.getBytes(StandardCharsets.UTF_8));
        }
    }
}
