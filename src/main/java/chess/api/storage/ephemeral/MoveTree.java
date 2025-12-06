package chess.api.storage.ephemeral;

import chess.api.configuration.PieceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MoveTree {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoveTree.class);

    public static final MoveLink STARTING_LINK;

    static {
        int[] startingMoves = new int[]{
            80, 82, 405, 407, 528, 536, 593, 604, 658, 666, 723, 731, 788, 796, 853, 861, 918, 926, 983, 991
        };
        final List<MoveLink> startingMoveLinks = Arrays.stream(startingMoves)
            .mapToObj(m -> new MoveLink((short) m, null, null))
            .toList();
        final MoveLink[] startingMoveLinkArray = new MoveLink[startingMoveLinks.size()];
        for(int i = 0; i < 20; i++) {
            startingMoveLinkArray[i] = startingMoveLinks.get(i);
        }
        STARTING_LINK =  new MoveLink((short)0, null, startingMoveLinkArray);
    }

    public static void addChildMoveLinks(short[] moveHistoryToParent, List<PieceConfiguration> onwardConfigurations) {
        final MoveLink parentMoveLink = getMoveLink(moveHistoryToParent);
        if (parentMoveLink == null) {
            LOGGER.warn("No route to parent configuration exists for {}", moveHistoryToParent);
            return;
        }
        if (moveHistoryToParent.length > 6) {
            short[] moveHistoryToPruneTo = Arrays.copyOfRange(moveHistoryToParent, 0, moveHistoryToParent.length - 6);
            prune(moveHistoryToPruneTo);
        }
        final List<MoveLink> onwardMoves = onwardConfigurations.stream()
            .map(PieceConfiguration::getHistoricMoves)
            .map(hm -> hm[hm.length - 1])
            .map(m -> new MoveLink(m, parentMoveLink, null))
            .toList();
        final MoveLink[] moveLinkArray = new MoveLink[onwardMoves.size()];
        for(int i = 0; i < onwardMoves.size(); i++) {
            moveLinkArray[i] = onwardMoves.get(i);
        }
        parentMoveLink.setChildMoves(moveLinkArray);
    }

    public static MoveLink getMoveLink(short[] moveHistory) {
        if (moveHistory == null) {
            return null;
        } else if (moveHistory.length == 0) {
            return STARTING_LINK;
        }
        MoveLink currentLink = STARTING_LINK;
        for(short move : moveHistory) {
            if (currentLink.getChildMoves() == null) {
                return null;
            }
            final Optional<MoveLink> onwardLink = Arrays.stream(currentLink.getChildMoves())
                .filter(cm -> cm.getMove() == move)
                .findAny();
            if (onwardLink.isEmpty()) {
                return null;
            }
            currentLink = onwardLink.get();
        }
        return currentLink;
    }

    private static void prune(short[] moveHistory) {
        MoveLink currentLink = getMoveLink(moveHistory);
        while(currentLink != null) {
            final MoveLink parentMove = currentLink.getParentMove();
            if (parentMove != null) {
                parentMove.setChildMoves(new MoveLink[]{currentLink});
            }
            currentLink = parentMove;
        }
    }
}
