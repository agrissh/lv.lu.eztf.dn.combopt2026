package lv.lu.eztf.dn.combopt.matchmaking;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import lv.lu.eztf.dn.combopt.matchmaking.domain.*;
import lv.lu.eztf.dn.combopt.matchmaking.solver.CostFunction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;


public class ConstraintTest {
    private ConstraintVerifier<CostFunction, Game> constraintVerifier =
            ConstraintVerifier.build(new CostFunction(), Game.class, Player.class);
    @Test
    void testWrongNumberOfPlayers() {
        Team team = new Team("Team A", null, 3, new ArrayList<>(), new ArrayList<>());
        Player player = new Player("Alice", 2000, team, 15, null, null);

        constraintVerifier.verifyThat(CostFunction::wrongNumberOfPlayers)
                .given(team, player)
                .penalizesBy(2);
    }
}
