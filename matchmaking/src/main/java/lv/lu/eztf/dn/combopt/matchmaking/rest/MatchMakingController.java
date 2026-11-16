package lv.lu.eztf.dn.combopt.matchmaking.rest;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.api.solver.SolverStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Game;
import lv.lu.eztf.dn.combopt.matchmaking.domain.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("/mm")
public class MatchMakingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchMakingController.class);

    private final SolverManager<Game> solverManager;
    private final SolutionManager<Game, HardSoftScore> solutionManager;

    private final ConcurrentMap<String, Job> jobIdToJob = new ConcurrentHashMap<>();

    public MatchMakingController(SolverManager<Game> solverManager,
                               SolutionManager<Game, HardSoftScore> solutionManager) {
        this.solverManager = solverManager;
        this.solutionManager = solutionManager;
    }

    @Operation(summary = "List the job IDs of all submitted games.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all job IDs.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "array", implementation = String.class))) })
    @GetMapping
    public Collection<String> listGames() {
        return jobIdToJob.keySet();
    }

    @Operation(summary = "Submit a game to start solving as soon as CPU resources are available.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202",
                    description = "The job ID. Use that ID to get the solution with the other methods.",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(implementation = String.class))) })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String solveGame(@RequestBody Game problem) {
        String jobId = UUID.randomUUID().toString();
        jobIdToJob.put(jobId, Job.ofGame(problem));
        solverManager.solveBuilder()
                .withProblemId(jobId)
                .withProblemFinder(jobId_ -> jobIdToJob.get(jobId).game)
                .withBestSolutionEventConsumer(event ->
                        jobIdToJob.put(jobId, Job.ofGame(event.solution())))
                .withExceptionHandler((jobId_, exception) -> {
                    jobIdToJob.put(jobId, Job.ofException(exception));
                    LOGGER.error("Failed solving jobId ({}).", jobId, exception);
                })
                .run();
        return jobId;
    }

    @GetMapping(value = "/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Game getGame(
            @Parameter(description = "The job ID returned by the POST method.") @PathVariable("jobId") String jobId) {
        Game game = getGameAndCheckForExceptions(jobId);
        SolverStatus solverStatus = solverManager.getSolverStatus(jobId);
        game.setSolverStatus(solverStatus);
        return game;
    }

    @GetMapping(value = "score/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ScoreAnalysis<HardSoftScore> score(@Parameter(description = "The job ID returned by the POST method.") @PathVariable("jobId") String jobId) {
        Game game = getGameAndCheckForExceptions(jobId);
        return solutionManager.analyze(
                game,
                ScoreAnalysisFetchPolicy.FETCH_ALL
        );
    }

    private Game getGameAndCheckForExceptions(String jobId) {
        Job job = jobIdToJob.get(jobId);
        if (job == null) {
            throw new MatchMakingSolverException(jobId, HttpStatus.NOT_FOUND, "No timetable found.");
        }
        if (job.exception != null) {
            throw new MatchMakingSolverException(jobId, job.exception);
        }
        return job.game;
    }

    private record Job(Game game, Throwable exception) {

        static Job ofGame(Game game) {
            return new Job(game, null);
        }

        static Job ofException(Throwable error) {
            return new Job(null, error);
        }
    }
}

