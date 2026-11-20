package com.example.demo.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// C06-4 엔진 선택지 (원작 temper · SMALL · HUM 결정을 본뜸, 문장은 우리 말로 새로 씀)
// 윤아님 이벤트(C09)와 따로, 엔진이 그해 처지를 보고 직접 띄우는 작은 선택
//   - 기질 (2세, 한 번): 고른 쪽이 그 사람의 기질이 됨 (특성 칸에 표시)
//   - 어린 시절 (7~15세): 방과 후 · 주운 지갑 · 명절 용돈 · 시험 전날 · 친구와 다툼 · 장래 희망
//   - 어른 (16세~): 주말 · 휴가 · 동창회 · 부모님께 연락 · 혼자 사는 저녁 · 봉사 · 잠들기 전 휴대폰
//   - 사람 사이 (16세~, 원작 HUM): 쓰러진 사람 · 놀림받는 동료 · 옆집 아이 · 이웃 돕기 모금
//   - 진학 (원작 school2 · school3 · college): 중학교·고등학교·대학에 붙은 해에 갈지 말지 직접 고름
//     (붙었는지는 엔진 진학 확률로 먼저 정하고, 붙었을 때만 창이 뜸. 못 붙으면 failText 한 줄)
// 효과 숫자: h 행복, hl 건강, re 관계, kn 지식 (원작 eff 와 같은 크기)
// cost: 중위 소득 × 비율 (어른이고 벌이·재산이 있을 때만 냄, 문장 끝에 금액을 붙임)
// 결과 문장은 선택지마다 다르게 → 서버를 다시 켜도 인생 기록 문장으로 이미 겪은 선택을 다시 찾을 수 있음
public class EngineChoice {

	public static final String TEMPER = "TEMPER";
	public static final String KID = "KID";
	public static final String ADULT = "ADULT";
	public static final String HUM = "HUM";
	public static final String HIST = "HIST";     // 역사 현장 선택 (그해·그 나라에서만, rollEngineChoice 가 먼저 띄움)
	public static final String SCHOOL = "SCHOOL"; // 무작위로 뽑지 않음 (schoolStep 이 붙었을 때만 띄움)
	public static final String SVC = "SVC";       // 병역 (한국 남성, EngineService.svcPick 이 직접 띄움)
	public static final String HOBBY = "HOBBY";   // 취미 고르기 (7·15·27·50세, EngineService 가 직접 띄움, 원작 hobby)
	public static final String MONEY = "MONEY";   // 돈 굴리기·배우자 주식 권유·유언장 (원작 invest · pstock · will, EngineService.moneyPick 이 직접 띄움)

	// 취미 (원작 HOBBIES): 코드 → {이름, 해마다 행복, 건강, 관계, 지식(×0.25로 적용), 비용(중위 소득 비율)}
	public static final String[][] HOBBIES = {
			{ "sport", "운동", "3", "4", "2", "0", "0.01" },
			{ "music", "노래·악기 연주", "4", "0", "2", "1", "0.01" },
			{ "dance", "춤", "4", "2", "2", "0", "0.005" },
			{ "screen", "텔레비전·영화 보기", "3", "-1", "0", "0", "0.005" },
			{ "sns", "스마트폰·SNS", "2", "-1", "2", "0", "0.01" },
			{ "games", "비디오 게임", "4", "-1", "1", "1", "0.02" },
			{ "reading", "독서", "2", "0", "0", "4", "0.01" },
			{ "fishing", "낚시", "3", "1", "1", "0", "0.01" },
			{ "cooking", "요리", "3", "1", "2", "1", "0.01" },
			{ "garden", "텃밭 가꾸기", "3", "2", "0", "0", "0.005" },
			{ "hiking", "등산·산책", "3", "3", "1", "0", "0.005" },
			{ "travel", "여행", "5", "0", "1", "2", "0.06" },
			{ "art", "그림 그리기", "3", "0", "1", "2", "0.01" },
			{ "karaoke", "노래방", "4", "0", "3", "0", "0.02" },
			{ "calli", "서예", "3", "0", "1", "2", "0.005" },
			{ "gym", "헬스", "2", "5", "0", "0", "0.03" } };

	public static String[] hobbyOf(String code) {
		for (String[] h : HOBBIES) {
			if (h[0].equals(code)) {
				return h;
			}
		}
		return null;
	}

	// 선택지 하나
	public static class Opt {
		public final String label;
		public final int h, hl, re, kn;
		public final double cost;
		public final String[] results; // 여러 개면 무작위 (bad 확률은 badP)
		public final double badP;      // results[1]이 나올 확률 (0이면 항상 results[0])
		public final int[] badEff;     // results[1]일 때 효과 {h, hl, re, kn}
		public final String temper;    // 기질 선택지면 기질 이름
		public final Predicate<LifeDTO> show; // 이 선택지를 보여 줄 조건 (null 이면 항상)
		public String act;                    // 진학 선택: ENROLL 간다 / QUIT 안 간다

		Opt(String label, int h, int hl, int re, int kn, double cost, String result) {
			this(label, h, hl, re, kn, cost, new String[] { result }, 0, null, null, null);
		}

		Opt(String label, int h, int hl, int re, int kn, double cost, String[] results, double badP, int[] badEff,
				String temper, Predicate<LifeDTO> show) {
			this.label = label;
			this.h = h;
			this.hl = hl;
			this.re = re;
			this.kn = kn;
			this.cost = cost;
			this.results = results;
			this.badP = badP;
			this.badEff = badEff;
			this.temper = temper;
			this.show = show;
		}

