package com.example.demo.story;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.result.EventLogDTO;
import com.example.demo.result.ResultDTO;
import com.example.demo.result.YearLogDTO;

// C19·C20 공통: Gemini에 넘길 근거 글 만들기 (DB 조회 기반, 벡터 검색 미사용)
//  - character(): [캐릭터 기록] 게임 속 가상 인물의 요약 + 인생 기록
//  - facts():     [역사·통계 자료] 살았던 나라·기간의 역사 사건 + 연대별 통계, 출처에 [1] [2] 번호
@Component
public class LifeEvidence {

	private static final int MAX_HISTORY = 8;	// 역사 사건 최대 개수 (겪은 사건 먼저)
	private static final int MAX_LOGS = 140;	// 인생 기록 최대 줄 수 (11/25: 40줄이면 어린 시절에서 끝나서 고름)

	private final StoryDao storyDao;

	public LifeEvidence(StoryDao storyDao) {
		this.storyDao = storyDao;
	}

	// 근거 글 + 출처 목록 (sources의 n번째 = 글 속 [n])
	public static class Facts {
		public String text;
		public List<SourceDTO> sources = new ArrayList<>();
	}

	// [캐릭터 기록]
	public String character(ResultDTO r) {
		StringBuilder sb = new StringBuilder("[캐릭터 기록] (아래 목록은 모두 이 사람에게 실제로 있었던 일)\n");
		sb.append("이름: ").append(r.getName()).append(" (").append("M".equals(r.getGender()) ? "남" : "여").append(")\n");
		sb.append("태어남: ").append(r.getBirthYear()).append("년, ").append(r.getCountryName());
		if (r.getHistName() != null && !r.getHistName().equals(r.getCountryName())) {
			sb.append(" (당시 ").append(r.getHistName()).append(")");
		}
		if (r.getOriginClass() != null) {
			sb.append(", 태어난 집 형편: 그 나라 소득 ").append(r.getOriginClass()).append("분위 (1 가난 ~ 5 부유)");
		}
		sb.append("\n");
		sb.append("살았던 나라: ").append(countryRuns(r)).append("\n");
		sb.append("학력: ").append(eduText(r)).append("\n");
		sb.append("일한 직업 (나이순, 모두 실제로 한 일): ").append(jobRuns(r)).append("\n");
		int ret = retireAge(r);
		if (ret < 999) {
			sb.append("은퇴: ").append(ret).append("세 (그 뒤로는 일하지 않음)\n");
		}
		sb.append("결혼: ").append(marriage(r)).append(", 자녀 ").append(r.getChildCount()).append("명\n");
		sb.append("평균 소득: 연 ").append(r.getAvgIncome()).append("달러 (구매력 기준), 아시아 소득 상위 ").append(r.getIncomeTopPct()).append("%\n");
		String peaks = peaks(r);
		if (!peaks.isEmpty()) {
			sb.append(peaks).append("\n");
		}
		sb.append("세상을 떠남: ").append(r.getDeathYear()).append("년, ").append(r.getDeathAge()).append("세, 원인 ").append(causeText(r.getDeathCause())).append("\n");
		sb.append("인생 기록 (나이순):\n");
		for (EventLogDTO e : pickLogs(r.getEvents())) {
			sb.append("- ").append(e.getAge()).append("세(").append(e.getYearNo()).append("): ").append(noMoney(e.getLogText()));
			if (e.getChoiceText() != null) {
				sb.append(" → 선택: ").append(e.getChoiceText());
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	// 기록 줄의 돈 표시 '(−4,228만 원)' · '(+12만 원)' 빼기 (AI가 '775만 원을 정리해야 했다'처럼 금액을 읽어 말해서, 11/25)
	static String noMoney(String t) {
		return t == null ? "" : t.replaceAll("\\s*\\([−+\\-]?\\$?[0-9][0-9,.억만\\s]*(원|달러)[^)]*\\)", "").trim();
	}

	// 사망 원인 분류 이름 → 말 ('기타 질환' → '병', AI가 '기타 질환으로 생을 마감'이라고 씀)
	static String causeText(String c) {
		if (c == null || c.isBlank() || "기타 질환".equals(c) || "질병".equals(c)) {
			return "병";
		}
		if ("기타 사고·손상".equals(c)) {
			return "사고";
		}
		return c;
	}

	// 일한 직업: 연도별 기록에서 같은 일이 이어진 구간 '용접공·배관공(21~42세), 경비원(43~58세)'
	//   (11/25: 마지막 직업만 넘겨서 AI가 앞서 한 일을 '기억 안 난다'고 하던 것)
	private String jobRuns(ResultDTO r) {
		List<String> out = new ArrayList<>();
		String cur = null;
		int from = 0;
		int to = 0;
		int ret = retireAge(r);
		for (YearLogDTO y : years(r)) {
			String j = y.getAge() >= ret ? null : y.getJobName(); // 은퇴한 뒤에도 연도 기록에 마지막 직업이 남아 있음 (11/25: '봉제 공장 50~98세')
			if (j != null && j.equals(cur) && y.getAge() <= to + 1) {
				to = y.getAge();
				continue;
			}
			if (cur != null) {
				out.add(cur + "(" + from + "~" + to + "세)");
			}
			cur = j;
			from = y.getAge();
			to = y.getAge();
		}
		if (cur != null) {
			out.add(cur + "(" + from + "~" + to + "세)");
		}
		if (out.isEmpty()) {
			return r.getJobName() == null ? "일한 적 없음" : r.getJobName();
		}
		return String.join(", ", out);
	}

	// 은퇴한 나이: 인생 기록의 은퇴 문장 ('~ 일을 마치고 은퇴했다'), 없으면 999 (프론트 filmPlan retireAge 와 같은 기준)
	private int retireAge(ResultDTO r) {
		if (r.getEvents() != null) {
			for (EventLogDTO e : r.getEvents()) {
				String t = e.getLogText() == null ? "" : e.getLogText();
				if ((t.contains("은퇴했") || t.contains("정년퇴직") || t.contains("은퇴하기로 했")) && !t.contains("않고") && !t.contains("않기로")) {
					return e.getAge();
				}
			}
		}
		return 999;
	}

	// 살았던 나라 구간 '방글라데시(0~13세), 파키스탄(14~58세)'
	private String countryRuns(ResultDTO r) {
		List<String> out = new ArrayList<>();
		String cur = null;
		String name = null;
		int from = 0;
		int to = 0;
		for (YearLogDTO y : years(r)) {
			String c = y.getCountryCode();
			if (c == null || c.equals(cur)) {
				if (c != null) {
					to = y.getAge();
				}
				continue;
			}
			if (cur != null) {
				out.add(name + "(" + from + "~" + to + "세)");
			}
			cur = c;
			name = y.getCountryName() != null ? y.getCountryName() : countryName(r, c);
			from = y.getAge();
			to = y.getAge();
		}
		if (cur != null) {
			out.add(name + "(" + from + "~" + to + "세)");
		}
		return out.isEmpty() ? r.getCountryName() : String.join(", ", out);
	}

	private String countryName(ResultDTO r, String code) {
		if (code.equals(r.getBirthCountry())) {
			return r.getCountryName();
		}
		if (code.equals(r.getCurrentCountry())) {
			return r.getCurrentCountryName();
		}
		return code;
	}

	// 학력: 코드 → 말 ('대학교 졸업', '고등학교 중퇴', '학교 안 다님')
	private String eduText(ResultDTO r) {
		String lv = r.getEduLevel();
		String name = lv == null ? null : switch (lv) {
			case "ELEM" -> "초등학교";
			case "MIDDLE" -> "중학교";
			case "HIGH" -> "고등학교";
			case "UNI" -> "대학교";
			case "GRAD" -> "대학원";
			default -> null;
		};
		if (name == null) {
			return "학교 안 다님";
		}
		if ("GRADUATED".equals(r.getSchoolStatus())) {
			return name + " 졸업";
		}
		if ("DROPPED".equals(r.getSchoolStatus())) {
			return name + " 중퇴";
		}
		return name + "까지 다님";
	}

	// 가장 행복했던 때 · 가장 힘들었던 때 (행복 수치, 앞뒤 1년 평균)
	private String peaks(ResultDTO r) {
		List<YearLogDTO> ys = years(r);
		if (ys.size() < 3) {
			return "";
		}
		int hi = 0;
		int lo = 0;
		for (int i = 0; i < ys.size(); i++) {
			if (avg(ys, i) > avg(ys, hi)) {
				hi = i;
			}
			if (avg(ys, i) < avg(ys, lo)) {
				lo = i;
			}
		}
		return "행복 수치로 본 가장 행복했던 때: " + ys.get(hi).getAge() + "세 무렵, 가장 힘들었던 때: " + ys.get(lo).getAge() + "세 무렵";
	}

	private double avg(List<YearLogDTO> ys, int i) {
		int a = Math.max(0, i - 1);
		int b = Math.min(ys.size() - 1, i + 1);
		double sum = 0;
		for (int k = a; k <= b; k++) {
			sum += ys.get(k).getHappiness();
		}
		return sum / (b - a + 1);
	}

	private List<YearLogDTO> years(ResultDTO r) {
		return r.getYears() == null ? new ArrayList<>() : r.getYears();
	}

	// 인생 기록 고르기: 앞에서부터 자르면 어린 시절만 들어가서, 온 생애에 고르게
	//   사건(EVENT_ID 있는 줄)과 고른 것이 있는 줄은 모두, 그 밖의 줄은 나이마다 첫 줄만 → 그래도 많으면 고르게 건너뜀
	private List<EventLogDTO> pickLogs(List<EventLogDTO> events) {
		List<EventLogDTO> out = new ArrayList<>();
		if (events == null) {
			return out;
		}
		int lastAge = -1;
		for (EventLogDTO e : events) {
			String t = e.getLogText() == null ? "" : e.getLogText();
			if (t.startsWith("세계 인구가") || t.matches("^\\d+세\\. 같은 해 .*") || t.endsWith("?")) {
				continue; // 세계 소식 · 또래 생존율 · 고르기 전 질문 줄은 이 사람의 일이 아니라서 뺌 (AI가 '14살에 세계 인구가 80억'을 씀)
			}
			boolean big = e.getEventId() != null || e.getChoiceText() != null;
			if (big || e.getAge() != lastAge) {
				out.add(e);
			}
			lastAge = e.getAge();
		}
		if (out.size() <= MAX_LOGS) {
			return out;
		}
		List<EventLogDTO> even = new ArrayList<>();
		for (int i = 0; i < MAX_LOGS; i++) {
			even.add(out.get((int) ((long) i * out.size() / MAX_LOGS)));
		}
		return even;
	}

	// 결혼 이력 (이혼·사별하면 마지막엔 미혼이라 '안 함'으로 나오던 것)
	private String marriage(ResultDTO r) {
		int w = r.getWeddingCount() == null ? 0 : r.getWeddingCount();
		int d = r.getDivorceCount() == null ? 0 : r.getDivorceCount();
		int x = r.getWidowCount() == null ? 0 : r.getWidowCount();
		if (w == 0 && !"Y".equals(r.getMarriedYn())) {
			return "안 함";
		}
		String s = "함" + (w > 1 ? "(" + w + "번)" : "");
		if (d > 0) {
			s += ", 이혼 " + d + "번";
		}
		if (x > 0) {
			s += ", 배우자와 사별 " + x + "번";
		}
		return s;
	}

	// C19 이야기 장: 나이대 (프론트는 장 제목의 나이 범위로 큰 장면 사진을 붙임)
	private static final int[][] CH_AGE = { { 0, 12 }, { 13, 19 }, { 20, 34 }, { 35, 59 }, { 60, 200 } };
	private static final String[] CH_NAME = { "어린 시절", "10대", "청년기", "중년기", "노년기" };

	// C19 [나눌 장]: 살았던 나이까지만
	public String chapters(int deathAge) {
		StringBuilder sb = new StringBuilder("[나눌 장]\n");
		for (int i = 0; i < CH_AGE.length && CH_AGE[i][0] <= deathAge; i++) {
			sb.append(chapterTitle(i, deathAge)).append("\n");
		}
		return sb.toString();
	}

	private String chapterTitle(int i, int deathAge) {
		return "## " + CH_NAME[i] + " (" + CH_AGE[i][0] + "~" + Math.min(CH_AGE[i][1], deathAge) + "세)";
	}

	// C19 AI가 안 될 때 대신 보여 줄 이야기: 장마다 그 나이대의 큰 사건 줄을 이어 붙임 (AI 없이, 지어낸 말 없이)
	//   사건(EVENT_ID 있는 줄) 먼저, 모자라면 나이마다 첫 줄로 채움. 질문으로 끝나는 줄('어떻게 할까?')은 뺌
	public String recordStory(ResultDTO r) {
		int deathAge = r.getDeathAge() == null ? 0 : r.getDeathAge();
		List<EventLogDTO> events = r.getEvents() == null ? new ArrayList<>() : r.getEvents();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < CH_AGE.length && CH_AGE[i][0] <= deathAge; i++) {
			List<String> lines = new ArrayList<>();
			for (int pass = 0; pass < 2 && lines.size() < 4; pass++) {
				int lastAge = -1;
				for (EventLogDTO e : events) {
					if (e.getAge() < CH_AGE[i][0] || e.getAge() > CH_AGE[i][1] || lines.size() >= 4) {
						continue;
					}
					String t = noMoney(e.getLogText());
					boolean ok = !t.isEmpty() && !t.endsWith("?") && !t.startsWith("세계 인구가") && !t.matches("^\\d+세\\. 같은 해 .*") && !lines.contains(t)
							&& (pass == 0 ? e.getEventId() != null : e.getAge() != lastAge);
					if (ok) {
						lines.add(t);
					}
					lastAge = e.getAge();
				}
			}
			if (lines.isEmpty()) {
				continue;
			}
			sb.append(chapterTitle(i, deathAge)).append("\n").append(String.join(" ", lines)).append("\n");
		}
		return sb.toString();
	}

	// [역사·통계 자료]
	public Facts facts(int lifeId) {
		return facts(lifeId, true);
	}

	// withStats = false: 연대별 통계(기대수명 · 도시 인구 · GDP · 출생아)는 빼고 역사 사건만
	//   (C20 묻기: 직업 · 결혼을 물어도 통계 숫자를 끼워 넣어서, 시대 · 나라를 물을 때만 통계를 넘김, 11/25)
	public Facts facts(int lifeId, boolean withStats) {
		Map<Integer, SourceDTO> sourceMap = new LinkedHashMap<>();	// 출처 번호 → 출처 (넣은 순서 = [번호])
		StringBuilder sb = new StringBuilder("[역사·통계 자료]\n");

		List<HistoryFactDTO> hist = storyDao.selectHistoryFacts(lifeId);
		int n = Math.min(hist.size(), MAX_HISTORY);
		for (int i = 0; i < n; i++) {
			HistoryFactDTO h = hist.get(i);
			sb.append("- ").append(h.getTitle()).append(" (").append(h.getStartYear()).append("~").append(h.getEndYear()).append(")");
			if ("Y".equals(h.getExperienced())) {
				sb.append(" ※ 이 인물이 직접 겪음");
			}
			sb.append(": ").append(nvl(h.getDescription())).append(cite(h.getSourceId(), h.getSourceTitle(), h.getSourceUrl(), sourceMap)).append("\n");
		}

		for (DecadeStatDTO s : withStats ? storyDao.selectDecadeStats(lifeId) : new ArrayList<DecadeStatDTO>()) {
			sb.append("- ").append(s.getDecade()).append("년대 이 나라: 기대수명 ").append(round(s.getLifeExpectancy())).append("세")
				.append(", 1인당 GDP ").append(round(s.getGdpPerCapita())).append("달러")
				.append(", 도시 인구 ").append(round(s.getUrbanRate())).append("%")
				.append(", 여성 1명당 출생아 ").append(s.getTfr()).append("명");
			if ("Y".equals(s.getIsEstimated())) {
				sb.append(" (추정값)");
			}
			sb.append(cite(s.getSourceId(), s.getSourceTitle(), s.getSourceUrl(), sourceMap)).append("\n");
		}

		Facts f = new Facts();
		f.text = sb.toString();
		f.sources.addAll(sourceMap.values());
		return f;
	}

	// 출처에 번호를 붙여서 " [2]" 같은 글자를 돌려줌 (같은 출처는 같은 번호)
	private String cite(Integer sourceId, String title, String url, Map<Integer, SourceDTO> sourceMap) {
		if (sourceId == null) {
			return "";
		}
		if (!sourceMap.containsKey(sourceId)) {
			SourceDTO s = new SourceDTO();
			s.setSourceId(sourceId);
			s.setTitle(title);
			s.setUrl(url);
			s.setNo(sourceMap.size() + 1);
			sourceMap.put(sourceId, s);
		}
		return " [" + sourceMap.get(sourceId).getNo() + "]";
	}

	// 출처 목록 → "3,7,12" (DB 저장용)
	public String joinIds(List<SourceDTO> sources) {
		List<String> ids = new ArrayList<>();
		for (SourceDTO s : sources) {
			ids.add(String.valueOf(s.getSourceId()));
		}
		return String.join(",", ids);
	}

	// "3,7,12" → 출처 목록 (저장할 때의 순서 그대로, 번호도 다시 붙임)
	public List<SourceDTO> loadSources(String sourceIds) {
		List<SourceDTO> ordered = new ArrayList<>();
		if (sourceIds == null || sourceIds.isBlank()) {
			return ordered;
		}
		List<Integer> ids = new ArrayList<>();
		for (String s : sourceIds.split(",")) {
			ids.add(Integer.parseInt(s.trim()));
		}
		List<SourceDTO> found = storyDao.selectSources(ids);
		for (int i = 0; i < ids.size(); i++) {
			for (SourceDTO s : found) {
				if (s.getSourceId() == ids.get(i)) {
					s.setNo(i + 1);
					ordered.add(s);
				}
			}
		}
		return ordered;
	}

	private String nvl(String s) {
		return s == null ? "없음" : s;
	}

	private String round(Double v) {
		return v == null ? "?" : String.valueOf(Math.round(v));
	}
}
