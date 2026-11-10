package lv.lu.eztf.dn.combopt.matchmaking.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@JsonIdentityInfo(scope = Role.class, property = "roleId", generator = ObjectIdGenerators.PropertyGenerator.class)
public class Role {
    String roleId;

    @Override
    public String toString() {
        return roleId;
    }
}
