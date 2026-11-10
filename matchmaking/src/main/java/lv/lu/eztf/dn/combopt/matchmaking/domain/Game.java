package lv.lu.eztf.dn.combopt.matchmaking.domain;

import ai.timefold.solver.core.api.domain.solution.*;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@PlanningSolution
public class Game {
    @PlanningScore
    HardSoftScore score;
    String name;
    @PlanningEntityCollectionProperty
    List<Player> playerList = new ArrayList<>();
    @PlanningEntityCollectionProperty
    @ValueRangeProvider
    List<Team> teamList = new ArrayList<>();
    @ProblemFactCollectionProperty
    List<Party> partyList = new ArrayList<>();
    @ProblemFactCollectionProperty
    List<Role> roleList = new ArrayList<>();
    @ProblemFactProperty
    MatchmakingConfig config = new MatchmakingConfig();

    private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);
    public void print() {
        LOGGER.debug("============" + this.getName() + "==============");
        this.teamList.forEach(team -> {
            LOGGER.debug(team.id + " " + team.averageRating() + " {" + team.getRequiredRoles() +"} vs " + team.opponent.getId() + " " + team.opponent.averageRating());
        });
        LOGGER.debug("===============PLAYERS=====================");
        this.playerList.forEach(player -> {
            LOGGER.debug(player.id + "(" +  player.getRating() + ")[" + player.getRole().getRoleId() + "](wt=" +
                    player.getWaitingTime() +") " +
                    (player.getParty() != null ? "from " + player.getParty().getPartyId() + " " : "")
                    + (player.getAssignedTeam() != null ? player.getAssignedTeam().getId() : "(no team)"));
        });
        LOGGER.debug("===========================================");
    }

    public static Game createExample() {
        Game game = new Game();
        game.setName("Default Test Game");

        game.setConfig(new MatchmakingConfig(
                100,60,1000,2000
        ));

        Role TANK = new Role("TANK");
        Role ATTACKER = new Role("ATTACKER");
        Role HEALER = new Role("HEALER");
        game.getRoleList().addAll(List.of(TANK,ATTACKER,HEALER));

        Team team1 = new Team();
        team1.setId("Team A");
        team1.setNrOfPlayers(6);
        team1.getRequiredRoles().addAll(List.of(TANK,ATTACKER,ATTACKER));
        Team team2 = new Team();
        team2.setId("Team B");
        team2.setNrOfPlayers(6);
        team2.getRequiredRoles().addAll(List.of(TANK,HEALER,ATTACKER));
        team1.setOpponent(team2);
        team2.setOpponent(team1);
        game.getTeamList().addAll(List.of(team1, team2));

        Party party1 = new Party("Friends", true);
        Party party2 = new Party("Highlanders", true);
        game.getPartyList().addAll(List.of(party1, party2));

        Player p1 = new Player("Alice", 2000, null, 10, party2, TANK);
        Player p2 = new Player("Bob", 2000, null, 15, party2, ATTACKER);
        Player p3 = new Player("Celine", 2400, null, 20, null, ATTACKER);
        Player p4 = new Player("Donald", 2200, null, 15, null, TANK);
        Player p5 = new Player("Eve", 1400, null, 60, null, HEALER);
        Player p6 = new Player("Felix", 1900, null, 45, null, ATTACKER);
        Player p7 = new Player("Gustav", 2700, null, 20, party1, ATTACKER);
        Player p8 = new Player("Henry", 1200, null, 70, null, ATTACKER);
        Player p9 = new Player("Isac", 800, null, 90, null, TANK);
        Player p0 = new Player("John", 1000, null, 45, null, TANK);
        Player p10 = new Player("Kate", 3000, null, 0, party1, ATTACKER);
        Player p11 = new Player("Leo", 2000, null, 15, null, ATTACKER);

        game.getPlayerList().addAll(List.of(p1,p2,p3,p4,p5,p6,p7,p8,p9,p0,p10,p11));

        return game;
    }

    public static Game createExample(int playerCount, int teamCount, int teamSize, int partyDensity) {
        Game game = new Game();
        game.setName("Game[#"+playerCount+"][#"+teamCount+"|"+teamSize+"|]");

        game.setConfig(new MatchmakingConfig(
                100,60,1000,2000
        ));

        Role TANK = new Role("TANK");
        Role ATTACKER = new Role("ATTACKER");
        Role HEALER = new Role("HEALER");
        game.getRoleList().addAll(List.of(TANK,ATTACKER,HEALER));

        for (int i=1;i<=teamCount;i++) {
            Team team1 = new Team();
            team1.setId("Team "+i);
            team1.setNrOfPlayers(teamSize);
            int rolesNeeded = ThreadLocalRandom.current().nextInt(0, teamSize + 1);
            for (int j=0;j<rolesNeeded;j++) {
                team1.getRequiredRoles().add(game.getRoleList().get(ThreadLocalRandom.current().nextInt(game.getRoleList().size())));
            }
            game.getTeamList().add(team1);
        }

        // We have to set opponents for the teams!
        // Assume there is an even number of teams. Go through and choose every two teams;
        for (int i=1;i<teamCount;i+=2) {
            game.getTeamList().get(i-1).setOpponent(game.getTeamList().get(i));
            game.getTeamList().get(i).setOpponent(game.getTeamList().get(i-1));
        }


        // Create playerCount/10 parties
        for (int i=1;i <= (playerCount / 10);i++) {
            Party party1 = new Party("Prt"+i, true);
            game.getPartyList().add(party1);
        }


        // Lets create a required role list of all teams. Then distribute the roles among players;
        List<Role> requiredRoles = game.getTeamList().stream()
                .flatMap((team -> team.getRequiredRoles().stream()))
                .collect(Collectors.toCollection(ArrayList::new));
        for (int i=1;i<=playerCount;i++) {
            int playerRating = ThreadLocalRandom.current().nextInt(500, 2501);
            int waitingTime = ThreadLocalRandom.current().nextInt(1, 129);
            Role role;
            if (i>requiredRoles.size()) {
                // no roles left - choose randomly
                role = game.getRoleList().get(ThreadLocalRandom.current().nextInt(game.getRoleList().size()));
            } else {
                role = requiredRoles.remove(ThreadLocalRandom.current().nextInt(requiredRoles.size()));
            }

            Party party = ThreadLocalRandom.current().nextInt(1, 11) >= partyDensity ? null :
                game.getPartyList().get(ThreadLocalRandom.current().nextInt(game.getPartyList().size()));

            Player p1 = new Player("P "+i, playerRating, null, waitingTime, party, role);
            game.getPlayerList().add(p1);
        }

        return game;
    }
}
