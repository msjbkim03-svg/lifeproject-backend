package com.example.demo.story;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.example.demo.result.ResultDTO;

@Service
public class StoryService {

	// AI에게 주는 규칙 (모든 이야기에 똑같이 적용)
	private static final String SYSTEM_PROMPT = """
			너는 아시아 인생 시뮬레이션 게임 '환승인생'의 인생 이야기 작가야.
			[캐릭터 기록]과 [장별 기록]은 게임 속 가상 인물의 일생이고, [역사·통계 자료]는 실제 자료야.
			이 글은 사진첩처럼 나이대별 장으로 나뉘어 화면에 나오고, 장마다 그 나이의 장면 사진이 붙어.
			형식:
			1. [장별 기록]에 적힌 장을 그 순서대로 모두 써. 각 장은 '## 장 이름 (시작~끝세)' 한 줄로 시작해. 예: ## 어린 시절 (0~12세)
			2. 장마다 한 문단, 3~5문장(150~280자). 이름을 넣은 3인칭 과거형 '~했다' 체로만 써. '나', '저', '우리' 같은 1인칭은 절대 쓰지 마.
			3. 나이를 문장마다 늘어놓는 연대기('6살에 … 7살에 … 10살에 …')로 쓰지 마. 장마다 가장 큰 일 2~3개를 골라 앞뒤가 이어지는 이야기로 써.
			4. 장 제목 줄 말고 다른 제목 · 머리말 · 맺음말 설명은 쓰지 마.
			장과 사진:
			5. 각 장에는 [장별 기록]에서 그 장 아래 적힌 일만 써. 다른 장의 일을 앞당기거나 미뤄 쓰지 마. (예: 18세에 시작한 일은 10대 장에, 23세 일은 청년기 장에)
			6. ★ 줄은 그 장에 사진으로 붙는 장면이야. ★ 일은 모두 그 장 글에 꼭 넣어. 사진은 있는데 글에 없으면 안 돼.
			7. 앞 장에서 이미 시작한 일(직업, 결혼 등)을 다음 장에서 이어 말할 땐 '처음 시작했다'처럼 새로 시작한 것처럼 쓰지 마.
			문장:
			8. 기록에 있는 구체적인 일(나이, 장소, 사람 이름, 직업, 사건)을 중심으로 쓰고, '묵묵히', '땀 흘려', '굴곡진', '소중한' 같은 뻔한 말과 장마다 같은 맺음 문장('~한 시기였다')을 되풀이하지 마.
			9. 마지막 장만 삶 전체를 한 문장으로 돌아봐도 돼.
			사실:
			10. '살았던 나라', '학력', '일한 직업', '결혼'은 실제로 있었던 일이니 알맞은 장에 빠짐없이 써.
			11. 기록에 없는 사건(결혼, 이민, 직업, 자녀, 사업, 재산, 가족이 곁을 지킨 일 등)은 지어내지 마. 감정 · 풍경 묘사는 해도 돼.
			12. 연도를 쓰면 그해 나이(연도 - 태어난 해)가 그 장 나이 범위 안이어야 해. 범위 밖 연도의 일은 그 장에 쓰지 마.
			13. 역사 사건은 [역사·통계 자료]에 있는 것 가운데 그 장 나이대에 일어난 것만, 장마다 많아야 하나 써. 통계 숫자는 이야기 전체에서 많아야 두 번. 자료를 쓴 문장 끝에는 [1]처럼 번호를 붙이고, 자료에 없는 사건 · 숫자 · 실존 인물은 만들지 마. 역사 사건이 이 사람에게 끼친 일은 기록에 있는 것만 써.
			14. 차분한 회고 말투로 쓰고, 정치적 평가나 특정 집단 비하는 하지 마.
			""";

	private static final String HOW = "\n[쓰는 방식] 이름을 넣은 3인칭 '~했다' 체 (1인칭 금지), 장마다 3~5문장, 나이 나열 말고 큰 일 2~3개를 이어서. "
			+ "각 장에는 그 장 아래 기록만, ★ 장면은 그 장에 꼭. 금액은 쓰지 말고, 통계 숫자는 이야기 전체에서 두 번까지. '묵묵히', '굴곡진', '소중한' 같은 뻔한 말과 맺음 문장을 되풀이하지 마.";

	private static final int TIMEOUT_SEC = 50;	// 이야기는 길어서 넉넉히

	private final StoryDao storyDao;
	private final LifeEvidence evidence;	// 근거 글 만들기 (C20과 같이 씀)
	private final AiCaller ai;			// Gemini 부르기 (시간 제한 · 실패 원인 나누기)

	public StoryService(StoryDao storyDao, LifeEvidence evidence, AiCaller ai) {
		this.storyDao = storyDao;
		this.evidence = evidence;
		this.ai = ai;
	}

