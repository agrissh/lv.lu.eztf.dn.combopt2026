package lv.lu.eztf.dn.combopt.matchmaking.domain;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.*;
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

    @InverseRelationShadowVariable(sourceVariableName = "players")
    Team assignedTeam;
    Integer waitingTime;
    Party party;
    Role role;
    @NextElementShadowVariable(sourceVariableName = "players")
    Player next;
    @PreviousElementShadowVariable(sourceVariableName = "players")
    Player previous;

    public Integer gameStartTime() {
        if (this.getAssignedTeam() == null) {
            return null;
        }
        if (this.getPrevious() == null) {;
            return 0;
        }

        return this.getPrevious().gameStartTime() + 60;
    };

    Integer gameStartTime;
    /*public Integer gameStartTime() {
        return this.getGameStartTime();
    }

    @CascadingUpdateShadowVariable(targetMethodName = "updateTotalWaitingTime")
    Integer gameStartTime;

    @ShadowSources("players")
    public void updateTotalWaitingTime() {
        if (this.getAssignedTeam() == null) {
            this.setGameStartTime(null);
            return;
        }
        if (this.getPrevious() == null) {
            this.setGameStartTime(0);
            return;
        }

        this.setGameStartTime(this.getPrevious().getGameStartTime() + 60);
    }
    */

    public Integer waitingCost() {
        return this.waitingTime * this.waitingTime;
    }

    @Override
    public String toString() {
        return id + "(" + rating + ")[" + role.getRoleId() + "]" + (assignedTeam != null ? assignedTeam.getId()
                : "");
    }
}
