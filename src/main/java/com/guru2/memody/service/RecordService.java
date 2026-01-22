package com.guru2.memody.service;

import com.guru2.memody.Exception.NotAllowedException;
import com.guru2.memody.Exception.RecordNotFoundException;
import com.guru2.memody.Exception.UserNotFoundException;
import com.guru2.memody.dto.*;
import com.guru2.memody.entity.*;
import com.guru2.memody.entity.Record;
import com.guru2.memody.repository.LikeRepository;
import com.guru2.memody.repository.RecordImageRepository;
import com.guru2.memody.repository.RecordRepository;
import com.guru2.memody.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RecordService {
    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final RecordImageRepository recordImageRepository;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<RecordPinResponseDto> getMeInMap(Long userId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );

        List<Record> records = recordRepository.findAllByUser(user);
        List<RecordPinResponseDto> recordPinResponseDtos = new ArrayList<>();
        for (Record record : records) {
            RecordPinResponseDto recordPinResponseDto = new RecordPinResponseDto();
            recordPinResponseDto.setRecordId(record.getRecordId());
            recordPinResponseDto.setThumbnailUrl(record.getRecordMusic().getThumbnailUrl());
            recordPinResponseDto.setLatitude(record.getLatitude());
            recordPinResponseDto.setLongitude(record.getLongitude());
            recordPinResponseDtos.add(recordPinResponseDto);
        }

        return recordPinResponseDtos;
    }

    public List<RecordPinResponseDto> getOtherInMap(Long userId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );

        List<Record> records = recordRepository.findAllByUserNot(user);
        List<RecordPinResponseDto> recordPinResponseDtos = new ArrayList<>();
        for (Record record : records) {
            RecordPinResponseDto recordPinResponseDto = new RecordPinResponseDto();
            recordPinResponseDto.setRecordId(record.getRecordId());
            recordPinResponseDto.setThumbnailUrl(record.getRecordMusic().getThumbnailUrl());
            recordPinResponseDto.setLatitude(record.getLatitude());
            recordPinResponseDto.setLongitude(record.getLongitude());
            recordPinResponseDtos.add(recordPinResponseDto);
        }

        return recordPinResponseDtos;
    }

    public RecordDetailDto getRecordDetail(Long userId, Long recordId) {
        Record record = recordRepository.findById(recordId).orElseThrow(
                RecordNotFoundException::new
        );
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );
        RecordDetailDto recordDetailDto = new RecordDetailDto();

        List<RecordImage> recordImages = recordImageRepository.findAllByRecord(record);
        List<String> strings = new ArrayList<>();
        for (RecordImage recordImage : recordImages) {
            String imageUrl = recordImage.getImageUrl();
            strings.add(imageUrl);
        }
        recordDetailDto.setImageUrls(strings);

        if(user == record.getUser()) {
            recordDetailDto.setLiked(null);
            recordDetailDto.setLikeCount(null);
        }
        else recordDetailDto.setLiked(likeRepository.findByUserAndRecord(user, record).isPresent());

        recordDetailDto.setTitle(record.getRecordMusic().getTitle());
        recordDetailDto.setArtist(record.getRecordMusic().getArtist());
        recordDetailDto.setContent(record.getText());
        recordDetailDto.setThumbnail(record.getRecordMusic().getThumbnailUrl());
        recordDetailDto.setSpotifyUrl(record.getRecordMusic().getSpotifyUrl());
        recordDetailDto.setITunesUrl(record.getRecordMusic().getAppleMusicUrl());
        recordDetailDto.setRecordDate(record.getRecordTime().format(formatter));
        recordDetailDto.setLikeCount(record.getLikeCount());
        return recordDetailDto;
    }

    public List<CommunityResponseDto> getCommunity() {
//        User user = userRepository.findUserByUserId(userId).orElseThrow(
//                UserNotFoundException::new
//        );
        List<CommunityResponseDto> communityResponseDtos = new ArrayList<>();
        List<Record> records = recordRepository.findAllByOrderByRecordTimeDesc();
        for (Record record : records) {
            CommunityResponseDto communityResponseDto = new CommunityResponseDto();
            communityResponseDto.setRecordId(record.getRecordId());
            communityResponseDto.setUserName(record.getUser().getName());
            communityResponseDto.setMusicName(record.getRecordMusic().getTitle());
            communityResponseDto.setArtistName(record.getRecordMusic().getArtist());
            communityResponseDto.setContent(record.getText());
            communityResponseDto.setThumbnailUrl(record.getRecordMusic().getThumbnailUrl());
            communityResponseDto.setSpotifyUrl(record.getRecordMusic().getSpotifyUrl());
            communityResponseDto.setAppleMusicUrl(record.getRecordMusic().getAppleMusicUrl());
            communityResponseDto.setLikeCount(record.getLikeCount());
            communityResponseDto.setRecordDate(record.getRecordTime().format(formatter));
            communityResponseDto.setRegionName(record.getRecordLocation());
            communityResponseDtos.add(communityResponseDto);
        }

        return communityResponseDtos;
    }

    @Transactional
    public LikeResponseDto likeRecord(Long userId, Long recordId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );
        Record record = recordRepository.findById(recordId).orElseThrow(
                RecordNotFoundException::new
        );
        User writer = userRepository.findUserByUserId(record.getUser().getUserId()).orElseThrow(
                () -> new UserNotFoundException("Writer not found")
        );

        if (user == writer) {
            throw new NotAllowedException("Self-Like Not Allowed");
        }

        Like like = likeRepository.findByUserAndRecord(user, record)
                .map(existLike -> {
                    if (existLike.getIsLiked()){
                        existLike.setIsLiked(false);
                        existLike.getRecord().setLikeCount(existLike.getRecord().getLikeCount() - 1);
                    }
                    else{
                        existLike.setIsLiked(true);
                        existLike.getRecord().setLikeCount(existLike.getRecord().getLikeCount() + 1);
                    }
                    return existLike;
                })
                .orElseGet(() -> {
                    Like newLike = Like.builder()
                            .user(user)
                            .record(record)
                            .build();
                    record.setLikeCount(record.getLikeCount() + 1);
                    return newLike;
                    }
                );
        likeRepository.save(like);

        LikeResponseDto likeResponseDto = new LikeResponseDto();
        likeResponseDto.setLikeType(LikeType.RECORD);
        likeResponseDto.setLike(like.getIsLiked());
        likeResponseDto.setLikeCount(record.getLikeCount());

        return likeResponseDto;
    }

    public List<MyRecordResponseDto> getMyRecordList(Long userId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );
        List<Record> records = recordRepository.findAllByUserOrderByRecordTimeDesc(user);
        List<MyRecordResponseDto> myRecordResponseDtos = new ArrayList<>();
        for (Record record : records) {
            MyRecordResponseDto myRecordResponseDto = new MyRecordResponseDto();
            myRecordResponseDto.setTitle(record.getRecordMusic().getTitle());
            myRecordResponseDto.setArtist(record.getRecordMusic().getArtist());
            myRecordResponseDto.setContent(record.getText());
            myRecordResponseDto.setRecordDate(record.getRecordTime().format(formatter));
            myRecordResponseDto.setSpotifyUrl(record.getRecordMusic().getSpotifyUrl());
            myRecordResponseDto.setITunesUrl(record.getRecordMusic().getAppleMusicUrl());
            myRecordResponseDto.setThumbnail(record.getRecordMusic().getThumbnailUrl());
            myRecordResponseDto.setRegionName(record.getRecordLocation());
            myRecordResponseDtos.add(myRecordResponseDto);
        }

        return myRecordResponseDtos;
    }

    public List<PinnedListDto> getLatentPin(Long userId){
        User user = userRepository.findUserByUserId(userId)
                .orElseThrow(UserNotFoundException::new);
        List<Record> records = recordRepository.findAllByUserOrderByRecordTimeDesc(user);
        List<PinnedListDto> pinnedListDtos = new ArrayList<>();

        String lastDate = "";
        PinnedListDto currentPinnedListDto = null;

        for (Record record : records) {
            String recordDate = record.getRecordTime().format(formatter);

            if(!recordDate.equals(lastDate)){
                currentPinnedListDto = new PinnedListDto();
                currentPinnedListDto.setPinnedDate(recordDate);
                currentPinnedListDto.setMusicList(new ArrayList<>());

                pinnedListDtos.add(currentPinnedListDto);
                lastDate = recordDate;
            }

            PinnedRecordDto pinnedRecordDto = new PinnedRecordDto();
            pinnedRecordDto.setTitle(record.getRecordMusic().getTitle());
            pinnedRecordDto.setArtist(record.getRecordMusic().getArtist());
            pinnedRecordDto.setThumbnailUrl(record.getRecordMusic().getThumbnailUrl());

            String location = record.getRecordLocation();
            if(location != null && !location.isEmpty()){
                String[] parts = location.split(" ");
                pinnedRecordDto.setRegion(parts[1]);
            }

            if (currentPinnedListDto != null) {
                currentPinnedListDto.getMusicList().add(pinnedRecordDto);
            }
        }
        return pinnedListDtos;
    }
}
