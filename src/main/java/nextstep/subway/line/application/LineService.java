package nextstep.subway.line.application;

import nextstep.subway.exception.LineException;
import nextstep.subway.exception.SectionException;
import nextstep.subway.line.domain.line.Line;
import nextstep.subway.line.domain.line.LineRepository;
import nextstep.subway.line.domain.section.Distance;
import nextstep.subway.line.domain.section.Section;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.SectionRequest;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.dto.StationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class LineService {
    private LineRepository lineRepository;
    private StationService stationService;

    public LineService(LineRepository lineRepository, StationService stationService) {
        this.lineRepository = lineRepository;
        this.stationService = stationService;
    }

    public LineResponse saveLine(LineRequest request) {
        Station upStation = stationService.findById(request.getUpStationId());
        Station downStation = stationService.findById(request.getDownStationId());
        Line persistLine = lineRepository.save(new Line(request.getName(), request.getColor(), upStation, downStation, request.getDistance()));
        List<StationResponse> stations = getStations(persistLine).stream()
                .map(it -> StationResponse.of(it))
                .collect(Collectors.toList());
        return LineResponse.of(persistLine, stations);
    }

    public List<LineResponse> findLines() {
        List<Line> persistLines = findAllLine();
        return persistLines.stream()
                .map(line -> {
                    List<StationResponse> stations = getStations(line).stream()
                            .map(it -> StationResponse.of(it))
                            .collect(Collectors.toList());
                    return LineResponse.of(line, stations);
                })
                .collect(Collectors.toList());
    }

    public Line findLineById(Long id) {
        return lineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("노선이 존재하지 않습니다."));
    }

    public LineResponse findLineResponseById(Long id) {
        Line persistLine = findOneLineWithSectionsById(id)
                .orElseThrow(() -> new RuntimeException("노선이 존재하지 않습니다."));
        List<StationResponse> stations = getStations(persistLine).stream()
                .map(it -> StationResponse.of(it))
                .collect(Collectors.toList());
        return LineResponse.of(persistLine, stations);
    }

    public void updateLine(Long id, LineRequest lineUpdateRequest) {
        Line persistLine = lineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("노선이 존재하지 않습니다."));
        persistLine.update(new Line(lineUpdateRequest.getName(), lineUpdateRequest.getColor()));
    }

    public void deleteLineById(Long id) {
        lineRepository.deleteById(id);
    }

    public void addLineStation(Long lineId, SectionRequest request) {
        Line line = findOneLineWithSectionsById(lineId).orElseThrow(() -> new LineException("노선이 존재하지 않습니다."));
        Station upStation = stationService.findStationById(request.getUpStationId());
        Station downStation = stationService.findStationById(request.getDownStationId());
        line.addSection(new Section(line, upStation, downStation, request.getDistance()));
    }

    public void removeLineStation(Long lineId, Long stationId) {
        Line line = findLineById(lineId);
        Station station = stationService.findStationById(stationId);
        if (line.getSections().size() <= 1) {
            throw new RuntimeException();
        }

        Optional<Section> upLineStation = line.getSections().stream()
                .filter(it -> it.getUpStation() == station)
                .findFirst();
        Optional<Section> downLineStation = line.getSections().stream()
                .filter(it -> it.getDownStation() == station)
                .findFirst();

        if (upLineStation.isPresent() && downLineStation.isPresent()) {
            Station newUpStation = downLineStation.get().getUpStation();
            Station newDownStation = upLineStation.get().getDownStation();
            int newDistance = upLineStation.get().getDistance().getDistance() + downLineStation.get().getDistance().getDistance();
            line.getSections().add(new Section(line, newUpStation, newDownStation, newDistance));
        }

        upLineStation.ifPresent(it -> line.getSections().remove(it));
        downLineStation.ifPresent(it -> line.getSections().remove(it));
    }


    public List<Station> getStations(Line line) {
        if (line.getSections().isEmpty()) {
            return Arrays.asList();
        }

        List<Station> stations = new ArrayList<>();
        Station downStation = findUpStation(line);
        stations.add(downStation);

        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = line.getSections().stream()
                    .filter(it -> it.getUpStation() == finalDownStation)
                    .findFirst();
            if (!nextLineStation.isPresent()) {
                break;
            }
            downStation = nextLineStation.get().getDownStation();
            stations.add(downStation);
        }

        return stations;
    }

    private Station findUpStation(Line line) {
        Station downStation = line.getSections().get(0).getUpStation();
        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = line.getSections().stream()
                    .filter(it -> it.getDownStation() == finalDownStation)
                    .findFirst();
            if (!nextLineStation.isPresent()) {
                break;
            }
            downStation = nextLineStation.get().getUpStation();
        }

        return downStation;
    }

    private List<Line> findAllLine() {
        return lineRepository.findAll();
    }

    private Optional<Line> findOneLine(Long id) {
        return lineRepository.findById(id);
    }

    private Optional<Line> findOneLineWithSectionsById(Long id) {
        return lineRepository.findLineWithSectionsById(id);
    }

}
