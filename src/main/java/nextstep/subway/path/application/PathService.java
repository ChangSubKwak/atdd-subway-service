package nextstep.subway.path.application;

import java.util.*;

import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import nextstep.subway.common.exception.*;
import nextstep.subway.fare.*;
import nextstep.subway.line.domain.*;
import nextstep.subway.path.domain.*;
import nextstep.subway.station.domain.*;

@Service
@Transactional(readOnly = true)
public class PathService {
    private static final String STATION = "역";
    private static final int DEFAULT_GENERAL_AGE = 19;

    private final StationRepository stationRepository;
    private final LineRepository lineRepository;

    public PathService(StationRepository stationRepository, LineRepository lineRepository) {
        this.stationRepository = stationRepository;
        this.lineRepository = lineRepository;
    }

    public Path shortestPath(long source, long target) {
        return shortestPath(source, target, DEFAULT_GENERAL_AGE);
    }

    public Path shortestPath(long source, long target, int age) {
        List<Station> findResult = stationRepository.findAllByIdIn(Arrays.asList(source, target));

        Station startStation = getStation(findResult, source);
        Station endStation = getStation(findResult, target);

        List<Line> lines = lineRepository.findAll();
        PathFinder pathFinder = DijkstraPathFinder.from(lines);
        Path path = pathFinder.shortestPath(startStation, endStation);

        return Path.of(path.getStations(),
            path.getTotalDistance(),
            FareByAgePolicy.calculateFare(age, path.getFare())
        );
    }

    private Station getStation(List<Station> findResult, Long stationId) {
        return findResult.stream()
            .filter(station -> station.getId().equals(stationId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(STATION));
    }
}
