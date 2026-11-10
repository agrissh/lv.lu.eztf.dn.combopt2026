package lv.lu.eztf.dn.combopt.matchmaking.domain;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@PlanningEntity
@JsonIdentityInfo(scope = Player.class, property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
public class Player {
    @PlanningId
    String id;
    Integer rating;

    @JsonIdentityReference(alwaysAsId = true)
    @PlanningVariable(allowsUnassigned = true)
    Team assignedTeam;
    Integer waitingTime;
    @JsonIdentityReference(alwaysAsId = true)
    Party party;
    @JsonIdentityReference(alwaysAsId = true)
    Role role;

    @Override
    public String toString() {
        return id + "(" + rating + ")[" + role.getRoleId() + "]" + (assignedTeam != null ? assignedTeam.getId()
                : "");
    }
}
