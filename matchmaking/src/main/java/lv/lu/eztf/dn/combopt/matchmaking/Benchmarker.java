package lv.lu.eztf.dn.combopt.matchmaking;

import ai.timefold.solver.benchmark.api.PlannerBenchmark;
import ai.timefold.solver.benchmark.api.PlannerBenchmarkFactory;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Game;

public class Benchmarker {
    public static void main(String[] args) {
        //PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory.
        //        createFromSolverConfigXmlResource("solverConfig.xml");
        PlannerBenchmarkFactory benchmarkFactory = PlannerBenchmarkFactory
                .createFromXmlResource("benchmarkConfig.xml");
        //Game game = Game.createExample();
        //Game ggame0 = Game.createExample(12, 2, 5, 9);
        //Game ggame1 = Game.createExample(24, 4, 5, 9);
        //Game ggame2 = Game.createExample(124, 20, 5, 9);
        //Game ggame3 = Game.createExample(424, 80, 5, 9);
        //Game ggame4 = Game.createExample(848, 108, 5, 9);
        //Game ggame5 = Game.createExample(1696, 336, 5, 9);
        //PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark(ggame3,ggame4,ggame5);
        PlannerBenchmark benchmark = benchmarkFactory.buildPlannerBenchmark();
        benchmark.benchmark();
    }
}
