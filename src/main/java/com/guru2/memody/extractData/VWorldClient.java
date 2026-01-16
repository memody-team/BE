package com.guru2.memody.extractData;

import com.guru2.memody.dto.RegionFullName;
import com.guru2.memody.dto.VWorldFeatureResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

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

    public List<RegionFullName> getRegions() {
        // 시도 코드 prefix (서울만 먼저)
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
}