	// ================= C19 AI 인생 이야기 =================
	// 11/25: 나이대별 장(## 어린 시절 (0~12세))으로 나눠 씀 → 화면에서 장마다 큰 장면 사진을 붙임 (예전에 만든 이야기는 장 없이 그대로 보여 줌)
	// 저장된 이야기가 있으면 그대로, 없으면 Gemini로 새로 만들어 저장 (인생 1개당 1번만 호출)
	// result: C18 결과 (요약 + 인생 기록), Gemini 호출 실패 시 예외
	public StoryDTO getOrCreateStory(ResultDTO result, String sceneParam) {
		java.util.Map<Integer, String> scenes = LifeEvidence.parseScenes(sceneParam);
		int lifeId = result.getLifeId();

		// 1. 이미 만든 이야기 (RULE_SINCE 전 예전 규칙으로 만든 것은 한 번 다시 만듦)
		StoryDTO saved = storyDao.selectStory(lifeId);
		if (saved != null && !isOld(saved)) {
			saved.setSources(evidence.loadSources(saved.getSourceIds()));
			return saved;
		}
		if (saved != null) {
			try {
				return create(result, scenes, true);
			} catch (AiCaller.AiFail f) {
				saved.setSources(evidence.loadSources(saved.getSourceIds()));
				return saved; // AI가 안 되면 예전 이야기라도 보여 줌 (다음에 다시 시도)
			}
		}
		return create(result, scenes, false);
	}

	// 예전 규칙 이야기: 만든 시각이 새 규칙 적용 전 (createdAt 'YYYY-MM-DD HH24:MI', 한국 시간)
	//   15:44 전: 1인칭 · 통계 끼워 넣기 · 장 없음 / 16:30 전: 장 사진과 글이 어긋남(18세 일 시작을 청년기에 씀, 사진 장면이 글에 없음)
	private static final String RULE_SINCE = "2026-11-25 16:30";

	private boolean isOld(StoryDTO s) {
		return s.getCreatedAt() == null || s.getCreatedAt().compareTo(RULE_SINCE) < 0;
	}

