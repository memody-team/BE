package com.guru2.memody.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class VWorldFeatureResponse {
    private Response response;

    @Getter
    public static class Response {
        private Result result;
    }

    @Getter
    public static class Result {
        private FeatureCollection featureCollection;
    }

    @Getter
    public static class FeatureCollection {
        private List<Feature> features;
    }

    @Getter
    public static class Feature {
        private Properties properties;
    }

    @Getter
    public static class Properties {
        private String emd_cd;
        private String full_nm;
    }
}
