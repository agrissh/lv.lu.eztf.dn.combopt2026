package lv.lu.eztf.dn.combopt.matchmaking;

import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import lv.lu.eztf.dn.combopt.matchmaking.domain.*;
import lv.lu.eztf.dn.combopt.matchmaking.solver.CostFunction;
import org.junit.jupiter.api.Test;


public class ConstraintTest {
    private ConstraintVerifier<CostFunction, Game> constraintVerifier =
            ConstraintVerifier.build(new CostFunction(), Game.class, Player.class);
    @Test
    void testWrongNumberOfPlayers() {
        Team team = new Team("Team A", null, 3);
        Player player = new Player("Alice", 2000, team);

        constraintVerifier.verifyThat(CostFunction::wrongNumberOfPlayers)
                .given(team, player)
                .penalizesBy(2);
    }
}
