package com.guru2.memody.extractData;

import com.guru2.memody.Exception.RegionWrongException;
import com.guru2.memody.dto.RegionFullName;
import com.guru2.memody.dto.VWorldFeatureResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;

@Slf4j
@Component
public class VWorldClient {

    private static final String BASE_URL = "https://api.vworld.kr/req/data";
    private static final String API_KEY = System.getenv("VWORLD_API_KEY");

    private final RestTemplate restTemplate = new RestTemplate();

    public List<VWorldFeatureResponse.Feature> getFNByPrefix(String codePrefix){
        String uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("service", "data")
                .queryParam("version", 2.0)
                .queryParam("request", "GetFeature")
                .queryParam("data", "LT_C_ADEMD_INFO")
                .queryParam("key", API_KEY)
                .queryParam("format", "json")
                .queryParam("singleSearch", "Y")
                .queryParam("attrFilter", "emd_cd:like:" + codePrefix)
                .queryParam("size", 1000)
                .toUriString();

        VWorldFeatureResponse response = restTemplate.getForObject(uri, VWorldFeatureResponse.class);

        if (response == null || response.getResponse() == null || response.getResponse().getResult() == null) {
            return List.of();
        }

        return response.getResponse()
                .getResult()
                .getFeatureCollection().getFeatures();
    }

    public List<VWorldFeatureResponse.Feature> getFNByGpsPoint(Double lon, Double lat) { // 순서: 경도, 위도

        String geomFilter = "POINT(" + lon + " " + lat + ")";

        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("service", "data")
                .queryParam("version", "2.0")
                .queryParam("request", "GetFeature")
                .queryParam("data", "LT_C_ADEMD_INFO")
                .queryParam("key", API_KEY)
                .queryParam("format", "json")
                .queryParam("geomFilter", geomFilter)
                .queryParam("singleSearch", "Y")
                .queryParam("buffer", "10")
                .queryParam("crs", "EPSG:4326") // 좌표계
                .queryParam("size", 1)
                .build()
                .encode()
                .toUri();
        log.info("VWorld Request URI: {}", uri);

        try {

            VWorldFeatureResponse response = restTemplate.getForObject(uri, VWorldFeatureResponse.class);

            // 4. Null 체크
            if (response == null
                    || response.getResponse() == null
                    || response.getResponse().getStatus().equals("ERROR") // 에러 상태 체크 추가
                    || response.getResponse().getResult() == null) {

                System.out.println("VWorld API Error or No Result");
                if (response != null && response.getResponse() != null && response.getResponse().getError() != null) {
                    log.info("Error Detail: " + response.getResponse().getError().getText());
                }
                return new ArrayList<>();
            }

            return response.getResponse()
                    .getResult()
                    .getFeatureCollection().getFeatures();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }




    public List<RegionFullName> getRegions() {
        List<String> prefixes = List.of(
                "11", // 서울
                "26", // 부산
                "27", // 대구
                "28", // 인천
                "29", // 광주
                "30", // 대전
                "31", // 울산
                "36", // 세종
                "41", // 경기
                "43", // 충북
                "44", // 충남
                "46", // 전남
                "47", // 경북
                "48", // 경남
                "50", // 제주
                "51", // 강원
                "52"  // 전북
        );
        System.out.println("prefixes List" + prefixes);

        List<RegionFullName> result = new ArrayList<>();

        for (String prefix : prefixes) {
            List<VWorldFeatureResponse.Feature> features =
                    getFNByPrefix(prefix);
            System.out.println("regions List" + features);

            for (VWorldFeatureResponse.Feature feature : features) {
                var p = feature.getProperties();

                if (!p.getEmd_cd().startsWith(prefix)) continue;

                RegionFullName regionFullName = new RegionFullName();
                regionFullName.setCode(p.getEmd_cd());
                regionFullName.setName(p.getFull_nm());

                result.add(regionFullName);
                System.out.println("regionFN" + regionFullName);
            }
        }

        return result;
    }

    public RegionFullName setRecordRegion(Double lon, Double lat){
        RegionFullName regionFullName = new RegionFullName();

        List<VWorldFeatureResponse.Feature> features = getFNByGpsPoint(lon, lat);

        if (features == null || features.isEmpty()) {
            log.info("바다 혹은 데이터 미제공 지역: " + lon + ", " + lat);
            regionFullName.setCode("00000000");
            regionFullName.setName("위치 정보 없음");

            return regionFullName;
//            throw new RegionWrongException("Cannt found region from user GPS point");
        }

        var p = features.get(0).getProperties();

        regionFullName.setCode(p.getEmd_cd());
        regionFullName.setName(p.getFull_nm());

        return regionFullName;
    }
}
