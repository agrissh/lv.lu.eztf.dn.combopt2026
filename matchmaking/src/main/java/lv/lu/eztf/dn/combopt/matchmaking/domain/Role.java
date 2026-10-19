package lv.lu.eztf.dn.combopt.matchmaking.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Role {
    String roleId;

    @Override
    public String toString() {
        return roleId;
    }
}
