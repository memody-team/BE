package com.guru2.memody.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class PinnedListDto {
    private String pinnedDate;
    private List<PinnedRecordDto> musicList;

}
