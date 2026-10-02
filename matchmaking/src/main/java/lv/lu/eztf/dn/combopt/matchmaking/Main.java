package lv.lu.eztf.dn.combopt.matchmaking;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Game;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Player;
import lv.lu.eztf.dn.combopt.matchmaking.solver.CostFunctionEasy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Game problem = Game.createExample();
        SolverFactory<Game> solverFactory = SolverFactory.create(new SolverConfig()
                .withSolutionClass(Game.class)
                .withEntityClasses(Player.class)
                .withEasyScoreCalculatorClass(CostFunctionEasy.class)
                .withTerminationSpentLimit(Duration.ofSeconds(5)));
        Solver<Game> solver = solverFactory.buildSolver();
        Game solution = solver.solve(problem);
        solution.print();
    }
}