		Opt when(Predicate<LifeDTO> p) {
			Opt o = new Opt(label, h, hl, re, kn, cost, results, badP, badEff, temper, p);
			o.act = act; // act 를 먼저 붙인 선택지도 그대로 (예전엔 빠져서 취미 HOB_ 가 저장되지 않았음)
			return o;
		}

		Opt act(String a) {
			this.act = a;
			return this;
		}
	}

	// 선택 하나
	public static class Dec {
		public final String key;
		public final String group;
		public final int minAge, maxAge;
		public final int max; // 한 인생에 최대 몇 번
		public final int gap; // 같은 선택이 다시 나오기까지 최소 몇 해
		public final Predicate<LifeDTO> ok;
		public final String title;
		public final String[] bodies;
		public final Opt[] opts;
		public String failText; // 진학 선택: 붙지 못했을 때 기록 한 줄
		public double badDeath;  // 나쁜 결과일 때 다음 해 사망 확률 가산 (역사 현장 선택)
		public String badCause;  // 그때 사망 원인

		Dec(String key, String group, int minAge, int maxAge, int max, int gap, Predicate<LifeDTO> ok, String title,
				String[] bodies, Opt... opts) {
			this.key = key;
			this.group = group;
			this.minAge = minAge;
			this.maxAge = maxAge;
			this.max = max;
			this.gap = gap;
			this.ok = ok;
			this.title = title;
			this.bodies = bodies;
			this.opts = opts;
		}
	}

	// 취미 선택지 하나 (고르면 그해 행복 +1, 이후 해마다 효과는 EngineService.hobbyYear)
	static Opt hob(String code, Predicate<LifeDTO> p) {
		String[] h = hobbyOf(code);
		return new Opt(h[1], 1, 0, 0, 0, 0, eul(h[1]) + " 취미로 삼았다.").act("HOB_" + code).when(p);
	}

	// 을/를 (받침 있으면 '을', 영문으로 끝나면 '를')
	static String eul(String w) {
		char ch = w.charAt(w.length() - 1);
		if (ch < 0xAC00 || ch > 0xD7A3) {
			return w + "를";
		}
		return w + ((ch - 0xAC00) % 28 == 0 ? "를" : "을");
	}

	// ===== 처지 판단 (LifeDTO 만 보고) =====
	static boolean inSchool(LifeDTO l) {
		return "ENROLLED".equals(l.getSchoolStatus());
	}

	static boolean working(LifeDTO l) {
		return "EMPLOYED".equals(l.getJobStatus());
	}

	static boolean married(LifeDTO l) {
		return "Y".equals(l.getMarriedYn());
	}

	static boolean parents(LifeDTO l) {
		return !"N".equals(l.getParentAlive());
	}

	static boolean family(LifeDTO l) {
		return married(l) || l.getChildCount() > 0;
	}

	static int year(LifeDTO l) {
		return l.getBirthYear() + l.getCurrentAge();
	}

	static boolean hasPhone(LifeDTO l) {
		String o = l.getOwns() == null ? "" : l.getOwns();
		return o.contains("PHONE") || o.contains("CELL") || EngineStory.had(l.getLifeId(), "era_smart")
				|| EngineStory.had(l.getLifeId(), "era_mobile");
	}

	// 반려동물이 있는지 (OWNS 에 PET-종류-이름-들인나이-수명 토큰)
	static boolean hasPet(LifeDTO l) {
		return l.getOwns() != null && l.getOwns().contains("PET-");
	}

	static boolean schooled(LifeDTO l) {
		return l.getEduLevel() != null && !"NONE".equals(l.getEduLevel()) || inSchool(l);
	}

	// ===== 선택 목록 =====
	public static final List<Dec> ALL = new ArrayList<>();

