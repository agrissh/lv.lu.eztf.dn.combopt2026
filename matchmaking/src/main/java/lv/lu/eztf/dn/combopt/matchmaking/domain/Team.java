package lv.lu.eztf.dn.combopt.matchmaking.domain;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@PlanningEntity
@JsonIdentityInfo(scope = Team.class, property = "id", generator = ObjectIdGenerators.PropertyGenerator.class)
public class Team {
    @PlanningId
    String id;
    @JsonIdentityReference(alwaysAsId = true)
    Team opponent;
    Integer nrOfPlayers;
    @JsonIdentityReference(alwaysAsId = true)
    List<Role> requiredRoles = new ArrayList<>();

    @JsonIdentityReference(alwaysAsId = true)
    @InverseRelationShadowVariable(sourceVariableName = "assignedTeam")
    List<Player> players = new ArrayList<>();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Double averageRating() {
        return players.stream()
                .mapToInt(Player::getRating)
                .average()
                .orElse(0.0);
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Integer numberOfPlayers() {
        return players.size();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public List<Role> availableRoles() {
        return players.stream().map(Player::getRole).toList();
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
        return id + "[" + numberOfPlayers() + "/" +nrOfPlayers + "](" + averageRating() + ") vs " + (this.opponent != null ? opponent.getId() : "");
    }
}
