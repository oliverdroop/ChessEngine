package chess.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class PCTreeCallable implements Callable<Map<String, Collection<String>>> {

    private final PieceConfiguration rootConfiguration;

    private final int maxDepth;

    public PCTreeCallable(PieceConfiguration rootConfiguration, int maxDepth) {
        this.rootConfiguration = rootConfiguration;
        this.maxDepth = maxDepth;
    }
    @Override
    public Map<String, Collection<String>> call() throws Exception {
        int depth = 0;
        int[] indexes = new int[maxDepth];
        PieceConfiguration currentConfiguration = rootConfiguration;
        Map<String, Collection<String>> fenMap = new HashMap<>();
        while (depth >= 0 && depth < maxDepth) {
            List<PieceConfiguration> childConfigurations;
            String currentFEN = FENWriter.write(currentConfiguration);
            if (currentConfiguration.getChildConfigurations().isEmpty()) {
                childConfigurations = currentConfiguration.getPossiblePieceConfigurations();
                List<String> onwardFENs = childConfigurations.stream()
                        .map(childConfiguration -> FENWriter.write(childConfiguration))
                        .collect(Collectors.toList());
                fenMap.put(currentFEN, onwardFENs);
            } else {
                childConfigurations = currentConfiguration.getChildConfigurations();
            }

            if (indexes[depth] < childConfigurations.size()) {
                // Go deeper
                currentConfiguration = childConfigurations.get(indexes[depth]);
                depth ++;
                if (depth < maxDepth) {
                    indexes[depth] = 0;
                }
            }

            if (depth == maxDepth || indexes[depth] >= childConfigurations.size()) {
                // Go back up
                currentConfiguration = currentConfiguration.getParentConfiguration();
                depth --;
                if (depth >= 0) {
                    indexes[depth]++;
                }
            }
        }
        return  fenMap;
    }
}
