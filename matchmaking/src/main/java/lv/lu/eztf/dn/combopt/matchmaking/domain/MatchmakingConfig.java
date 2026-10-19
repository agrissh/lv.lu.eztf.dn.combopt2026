package lv.lu.eztf.dn.combopt.matchmaking.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MatchmakingConfig {
    private Integer ratingDiffReference = 100;
    private Integer waitingReference = 60;
    private Integer ratingDiffPenaltyAtReference = 1000;
    private Integer waitingPenalyAtReference = 200;
}
