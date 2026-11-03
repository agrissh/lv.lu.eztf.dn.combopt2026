package lv.lu.eztf.dn.combopt.matchmaking.solver;

import ai.timefold.solver.core.preview.api.move.builtin.*;
import ai.timefold.solver.core.preview.api.neighborhood.Neighborhood;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.PillarSampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import io.smallrye.common.constraint.NotNull;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Game;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Player;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Team;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class CustomNeighborhoodProvider implements NeighborhoodProvider<Game> {
    @Override
    public Neighborhood defineNeighborhood(NeighborhoodBuilder<Game> builder) {
        var playerVar = builder.getSolutionMetaModel()
                .genuineEntity(Player.class);
        var assignedTeamVar = builder.getSolutionMetaModel()
                .genuineEntity(Player.class)
                .basicVariable("assignedTeam", Team.class);
        return builder
                .add(new ChangeMoveProvider<>(assignedTeamVar))
                //.add(new SubPillarSwapMoveProvider<>(playerVar,
                //        Samplers.pillar(Samplers.exactly(2)),
                //        Samplers.pillar(Samplers.exactly(2))))
                .add(new PartySwapMoveProvider(assignedTeamVar))
                .build();
    }
}
