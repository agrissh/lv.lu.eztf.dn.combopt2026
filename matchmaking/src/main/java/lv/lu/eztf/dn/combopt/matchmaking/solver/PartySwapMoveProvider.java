package lv.lu.eztf.dn.combopt.matchmaking.solver;

import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.NeighborhoodsCollectors;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Game;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Player;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Team;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PartySwapMoveProvider implements MoveProvider<Game> {

    private final PlanningVariableMetaModel<Game, Player, Team> teamVar;

    public PartySwapMoveProvider(
            PlanningVariableMetaModel<Game, Player, Team> teamVar) {
        this.teamVar = Objects.requireNonNull(teamVar);
    }

    @Override
    public MoveStream<Game> build(MoveStreamFactory<Game> factory) {

        // 1. ENUMERATION: Players grouped into atomic units.
        var groups = factory.forEachUnfiltered(Player.class, false)
                .groupBy(
                        (view, player) -> {
                            var party = player.getParty();
                            return party != null && party.getMandatory()
                                    ? party : player;
                        },
                        NeighborhoodsCollectors.toList())
                .map((view, key, players) -> Sample.of(players));
        return factory.pick(groups)
                // 2. PICKING: Select two compatible groups.
                .pick(groups,
                        // Indexed join: consider only equally sized groups.
                        NeighborhoodsJoiners.equal(
                                (Sample<Player> group) -> group.size()),
                        // Additional checks on the selected pair.
                        NeighborhoodsJoiners.filtering(this::isValidSwap))
                // 3. GENERATING: Generate move
                .asMove((view, left, right) ->
                        Moves.pillarSwap(teamVar, left, right));
    }

    private boolean isValidSwap(
            SolutionView<Game> view,
            Sample<Player> left,
            Sample<Player> right) {

        if (left.equals(right) || left.size() != right.size()) {
            return false;
        }

        Team leftTeam = view.getValue(
                teamVar, Objects.requireNonNull(left.representative()));
        Team rightTeam = view.getValue(
                teamVar, Objects.requireNonNull(right.representative()));

        if (leftTeam == null || rightTeam == null
                || Objects.equals(leftTeam, rightTeam)) {
            return false;
        }

        return canMove(view, left, leftTeam, rightTeam)
                && canMove(view, right, rightTeam, leftTeam);
    }

    private boolean canMove(
            SolutionView<Game> view,
            Sample<Player> group,
            Team source,
            Team destination) {

        for (Player player : group) {
            if (view.isPinned(teamVar, player)
                    || !Objects.equals(
                    view.getValue(teamVar, player), source)
                    || !view.isValueInRange(
                    teamVar, player, destination)) {
                return false;
            }
        }
        return true;
    }
}
