package com.example.demo.story;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.demo.result.EventLogDTO;
import com.example.demo.result.ResultDTO;

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
		StringBuilder sb = new StringBuilder("[캐릭터 기록]\n");
		sb.append("이름: ").append(r.getName()).append(" (").append("M".equals(r.getGender()) ? "남" : "여").append(")\n");
		sb.append("태어난 곳: ").append(r.getCountryName());
		if (r.getHistName() != null && !r.getHistName().equals(r.getCountryName())) {
			sb.append(" (당시 ").append(r.getHistName()).append(")");
		}
		sb.append(", ").append(r.getBirthYear()).append("년\n");
		if (r.getCurrentCountry() != null && !r.getCurrentCountry().equals(r.getBirthCountry())) {
			sb.append("마지막에 산 나라: ").append(r.getCurrentCountryName()).append("\n");
		}
		sb.append("사망: ").append(r.getDeathYear()).append("년, ").append(r.getDeathAge()).append("세, 원인 ").append(r.getDeathCause()).append("\n");
		sb.append("학력: ").append(nvl(r.getEduLevel())).append(" (").append(nvl(r.getSchoolStatus())).append(")\n");
		sb.append("마지막 직업: ").append(nvl(r.getJobName())).append("\n");
		sb.append("평균 소득: 연 ").append(r.getAvgIncome()).append("달러, 아시아 소득 상위 ").append(r.getIncomeTopPct()).append("%\n");
		sb.append("결혼: ").append(marriage(r)).append(", 자녀 ").append(r.getChildCount()).append("명\n");
		sb.append("인생 기록:\n");
		for (EventLogDTO e : pickLogs(r.getEvents())) {
			sb.append("- ").append(e.getAge()).append("세(").append(e.getYearNo()).append("): ").append(e.getLogText());
			if (e.getChoiceText() != null) {
				sb.append(" → 선택: ").append(e.getChoiceText());
			}
			sb.append("\n");
		}
		return sb.toString();
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

	// C19 [나눌 장]: 이야기를 나이대별로 나눌 이름 (프론트는 장 제목의 나이 범위로 큰 장면 사진을 붙임)
	public String chapters(int deathAge) {
		int[][] r = { { 0, 12 }, { 13, 19 }, { 20, 34 }, { 35, 59 }, { 60, 200 } };
		String[] name = { "어린 시절", "10대", "청년기", "중년기", "노년기" };
		StringBuilder sb = new StringBuilder("[나눌 장]\n");
		for (int i = 0; i < r.length; i++) {
			if (r[i][0] > deathAge) {
				break;
			}
			sb.append("## ").append(name[i]).append(" (").append(r[i][0]).append("~").append(Math.min(r[i][1], deathAge)).append("세)\n");
		}
		return sb.toString();
	}

	// [역사·통계 자료]
	public Facts facts(int lifeId) {
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

		for (DecadeStatDTO s : storyDao.selectDecadeStats(lifeId)) {
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
