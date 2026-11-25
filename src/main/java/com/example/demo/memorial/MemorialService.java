package com.example.demo.memorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.costume.CostumeLifeDTO;
import com.example.demo.costume.CostumeService;
import com.example.demo.engine.EngineService;
import com.example.demo.result.ResultService;

@Service
public class MemorialService {

	private static final int PAGE_SIZE = 12;
	private static final int MAX_TEXT = 100;

	private static final Map<String, String> OWN_NAMES = Map.of(
			"PHONE", "전화기", "CELL", "휴대폰", "BIKE", "자전거", "MOTO", "오토바이",
			"CAR", "자동차", "TV", "텔레비전", "PC", "컴퓨터", "HOUSE", "집 열쇠");

	private MemorialDAO dao;
	private EngineService engineService;   // 엔딩 영상 재료 (C22, 동현님 것을 불러 쓰기만)
	private ResultService resultService;   // 인생 결과 (C18, 신영님 것을 불러 쓰기만)
	private CostumeService costumeService; // 그 생에 입은 옷 (C21)

	public MemorialService(MemorialDAO dao, EngineService engineService, ResultService resultService,
			CostumeService costumeService) {
		this.dao = dao;
		this.engineService = engineService;
		this.resultService = resultService;
		this.costumeService = costumeService;
	}

	public Map<String, Object> list(int page) {
		if (page < 1) page = 1;
		int start = (page - 1) * PAGE_SIZE;
		List<MemorialCardDTO> list = dao.selectCards(start, start + PAGE_SIZE);
		return Map.of("total", dao.countCards(), "size", PAGE_SIZE, "list", list);
	}

	// 안 열린 방은 주인만 볼 수 있음 → 볼 수 없으면 null
	public MemorialRoomDTO room(int lifeId, Integer memberId, boolean view) {
		MemorialRoomDTO room = dao.selectRoom(lifeId);
		if (room == null) return null;

		boolean mine = memberId != null && memberId.equals(room.getOwnerId());
		boolean opened = room.getPostId() != null;
		if (!opened && !mine) return null;

		room.setMine(mine);
		room.setOpened(opened);
		room.setSummary(makeSummary(lifeId));
		room.setHappiness(dao.selectHappiness(lifeId));
		room.setLeftThings(makeLeftThings(room, lifeId));
		room.setEvents(dao.selectHistEvents(lifeId));
		room.setPhotos(makePhotos(dao.selectLogs(lifeId)));
		room.setLast(makeLast(room, lifeId));

		List<MemorialCommentDTO> comments = new ArrayList<>();
		if (opened) {
			if (view) dao.plusViewCount(room.getPostId());
			comments = dao.selectComments(room.getPostId());
			for (MemorialCommentDTO c : comments) {
				c.setMine(memberId != null && memberId == c.getMemberId());
			}
			room.setMyCandle(memberId != null && dao.countMyCandle(room.getPostId(), memberId) > 0);
		}
		room.setComments(comments);
		return room;
	}

	// 추모관 영상·사진첩 재료 = 엔딩 영상(C22)과 같은 재료 { result, film, wear }
	// 열린 추모관이면 누구나, 안 열렸으면 주인만 → 볼 수 없으면 null
	// (엔진·결과 API 는 '내 인생'만 주니까, 추모관이 주인 번호로 대신 꺼내 줌)
	public Map<String, Object> film(int lifeId, Integer memberId) {
		MemorialRoomDTO room = dao.selectRoom(lifeId);
		if (room == null || room.getOwnerId() == null) return null;
		boolean mine = memberId != null && memberId.equals(room.getOwnerId());
		if (room.getPostId() == null && !mine) return null;

		int ownerId = room.getOwnerId();
		Map<String, Object> film = engineService.filmData(lifeId, ownerId, null);
		if (film == null || film.isEmpty()) return null;

		CostumeLifeDTO life = costumeService.myLife(lifeId, ownerId, null);
		Map<String, Object> out = new HashMap<>();
		out.put("result", resultService.getResult(lifeId));
		out.put("film", film);
		out.put("wear", life == null ? null : costumeService.list(life).get("wear"));
		return out;
	}

