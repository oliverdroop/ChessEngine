package chess.api.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class TimingUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimingUtil.class);

    public static <T> T logTime(Supplier<T> supplier) {
        return logTime("Operation", supplier);
    }

    public static <T> T logTime(String description, Supplier<T> supplier) {
        long t1 = System.currentTimeMillis();
        T output = supplier.get();
        long t2 = System.currentTimeMillis();
        long diff = t2 - t1;
        LOGGER.info("{} took {} milliseconds", description, diff);
        return output;
    }
}
