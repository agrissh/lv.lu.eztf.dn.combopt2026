package lv.lu.eztf.dn.combopt.matchmaking.domain;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardSoftScore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@PlanningSolution
public class Game {
    @PlanningScore
    HardSoftScore score;
    String name;
    @PlanningEntityCollectionProperty
    List<Player> playerList = new ArrayList<>();
    @ProblemFactCollectionProperty
    @ValueRangeProvider
    List<Team> teamList = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
    public void print() {
        this.playerList.forEach(player -> {
            LOGGER.debug(player.id + "(" +  player.getRating() + ") " + player.getAssignedTeam().getId());
        });
    }

    public static Game createExample() {
        Game game = new Game();

        Team team1 = new Team();
        team1.setId("Team A");
        team1.setNrOfPlayers(5);
        Team team2 = new Team();
        team2.setId("Team B");
        team2.setNrOfPlayers(5);
        team1.setOpponent(team2);
        team2.setOpponent(team1);
        game.getTeamList().addAll(List.of(team1, team2));

        Player p1 = new Player("Alice", 2000, null);
        Player p2 = new Player("Bob", 2000, null);
        Player p3 = new Player("Celine", 2400, null);
        Player p4 = new Player("Donald", 2200, null);
        Player p5 = new Player("Eve", 1400, null);
        Player p6 = new Player("Felix", 1900, null);
        Player p7 = new Player("Gustav", 2700, null);
        Player p8 = new Player("Henry", 1200, null);
        Player p9 = new Player("Isac", 800, null);
        Player p0 = new Player("John", 1000, null);

        game.getPlayerList().addAll(List.of(p1,p2,p3,p4,p5,p6,p7,p8,p9,p0));

        return game;
    }
}