	static {
		// ---------- 기질 (2세, 원작 temper) ----------
		ALL.add(new Dec("TEMPER", TEMPER, 2, 2, 1, 99, l -> true, "두 살, 처음 보는 것들",
				new String[] { "이제 제법 잘 걸어요. 처음 보는 것들로 가득한 바깥에서, 아이는 무엇부터 할까요? 고른 쪽으로 아이의 기질이 기울어요.",
						"새 장난감이 생겼어요. 아이는 무엇부터 할까요? 고른 쪽으로 아이의 기질이 기울어요." },
				new Opt("한참을 들여다보며 이리저리 만져 본다", 1, 0, 0, 3, 0, new String[] { "궁금한 것은 오래 들여다보는 아이로 자라기 시작했다." }, 0, null, "호기심 많은", null),
				new Opt("있는 힘껏 달려 나간다", 1, 3, 0, 0, 0, new String[] { "잠시도 가만히 있지 않고 뛰어다니는 아이로 자라기 시작했다." }, 0, null, "활발한", null),
				new Opt("우는 아이에게 다가가 손을 잡는다", 1, 0, 3, 0, 0, new String[] { "다른 아이가 울면 먼저 다가가는 아이로 자라기 시작했다." }, 0, null, "다정한", null),
				new Opt("넘어져도 혼자 일어나 다시 해 본다", 2, 1, 0, 1, 0, new String[] { "넘어져도 다시 일어나는 아이로 자라기 시작했다." }, 0, null, "끈기 있는", null)));

		// ---------- 어린 시절 (원작 SMALL 의 아이 쪽) ----------
		ALL.add(new Dec("AFTER", KID, 8, 14, 2, 3, EngineChoice::inSchool, "방과 후",
				new String[] { "수업이 끝났는데 해는 아직 한참 남았어요. 오늘은 무엇을 할까요?", "학교를 나서는데 친구들이 같이 놀자고 불러요." },
				new Opt("친구들과 해 질 때까지 뛰어논다", 2, 1, 3, 0, 0, "해 질 녘까지 친구들과 뛰어놀았다."),
				new Opt("도서관에 들러 책을 읽는다", 1, 0, 0, 3, 0, "도서관 창가에 앉아 책 한 권을 끝까지 읽었다."),
				new Opt("집에 가서 집안일을 돕는다", 0, 0, 2, 0, 0, "집에 가서 빨래를 개고 집안일을 도왔다. 식구들이 고맙다고 했다.")));
		ALL.add(new Dec("WALLET", KID, 7, 14, 1, 99, l -> true, "주운 지갑",
				new String[] { "길에서 지갑을 주웠어요. 안에 돈이 꽤 들어 있어요." },
				new Opt("어른께 알리고 주인을 찾아 준다", 3, 0, 2, 0, 0, "주인을 찾아 주었다. 몇 번이고 고맙다는 인사를 들었다."),
				new Opt("몰래 가진다", -3, 0, 0, 0, 0, "그 돈으로 군것질을 했지만 한동안 마음이 편하지 않았다.")));
		ALL.add(new Dec("POCKET", KID, 8, 15, 1, 99, l -> l.getWealthClass() >= 2, "명절 용돈",
				new String[] { "명절에 어른들께 용돈을 꽤 받았어요. 어디에 쓸까요?" },
				new Opt("저금통에 모은다", 1, 0, 0, 1, 0, "저금통이 묵직해질수록 뿌듯했다."),
				new Opt("친구들과 맛있는 걸 사 먹는다", 2, 0, 3, 0, 0, "친구들과 군것질을 하며 한참 웃었다."),
				new Opt("부모님께 드릴 선물을 산다", 2, 0, 3, 0, 0, "부모님께 작은 선물을 드렸다. 그날 저녁 식탁이 유난히 환했다.")
						.when(EngineChoice::parents)));
		ALL.add(new Dec("EXAM", KID, 11, 17, 2, 3, EngineChoice::inSchool, "시험 전날",
				new String[] { "내일이 시험인데 친구들이 잠깐 놀자고 불러요." },
				new Opt("밤늦게까지 공부한다", 0, -1, 0, 4, 0, "밤늦게까지 공부했다. 시험지를 받자 아는 문제가 꽤 보였다."),
				new Opt("잠깐만 놀다 온다", 2, 0, 2, -1, 0, "잠깐만 논다는 게 해가 질 때까지 놀았다. 시험은 그럭저럭 봤다."),
				new Opt("일찍 자고 아침에 훑어본다", 0, 2, 0, 1, 0, "푹 자고 일어나 아침에 한 번 훑어보고 시험을 봤다.")));
		ALL.add(new Dec("FIGHT", KID, 9, 15, 1, 99, l -> true, "친구와 다툼",
				new String[] { "가장 친한 친구와 사소한 일로 크게 다퉜어요." },
				new Opt("내가 먼저 사과한다", 1, 0, 3, 0, 0, "먼저 미안하다고 했다. 다음 날 둘은 언제 그랬냐는 듯 함께 놀았다."),
				new Opt("친구가 먼저 사과할 때까지 기다린다", -1, 0, -2, 0, 0, new String[] { "며칠을 서먹하게 지내다 겨우 다시 말을 텄다.", "끝내 서로 말을 꺼내지 못했고, 그 친구와는 멀어졌다." }, 0.3, new int[] { -2, 0, -4, 0 }, null, null)));
		ALL.add(new Dec("DREAM", KID, 10, 16, 1, 99, EngineChoice::inSchool, "장래 희망",
				new String[] { "학교에서 장래 희망을 써 오라고 했어요. 무엇이라고 쓸까요?" },
				new Opt("선생님", 1, 0, 0, 2, 0, "공책에 '선생님'이라고 또박또박 적었다."),
				new Opt("운동선수", 1, 2, 0, 0, 0, "공책에 '운동선수'라고 큼직하게 적었다."),
				new Opt("의사", 1, 0, 0, 2, 0, "공책에 '의사'라고 적고 한참을 바라보았다."),
				new Opt("아직 모르겠다", 0, 0, 0, 0, 0, "한참을 고민하다 '아직 모르겠다'고 솔직하게 적었다.")));

		// ---------- 병역 (원작 service, 한국 남성) ----------
		// 현역: 육군 입대 / 해군·공군 지원(붙을 가능성은 해마다 다름, 떨어지면 육군) / 1년 미루기(27세까지)
		// 보충역(방위병·공익·사회복무): 집에서 출퇴근 / 1년 미루기 · 면제는 창 없이 한 줄
		// 복무 기간·결과 문장은 EngineService.chooseEngine 이 그해 기준으로 만듦
		ALL.add(new Dec("SVC_ACT", SVC, 18, 30, 99, 0, l -> true, "입영 통지서",
				new String[] { "병역 의무가 있어요." },
				new Opt("육군으로 입대한다", 0, 0, 0, 0, 0, "육군으로 입대했다.").act("SVC_GO"),
				new Opt("해군에 지원한다 (떨어지면 육군)", 0, 0, 0, 0, 0, "해군에 지원했다.").act("SVC_NAVY"),
				new Opt("공군에 지원한다 (떨어지면 육군)", 0, 0, 0, 0, 0, "공군에 지원했다.").act("SVC_AIR"),
				new Opt("입대를 1년 미룬다", 0, 0, 0, 0, 0, "입대를 1년 미뤘다.").act("SVC_DELAY").when(l -> l.getCurrentAge() < 27)));
		ALL.add(new Dec("SVC_SUP", SVC, 18, 30, 99, 0, l -> true, "소집 통지서",
				new String[] { "보충역 판정을 받았어요." },
				new Opt("소집에 응한다 (집에서 출퇴근)", 0, 0, 0, 0, 0, "보충역으로 소집되어 집에서 출퇴근하며 복무를 시작했다.").act("SVC_SUP"),
				new Opt("소집을 1년 미룬다", 0, 0, 0, 0, 0, "소집을 1년 미뤘다.").act("SVC_DELAY").when(l -> l.getCurrentAge() < 27)));

		// ---------- 취미 (원작 hobby, 7·15·27·50세) ----------
		// 고를 수 있는 것 가운데 4개만 보여 줌 (EngineService.openEngineChoice), 고른 취미는 해마다 조금씩 효과·비용
		ALL.add(new Dec("HOBBY", HOBBY, 6, 99, 99, 0, l -> true, "취미를 하나 가져 볼까?",
				new String[] { "남는 시간에 즐겨 할 것이 있으면 좋겠어요.", "요즘 마음이 가는 것이 있어요." },
				hob("sport", l -> true),
				hob("music", l -> true),
				hob("dance", l -> true),
				hob("screen", l -> year(l) >= 1965),
				hob("sns", l -> year(l) >= 2010 && hasPhone(l)),
				hob("games", l -> year(l) >= 1985 && l.getWealthClass() >= 3),
				hob("reading", l -> schooled(l)),
				hob("fishing", l -> l.getCurrentAge() >= 10),
				hob("cooking", l -> l.getCurrentAge() >= 12),
				hob("garden", l -> l.getCurrentAge() >= 25),
				hob("hiking", l -> true),
				hob("travel", l -> l.getCurrentAge() >= 20 && year(l) >= 1985 && l.getWealthClass() >= 3),
				hob("art", l -> true),
				hob("karaoke", l -> l.getCurrentAge() >= 15 && year(l) >= 1990 && java.util.Set.of("KOR", "JPN", "PHL", "VNM", "THA", "CHN", "PRK").contains(l.getCurrentCountry())),
				hob("calli", l -> schooled(l) && java.util.Set.of("CHN", "JPN", "KOR").contains(l.getCurrentCountry())),
				hob("gym", l -> l.getCurrentAge() >= 18 && year(l) >= 1990 && "Y".equals(l.getIsUrban()))));

		// ---------- 반려동물 (원작 petq) ----------
		// 아이(6~14세): 강아지를 키우고 싶다고 조름 → 허락은 집안 형편·그 나라 반려동물 비율에 달림 (EngineService.chooseEngine)
		// 어른(20~70세): 강아지·고양이를 들일지 고름. 들이면 해마다 돌봄 비용, 수명이 다하면 무지개다리 (EngineService.petYear)
		// 원작처럼 이름을 직접 적을 수 있음 (화면: ENG_PET_ 창에 이름 칸, 비우면 그 나라 흔한 이름)
		// 선택지 아래 줄(한 해 비용 · 함께하는 햇수)은 EngineService.openEngineChoice 가 붙임
		ALL.add(new Dec("PET_BEG", KID, 6, 14, 2, 3, l -> !hasPet(l) && year(l) >= 1960, "강아지를 키우고 싶어요",
				new String[] { "친구 집 강아지를 보고 온 날부터 부모님께 조르고 있어요.",
						"이웃집 개가 새끼를 낳았다는 말을 듣고부터 부모님께 조르고 있어요.",
						"친구가 강아지와 산책하는 모습을 본 뒤로 부모님께 조르고 있어요." },
				new Opt("조르고 또 조른다 (허락은 집안 형편에 달려 있어요)", 0, 0, 0, 0, 0, "강아지를 키우게 해 달라고 조르고 또 졸랐다.").act("PET_BEG"),
				new Opt("꾹 참는다", -1, 0, 0, 0, 0, "강아지를 키우고 싶은 마음을 꾹 참았다. 크면 꼭 키우겠다고 마음먹었다.")));
		ALL.add(new Dec("PET_ADOPT", ADULT, 20, 75, 3, 5, l -> !hasPet(l) && year(l) >= 1970, "반려동물을 들일까?",
				new String[] { "이웃집에서 태어난 새끼들이 새 가족을 찾고 있어요.",
						"보호소에서 새 가족을 기다리는 동물들을 보았어요.",
						"동네에서 자주 마주치던 길고양이가 자꾸 눈에 밟혀요.",
						"친구네 동물을 며칠 맡아 돌보고 나니, 집에 동물이 있으면 어떨까 싶어졌어요." },
				new Opt("강아지를 들인다", 0, 0, 0, 0, 0, "강아지를 들였다.").act("PET_DOG"),
				new Opt("고양이를 들인다", 0, 0, 0, 0, 0, "고양이를 들였다.").act("PET_CAT"),
				new Opt("작은 동물을 들인다", 0, 0, 0, 0, 0, "작은 동물을 들였다.").act("PET_SMALL"),
				new Opt("지금은 아니다", 0, 0, 0, 0, 0, "지금은 들이지 않기로 했다. 끝까지 돌볼 자신이 생기면 그때 생각하기로 했다."),
				new Opt("반려동물은 들이지 않는다 (다시 묻지 않아요)", 0, 0, 0, 0, 0, "반려동물은 들이지 않기로 했다.").act("PET_NEVER")));

		// ---------- 돈·유언 (원작 invest · pstock · will) ----------
		// 본문(금액)은 EngineService.openEngineChoice 가 채움, 고른 뒤 처리는 chooseEngine (INV_ · PSTOCK_ · WILL_)
		// 결과 문장 앞부분이 늘 같아서, 서버를 다시 켜도 인생 기록에서 이미 고른 것을 찾음
		ALL.add(new Dec("INVEST", MONEY, 18, 90, 1, 99, l -> true, "모은 돈을 어떻게 굴릴까?",
				new String[] { "모은 돈을 어디에 둘까요?" },
				new Opt("예금에 넣는다 (물가 빼고 연 1~1.5% · 거의 안전)", 0, 0, 0, 0, 0, "모은 돈을 예금에 맡기기로 했다.").act("INV_DEPOSIT"),
				new Opt("주식·채권 반반 펀드에 넣는다 (평균 연 3.5% · 출렁임 중간)", 0, 0, 0, 0, 0, "모은 돈을 주식·채권 반반 펀드에 맡기기로 했다.").act("INV_MIX"),
				new Opt("주식에 투자한다 (평균 연 5% · 폭락할 수도)", 0, 0, 0, 0, 0, "모은 돈을 주식 (지수)에 맡기기로 했다.").act("INV_STOCK"),
				new Opt("부동산에 투자한다 (평균 연 3.5% · 변동 중간)", 0, 0, 0, 0, 0, "모은 돈을 부동산 펀드에 맡기기로 했다.").act("INV_PROPERTY")));
		ALL.add(new Dec("PSTOCK", MONEY, 25, 75, 1, 99, l -> "Y".equals(l.getMarriedYn()), "배우자가 주식에 크게 넣자고 해요",
				new String[] { "배우자가 아는 사람에게 확실한 종목이라는 이야기를 들었다며, 모아 둔 돈의 절반을 넣자고 해요." },
				new Opt("믿고 넣어 본다 (크게 벌거나 크게 잃음)", 0, 0, 0, 0, 0, "배우자의 말을 믿고 모아 둔 돈의 절반을 주식에 넣었다.").act("PSTOCK_GO"),
				new Opt("위험하다고 말린다 (사이가 조금 서먹해짐)", 0, 0, -1, 0, 0, "한바탕 언쟁 끝에 주식 이야기는 없던 일이 되었다.")));
		ALL.add(new Dec("WILL", MONEY, 50, 110, 1, 99, l -> true, "유언장을 쓸까?",
				new String[] { "남길 것을 생각할 나이가 되었어요." },
				new Opt("쓰지 않고 법에 맡긴다", 0, 0, 0, 0, 0, "유언장은 쓰지 않기로 했다. 남은 사람들이 법대로 나누면 된다고 생각했다.").act("WILL_LAW"),
				new Opt("배우자에게 먼저 남긴다", 2, 0, 0, 0, 0, "유언장을 썼다. 배우자에게 먼저 남기기로 했다.").act("WILL_SPOUSE")
						.when(l -> "Y".equals(l.getMarriedYn()) && l.getChildCount() > 0),
				new Opt("가족에게 똑같이 나눈다", 2, 0, 0, 0, 0, "유언장을 썼다. 가족이 똑같이 나누도록 했다.").act("WILL_FAM")
						.when(l -> ("Y".equals(l.getMarriedYn()) ? 1 : 0) + l.getChildCount() >= 2),
				new Opt("5분의 1을 구호 단체에 남긴다 (나머지는 가족에게)", 2, 0, 1, 0, 0, "유언장을 썼다. 재산의 5분의 1은 구호 단체에 남기기로 했다.").act("WILL_GIVE"),
				new Opt("모두 구호 단체에 남긴다", 3, 0, 1, 0, 0, "유언장을 썼다. 남길 수 있는 만큼을 구호 단체에 남기기로 했다.").act("WILL_GIVEALL")
						.when(l -> !"Y".equals(l.getMarriedYn()) && l.getChildCount() == 0)));

		// ---------- 어른 (원작 SMALL) ----------
		// 휴대폰이 있는 사람만 (돈 쓰기로 산 휴대폰, 또는 인생 기록에 '휴대전화·스마트폰이 생겼다'가 나온 사람)
		ALL.add(new Dec("PHONE", ADULT, 13, 60, 1, 99, l -> year(l) >= 2012 && hasPhone(l), "잠들기 전 휴대폰",
				new String[] { "누워서 휴대폰을 보다 보면 어느새 새벽이에요." },
				new Opt("휴대폰을 멀리 두고 일찍 잔다", 0, 2, 0, 1, 0, "휴대폰을 침대 밖에 두고 잤다. 아침이 한결 가벼웠다."),
				new Opt("조금만 더 본다, 이게 쉬는 시간이다", 1, -1, 0, 0, 0, "영상 몇 개만 더 본다는 게 또 늦게 잠들었다.")));
		ALL.add(new Dec("COOK", ADULT, 20, 70, 1, 99, l -> true, "요리를 배워 볼까?",
				new String[] { "늘 사 먹거나 대충 때우는 끼니가 마음에 걸려요." },
				new Opt("동네 요리 강좌에 다닌다", 1, 1, 1, 0, 0.01, "동네 요리 강좌에 다니며 몇 가지 음식을 제대로 배웠다."),
				new Opt("지금처럼 지낸다", 0, 0, 0, 0, 0, "요리는 다음에 배우기로 했다. 끼니는 오늘도 간단히 때웠다.")));
		ALL.add(new Dec("TIDY", ADULT, 25, 80, 1, 99, l -> true, "물건으로 가득 찬 집",
				new String[] { "언제부턴가 집 안에 물건이 쌓여 발 디딜 틈이 줄었어요." },
				new Opt("주말에 날 잡고 싹 정리한다", 2, 0, 0, 0, 0, "주말 내내 쓰지 않는 물건을 정리했다. 집이 한결 넓어졌다."),
				new Opt("언젠가 쓸 데가 있겠지, 그냥 둔다", -1, 0, 0, 0, 0, "버리지 못한 물건이 또 한 상자 늘었다.")));
		ALL.add(new Dec("WALK", ADULT, 40, 85, 1, 99, l -> l.getHealth() < 70, "의사가 걸으라고 했어요",
				new String[] { "건강검진에서 의사가 하루에 30분씩이라도 걸으라고 했어요." },
				new Opt("매일 30분씩 걷는다", 1, 3, 0, 0, 0, "매일 저녁 30분씩 걸었다. 몸이 조금 가벼워졌다."),
				new Opt("바빠서 시간이 없다", 0, -1, 0, 0, 0, "걷기는 마음뿐이었다. 한 해가 바쁘게 지나갔다.")));
		ALL.add(new Dec("WEEKEND", ADULT, 18, 75, 2, 8, l -> true, "모처럼 쉬는 주말",
				new String[] { "아무 약속도 없는 주말이에요. 무엇을 할까요?", "늦잠을 자고 일어나니 창밖이 맑아요." },
				new Opt("오랜만에 친구들을 만난다", 2, 0, 3, 0, 0.01, "친구들과 늦게까지 이야기를 나눴다."),
				new Opt("집에서 푹 쉰다", 1, 2, 0, 0, 0, "늦잠을 자고 아무것도 하지 않았다. 그것도 좋았다."),
				new Opt("가족과 나들이를 간다", 2, 0, 3, 0, 0.01, "가족과 도시락을 싸 들고 가까운 공원에 다녀왔다.").when(EngineChoice::family),
				new Opt("혼자 가 보고 싶던 곳에 간다", 3, 0, 0, 0, 0.01, "혼자 가 보고 싶던 동네에 다녀왔다. 낯선 길을 걷는 것만으로 즐거웠다.").when(l -> !family(l))));
		ALL.add(new Dec("HOLIDAY", ADULT, 22, 72, 2, 10, EngineChoice::working, "휴가를 어디서 보낼까?",
				new String[] { "며칠 쉴 수 있게 되었어요." },
				new Opt("부모님 댁에 다녀온다", 2, 0, 4, 0, 0, "부모님 댁에서 며칠을 보냈다. 어릴 적 방이 그대로였다.").when(EngineChoice::parents),
				new Opt("가까운 바닷가나 산으로 놀러 간다", 4, 1, 0, 0, 0.03, "가까운 바닷가와 산을 돌며 오랜만에 숨을 돌렸다."),
				new Opt("해외여행을 간다", 6, 0, 0, 1, 0.12, "처음 가 보는 나라의 골목을 걸었다. 세상이 넓다는 걸 실감했다.")
						.when(l -> year(l) >= 1990 && l.getAssets() > 0),
				new Opt("집에서 쉰다", 0, 2, 0, 0, 0, "집에서 밀린 잠을 잤다.")));
		ALL.add(new Dec("REUNION", ADULT, 25, 75, 1, 99, EngineChoice::schooled, "동창회 연락",
				new String[] { "오랜만에 동창회를 한다는 연락이 왔어요." },
				new Opt("나가 본다", 2, 0, 3, 0, 0.01, new String[] { "옛 친구들과 학창 시절 이야기로 밤을 새웠다. 다시 연락하며 지내게 되었다.", "다들 잘 지내는 것 같아 괜히 씁쓸한 마음으로 돌아왔다." }, 0.3, new int[] { -2, 0, 1, 0 }, null, null),
				new Opt("이번엔 빠진다", 0, 0, 0, 0, 0, "이번 동창회는 빠지기로 했다.")));
		ALL.add(new Dec("CALL", ADULT, 20, 60, 2, 10, EngineChoice::parents, "부모님께 연락",
				new String[] { "바쁘게 지내다 보니 부모님께 연락을 드린 지 오래됐어요." },
				new Opt("주말에 찾아뵌다", 2, 0, 4, 0, 0.01, "주말에 부모님을 찾아뵙고 저녁을 함께 먹었다."),
				new Opt("전화로 안부를 여쭌다", 1, 0, 2, 0, 0, "전화로 안부를 여쭈었다. 목소리가 밝으셔서 마음이 놓였다."),
				new Opt("다음에 연락드린다", 0, 0, -2, 0, 0, "다음에 연락드려야지 하고 미루다 한 해가 지나갔다.")));
		ALL.add(new Dec("DINNER", ADULT, 18, 35, 1, 99, l -> !married(l), "혼자 사는 저녁",
				new String[] { "퇴근하고 돌아오니 배가 고파요. 저녁은 어떻게 할까요?" },
				new Opt("장을 봐서 직접 해 먹는다", 1, 2, 0, 1, 0, "서툴지만 직접 장을 봐서 저녁을 해 먹었다. 생각보다 맛있었다."),
				new Opt("간단히 사 먹는다", 1, -1, 0, 0, 0.005, "가까운 식당에서 간단히 저녁을 사 먹었다.")));
		ALL.add(new Dec("VOLUNTEER", ADULT, 16, 70, 1, 99, l -> true, "주말 봉사",
				new String[] { "동네에서 주말 봉사할 사람을 찾는다는 안내문을 봤어요." },
				new Opt("신청해서 다녀온다", 3, 0, 3, 0, 0, "주말 내내 봉사를 하고 왔다. 몸은 고단했지만 마음은 가벼웠다."),
				new Opt("이번엔 쉰다", 0, 0, 0, 0, 0, "이번 주말은 쉬기로 했다.")));

		// ---------- 사람 사이 (원작 HUM) ----------
		ALL.add(new Dec("STRANGER", HUM, 16, 79, 1, 99, l -> true, "길에 쓰러진 사람",
				new String[] { "길에 낯선 사람이 쓰러져 있어요. 다들 바쁘게 지나가요." },
				new Opt("병원에 데려간다", 4, 0, 3, 0, 0.02, "병원까지 함께 갔다. 며칠 뒤 그 사람의 가족이 고맙다며 찾아왔다."),
				new Opt("누군가 돕겠지, 지나간다", -2, 0, 0, 0, 0, "그 사람이 어떻게 되었는지 끝내 알지 못했다.")));
		ALL.add(new Dec("COWORKER", HUM, 18, 64, 1, 99, EngineChoice::working, "놀림받는 동료",
				new String[] { "동료 한 명이 말투 때문에 사람들 앞에서 놀림을 받고 있어요." },
				new Opt("그러지 말자고 말한다", 2, 0, 3, 0, 0, new String[] { "그날 이후 그 동료와 가장 가까운 사이가 되었다.", "한동안 몇몇 동료가 나를 어색하게 대했다. 그래도 그 동료는 나를 믿고 따랐다." }, 0.33, new int[] { 1, 0, 1, 0 }, null, null),
				new Opt("모른 척한다", -3, 0, 0, 0, 0, "그날 밤 마음이 오래 무거웠다.")));
		ALL.add(new Dec("NEIGHBORKID", HUM, 25, 70, 1, 99, l -> true, "옆집 아이",
				new String[] { "옆집 아이가 늦은 밤까지 혼자 집 앞에 앉아 있어요." },
				new Opt("말을 걸고 간식을 건넨다", 1, 0, 2, 0, 0, "아이에게 간식을 건네며 말을 걸었다. 그 뒤로 그 집을 눈여겨보게 되었다."),
				new Opt("남의 집 일이니 모른 척한다", -2, 0, 0, 0, 0, "그 집 앞을 지날 때마다 마음이 무거웠다.")));
		ALL.add(new Dec("DONATE", HUM, 25, 80, 1, 99, l -> l.getAssets() > 0, "이웃 돕기 모금",
				new String[] { "동네에 큰불이 나서 집을 잃은 이웃을 돕는 모금을 하고 있어요." },
				new Opt("넉넉히 낸다", 3, 0, 2, 0, 0.02, "넉넉히 보탰다. 이웃이 몇 번이고 고개를 숙였다."),
				new Opt("조금 보탠다", 1, 0, 1, 0, 0.003, "형편껏 조금 보탰다."),
				new Opt("이번엔 지나친다", -1, 0, 0, 0, 0, "모금함 앞을 그냥 지나쳤다.")));
	}

