# 🏃 작심삼일

> **"3일씩 꾸준하게 나아가자."**
> 생성형 AI(Google Gemini)를 활용한 목표 추천 및 달성 관리 백엔드 서비스

## 📖 프로젝트 소개 (Project Description)
**작심삼일**은 사용자가 막연하게 생각하는 목표를 구체적이고 실천 가능한 행동으로 바꿔주는 API 서버입니다.
사용자가 관심 있는 카테고리와 의도를 입력하면, **Google Gemini API**를 통해 AI 코치가 구체적인 실행 목표를 제안해줍니다. 복잡한 회원가입 없이 디바이스 ID를 이용한 간편 접근 방식을 채택하여 사용자 경험(UX)을 높였습니다.

## 🛠 기술 스택 (Tech Stack)

### Backend
- **Java 21 (LTS)**
- **Spring Boot 3.x**
- **Spring Data JPA**
- **Gradle**

### AI & Data Processing
- **Google Gemini API** (Generative AI)
- **Jackson** (JSON Processing)

### Infrastructure & Database
- **MySQL 8.0**
- **Docker & Docker Compose**
- **AWS(RDS & EC2)**

---

## 📂 주요 기능 (Key Features)

1.  **🤖 AI 기반 목표 추천 (AI Goal Coaching)**
    - 사용자의 의도(Intent)를 분석하여 행동 중심적인 3가지 목표를 생성합니다.
    - 프롬프트 엔지니어링을 통해 정형화된 JSON 데이터를 응답받습니다.

2.  **📅 목표 생애주기 관리**
    - 추천받은 목표의 저장, 진행률 확인, 완료 처리를 관리합니다.

---

## 💡 핵심 기능별 코드 설명
### LLM을 통한 주제 추천
```java
String prompt = String.format("""
            당신은 목표 달성을 돕는 AI 코치입니다.
            사용자가 입력한 [카테고리]와 [의도]를 바탕으로 구체적이고 실천 가능한 목표 3가지를 추천해주세요.

            [입력 정보]
            - 카테고리: %s
            - 의도: %s

            [제약 조건]
            1. 목표는 행동 중심적이고 명확해야 합니다.
            2. 다른 말은 절대 하지 말고, 오직 JSON 문자열 배열 포맷으로만 응답하세요.
            3. 응답 예시: ["매일 아침 30분 조깅하기", "주 3회 샐러드 먹기", "엘리베이터 대신 계단 이용하기"]
            """, category, intention);
String rawResponse = geminiClient.generate(prompt);
JsonNode rootNode = objectMapper.readTree(rawResponse);

            String textContent = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            String jsonString = textContent
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});
```

---

## 💡 주요 코드 설명

### 1. LLM(Gemini) 비정형 응답의 정형화 처리
AI 모델은 응답 시 마크다운(```json)이나 불필요한 서술어를 포함하는 경우가 많습니다. 이를 클라이언트가 바로 사용할 수 있는 `List<String>` 형태로 안정적으로 변환하기 위해 **2단계 파싱 전략**을 도입했습니다.

**[GoalService.java]**
```java
private List<String> parseResponse(String rawResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawResponse);

            String textContent = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            String jsonString = textContent
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});

        } catch (Exception e) {
            // ...
        }
    }
```
### 2. 챌린지 시작 및 일정 자동 생
3일 챌린지의 생애주기, 상태 유효성 검증, 티어(점수) 산정을 담당합니다.

**[ProgressService.java]**
```java
@Transactional
public RunDetailResponse startRun(Long userId, StartRunRequest req) {
    // ... (유효성 검증 생략)

    // 1. 챌린지 Run 생성 (시작일 설정)
    ChallengeRun run = runRepository.save(ChallengeRun.start(goal, startDate));

    // 2. 3일치 Day 데이터 일괄 생성 (1일차~3일차)
    dayRepository.saveAll(List.of(
            ChallengeDay.create(run, 1, startDate),
            ChallengeDay.create(run, 2, startDate.plusDays(1)),
            ChallengeDay.create(run, 3, startDate.plusDays(2))
    ));

    return toResponse(run, days);
}
```
### 3. 티어 산정 및 조기 종료 조건
하루를 마무리(finalizeDay)할 때, 실패(FAIL) 조건에 해당하면 즉시 챌린지를 종료시키고, 3일 모두 완료했을 때 최종 티어(GOLD, BRONZE)를 결정하는 핵심 로직입니다.

**[GoalService.java]**
```java
// updateDay 메서드 - finalizeDay 처리 부분
if (wantsFinalize) {
    day.finalizeDay(); // 해당 일자 확정 처리

    // 실패(FAIL) 및 부분 성공(PARTIAL) 횟수 집계
    long failCnt = days.stream().filter(d -> d.getDayResult() == DayResult.FAIL).count();
    long poorCnt = days.stream().filter(d -> d.getDayResult() == DayResult.PARTIAL).count();

    // [Fail-Fast 정책]
    // 1. 한번이라도 '실패' 기록 시 -> 즉시 FAIL 처리 및 종료
    // 2. '세모(Partial)'가 2회 이상 누적 시 -> 즉시 FAIL 처리 및 종료
    if (failCnt >= 1 || poorCnt >= 2) {
        run.endWithTier(TierStatus.FAIL);
        return toResponse(run, days);
    }

    // [최종 티어 결정] 3일 모두 확정된 경우
    boolean allFinalized = days.stream().allMatch(ChallengeDay::isFinalized);
    if (allFinalized) {
        // 성공(DONE) 3회 -> GOLD
        // 성공 2회 + 세모 1회 -> BRONZE
        TierStatus finalTier = (successCnt == 3) ? TierStatus.GOLD : TierStatus.BRONZE;
        run.endWithTier(finalTier);
    }
}
```
