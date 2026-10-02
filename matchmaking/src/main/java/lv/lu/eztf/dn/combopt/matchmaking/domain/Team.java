package lv.lu.eztf.dn.combopt.matchmaking.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Team {
    String id;
    Team opponent;
    Integer nrOfPlayers;

    @Override
    public String toString() {
        return id + "[" + nrOfPlayers + "] vs " + opponent.getId();
    }
}
