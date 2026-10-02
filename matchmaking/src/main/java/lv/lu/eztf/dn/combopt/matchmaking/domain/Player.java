package lv.lu.eztf.dn.combopt.matchmaking.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@PlanningEntity
public class Player {
    String id;
    Integer rating;

    @PlanningVariable
    Team assignedTeam;

    @Override
    public String toString() {
        return id + "(" + rating + ")" + (assignedTeam != null ? assignedTeam.getId()
                : "");
    }
}