	static {
		// ---------- 진학 (원작 school2 · school3 · college) ----------
		Dec m = new Dec("SCHOOL_MIDDLE", SCHOOL, 0, 99, 1, 99, l -> true, "중학교에 갈까?",
				new String[] { "초등학교를 마쳤어요. 중학교에 갈 수 있게 됐어요. 공부를 계속할까요?" },
				new Opt("중학교에 간다", 1, 0, 0, 2, 0, "중학교에 진학했다. 새 교과서 냄새가 좋았다.").act("ENROLL"),
				new Opt("공부는 여기까지 하고 집안일을 돕는다", -2, 0, 2, -1, 0, "학교 대신 집안일을 돕기로 했다. 교복 입은 친구들이 지나갈 때마다 눈길이 갔다.").act("QUIT"));
		m.failText = "집안 형편이 어려워 중학교에 가지 못했다.";
		ALL.add(m);
		Dec h = new Dec("SCHOOL_HIGH", SCHOOL, 0, 99, 1, 99, l -> true, "고등학교에 갈까?",
				new String[] { "중학교를 마쳤어요. 고등학교에 갈 수 있게 됐어요. 공부를 계속할까요?" },
				new Opt("고등학교에 간다", 1, 0, 0, 2, 0, "고등학교에 진학했다. 공부가 한결 어려워졌다.").act("ENROLL"),
				new Opt("공부는 여기까지 하고 집안일을 거든다", -1, 0, 1, 0, 0, "고등학교 대신 집안일을 거들며 지내기로 했다. 친구들 소식이 가끔 부러웠다.").act("QUIT"));
		h.failText = "집안 형편이 어려워 고등학교에 가지 못했다.";
		ALL.add(h);
		Dec u = new Dec("SCHOOL_UNI", SCHOOL, 0, 99, 1, 99, l -> true, "대학에 갈까?",
				new String[] { "대학에 붙었어요. 몇 년 더 공부하면 더 좋은 일자리를 얻을 수 있지만, 그동안은 벌이가 없어요." },
				new Opt("대학에 간다", 2, 0, 0, 2, 0, "대학에 진학했다. 강의실이 생각보다 넓었다.").act("ENROLL"),
				new Opt("대학 대신 바로 일을 시작한다", 0, 0, 1, 0, 0, "대학 대신 바로 일을 시작하기로 했다.").act("QUIT"));
		u.failText = "대학 입시에 또 떨어졌다. 공부는 여기까지 하기로 했다.";
		ALL.add(u);
	}

