package lv.lu.eztf.dn.combopt.matchmaking.domain;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@PlanningEntity
public class Team {
    @PlanningId
    String id;
    Team opponent;
    Integer nrOfPlayers;
    List<Role> requiredRoles = new ArrayList<>();

    @InverseRelationShadowVariable(sourceVariableName = "assignedTeam")
    List<Player> players = new ArrayList<>();

    public Double averageRating() {
        return players.stream()
                .mapToInt(Player::getRating)
                .average()
                .orElse(0.0);
    }

    public Integer numberOfPlayers() {
        return players.size();
    }

    public List<Role> availableRoles() {
        return players.stream().map(p -> p.getRole()).toList();
    }

    public List<Role> findMissingRoles() {
        List<Role> availableRoles =
                new ArrayList<>(this.availableRoles());
        List<Role> missingRoles = new ArrayList<>();
        for (Role requiredRole : this.getRequiredRoles()) {
            if (!availableRoles.remove(requiredRole)) {
                missingRoles.add(requiredRole);
            }
        }
        return missingRoles;
    }
    @Override
    public String toString() {
        return id + "[" + numberOfPlayers() + "/" +nrOfPlayers + "](" + averageRating() + ") vs " + opponent.getId();
    }
}
