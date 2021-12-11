package nextstep.subway.path.ui;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import nextstep.subway.auth.domain.*;
import nextstep.subway.fare.*;
import nextstep.subway.path.application.*;
import nextstep.subway.path.domain.*;
import nextstep.subway.path.dto.*;

@RestController
public class PathController {
    private final PathService pathService;

    public PathController(PathService pathService) {
        this.pathService = pathService;
    }

    @GetMapping(value = "/paths")
    public ResponseEntity<PathResponse> shortPath(@AuthenticationPrincipal LoginMember member,
        @RequestParam long source, @RequestParam long target) {
        Path shortestPath = pathService.shortestPath(source, target);
        Fare fare = FareCalculator.from(shortestPath.getTotalDistance(), shortestPath.getLines(), member.getAge())
            .totalFare();

        return ResponseEntity.ok().body(PathResponse.of(shortestPath, fare.fare()));
    }
}
