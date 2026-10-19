package lv.lu.eztf.dn.combopt.matchmaking.solver;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import lv.lu.eztf.dn.combopt.matchmaking.domain.MatchmakingConfig;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Player;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Role;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Team;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.*;
import static ai.timefold.solver.core.api.score.stream.Joiners.*;
import static ai.timefold.solver.core.impl.score.stream.collector.tri.InnerTriConstraintCollectors.min;
import static ai.timefold.solver.core.impl.score.stream.collector.tri.InnerTriConstraintCollectors.max;

public class CostFunction implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
            //penalizeEveryPlayer(constraintFactory),
            //wrongNumberOfPlayers(constraintFactory),
            //ratingDifference(constraintFactory),
            wrongNumberOfPlayersShadow(constraintFactory),
            //ratingDiferenceShadow(constraintFactory),
            partyHardViolation(constraintFactory),
            roleMissingForTeam(constraintFactory),
            //hardWaitingThreshold(constraintFactory),
            softWaitingCost(constraintFactory),
            ratingDiferenceShadowAdjusted(constraintFactory),
        };
    }

    public Constraint penalizeEveryPlayer(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Player.class)
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("every Player");
    }

    public Constraint wrongNumberOfPlayers(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Player.class)
                .groupBy(Player::getAssignedTeam, count())
                .complement(Team.class, team -> 0L)
                .filter((team, playerCount) -> team.getNrOfPlayers().longValue() != playerCount)
                .penalize(HardSoftScore.ONE_HARD, (team, playerCount) -> Math.abs(team.getNrOfPlayers() - playerCount ))
                .asConstraint("wrong number of players");
    }

    private Constraint ratingDifference(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Player.class)
                // (team, averageRating)
                .groupBy(
                        Player::getAssignedTeam,
                        average(Player::getRating)
                )
                // Include teams which currently have no players.
                .complement(Team.class, team -> 0.0)
                // (team, averageRating, opponent)
                .join(
                        Team.class,
                        equal(
                                (team, averageRating) -> team.getOpponent(),
                                Function.identity()
                        )
                )

                // Merge:
                // (A, avgA, B) and (B, avgB, A)
                .groupBy(
                        matchKey,
                        min((Team team, Double avg, Team opponent) -> avg),
                        max((Team team, Double avg, Team opponent) -> avg)
                )

                // (team1, minAverage, maxAverage)
                .penalize(
                        HardSoftScore.ONE_SOFT,
                        (team, minAverage, maxAverage) ->
                                (int) Math.ceil(maxAverage - minAverage)
                )
                //.justifyWith((team,avg1,avg2,just) ->
                //        new ObjectListJustification(List.of(team, team.getOpponent())))
                .asConstraint("Rating difference");
    }

    Constraint wrongNumberOfPlayersShadow(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Team.class)
                .filter(team -> team.numberOfPlayers() != team.getNrOfPlayers())
                .penalize(HardSoftScore.ONE_HARD, (team ->
                        10L * Math.abs(team.numberOfPlayers() - team.getNrOfPlayers())))
                .asConstraint("Wrong number of players shadow");
    }

    Constraint ratingDiferenceShadow(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Team.class, equal(Team::getOpponent, Function.identity()))
                //.forEach(Team.class)
                //.join(Team.class, equal(Team::getOpponent, Function.identity()))
                .penalize(HardSoftScore.ONE_SOFT, (team1, team2) ->
                        (int) Math.abs(team1.averageRating() - team2.averageRating()))
                .asConstraint("rating difference shadow");
    }

    Constraint partyHardViolation_old(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Player.class,
                        equal(Player::getParty),
                        filtering((p1, p2) -> p1.getParty() != null))
                .filter((p1,p2)->p1.getAssignedTeam() != null && p2.getAssignedTeam() != null &&
                        !p1.getAssignedTeam().equals(p2.getAssignedTeam()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("party violation");
    }

    Constraint partyHardViolation(ConstraintFactory constraintFactory) {
        var mandatoryPartyPlayers = constraintFactory
                .forEachIncludingUnassigned(Player.class)
                .filter(player ->
                        player.getParty() != null && player.getParty().getMandatory());
        return mandatoryPartyPlayers
                .join(
                        mandatoryPartyPlayers,
                        lessThan(Player::getId),
                        equal(Player::getParty)
                )
                .filter((p1, p2) ->
                        !Objects.equals(
                                p1.getAssignedTeam(),
                                p2.getAssignedTeam()
                        ))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("party violation");
    }

    Constraint roleMissingForTeam_old(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Team.class)
                .filter(team -> {
                    List<Role> availableRoles =
                            new ArrayList<>(team.availableRoles());
                    for (Role requiredRole : team.getRequiredRoles()) {
                        if (!availableRoles.remove(requiredRole)) {
                            return true; // required occurrence is missing
                        }
                    }
                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Missing required role");
    }

    Constraint roleMissingForTeam(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Team.class)

                // (Team) -> (Team, missingRoles)
                .expand(Team::findMissingRoles)

                .filter((team, missingRoles) -> missingRoles.size() > 0)

                .penalize(
                        HardSoftScore.ONE_HARD,
                        (team, missingRoles) -> missingRoles.size()
                )

                .asConstraint("Missing required roles");
    }

    Constraint hardWaitingThreshold(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingUnassigned(Player.class)
                .filter(player -> player.getAssignedTeam() == null &&
                        player.getWaitingTime() > MAX_WAITING_TIME)
                .penalize(HardSoftScore.ONE_HARD, player ->
                        player.getWaitingTime() - MAX_WAITING_TIME)
                .asConstraint("hard waiting threshold");
    }

    Constraint softWaitingCost(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingUnassigned(Player.class)
                .filter(player -> player.getAssignedTeam() == null)
                .join(MatchmakingConfig.class)
                .penalize(HardSoftScore.ONE_SOFT, (player, config) -> (long) Math.ceil(
                        config.getWaitingPenalyAtReference() * Math.pow((double) player.getWaitingTime() / config.getWaitingReference(),2)))
                .asConstraint("soft waiting cost");
    }

    Constraint ratingDiferenceShadowAdjusted(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Team.class, equal(Team::getOpponent, Function.identity()))
                .join(MatchmakingConfig.class)
                .penalize(HardSoftScore.ONE_SOFT, (team1, team2, config) ->
                        (long) Math.ceil(config.getRatingDiffPenaltyAtReference() *
                                        (Math.abs(team1.averageRating() - team2.averageRating()) / config.getRatingDiffReference())))
                .asConstraint("rating difference shadow with config");
    }

    private TriFunction<Team, Double, Team, Team> matchKey =
            (team, averageRating, opponent) ->
                    team.getId().compareTo(opponent.getId()) < 0
                            ? team
                            : opponent;

    private record ObjectListJustification( List<Object> objects
    ) implements ConstraintJustification { }

    static private Integer MAX_WAITING_TIME = 10;
}
