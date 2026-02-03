# 멜로디로 기억되는 일상, MeMody

**SWU 2025 Winter GURU2 Android - 27조 Memody 백엔드 레포지토리**
<img width="3840" height="2160" alt="image" src="https://github.com/user-attachments/assets/2402e5fa-930c-4824-9a75-bf2f29229203" />

> 🏆2025 Winter GURU2 최우수상(2등) 수상작🥈<br>
> 개발 기간: 2026.01.13 ~ 2026.01.27 (전체: 2025.12.29 ~ 2026.01.27)
<br>

### Core Features
- JWT 기반 사용자 인증
- 기록 CRUD
- 음악 검색 및 메타데이터 연동 (iTunes, SongLink)
- 위치 기반 사용자 및 기록 데이터 관리 (VWorld API)
- AI 기반 감정/키워드 분석 (Gemini API)
<br>

### Tech Stacks
<img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=coffeescript&logoColor=white">  <img src="https://img.shields.io/badge/spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white">  <img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white">

<br>

### Directory Structure
```
memody
 ┣ src
 ┃ ┣ main
 ┃ ┃ ┣ java
 ┃ ┃ ┃ ┗ com
 ┃ ┃ ┃ ┃ ┗ guru2
 ┃ ┃ ┃ ┃ ┃ ┗ memody
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ config                           // Security, JWT, Web 설정
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ controller                       // API Controller
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ dto                              // Request, Response DTO
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ entity                           // Domain Entity
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ exception                        // 커스텀 예외 처리
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ extractData                      // 초기 데이터 적재 로직
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ repository                       // JPA Repository
 ┃ ┃ ┃ ┃ ┃ ┃ ┣ service                          // 비즈니스 로직
 ┃ ┃ ┃ ┃ ┃ ┃ ┗ MemodyApplication.java
 ┃ ┃ ┗ resources
 ┃ ┃ ┃ ┣ static
 ┃ ┃ ┃ ┣ templates
 ┃ ┃ ┃ ┗ application.properties
 ┣ uploads
 ┃ ┗ images                                    // 사용자 업로드 이미지 저장소
 ┣ .gitignore
 ┣ build.gradle
 ┗ settings.gradle
```
