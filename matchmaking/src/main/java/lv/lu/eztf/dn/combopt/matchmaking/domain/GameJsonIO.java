package lv.lu.eztf.dn.combopt.matchmaking.domain;

import ai.timefold.solver.jackson.impl.domain.solution.JacksonSolutionFileIO;

public class GameJsonIO extends JacksonSolutionFileIO<Game> {
    public GameJsonIO() {
        super(Game.class);
    }
}
