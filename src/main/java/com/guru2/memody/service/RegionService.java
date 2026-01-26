package com.guru2.memody.service;

import com.guru2.memody.extractData.VWorldClient;
import com.guru2.memody.dto.RegionFullName;
import com.guru2.memody.entity.Region;
import com.guru2.memody.repository.RegionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;
    private final VWorldClient vWorldClient;

    // 지역명 데이터 적재
    @Transactional
    public void initRegions() {
        List<RegionFullName> regionFN = vWorldClient.getRegions();

        for (RegionFullName regionFullName : regionFN) {
            Region region = Region.builder()
                    .code(regionFullName.getCode())
                    .fullName(regionFullName.getName())
                    .build();
            regionRepository.save(region);
        }
    }

    // 지역명 검색
    public ResponseEntity<List<String>> searchRegions(@RequestParam String region) {
        List<Region> regions = regionRepository.findAllByFullNameContaining(region);
        List<String> regionNames = new ArrayList<>();

        if (regions.isEmpty()) {
            return new ResponseEntity<>(regionNames, HttpStatus.OK);
        }

        for (Region reg : regions) {
            regionNames.add(reg.getFullName());
        }
        return new ResponseEntity<>(regionNames, HttpStatus.OK);
    }

    public ResponseEntity<String> getRegion(@RequestParam Double lat, @RequestParam Double lon) {
        String response = vWorldClient.getRegionName(lat, lon);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
