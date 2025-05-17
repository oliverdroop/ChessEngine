package chess.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WeightingConfigTest {

    @Test
    void testCalculateMaxWeighting() {
        assertThat(WeightingConfig.calculateMaxWeighting(0)).isEqualTo(1);
        assertThat(WeightingConfig.calculateMaxWeighting(1)).isEqualTo(1);
    }
}
