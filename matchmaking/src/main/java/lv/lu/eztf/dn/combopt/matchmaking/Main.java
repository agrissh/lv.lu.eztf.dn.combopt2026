package lv.lu.eztf.dn.combopt.matchmaking;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.calculator.EasyScoreCalculator;
import ai.timefold.solver.core.api.solver.*;
import ai.timefold.solver.core.api.solver.event.NewBestSolutionEvent;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.enterprise.core.api.ThrottlingBestSolutionEventConsumer;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Game;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Player;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Team;
import lv.lu.eztf.dn.combopt.matchmaking.solver.CostFunction;
import lv.lu.eztf.dn.combopt.matchmaking.solver.CostFunctionEasy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Game problem = Game.createExample();
        /*SolverFactory<Game> solverFactory = SolverFactory.create(new SolverConfig()
                .withSolutionClass(Game.class)
                .withEntityClasses(Player.class, Team.class)
                //.withEasyScoreCalculatorClass(CostFunctionEasy.class)
                .withEnvironmentMode(EnvironmentMode.PHASE_ASSERT)
                .withScoreDirectorFactory(
                        new ScoreDirectorFactoryConfig()
                                .withConstraintProviderClass(CostFunction.class)
                                .withConstraintStreamProfilingEnabled(true)
                                //.withAssertionScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                                //        .withEasyScoreCalculatorClass(CostFunctionEasy.class)
                                //)
                )
                //.withConstraintProviderClass(CostFunction.class)
                .withTerminationSpentLimit(Duration.ofSeconds(5)));
                //.withTerminationConfig(new TerminationConfig().withDiminishedReturns())*/
        SolverConfig solverConfigFromXML = SolverConfig.createFromXmlResource("solverConfig.xml");
        SolverFactory<Game> solverFactory = SolverFactory.create(solverConfigFromXML.withTerminationConfig(
                new TerminationConfig().withSecondsSpentLimit(5L)));
        Solver<Game> solver = solverFactory.buildSolver();
        Game solution = solver.solve(problem);
        solution.print();

        /*SolverManager<Game> solverManager = SolverManager.create(solverFactory, new SolverManagerConfig());
        SolverJob<Game> solverJob = solverManager.solve(UUID.randomUUID().toString(), problem);
        SolverJob<Game> solverJob2 = solverManager.solveAndListen(UUID.randomUUID().toString(), problem, Game::print);

        solverManager.solveBuilder()
                .withProblemId(UUID.randomUUID().toString())
                .withProblem(problem)
                .withBestSolutionEventConsumer(Main::printGame)
                .run();
        solverManager.solveBuilder()
                .withProblemId(UUID.randomUUID().toString())
                .withProblem(problem)
                .run();*/

        SolutionManager<Game, HardSoftScore> solutionManager = SolutionManager.create(solverFactory);
        ScoreAnalysis<HardSoftScore> scoreAnalysis = solutionManager.analyze(solution);
        LOGGER.info(scoreAnalysis.summarize());

        /*scoreAnalysis.constraintMap().forEach((ref, ca) -> {
            LOGGER.info(ca.constraintId() + " " + ca.score());
            ca.matches().forEach(m -> {
               LOGGER.info("Match with score " + m.score().toString() +
                       " and justification " + m.justification().toString());
            });
        });*/
    }

    private static void printGame(NewBestSolutionEvent<Game> gameNewBestSolutionEvent) {
        gameNewBestSolutionEvent.solution().print();
    }
}