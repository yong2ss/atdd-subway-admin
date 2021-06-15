package nextstep.subway.section.domain;

import nextstep.subway.section.exception.CannotRemoveSectionSizeException;
import nextstep.subway.section.exception.ExistSameStationsException;
import nextstep.subway.section.exception.NotExistAnySameStationException;
import nextstep.subway.section.exception.NotUnderSectionDistanceException;
import nextstep.subway.station.domain.Station;
import org.springframework.util.CollectionUtils;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Embeddable
public class Sections {
    @OneToMany(mappedBy = "line", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Section> sections = new ArrayList<>();

    public void add(Section newSection) {
        if (!CollectionUtils.isEmpty(sections)) {
            validateSection(newSection);
        }

        connectIfExistSameUpStation(newSection);
        connectIfExistSameDownStation(newSection);

        this.sections.add(newSection);
    }

    public void delete(Station station) {
        if (sections.size() <= 1) {
            throw new CannotRemoveSectionSizeException();
        }

        Optional<Section> upSection = sections.stream()
                .filter(section -> section.hasSameDownStation(station))
                .findFirst();

        Optional<Section> downSection = sections.stream()
                .filter(section -> section.hasSameUpStation(station))
                .findFirst();

        if (!upSection.isPresent()) {
            sections.remove(downSection.get());
        }

        if (!downSection.isPresent()) {
            sections.remove(upSection.get());
        }

        disconnectSection(upSection.get(), downSection.get());
    }

    private void disconnectSection(Section upSection, Section downSection) {
        upSection.updateDownStation(downSection, false);
        sections.remove(downSection);
    }

    public List<Station> getStations() {
        return getOrderedSections().stream()
                .flatMap(section -> section.getStations().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Section> getOrderedSections() {

        Optional<Section> firstSection = findFirstSection();

        List<Section> result = new ArrayList<>();
        while (firstSection.isPresent()) {
            Section section = firstSection.get();
            result.add(section);
            firstSection = sections.stream()
                    .filter(nextSection -> nextSection.getUpStation().equals(section.getDownStation()))
                    .findFirst();
        }

        return result;
    }

    private void connectIfExistSameDownStation(Section newSection) {
        sections.stream()
                .filter(section -> section.hasSameDownStation(newSection.getDownStation()))
                .findFirst()
                .ifPresent(section -> section.updateDownStation(newSection, true));
    }

    private void connectIfExistSameUpStation(Section newSection) {
        sections.stream()
                .filter(section -> section.hasSameUpStation(newSection.getUpStation()))
                .findFirst()
                .ifPresent(section -> section.updateUpStation(newSection, true));
    }

    private Optional<Section> findFirstSection() {
        return this.sections.stream()
                .filter(section -> !getDownStations().contains(section.getUpStation()))
                .findFirst();
    }

    private List<Station> getUpStations() {
        return this.sections.stream()
                .map(Section::getUpStation)
                .collect(Collectors.toList());
    }

    private List<Station> getDownStations() {
        return this.sections.stream()
                .map(Section::getDownStation)
                .collect(Collectors.toList());
    }

    private void validateSection(Section newSection) {
        if (isNotValidDistance(newSection)) {
            throw new NotUnderSectionDistanceException();
        }

        boolean containsUpStation = containsUpStation(newSection.getUpStation());
        boolean containsDownStation = containsDownStation(newSection.getDownStation());

        if (containsUpStation && containsDownStation) {
            throw new ExistSameStationsException();
        }

        if (!containsUpStation && !containsDownStation) {
            throw new NotExistAnySameStationException();
        }
    }

    private boolean containsDownStation(Station station) {
        return getStations().contains(station);
    }

    private boolean containsUpStation(Station station) {
        return getStations().contains(station);
    }

    private boolean isNotValidDistance(Section newSection) {
        return sections.stream()
                .anyMatch(section -> (isSameUpStation(newSection, section) || isSameDownStation(newSection, section))
                        && section.getDistance() == newSection.getDistance());
    }

    private boolean isSameDownStation(Section newSection, Section section) {
        return section.hasSameDownStation(newSection.getDownStation());
    }

    private boolean isSameUpStation(Section newSection, Section section) {
        return section.hasSameUpStation(newSection.getUpStation());
    }
}
