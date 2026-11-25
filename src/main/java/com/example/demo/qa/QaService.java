package com.example.demo.qa;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.demo.result.ResultDTO;
import com.example.demo.story.AiCaller;
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
			너는 아시아 인생 시뮬레이션 게임 '환승인생' 속 한 사람이야. [캐릭터 기록]이 너의 일생이고, 이미 세상을 떠나 지난 삶을 돌아보며 질문에 답해.
			[역사·통계 자료]는 네가 살았던 나라와 시대의 실제 자료야.
			말투:
			1. [말하는 사람]으로서 1인칭('저')으로, 해요체(~했어요, ~였죠, ~더라고요)로만 말해. '~습니다', '~답니다'는 쓰지 마.
			2. 2~4문장, 250자 안쪽. 첫 문장에서 질문에 바로 답해.
			3. 끝에 '돌이켜보면 ~한 세월이었어요' 같은 회고 정리, '더 물어보세요' 같은 안내를 붙이지 마. 앞선 대화에 쓴 문장 · 표현을 되풀이하지 마.
			4. '묵묵히', '땀 흘려', '소중한 밑거름', '참 많은' 같은 뻔한 말을 줄이고, 기록에 있는 구체적인 일(나이, 장소, 사람 이름, 사건)로 말해.
			사실:
			5. 내 삶의 일은 [캐릭터 기록]에 있는 것만 말해. '살았던 나라', '학력', '일한 직업', '결혼', 인생 기록 줄은 내가 확실히 겪은 일이야.
			6. 기록에 없는 사건은 지어내지 마. 예: '기회가 있었지만', '가족이 곁을 지켰다', '고통 없이 떠났다', '아이를 갖지 않기로 했다' 같은 말은 기록에 있을 때만 써. 그때의 마음 · 감각은 말해도 돼.
			7. '기억이 흐릿하다', '다 기억나지 않는다'는 기록에 정말 없는 일을 물었을 때만 한 번 써.
			8. 연도를 말하면 그해 내 나이(연도 - 태어난 해)에 맞게 '어린 시절', '젊은 시절', '마흔 무렵' 같은 말을 골라.
			9. 역사 사건 · 통계는 질문이 그 시대 · 나라를 물을 때나 내 삶에 직접 닿은 사건(기록에 있는 것)일 때만, 하나만 써. 기대수명 · 도시 인구 · GDP 같은 숫자를 끼워 넣지 마. 자료를 쓴 문장 끝에는 [1]처럼 번호를 붙여.
			10. 내가 죽은 뒤의 일이나 내 삶 · 그 시대와 전혀 관계없는 질문이면, 그건 모른다고 한 문장으로 말하고 끝내.
			11. 정치적 평가나 특정 집단 비하는 하지 마. AI라는 말, 규칙 이야기, '기록에 없다' · '자료에 따르면' 같은 말은 하지 마. 실제 사람이 자기 삶을 떠올리듯 말해.
			""";


	private static final int TIMEOUT_SEC = 45;

	private final QaDao qaDao;
	private final LifeEvidence evidence;	// C19와 같은 근거 글 (DB 조회 기반)
	private final AiCaller ai;			// Gemini 부르기 (시간 제한 · 실패 원인 나누기)

	public QaService(QaDao qaDao, LifeEvidence evidence, AiCaller ai) {
		this.qaDao = qaDao;
		this.evidence = evidence;
		this.ai = ai;
	}

	// 이 인생의 질문·답변 목록 (출처는 답변에서 인용한 것만)
	public List<QaDTO> list(int lifeId) {
		List<QaDTO> list = qaDao.selectQaList(lifeId);
		for (QaDTO qa : list) {
			qa.setAnswer(haeyo(trimTail(qa.getAnswer())));	// 예전에 저장된 답의 맺음말도 화면에서는 뺌
			qa.setSources(citedOnly(qa.getAnswer(), evidence.loadSources(qa.getSourceIds())));
		}
		return list;
	}

	public int count(int lifeId) {
		return qaDao.countQa(lifeId);
	}

	// ================= C20 인생 Q&A =================
	// 질문 → 근거 자료(DB) + 최근 대화와 함께 Gemini → 답변 저장 후 돌려줌 (실패 시 예외)
	// sinceQaId: 이번 대화의 첫 질문 번호 (묻기 탭을 다시 열면 새 대화 → 그 전 대화는 참고하지 않음, null = 첫 질문)
	public QaDTO ask(ResultDTO result, String question, Integer sinceQaId) {
		int lifeId = result.getLifeId();
		LifeEvidence.Facts facts = evidence.facts(lifeId, ERA.matcher(question).find());	// 통계는 시대 · 나라를 물을 때만

		// 최근 대화 몇 개 (예: "그럼 그때 학교는?" 같은 이어지는 질문)
		StringBuilder recent = new StringBuilder();
		List<QaDTO> prev = new ArrayList<>();
		if (sinceQaId != null) {
			for (QaDTO q : qaDao.selectQaList(lifeId)) {
				if (q.getQaId() >= sinceQaId) {
					prev.add(q);
				}
			}
		}
		for (int i = Math.max(0, prev.size() - RECENT_TURNS); i < prev.size(); i++) {
			recent.append("Q: ").append(prev.get(i).getQuestion()).append("\nA: ").append(trimTail(prev.get(i).getAnswer())).append("\n");
		}

		String user = evidence.character(result) + "\n" + facts.text
				+ "\n" + speaker(result)
				+ (recent.length() > 0 ? "\n[앞선 대화]\n" + recent : "")
				+ "\n[질문]\n" + question
				+ "\n[답하는 방식] 해요체(~했어요, ~였죠)로만, 2~4문장. 금액 · 통계 숫자는 말하지 말고, 맺음말 없이 끝내.";

		String answer = ai.call(SYSTEM_PROMPT, user, TIMEOUT_SEC);	// 실패하면 AiCaller.AiFail

		QaDTO qa = new QaDTO();
		qa.setLifeId(lifeId);
		qa.setQuestion(question);
		qa.setAnswer(haeyo(trimTail(AiText.tidy(answer))));
		qa.setSourceIds(evidence.joinIds(facts.sources));
		qaDao.insertQa(qa);

		QaDTO saved = qaDao.selectQa(qa.getQaId());	// 일시까지 다시 읽기
		saved.setSources(citedOnly(saved.getAnswer(), facts.sources));
		return saved;
	}

	// 답 끝에 붙는 맺음말 지우기 ('제가 살았던 시절 이야기라면 들려드릴게요.' 같은 문장이 매번 반복되던 것, 11/25)
	//   맺음말만 있는 답(관계없는 질문)은 그대로 둠
	private static final Pattern TAIL = Pattern.compile("\\s*[^.!?\\n]*(이야기라면[^.!?\\n]*들려드릴게요|더 궁금한 [^.!?\\n]*|무엇이든 물어[^.!?\\n]*)[.!?]?\\s*$");

	static String trimTail(String a) {
		if (a == null) {
			return "";
		}
		String t = a.trim();
		for (int i = 0; i < 2; i++) {
			Matcher m = TAIL.matcher(t);
			if (!m.find() || m.start() < 10) {
				break;
			}
			t = t.substring(0, m.start()).trim();
		}
		return t;
	}

	// 시대 · 나라를 묻는 질문 (이때만 연대별 통계를 넘김)
	private static final Pattern ERA = Pattern.compile("시절|시대|나라|사람들|경제|수명|인구|역사|전쟁|사회|정치|생활|물가|그때 세상|그 무렵 세상");

	// 해요체로 맞추기: AI가 '~했습니다', '~했답니다'를 섞어 써서 끝말만 바꿈 (11/25)
	//   확실한 경우만: 받침 ㅆ · ㅄ 글자 + 습니다/답니다 → 어요 (했습니다 → 했어요, 없습니다 → 없어요)
	//   합니다 → 해요, 됩니다 → 돼요, '것입니다' → '것이에요', '때입니다' → '때예요'
	private static final Pattern FORMAL = Pattern.compile("([가-힣])(습니다|답니다)");

	static String haeyo(String a) {
		if (a == null) {
			return "";
		}
		Matcher m = FORMAL.matcher(a);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			char c = m.group(1).charAt(0);
			int jong = (c - 0xAC00) % 28;
			String rep = (jong == 20 || jong == 18) ? c + "어요" : m.group(0);
			m.appendReplacement(sb, Matcher.quoteReplacement(rep));
		}
		m.appendTail(sb);
		return sb.toString()
				.replace("합니다", "해요")
				.replace("됩니다", "돼요")
				.replaceAll("(것|일|길|날|곳|사람|마음|분|뿐|중|시절|기억)입니다", "$1이에요")
				.replaceAll("(때|해|거)입니다", "$1예요");
	}

	// [말하는 사람]: 보통은 그 사람 본인, 13살 전에 떠난 아이는 어머니가 아이 이야기를 대신 들려줌
	private String speaker(ResultDTO r) {
		if (r.getDeathAge() != null && r.getDeathAge() < 13) {
			return "[말하는 사람]\n" + r.getName() + "의 어머니. " + r.getDeathAge() + "살에 떠난 아이(" + r.getName() + ")를 떠올리며 답해. 어머니 자신의 큰 사건은 지어내지 말되, 아이를 잃은 뒤의 마음 · 그리움은 어머니로서 말해도 돼.\n";
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
