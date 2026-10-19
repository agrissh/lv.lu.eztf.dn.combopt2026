package lv.lu.eztf.dn.combopt.matchmaking.domain;

import ai.timefold.solver.core.api.domain.common.PlanningId;
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
    @PlanningId
    String id;
    Integer rating;

    @PlanningVariable(allowsUnassigned = true)
    Team assignedTeam;
    Integer waitingTime;
    Party party;
    Role role;

    public Integer waitingCost() {
        return this.waitingTime * this.waitingTime;
    }

    @Override
    public String toString() {
        return id + "(" + rating + ")[" + role.getRoleId() + "]" + (assignedTeam != null ? assignedTeam.getId()
                : "");
    }
}
