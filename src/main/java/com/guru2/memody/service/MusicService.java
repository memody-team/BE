package com.guru2.memody.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guru2.memody.Exception.NotFoundException;
import com.guru2.memody.Exception.UserNotFoundException;
import com.guru2.memody.dto.*;
import com.guru2.memody.entity.*;
import com.guru2.memody.entity.Record;
import com.guru2.memody.extractData.VWorldClient;
import com.guru2.memody.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MusicService {

    private final UserRepository userRepository;
    private final RecordRepository recordRepository;
    private final VWorldClient vWorldClient;
    private final MusicLikeRepository musicLikeRepository;
    private final RecordImageRepository recordImageRepository;
    private final ArtistRepository artistRepository;
    private final MusicRepository musicRepository;

    private final ITunesService itunesService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final AlbumRepository albumRepository;
    private final RecommendArtistRepository recommendArtistRepository;

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public List<MusicListResponseDto> searchTrack(String search) throws JsonProcessingException {
        String response = itunesService.searchTrackWithItunes(search);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        JsonNode results = root.path("results");

        List<MusicListResponseDto> trackList = new ArrayList<>();

        for (JsonNode trackNode : results) {

            Music music = null;
            music = musicRepository.findMusicByItunesId(trackNode.path("trackId").asLong()).orElse(
                    new Music()
            );
            music.setItunesId(trackNode.path("trackId").asLong());
            music.setTitle(trackNode.path("trackName").asText());
            music.setArtist(trackNode.path("artistName").asText());
            music.setAppleMusicUrl(trackNode.path("trackViewUrl").asText());
            music.setThumbnailUrl(trackNode.path("artworkUrl100").asText());

            musicRepository.save(music);

            trackList.add(new MusicListResponseDto(
                    music.getMusicId(),
                    trackNode.path("trackName").asText(),
                    trackNode.path("artistName").asText(),
                    trackNode.path("artworkUrl100").asText()
            ));

        }

        return trackList;
    }

    @Transactional
    public RecordPinResponseDto recordTrack(Long userId, MusicRecordDto musicRecordDto, List<MultipartFile> images) throws JsonProcessingException {
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                () -> new RuntimeException("User not found")
        );
        Music music = musicRepository.findMusicByMusicId(musicRecordDto.getMusicId());
        String spotifyUrl = itunesService.getSpotifyLinkFromItunes(music.getAppleMusicUrl());
        music.setSpotifyUrl(spotifyUrl);
        musicRepository.save(music);

        RegionFullName regionFullName = vWorldClient.setRecordRegion(musicRecordDto.getLongitude(), musicRecordDto.getLatitude());
        Record record = new Record();

        if (images != null){
            for (MultipartFile multipartFile : images){
                RecordImage recordImage = new RecordImage();
                if(multipartFile.isEmpty()) continue;

                String imageUrl = saveImage(multipartFile);

                recordImage.setRecord(record);
                recordImage.setImageUrl(imageUrl);
                recordImageRepository.save(recordImage);
            }
        }

        record.setRecordMusic(music);
        record.setText(musicRecordDto.getContent());
        record.setLatitude(musicRecordDto.getLatitude());
        record.setLongitude(musicRecordDto.getLongitude());
        record.setRecordLocation(regionFullName.getName());
        record.setRecordTime(LocalDateTime.now());
        record.setUser(user);
        recordRepository.save(record);

        RecordPinResponseDto recordPinResponseDto = new RecordPinResponseDto();
        recordPinResponseDto.setRecordId(record.getRecordId());
        recordPinResponseDto.setThumbnailUrl(music.getThumbnailUrl());
        recordPinResponseDto.setLatitude(record.getLatitude());
        recordPinResponseDto.setLongitude(record.getLongitude());
        return recordPinResponseDto;

    }


    public LikeResponseDto likeTrack(Long userId, Long musicId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );
        LikeResponseDto likeResponseDto = new LikeResponseDto();

        Music music = musicRepository.findMusicByMusicId(musicId);
        musicLikeRepository.findByUserAndMusic(user, music)
                        .ifPresentOrElse(existML -> {
                            musicLikeRepository.delete(existML);
                            likeResponseDto.setLikeType(LikeType.MUSIC);
                            likeResponseDto.setLike(false);
                            likeResponseDto.setLikeCount(0);
                                },
                                ()  -> {
                                    MusicLike newMusicLike = new MusicLike();
                                    newMusicLike.setUser(user);
                                    newMusicLike.setMusic(music);
                                    newMusicLike.setLikeDate(LocalDateTime.now());
                                    musicLikeRepository.save(newMusicLike);
                                    likeResponseDto.setLikeCount(1);
                                    likeResponseDto.setLikeType(LikeType.MUSIC);
                                    likeResponseDto.setLike(true);
                        });


        return likeResponseDto;
    }

    private static final String UPLOAD_DIR = "uploads/images/";

    public String saveImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new NotFoundException("IMAGE_FILE_EMPTY");
        }

        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));

            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + ext;

            Path path = Paths.get(UPLOAD_DIR, filename);
            Files.write(path, file.getBytes());

            return "/uploads/images/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public List<ArtistResponseDto> getArtistList(String search) throws JsonProcessingException {
        String response = itunesService.searchArtistWithItunes(search);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        JsonNode results = root.path("results");

        List<ArtistResponseDto> artistResponseDtos = new ArrayList<>();

        for (JsonNode trackNode : results) {

            Long itunesArtistId = trackNode.path("artistId").asLong();
            String artistName = trackNode.path("artistName").asText();

            String imageResponse = itunesService.lookupArtistImageWithItunes(itunesArtistId);
            ObjectMapper imageMapper = new ObjectMapper();
            JsonNode imageRoot = imageMapper.readTree(imageResponse);

            JsonNode imageResults = imageRoot.path("results");

            Artist artist = artistRepository.findByItunesArtistId(itunesArtistId)
                    .orElseGet(Artist::new);
            artist.setArtistName(artistName);
            artist.setItunesArtistId(itunesArtistId);

            artistRepository.save(artist);

            ArtistResponseDto artistResponseDto = new ArtistResponseDto();
            for (JsonNode imageResult : imageResults) {
                artistResponseDto.setImageUrl(imageResult.path("artworkUrl100").asText());
            }
            artistResponseDto.setArtistName(artist.getArtistName());
            artistResponseDto.setArtistId(artist.getArtistId());
            artistResponseDtos.add(artistResponseDto);
        }

        return artistResponseDtos;
    }

    public MusicDetailDto getMusicDetail(Long userId, Long musicId) {
        Music music = musicRepository.findById(musicId).orElseThrow(
                () -> new NotFoundException("Music Not Found")
        );
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );
        MusicDetailDto musicDetailDto = new MusicDetailDto();
        musicDetailDto.setThumbnailUrl(music.getThumbnailUrl());
        musicDetailDto.setTitle(music.getTitle());
        musicDetailDto.setArtistName(music.getArtist());
        musicDetailDto.setSpotifyMusicUrl(music.getSpotifyUrl());
        musicDetailDto.setAppleMusicUrl(music.getAppleMusicUrl());
        List<RecordListDto> recordListDtos = new ArrayList<>();
        List<Record> records = recordRepository.findAllByRecordMusic(music);
        for (Record record : records){
            RecordListDto recordListDto = new RecordListDto();
            recordListDto.setUserName(record.getUser().getName());
            recordListDto.setContent(record.getText());
            recordListDto.setRegionName(record.getRecordLocation());
            recordListDto.setCreationDate(record.getRecordTime().format(formatter));
            recordListDtos.add(recordListDto);
        }

        musicDetailDto.setRecord(recordListDtos);
        musicDetailDto.setLiked(musicLikeRepository.findByUserAndMusic(user, music).isPresent());

        return musicDetailDto;
    }


    public List<MusicListResponseDto> getLikedMusicList(Long userId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );
        List<MusicLike> musicLikes = musicLikeRepository.findAllByUser(user);
        List<MusicListResponseDto> musicListResponseDtos = new ArrayList<>();
        for (MusicLike musicLike : musicLikes) {
            MusicListResponseDto musicListResponseDto = new MusicListResponseDto(musicLike.getMusic().getMusicId(), musicLike.getMusic().getTitle(),
                    musicLike.getMusic().getArtist(), musicLike.getMusic().getThumbnailUrl());
            musicListResponseDtos.add(musicListResponseDto);
        }
        return musicListResponseDtos;
    }

    public AlbumDetailDto getAlbumDetail(Long userId, Long albumId) {
        Album album = albumRepository.findById(albumId).orElseThrow(
                () -> new NotFoundException("Album Not Found")
        );
        AlbumDetailDto albumDetailDto = new AlbumDetailDto();
        albumDetailDto.setAlbumName(album.getTitle());
        albumDetailDto.setArtistName(album.getArtist());
        albumDetailDto.setThumbnailUrl(album.getThumbnailUrl());
        List<MusicListResponseDto> musicListResponseDtos = new ArrayList<>();
        for(Music music : album.getIncludedSongs()){
            MusicListResponseDto musicListResponseDto = new MusicListResponseDto(music.getMusicId(), music.getTitle(), music.getArtist(), music.getThumbnailUrl());
            musicListResponseDtos.add(musicListResponseDto);
        }
        albumDetailDto.setMusicList(musicListResponseDtos);
        return albumDetailDto;
    }

    public ArtistRecommendDto getArtistRecommendDetail(Long userId) {
        User user = userRepository.findUserByUserId(userId).orElseThrow(
                UserNotFoundException::new
        );
        RecommendArtist recommendArtist = recommendArtistRepository.findByUser(user).orElseThrow(
                () -> new NotFoundException("Recommend Artist Not Found")
        );
        ArtistRecommendDto artistRecommendDto = new ArtistRecommendDto();
        List<MusicListResponseDto> musicListResponseDtos = new ArrayList<>();
        List<Music> musics = recommendArtist.getRecommendedItems();
        for (Music music : musics){
            artistRecommendDto.setArtistName(music.getArtist());
            MusicListResponseDto musicListResponseDto = new MusicListResponseDto(music.getMusicId(), music.getTitle(), music.getArtist(), music.getThumbnailUrl());
            musicListResponseDtos.add(musicListResponseDto);
        }
        artistRecommendDto.setMusicList(musicListResponseDtos);
        return artistRecommendDto;
    }


}
