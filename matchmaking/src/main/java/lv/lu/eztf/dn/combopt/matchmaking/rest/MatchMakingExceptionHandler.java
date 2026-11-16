package lv.lu.eztf.dn.combopt.matchmaking.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class MatchMakingExceptionHandler {
    @ExceptionHandler({MatchMakingSolverException.class})
    public ResponseEntity<ErrorInfo> handleTimetableSolverException(MatchMakingSolverException exception) {
        return new ResponseEntity<>(new ErrorInfo(exception.getJobId(), exception.getMessage()), exception.getStatus());
    }
}
