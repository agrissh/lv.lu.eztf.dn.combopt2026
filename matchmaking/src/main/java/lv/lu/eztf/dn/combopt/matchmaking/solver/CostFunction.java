package lv.lu.eztf.dn.combopt.matchmaking.solver;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Player;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Team;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Function;

import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.average;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.count;
import static ai.timefold.solver.core.api.score.stream.Joiners.equal;
import static ai.timefold.solver.core.impl.score.stream.collector.tri.InnerTriConstraintCollectors.min;
import static ai.timefold.solver.core.impl.score.stream.collector.tri.InnerTriConstraintCollectors.max;

public class CostFunction implements ConstraintProvider {
    @Override
    public Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory) {
        return new Constraint[] {
            penalizeEveryPlayer(constraintFactory),
            wrongNumberOfPlayers(constraintFactory),
            ratingDifference(constraintFactory)
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

    private TriFunction<Team, Double, Team, Team> matchKey =
            (team, averageRating, opponent) ->
                    team.getId().compareTo(opponent.getId()) < 0
                            ? team
                            : opponent;

    private record ObjectListJustification( List<Object> objects
    ) implements ConstraintJustification { }
}
