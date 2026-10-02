package lv.lu.eztf.dn.combopt.matchmaking.solver;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Game;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Player;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CostFunctionEasy implements EasyScoreCalculator<Game, HardSoftScore> {
    @Override
    public HardSoftScore calculateScore(Game game) {
        int hard = 0;
        int soft = 0;
        Map<Team, Integer> playerCounts = new HashMap<>();
        Map<Team, Integer> totalRatings = new HashMap<>();
        // One pass through all players.
        for (Player player : game.getPlayerList()) {
            Team team = player.getAssignedTeam();

            if (team != null) {
                playerCounts.merge(team, 1, Integer::sum);
                totalRatings.merge(team, player.getRating(), Integer::sum);
            }
        }
        Set<Team> checkedTeams = new HashSet<>();
        for (Team team : game.getTeamList()) {
            int playerCount = playerCounts.getOrDefault(team, 0);
            int totalRating = totalRatings.getOrDefault(team, 0);

            hard += Math.abs(team.getNrOfPlayers() - playerCount);

            if (checkedTeams.add(team)) {
                Team opponent = team.getOpponent();

                int opponentCount = playerCounts.getOrDefault(opponent, 0);
                int opponentTotalRating = totalRatings.getOrDefault(opponent, 0);

                double avgRating = playerCount > 0
                        ? totalRating / (double) playerCount
                        : 0.0;

                double avgOpponentRating = opponentCount > 0
                        ? opponentTotalRating / (double) opponentCount
                        : 0.0;

                soft += (int) Math.ceil(
                        Math.abs(avgRating - avgOpponentRating)
                );

                checkedTeams.add(opponent);
            }
        }
        return HardSoftScore.of(-hard, -soft);
    }
}
