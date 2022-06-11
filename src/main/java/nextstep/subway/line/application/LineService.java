package nextstep.subway.line.application;

import nextstep.subway.section.dto.SectionRequest;
import nextstep.subway.station.application.StationService;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.section.domain.Section;
import nextstep.subway.station.domain.SectionStations;
import nextstep.subway.station.domain.Station;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LineService {
    public static final String NO_ELEMENT_SEARCH_BY_ID_MSG = "해당 ID를 조회할 수 없습니다.";

    private LineRepository lineRepository;
    private StationService stationService;

    public LineService(LineRepository lineRepository, StationService stationService) {
        this.lineRepository = lineRepository;
        this.stationService = stationService;
    }

    @Transactional
    public LineResponse saveLine(LineRequest lineRequest) {
        Line persistLine = lineRequest.toLine();
        lineRepository.save(persistLine);
        SectionStations sectionStations = stationService.findUpDownStation(lineRequest.getUpStationId(),
                                                                           lineRequest.getDownStationId());
        Section section = new Section(sectionStations.getUpStation()
                                      ,sectionStations.getDownStation()
                                      ,lineRequest.getDistance());
        persistLine.addSection(section);

        return LineResponse.of(persistLine);
    }

    public List<LineResponse> findAllLines() {
        List<Line> lines = lineRepository.findAll();

        return lines.stream()
                .map(line -> LineResponse.of(line))
                .collect(Collectors.toList());
    }

    public LineResponse findById(Long id) {
        Line line = lineRepository.findById(id).orElseThrow(() -> new NoSuchElementException(NO_ELEMENT_SEARCH_BY_ID_MSG));
        return LineResponse.of(line);
    }

    @Transactional
    public void deleteLineById(Long id) {
        lineRepository.deleteById(id);
    }

    @Transactional
    public void saveSection(Long id, SectionRequest sectionRequest) {
        Line line = lineRepository.findById(id).orElseThrow(() -> new NoSuchElementException(NO_ELEMENT_SEARCH_BY_ID_MSG));
        Station upStation = stationService.findById(sectionRequest.getUpStationId());
        Station downStation = stationService.findById(sectionRequest.getDownStationId());
        Section section = new Section(upStation, downStation, sectionRequest.getDistance());
        line.addSection(section);
    }

    @Transactional
    public void removeSectionByStationId(Long lineId, Long stationId) {
        Line line = lineRepository.findById(lineId).orElseThrow(() -> new NoSuchElementException(NO_ELEMENT_SEARCH_BY_ID_MSG));
        Station station = stationService.findById(stationId);
        line.removeSectionByStation(station);
    }
}
