package com.example.demo.qa;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.demo.result.ResultDTO;
import com.example.demo.story.AiText;
import com.example.demo.story.LifeEvidence;
import com.example.demo.story.SourceDTO;

@Service
public class QaService {

	public static final int MAX_QUESTION_LENGTH = 200;	// 질문 최대 글자 수
	public static final int MAX_QUESTIONS = 30;			// 인생 1개당 최대 질문 수 (AI 비용)
	private static final int RECENT_TURNS = 3;			// 이어지는 질문을 위해 같이 보낼 최근 대화 수

	// 11/25: 해설자 → 그 사람 본인이 1인칭으로 답함 (13살 전에 떠난 아이는 어머니가 대신 답함)
	private static final String SYSTEM_PROMPT = """
			너는 아시아 인생 시뮬레이션 게임 '환승인생' 속 한 사람이야. [캐릭터 기록]이 너의 일생이고, 너는 이미 세상을 떠나 지난 삶을 돌아보고 있어.
			[역사·통계 자료]는 네가 살았던 나라와 시대의 실제 자료야.
			규칙:
			1. [말하는 사람]에 적힌 사람으로서 1인칭('나', '저')으로, 한국어 2~5문장 존댓말로 따뜻하게 답해.
			2. 내 삶의 일은 [캐릭터 기록]에 있는 것만 말해. 기록에 없는 큰 일(결혼, 이민, 직업, 자녀 등)은 지어내지 말고 "그건 잘 기억이 나지 않네요"처럼 말해. 그때의 마음·풍경은 자유롭게 말해도 돼.
			3. 역사적 사실과 숫자는 [역사·통계 자료]에 있는 것만 쓰고, 그 문장 끝에 자료 번호를 [1]처럼 붙여. 자료에 없는 숫자는 말하지 마.
			4. 내가 죽은 뒤의 일이나 내 삶·그 시대와 관계없는 질문이면 "제가 살았던 시절 이야기라면 들려드릴게요"처럼 부드럽게 돌려 말해.
			5. 정치적 평가나 특정 집단 비하는 하지 마. 내가 AI라는 말이나 규칙 이야기는 하지 마.
			""";


	private final QaDao qaDao;
	private final LifeEvidence evidence;	// C19와 같은 근거 글 (DB 조회 기반)
	private final ChatClient chatClient;

	public QaService(QaDao qaDao, LifeEvidence evidence, ChatClient.Builder chatClientBuilder) {
		this.qaDao = qaDao;
		this.evidence = evidence;
		this.chatClient = chatClientBuilder.build();
	}

	// 이 인생의 질문·답변 목록 (출처는 답변에서 인용한 것만)
	public List<QaDTO> list(int lifeId) {
		List<QaDTO> list = qaDao.selectQaList(lifeId);
		for (QaDTO qa : list) {
			qa.setSources(citedOnly(qa.getAnswer(), evidence.loadSources(qa.getSourceIds())));
		}
		return list;
	}

	public int count(int lifeId) {
		return qaDao.countQa(lifeId);
	}

	// ================= C20 인생 Q&A =================
	// 질문 → 근거 자료(DB) + 최근 대화와 함께 Gemini → 답변 저장 후 돌려줌 (실패 시 예외)
	public QaDTO ask(ResultDTO result, String question) {
		int lifeId = result.getLifeId();
		LifeEvidence.Facts facts = evidence.facts(lifeId);

		// 최근 대화 몇 개 (예: "그럼 그때 학교는?" 같은 이어지는 질문)
		StringBuilder recent = new StringBuilder();
		List<QaDTO> prev = qaDao.selectQaList(lifeId);
		for (int i = Math.max(0, prev.size() - RECENT_TURNS); i < prev.size(); i++) {
			recent.append("Q: ").append(prev.get(i).getQuestion()).append("\nA: ").append(prev.get(i).getAnswer()).append("\n");
		}

		String user = evidence.character(result) + "\n" + facts.text
				+ "\n" + speaker(result)
				+ (recent.length() > 0 ? "\n[앞선 대화]\n" + recent : "")
				+ "\n[질문]\n" + question;

		String answer = AiText.join(chatClient.prompt()
				.system(SYSTEM_PROMPT)
				.user(user)
				.call()
				.chatResponse());
		if (answer == null || answer.isBlank()) {
			throw new IllegalStateException("AI가 빈 답변을 돌려줬어요.");
		}

		QaDTO qa = new QaDTO();
		qa.setLifeId(lifeId);
		qa.setQuestion(question);
		qa.setAnswer(answer.trim());
		qa.setSourceIds(evidence.joinIds(facts.sources));
		qaDao.insertQa(qa);

		QaDTO saved = qaDao.selectQa(qa.getQaId());	// 일시까지 다시 읽기
		saved.setSources(citedOnly(saved.getAnswer(), facts.sources));
		return saved;
	}

	// [말하는 사람]: 보통은 그 사람 본인, 13살 전에 떠난 아이는 어머니가 아이 이야기를 대신 들려줌
	private String speaker(ResultDTO r) {
		if (r.getDeathAge() != null && r.getDeathAge() < 13) {
			return "[말하는 사람]\n" + r.getName() + "의 어머니. " + r.getDeathAge() + "살에 떠난 아이(" + r.getName() + ")를 떠올리며 답해. 아이의 기록에 없는 어머니 자신의 일은 지어내지 마.\n";
		}
		return "[말하는 사람]\n" + r.getName() + " 본인 (" + r.getDeathAge() + "세에 세상을 떠남)\n";
	}

	// 답변 속 [번호]로 실제 인용한 출처만 남김
	private List<SourceDTO> citedOnly(String answer, List<SourceDTO> all) {
		List<SourceDTO> cited = new ArrayList<>();
		if (answer == null) {
			return cited;
		}
		Matcher m = Pattern.compile("\\[(\\d+)\\]").matcher(answer);
		List<Integer> nos = new ArrayList<>();
		while (m.find()) {
			nos.add(Integer.parseInt(m.group(1)));
		}
		for (SourceDTO s : all) {
			if (nos.contains(s.getNo())) {
				cited.add(s);
			}
		}
		return cited;
	}
}