	public String open(int lifeId, int memberId, String message) {
		MemorialRoomDTO room = dao.selectRoom(lifeId);
		if (room == null || room.getOwnerId() == null || room.getOwnerId() != memberId) {
			return "내가 살았던 끝난 인생만 추모관을 열 수 있어요.";
		}
		if (room.getPostId() != null) {
			return "이미 열린 추모관이에요.";
		}
		String error = checkText(message);
		if (error != null) return error;
		dao.insertPost(lifeId, memberId, message.trim());
		return null;
	}

	@Transactional
	public String candle(int lifeId, int memberId) {
		MemorialRoomDTO room = dao.selectRoom(lifeId);
		if (room == null || room.getPostId() == null) {
			return "열린 추모관이 아니에요.";
		}
		if (dao.countMyCandle(room.getPostId(), memberId) > 0) {
			return "이미 촛불을 켰어요.";
		}
		dao.insertTribute(room.getPostId(), memberId);
		dao.plusTributeCount(room.getPostId());
		return null;
	}

	public int tributeCount(int lifeId) {
		return dao.selectRoom(lifeId).getTributeCount();
	}

	public String comment(int lifeId, int memberId, String content) {
		MemorialRoomDTO room = dao.selectRoom(lifeId);
		if (room == null || room.getPostId() == null) {
			return "열린 추모관이 아니에요.";
		}
		String error = checkText(content);
		if (error != null) return error;
		dao.insertComment(room.getPostId(), memberId, content.trim());
		return null;
	}

	public boolean deleteComment(int commentId, int memberId) {
		return dao.deleteComment(commentId, memberId) > 0;
	}

	private String checkText(String text) {
		if (text == null || text.trim().isEmpty()) return "한마디를 적어 주세요.";
		if (text.trim().length() > MAX_TEXT) return "100자까지 쓸 수 있어요.";
		return null;
	}

	private String makeSummary(int lifeId) {
		MemorialJobDTO job = dao.selectMainJob(lifeId);
		if (job == null) return null;
		return job.getJobName() + ro(job.getJobName()) + " " + job.getYears() + "년을 일한 삶";
	}

	// 마지막 글자 받침을 보고 '으로' / '로' 고르기 (받침 없음·ㄹ받침 → 로, 그 밖의 받침 → 으로)
	// 예: 의사 → 의사로, 군인 → 군인으로, 공무원 → 공무원으로, 농부 → 농부로, 경찰 → 경찰로
	private String ro(String word) {
		if (word == null || word.isEmpty()) return "로";
		char last = word.charAt(word.length() - 1);
		if (last < '가' || last > '힣') return "로"; // 한글이 아니면 '로'
		int jong = (last - '가') % 28; // 받침 번호 (0 = 받침 없음, 8 = ㄹ)
		return (jong == 0 || jong == 8) ? "로" : "으로";
	}

	private List<String> makeLeftThings(MemorialRoomDTO room, int lifeId) {
		List<String> things = new ArrayList<>();
		if (room.getOwns() != null) {
			for (String code : room.getOwns().split(",")) {
				String name = OWN_NAMES.get(code.trim());
				if (name != null && !things.contains(name)) things.add(name);
			}
		}
		if ("Y".equals(room.getIsMarried())) things.add("결혼사진");
		if (room.getChildCount() > 0) things.add("가족사진");
		if (dao.countAchievedQuests(lifeId) > 0) things.add("트로피");
		return things;
	}

	// 나이대(어린 시절·10대·20대…)마다 첫 기록 하나
	private List<MemorialEventDTO> makePhotos(List<MemorialEventDTO> logs) {
		List<MemorialEventDTO> photos = new ArrayList<>();
		String lastGroup = null;
		for (MemorialEventDTO log : logs) {
			int age = log.getAge();
			String group = age < 13 ? "child" : age < 20 ? "teen" : String.valueOf(age / 10);
			if (!group.equals(lastGroup)) {
				photos.add(log);
				lastGroup = group;
			}
		}
		return photos;
	}

	private Map<String, Object> makeLast(MemorialRoomDTO room, int lifeId) {
		Map<String, Object> last = new HashMap<>();
		if (room.getDeathCause() != null) {
			last.put("scene", "떠난 이유: " + room.getDeathCause());
		}
		if (room.getChildCount() > 0) {
			last.put("mourners", "자녀 " + room.getChildCount() + "명이 마지막 길을 배웅했어요.");
		}
		List<String> lines = new ArrayList<>();
		int countries = dao.countCountries(lifeId);
		if (countries > 1) lines.add(countries + "개 나라에서 살았어요.");
		last.put("lines", lines);
		return last;
	}
}
