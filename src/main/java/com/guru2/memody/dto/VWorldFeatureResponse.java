package com.guru2.memody.dto;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class VWorldFeatureResponse {
    private Response response;

    @Getter
    @ToString
    public static class Response {
        private String status; // "OK" 또는 "ERROR" 또는 "NOT_FOUND"
        private Error error;   // 에러 상세 정보
        private Result result; // 성공 데이터
    }

    @Getter
    @ToString
    public static class Error {
        private String level;
        private String code;
        private String text; // 에러 메시지
    }

    @Getter
    @ToString
    public static class Result {
        private FeatureCollection featureCollection;
    }

    @Getter
    @ToString
    public static class FeatureCollection {
        private List<Feature> features;
    }

    @Getter
    @ToString
    public static class Feature {
        private Properties properties;
    }

    @Getter
    @ToString
    public static class Properties {
        private String emd_cd;
        private String full_nm;
    }
}