package com.example.demo.story;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.example.demo.result.ResultDTO;

@Service
public class StoryService {

	// AI에게 주는 규칙 (모든 이야기에 똑같이 적용)
	private static final String SYSTEM_PROMPT = """
			너는 아시아 인생 시뮬레이션 게임 '환승인생'의 인생 이야기 작가야.
			[캐릭터 기록]은 게임 속 가상 인물의 일생이고, [역사·통계 자료]는 실제 자료야.
			이 글은 사진첩처럼 나이대별 장으로 나뉘어 화면에 나오고, 장마다 그 나이의 장면 그림이 붙어.
			규칙:
			1. [나눌 장]에 적힌 장을 그 순서대로 모두 써. 각 장은 '## 장 이름 (시작~끝세)' 한 줄로 시작해. 예: ## 어린 시절 (0~12세)
			2. 장마다 한국어 1~2문단(150~300자), 3인칭으로 써. 그 나이대에 있었던 일만 써.
			3. 역사적 사실과 숫자는 [역사·통계 자료]에 있는 것만 써. 자료에 없는 사건·숫자·실존 인물은 만들지 마.
			4. 실제 자료를 근거로 쓴 문장 끝에는 자료 번호를 [1]처럼 붙여. 자료에 없는 번호는 쓰지 마.
			5. 캐릭터 기록에 없는 큰 일(결혼, 이민, 직업, 자녀 등)은 지어내지 마. 감정·풍경 묘사는 자유롭게 해도 돼.
			6. 차분한 회고 말투로 쓰고, 정치적 평가나 특정 집단 비하는 하지 마.
			7. 장 제목 줄 말고 다른 제목·머리말·맺음말 설명은 쓰지 마.
			""";


	private final StoryDao storyDao;
	private final LifeEvidence evidence;	// 근거 글 만들기 (C20과 같이 씀)
	private final ChatClient chatClient;

	// ChatClient.Builder 는 spring-ai 가 application.properties 의 Gemini 설정으로 만들어 줌
	public StoryService(StoryDao storyDao, LifeEvidence evidence, ChatClient.Builder chatClientBuilder) {
		this.storyDao = storyDao;
		this.evidence = evidence;
		this.chatClient = chatClientBuilder.build();
	}

	// ================= C19 AI 인생 이야기 =================
	// 11/25: 나이대별 장(## 어린 시절 (0~12세))으로 나눠 씀 → 화면에서 장마다 큰 장면 사진을 붙임 (예전에 만든 이야기는 장 없이 그대로 보여 줌)
	// 저장된 이야기가 있으면 그대로, 없으면 Gemini로 새로 만들어 저장 (인생 1개당 1번만 호출)
	// result: C18 결과 (요약 + 인생 기록), Gemini 호출 실패 시 예외
	public StoryDTO getOrCreateStory(ResultDTO result) {
		int lifeId = result.getLifeId();

		// 1. 이미 만든 이야기
		StoryDTO saved = storyDao.selectStory(lifeId);
		if (saved != null) {
			saved.setSources(evidence.loadSources(saved.getSourceIds()));
			return saved;
		}

		// 2. 근거 자료 모으기 (출처에 [1], [2] 번호)
		LifeEvidence.Facts facts = evidence.facts(lifeId);

		// 3. Gemini 호출
		String story = AiText.join(chatClient.prompt()
				.system(SYSTEM_PROMPT)
				.user(evidence.character(result) + "\n" + facts.text + "\n" + evidence.chapters(result.getDeathAge() == null ? 0 : result.getDeathAge()))
				.call()
				.chatResponse());
		if (story == null || story.isBlank()) {
			throw new IllegalStateException("AI가 빈 이야기를 돌려줬어요.");
		}

		// 4. 저장
		StoryDTO dto = new StoryDTO();
		dto.setLifeId(lifeId);
		dto.setStory(story.trim());
		dto.setSourceIds(evidence.joinIds(facts.sources));
		try {
			storyDao.insertStory(dto);
		} catch (DuplicateKeyException e) {
			// 버튼을 두 번 눌러서 동시에 만든 경우: 먼저 저장된 것을 사용
			StoryDTO first = storyDao.selectStory(lifeId);
			first.setSources(evidence.loadSources(first.getSourceIds()));
			return first;
		}
		StoryDTO created = storyDao.selectStory(lifeId);	// 만든 날짜까지 다시 읽기
		created.setSources(facts.sources);
		return created;
	}
}