	static {
		// ---------- 역사 현장 (한국 민주화운동): 그해 그 나라에 사는 젊은이에게 한 번 ----------
		//   year(l) 은 '새 나이' 기준이라 지난 한 해(lived)는 year(l) - 1
		Dec a = new Dec("DEMO_1960", HIST, 15, 35, 1, 99, l -> "KOR".equals(l.getCurrentCountry()) && year(l) - 1 == 1960,
				"4·19 혁명",
				new String[] { "부정선거에 항의하는 학생들이 거리로 쏟아져 나왔어요. 학교 친구들도 교문을 나서고 있어요." },
				new Opt("거리로 나가 함께 외친다", 3, 0, 3, 1, 0,
						new String[] { "시위대와 함께 거리를 걸으며 목이 쉬도록 외쳤다. 그 봄, 정권이 무너졌다.",
								"시위대 속에서 총성을 피해 달아나다 크게 다쳤다." },
						0.15, new int[] { -2, -15, 2, 0 }, null, null),
				new Opt("멀리서 지켜본다", 0, 0, 0, 0, 0, "창문 너머로 행진하는 사람들을 지켜보았다."));
		a.badDeath = 0.03;
		a.badCause = "시위 진압";
		ALL.add(a);
		Dec b = new Dec("DEMO_1980", HIST, 17, 35, 1, 99, l -> "KOR".equals(l.getCurrentCountry()) && year(l) - 1 == 1980,
				"서울의 봄",
				new String[] { "계엄 해제와 민주화를 외치는 시위가 이어져요. 남쪽 광주에서는 계엄군이 시민을 막았다는 소문이 들려요." },
				new Opt("시위에 나간다", 2, 0, 3, 1, 0,
						new String[] { "수많은 사람들과 함께 민주주의를 외쳤다. 그 봄은 짧았지만 잊히지 않았다.",
								"계엄군의 진압에 붙잡혀 크게 다쳤다. 한동안 그날 일을 입 밖에 내지 못했다." },
						0.2, new int[] { -6, -15, 1, 0 }, null, null),
				new Opt("몸을 사린다", -1, 0, 0, 0, 0, "뉴스가 끊긴 라디오 앞에서 숨죽이며 지냈다."));
		b.badDeath = 0.03;
		b.badCause = "시위 진압";
		ALL.add(b);
		Dec c = new Dec("DEMO_1987", HIST, 16, 45, 1, 99, l -> "KOR".equals(l.getCurrentCountry()) && year(l) - 1 == 1987,
				"6월 민주항쟁",
				new String[] { "대학생의 죽음이 알려지면서 전국에서 직선제를 요구하는 시위가 일어나고 있어요. 넥타이를 맨 회사원들도 거리로 나와요." },
				new Opt("거리로 나간다", 5, 0, 3, 1, 0,
						new String[] { "최루탄 연기 속에서도 사람들과 함께 외쳤다. 그해 여름, 대통령 직선제가 약속됐다.",
								"최루탄 연기 속에서 넘어져 크게 다쳤다." },
						0.1, new int[] { 0, -8, 2, 0 }, null, null),
				new Opt("마음으로만 응원한다", 0, 0, 0, 0, 0, "퇴근길 버스 안에서 거리를 메운 사람들을 바라보았다."));
		c.badDeath = 0.005;
		c.badCause = "시위 진압";
		ALL.add(c);
	}

	public static int indexOf(String key) {
		for (int i = 0; i < ALL.size(); i++) {
			if (ALL.get(i).key.equals(key)) {
				return i;
			}
		}
		return -1;
	}

	public static Dec byKey(String key) {
		for (Dec d : ALL) {
			if (d.key.equals(key)) {
				return d;
			}
		}
		return null;
	}

	// 선택지 번호 → 화면 choiceId (음수: 윤아님 EVENT_CHOICE 번호와 겹치지 않게)
	public static int choiceId(int decIdx, int optIdx) {
		return -(decIdx * 100 + optIdx + 1); // 선택지가 10개 넘는 취미 때문에 100 단위 (예전 10 단위는 11번째부터 다음 선택과 겹쳤음)
	}

	public static int decIdxOf(int choiceId) {
		return (-choiceId - 1) / 100;
	}

	public static int optIdxOf(int choiceId) {
		return (-choiceId - 1) % 100;
	}
}
