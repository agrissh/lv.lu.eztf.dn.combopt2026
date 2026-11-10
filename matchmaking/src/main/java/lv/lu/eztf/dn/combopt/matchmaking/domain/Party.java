package lv.lu.eztf.dn.combopt.matchmaking.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@JsonIdentityInfo(scope = Party.class, property = "partyId", generator = ObjectIdGenerators.PropertyGenerator.class)
public class Party {
    String partyId;
    Boolean mandatory;
}