	// 새로 만들기 (replace: 예전 이야기를 지우고 저장)
	private StoryDTO create(ResultDTO result, java.util.Map<Integer, String> scenes, boolean replace) {
		int lifeId = result.getLifeId();

		// 2. 근거 자료 모으기 (출처에 [1], [2] 번호)
		LifeEvidence.Facts facts = evidence.facts(lifeId);
		String input = evidence.storyRecord(result, scenes) + "\n" + facts.text + HOW;

		// 3. Gemini 호출
		//    (실패하면 AiCaller.AiFail → 컨트롤러가 기록으로 엮은 이야기로 대신 보여 줌)
		long t0 = System.currentTimeMillis();
		String story = AiText.tidy(ai.call(SYSTEM_PROMPT, input, TIMEOUT_SEC, 12000));	// 이야기는 길어서 12초 안에 답이 없을 때만 예비 모델도 같이 부름

		// 3-1. 점검: 장 빠짐 · 사진 장면이 글에 없음 · 장 나이와 안 맞는 연도/나이 → 한 번만 고쳐 쓰게 함 (화면이 70초 기다리니 합쳐서 62초 안에서만)
		java.util.List<String> bad = problems(story, result, scenes);
		int left = 62 - (int) ((System.currentTimeMillis() - t0) / 1000);
		if (!bad.isEmpty()) {
			System.out.println("[C19] 이야기 점검 " + lifeId + ": " + bad);
		}
		if (!bad.isEmpty() && left >= 15) {
			try {
				String again = AiText.tidy(ai.call(SYSTEM_PROMPT,
						input + "\n\n[앞서 쓴 글]\n" + story + "\n\n[앞서 쓴 글의 문제 - 이것만 고쳐서 전체를 처음부터 다시 써]\n- " + String.join("\n- ", bad),
						Math.min(TIMEOUT_SEC, left), 12000));
				java.util.List<String> bad2 = problems(again, result, scenes);
				System.out.println("[C19] 고쳐 쓴 뒤 " + lifeId + ": " + (bad2.isEmpty() ? "문제 없음" : bad2));
				if (bad2.size() <= bad.size()) {
					story = again;
				}
			} catch (AiCaller.AiFail f) {
				// 고쳐 쓰기가 안 되면 처음 글을 씀
			}
		}

		// 4. 저장
		StoryDTO dto = new StoryDTO();
		dto.setLifeId(lifeId);
		dto.setStory(story);
		dto.setSourceIds(evidence.joinIds(facts.sources));
		if (replace) {
			storyDao.deleteStory(lifeId);
		}
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

	private static final java.util.regex.Pattern CH_HEAD = java.util.regex.Pattern.compile("(?m)^#{1,3}\\s*(.+?)\\s*\\((\\d+)\\s*[~∼-]\\s*(\\d+)\\s*세\\)\\s*$");
	private static final java.util.regex.Pattern YEAR = java.util.regex.Pattern.compile("(1[89]\\d\\d|20\\d\\d)년");
	private static final java.util.regex.Pattern OWN_AGE = java.util.regex.Pattern.compile("(?<![\\d가-힣])(\\d{1,3})(?:세|살)(?:에|때|무렵|이 되|가 되|이던|이었던|였던|의 나이| 나이)");

	// 이야기 점검 (AI 없이): 문제 목록 (없으면 빈 목록)
	//  - 장이 빠졌거나 순서가 다름
	//  - 그 장에 붙는 사진 장면(★)이 글에 안 나옴 (11/25 동현님: 18세 일 시작 사진은 10대인데 글은 청년기에)
	//  - 장 나이 범위 밖의 연도 · 나이 (앞뒤 1년은 봐줌)
	java.util.List<String> problems(String story, ResultDTO r, java.util.Map<Integer, String> scenes) {
		java.util.List<String> out = new java.util.ArrayList<>();
		int deathAge = deathAge(r);
		int want = LifeEvidence.chapterCount(deathAge);
		java.util.Map<Integer, String> body = new java.util.LinkedHashMap<>(); // 장 번호 → 글
		java.util.regex.Matcher h = CH_HEAD.matcher(story == null ? "" : story);
		java.util.List<int[]> heads = new java.util.ArrayList<>();
		while (h.find()) {
			heads.add(new int[] { h.start(), h.end(), Integer.parseInt(h.group(2)) });
		}
		for (int k = 0; k < heads.size(); k++) {
			int from = heads.get(k)[2];
			String text = story.substring(heads.get(k)[1], k + 1 < heads.size() ? heads.get(k + 1)[0] : story.length());
			body.put(LifeEvidence.chapterOf(from), text);
		}
		for (int i = 0; i < want; i++) {
			if (!body.containsKey(i)) {
				out.add("'" + LifeEvidence.chapterName(i) + "' 장이 빠짐");
			}
		}
		for (java.util.Map.Entry<Integer, String> c : body.entrySet()) {
			int i = c.getKey();
			int[] rg = LifeEvidence.chapterRange(i, deathAge);
			String name = LifeEvidence.chapterName(i);
			String text = c.getValue();
			// 사진 장면
			for (java.util.Map.Entry<Integer, String> s : scenes.entrySet()) {
				if (s.getKey() < rg[0] || s.getKey() > rg[1]) {
					continue;
				}
				String re = LifeEvidence.sceneCheck(s.getValue(), r, s.getKey());
				if (re != null && !java.util.regex.Pattern.compile(re).matcher(text).find()) {
					out.add("'" + name + "' 장에 " + s.getKey() + "세 사진(" + LifeEvidence.sceneWord(s.getValue()) + ")이 붙는데 글에 그 일이 없음 - 이 장에 넣기");
				}
			}
			// 연도
			java.util.regex.Matcher y = YEAR.matcher(text);
			while (y.find()) {
				int age = Integer.parseInt(y.group(1)) - r.getBirthYear();
				if (age < rg[0] - 1 || age > rg[1] + 1) {
					out.add("'" + name + "'(" + rg[0] + "~" + rg[1] + "세) 장에 " + y.group(1) + "년(" + age + "세) 이야기가 있음 - 맞는 장으로 옮기거나 빼기");
				}
			}
			// 이 사람의 나이
			java.util.regex.Matcher a = OWN_AGE.matcher(text);
			while (a.find()) {
				int age = Integer.parseInt(a.group(1));
				if (age <= deathAge && (age < rg[0] - 1 || age > rg[1] + 1)) {
					out.add("'" + name + "'(" + rg[0] + "~" + rg[1] + "세) 장에 " + age + "세 일이 있음 - 맞는 장으로 옮기기");
				}
			}
		}
		return out;
	}

	// AI가 안 될 때: 인생 기록 줄로 엮은 이야기 (저장하지 않음 → 다음에 다시 AI로 시도)
	public StoryDTO recordStory(ResultDTO result, String notice, String sceneParam) {
		StoryDTO dto = new StoryDTO();
		dto.setLifeId(result.getLifeId());
		dto.setStory(evidence.recordStory(result, LifeEvidence.parseScenes(sceneParam)));
		dto.setSources(new java.util.ArrayList<>());
		dto.setFallback(true);
		dto.setNotice(notice);
		return dto;
	}

	private int deathAge(ResultDTO r) {
		return r.getDeathAge() == null ? 0 : r.getDeathAge();
	}
}
