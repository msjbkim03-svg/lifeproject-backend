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
//     집안 형편에 따라 선택지가 더 생김 (어려우면 일터·공장·학자금 대출, 넉넉하면 학원·해외 대학)
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
	public static final String LIFE = "LIFE";     // 삶의 큰 갈림길 (원작 retire · jobchange · rankup · buy · care · oldhome · smoke · drink · lotto · lend …, EngineService.lifePick 이 직접 띄움, 11/23)
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

	// 집안 형편 (1~2 어려움, 4~5 넉넉함)
	static boolean poor(LifeDTO l) {
		return l.getWealthClass() <= 2;
	}

	static boolean rich(LifeDTO l) {
		return l.getWealthClass() >= 4;
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
				new Opt("집에 가서 집안일을 돕는다", 0, 0, 2, 0, 0, "집에 가서 빨래를 개고 집안일을 도왔다. 식구들이 고맙다고 했다."),
				new Opt("학원에 간다", -1, 0, 0, 4, 0, "학교가 끝나자 곧장 학원으로 갔다. 부모님이 학원비를 대 주셨다.")
						.when(l -> rich(l) && year(l) >= 1970),
				new Opt("부모님 일터에 가서 일손을 보탠다", -1, 0, 2, -1, 0, "학교가 끝나면 곧장 부모님 일터로 가서 일손을 보탰다. 숙제는 밤에 했다.")
						.when(EngineChoice::poor)));
		ALL.add(new Dec("WALLET", KID, 7, 14, 1, 99, l -> true, "주운 지갑",
				new String[] { "길에서 지갑을 주웠어요. 안에 돈이 꽤 들어 있어요." },
				new Opt("어른께 알리고 주인을 찾아 준다", 3, 0, 2, 0, 0, "주운 돈의 주인을 찾아 주었다. 몇 번이고 고맙다는 인사를 들었다."),
				new Opt("몰래 가진다", -3, 0, 0, 0, 0, "주운 돈으로 군것질을 했지만 한동안 마음이 편하지 않았다.")));
		ALL.add(new Dec("POCKET", KID, 8, 15, 1, 99, l -> l.getWealthClass() >= 2, "명절 용돈",
				new String[] { "명절에 어른들께 용돈을 꽤 받았어요. 어디에 쓸까요?" },
				new Opt("저금통에 모은다", 1, 0, 0, 1, 0, "명절 용돈을 저금통에 모았다. 저금통이 묵직해질수록 뿌듯했다."),
				new Opt("친구들과 맛있는 걸 사 먹는다", 2, 0, 3, 0, 0, "명절 용돈으로 친구들과 군것질을 하며 한참 웃었다."),
				new Opt("부모님께 드릴 선물을 산다", 2, 0, 3, 0, 0, "명절 용돈으로 부모님께 작은 선물을 사 드렸다. 그날 저녁 식탁이 유난히 환했다.")
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
				new Opt("선생님", 1, 0, 0, 2, 0, "장래 희망 칸에 '선생님'이라고 또박또박 적었다."),
				new Opt("운동선수", 1, 2, 0, 0, 0, "장래 희망 칸에 '운동선수'라고 큼직하게 적었다."),
				new Opt("의사", 1, 0, 0, 2, 0, "장래 희망 칸에 '의사'라고 적고 한참을 바라보았다."),
				new Opt("아직 모르겠다", 0, 0, 0, 0, 0, "장래 희망 칸 앞에서 한참을 고민하다 '아직 모르겠다'고 솔직하게 적었다.")));

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
				new Opt("지금은 아니다", 0, 0, 0, 0, 0, "반려동물은 아직 들이지 않았다. 끝까지 돌볼 자신이 생기면 그때 생각하기로 했다."),
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
				// 원작: 집이 있고 자녀가 있으면 집을 한 자녀에게 ({첫째}는 EngineService.willWords 가 이름으로 바꿈)
				new Opt("집은 {첫째}에게, 나머지는 법대로", 2, 0, 1, 0, 0, "유언장을 썼다. 집은 {첫째}에게 남기기로 했다.").act("WILL_HOME")
						.when(l -> l.getOwns() != null && l.getOwns().contains("HOUSE") && l.getChildCount() > 0
								&& ("Y".equals(l.getMarriedYn()) || l.getChildCount() >= 2)),
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
				new Opt("신청해서 다녀온다", 3, 0, 3, 0, 0, "주말 내내 동네 봉사를 하고 왔다. 몸은 고단했지만 마음은 가벼웠다."),
				new Opt("이번엔 쉰다", 0, 0, 0, 0, 0, "이번 주말 봉사는 쉬기로 했다.")));

		// ---------- 사람 사이 (원작 HUM) ----------
		ALL.add(new Dec("STRANGER", HUM, 16, 79, 1, 99, l -> true, "길에 쓰러진 사람",
				new String[] { "길에 낯선 사람이 쓰러져 있어요. 다들 바쁘게 지나가요." },
				// 원작 stranger: 돈(중위 소득 2%)은 벌이가 있는 어른만 · 30%는 가족이 고맙다며 찾아옴
				new Opt("병원에 데려간다", 3, 0, 0, 0, 0.02, new String[] { "길에 쓰러진 사람을 병원까지 데려가 의료진에게 맡기고 돌아왔다. 이름도 모르는 사람이었지만 무사하기를 빌었다.",
						"길에 쓰러진 사람을 병원까지 데려갔다. 며칠 뒤 그 사람의 가족이 고맙다며 찾아왔다." }, 0.3, new int[] { 4, 0, 3, 0 }, null, null),
				new Opt("누군가 돕겠지, 지나간다", -2, 0, 0, 0, 0, "길에 쓰러진 사람을 두고 지나쳤다. 그 사람이 어떻게 되었는지 끝내 알지 못했다.")));
		ALL.add(new Dec("COWORKER", HUM, 18, 64, 1, 99, EngineChoice::working, "놀림받는 동료",
				new String[] { "동료 한 명이 말투 때문에 사람들 앞에서 놀림을 받고 있어요." },
				new Opt("그러지 말자고 말한다", 2, 0, 3, 0, 0, new String[] { "놀림받던 동료 편을 들었다. 그날 이후 그 동료와 가장 가까운 사이가 되었다.", "놀림받던 동료 편을 들었다. 한동안 몇몇 동료가 나를 어색하게 대했다. 그래도 그 동료는 나를 믿고 따랐다." }, 0.33, new int[] { 1, 0, 1, 0 }, null, null),
				new Opt("모른 척한다", -3, 0, 0, 0, 0, "동료가 놀림받는 것을 모른 척했다. 그날 밤 마음이 오래 무거웠다.")));
		ALL.add(new Dec("NEIGHBORKID", HUM, 25, 70, 1, 99, l -> true, "옆집 아이",
				new String[] { "옆집 아이가 늦은 밤까지 혼자 집 앞에 앉아 있어요." },
				new Opt("말을 걸고 간식을 건넨다", 1, 0, 2, 0, 0, "늦은 밤 혼자 있던 옆집 아이에게 간식을 건네며 말을 걸었다. 그 뒤로 그 집을 눈여겨보게 되었다."),
				new Opt("남의 집 일이니 모른 척한다", -2, 0, 0, 0, 0, "혼자 있던 옆집 아이를 모른 척했다. 그 집 앞을 지날 때마다 마음이 무거웠다.")));
		ALL.add(new Dec("DONATE", HUM, 25, 80, 1, 99, l -> l.getAssets() > 0, "이웃 돕기 모금",
				new String[] { "동네에 큰불이 나서 집을 잃은 이웃을 돕는 모금을 하고 있어요." },
				new Opt("넉넉히 낸다", 3, 0, 2, 0, 0.02, "이웃 돕기 모금에 넉넉히 보탰다. 이웃이 몇 번이고 고개를 숙였다."),
				new Opt("조금 보탠다", 1, 0, 1, 0, 0.003, "이웃 돕기 모금에 형편껏 조금 보탰다."),
				new Opt("이번엔 지나친다", -1, 0, 0, 0, 0, "이웃 돕기 모금함 앞을 그냥 지나쳤다.")));
	}

	static {
		// ---------- 진학 (원작 school2 · school3 · college) ----------
		Dec m = new Dec("SCHOOL_MIDDLE", SCHOOL, 0, 99, 1, 99, l -> true, "중학교에 갈까?",
				new String[] { "초등학교를 마쳤어요. 중학교에 갈 수 있게 됐어요. 공부를 계속할까요?" },
				new Opt("중학교에 간다", 1, 0, 0, 2, 0, "중학교에 진학했다. 새 교과서 냄새가 좋았다.").act("ENROLL"),
				new Opt("공부는 여기까지 하고 집안일을 돕는다", -2, 0, 2, -1, 0, "학교 대신 집안일을 돕기로 했다. 교복 입은 친구들이 지나갈 때마다 눈길이 갔다.").act("QUIT"),
				new Opt("돈을 벌러 일터에 나간다", -2, -1, 2, -1, 0, "중학교 대신 일터에 나갔다. 처음 받은 돈을 어머니께 모두 드렸다.").act("QUIT")
						.when(EngineChoice::poor));
		m.failText = "집안 형편이 어려워 중학교에 가지 못했다.";
		ALL.add(m);
		Dec h = new Dec("SCHOOL_HIGH", SCHOOL, 0, 99, 1, 99, l -> true, "고등학교에 갈까?",
				new String[] { "중학교를 마쳤어요. 고등학교에 갈 수 있게 됐어요. 공부를 계속할까요?" },
				new Opt("고등학교에 간다", 1, 0, 0, 2, 0, "고등학교에 진학했다. 공부가 한결 어려워졌다.").act("ENROLL"),
				new Opt("공부는 여기까지 하고 집안일을 거든다", -1, 0, 1, 0, 0, "고등학교 대신 집안일을 거들며 지내기로 했다. 친구들 소식이 가끔 부러웠다.").act("QUIT"),
				new Opt("공장에 나가 동생들 학비를 보탠다", -2, -1, 3, 0, 0, "고등학교 대신 공장에 나갔다. 번 돈은 동생들 학비에 보탰다.").act("QUIT")
						.when(EngineChoice::poor));
		h.failText = "집안 형편이 어려워 고등학교에 가지 못했다.";
		ALL.add(h);
		Dec u = new Dec("SCHOOL_UNI", SCHOOL, 0, 99, 1, 99, l -> true, "대학에 갈까?",
				new String[] { "대학에 붙었어요. 몇 년 더 공부하면 더 좋은 일자리를 얻을 수 있지만, 그동안은 벌이가 없어요." },
				new Opt("대학에 간다", 2, 0, 0, 2, 0, "대학에 진학했다. 강의실이 생각보다 넓었다.").act("ENROLL"),
				new Opt("대학 대신 바로 일을 시작한다", 0, 0, 1, 0, 0, "대학 대신 바로 일을 시작하기로 했다.").act("QUIT"),
				new Opt("부모님 지원으로 해외 대학에 간다", 3, 0, -1, 4, 0, "부모님의 지원으로 바다 건너 대학에 들어갔다. 낯선 말로 듣는 첫 강의가 버거웠다.").act("ENROLL")
						.when(l -> l.getWealthClass() >= 5 && year(l) >= 1980),
				new Opt("학자금 대출을 받아 간다", 1, 0, 0, 2, 0, "학자금 대출을 받아 대학에 진학했다. 졸업하면 갚아야 할 돈이 마음에 걸렸다.").act("ENROLL")
						.when(EngineChoice::poor));
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

	// ===== 원작에서 자주 나오는데 없던 선택 (11/23, 원작 아시아 303번 비교) =====
	//   맨 뒤에 붙임: 선택지 번호(choiceId)가 선택 순서로 정해져서, 앞에 끼우면 진행 중인 인생의 번호가 어긋남
	//   언제 띄울지는 EngineService.lifePick (조건·확률), 고른 뒤 처리는 chooseEngine 의 act
	static {
		// ---- 사람 사이·작은 선택 (원작 thanks · sm_friend · newneighbor) ----
		ALL.add(new Dec("THANKS", ADULT, 22, 85, 1, 99, l -> true, "고맙다는 말",
				new String[] { "문득 오래전 힘들 때 곁을 지켜 준 사람이 떠올랐어요. 그때 고맙다는 말을 제대로 하지 못했어요." },
				new Opt("지금 전화해서 말한다", 3, 0, 3, 0, 0, "오래 미뤄 둔 고맙다는 말을 전화로 전했다. 수화기 너머에서 한참 말이 없었다."),
				new Opt("다음에 만나면 하자", 0, 0, 0, 0, 0, new String[] { "고맙다는 말은 다음에 만나면 하기로 했다.",
						"고맙다는 말은 다음에 만나면 하기로 했다. 그 다음은 끝내 오지 않았다." }, 0.3, new int[] { -2, 0, 0, 0 }, null, null)));
		ALL.add(new Dec("NEWFRIEND", ADULT, 16, 70, 1, 99, l -> true, "친구가 될까?",
				new String[] { "모임에서 자주 마주치는 사람이 있어요. 말이 잘 통할 것 같아요." },
				new Opt("먼저 다가가 친구가 된다", 2, 0, 4, 0, 0, "먼저 말을 걸었다. 그 뒤로 자주 만나는 친구가 되었다."),
				new Opt("인사만 하고 거리를 둔다", 0, 0, 0, 0, 0, "인사만 나누는 사이로 지냈다.")));
		ALL.add(new Dec("NEWNEIGHBOR", ADULT, 20, 85, 1, 99, l -> true, "옆집에 이사 온 가족",
				new String[] { "옆집에 새 가족이 이사를 왔어요. 짐을 나르느라 분주해 보여요." },
				new Opt("음식을 들고 인사하러 간다", 2, 0, 3, 0, 0.003, "음식을 들고 옆집에 인사하러 갔다. 그 뒤로 반찬을 나눠 먹는 사이가 되었다."),
				new Opt("적당히 거리를 둔다", 0, 0, 0, 0, 0, "옆집과는 오가며 눈인사만 하는 사이로 지냈다.")));

		// ---- 일 (원작 mentor · rankup · jobchange · harass · adultedu · retire) ----
		ALL.add(new Dec("MENTOR", LIFE, 32, 62, 1, 99, EngineChoice::working, "후배의 고민",
				new String[] { "일터의 후배가 진로 고민이 있다며 이야기를 좀 들어 달라고 해요." },
				new Opt("시간을 내어 들어준다", 2, 0, 3, 1, 0, "후배의 이야기를 저녁 늦게까지 들어 주었다. 몇 해 뒤 그 후배가 그때가 큰 힘이 되었다고 했다."),
				new Opt("바쁘다며 다음으로 미룬다", 0, 0, -1, 0, 0, "바쁘다는 말로 후배의 부탁을 미뤘다.")));
		ALL.add(new Dec("RANKUP", LIFE, 30, 58, 2, 8, EngineChoice::working, "더 큰 자리를 맡을까?",
				new String[] { "윗사람이 팀을 이끌어 보지 않겠냐고 해요. 벌이는 늘지만 책임도 커져요." },
				new Opt("맡는다 (소득↑ · 책임↑)", 2, -1, 0, 0, 0, "더 큰 자리를 맡기로 했다.").act("RANKUP"),
				new Opt("지금 자리가 좋다", 1, 0, 0, 0, 0, "지금 자리에 남기로 했다. 저녁이 있는 삶이 더 소중했다.")));
		ALL.add(new Dec("JOBCHANGE", LIFE, 25, 55, 3, 6, EngineChoice::working, "새로운 제안",
				new String[] { "다른 곳에서 함께 일하자는 제안이 왔어요." },
				new Opt("새 일로 옮긴다", 1, 0, -1, 0, 0, "새 일로 옮겼다.").act("JOBCHANGE"),
				new Opt("지금 일을 계속한다 (경력 유지)", 0, 0, 0, 0, 0, "제안을 고맙게 거절하고 지금 일을 이어 갔다.")));
		ALL.add(new Dec("HARASS", LIFE, 18, 60, 1, 99, EngineChoice::working, "일터에서 겪는 괴롭힘",
				new String[] { "윗사람이 사람들 앞에서 모욕을 주고, 하지 않은 일까지 떠넘겨요. 몇 달째 이어지고 있어요." },
				new Opt("회사에 정식으로 알린다", 1, 0, 0, 0, 0, new String[] { "회사에 정식으로 알렸다. 조사 끝에 그 사람은 다른 부서로 옮겨 갔다.",
						"회사에 정식으로 알렸다. 조사는 흐지부지 끝났고, 한동안 일터가 더 차갑게 느껴졌다." }, 0.4, new int[] { -3, -1, 0, 0 }, null, null),
				new Opt("동료와 가족에게 털어놓는다", 1, 0, 2, 0, 0, "동료와 가족에게 털어놓았다. 혼자가 아니라는 것만으로 조금 버틸 만해졌다."),
				new Opt("그 일터를 그만둔다 (새 일자리를 찾아야 함)", 2, 1, 0, 0, 0, "그 일터를 그만두었다. 새 일자리를 찾아야 했지만 마음은 가벼웠다.").act("QUIT_JOB"),
				new Opt("혼자 참고 견딘다", -5, -3, -1, 0, 0, "혼자 참고 견뎠다. 출근길마다 가슴이 답답했다.")));
		ALL.add(new Dec("ADULTEDU", LIFE, 22, 50, 1, 99, EngineChoice::working, "다시 공부해 볼까?",
				new String[] { "일하면서 다닐 수 있는 야간 과정이 있다는 이야기를 들었어요. 1년 동안 저녁마다 다녀야 해요." },
				new Opt("야간 과정에 다닌다 (1년 · 고단함)", 1, -2, 0, 5, 0.03, "일을 마치고 저녁마다 야간 과정에 다녔다. 고단했지만 배우는 기쁨이 컸다."),
				new Opt("지금은 일에 집중한다", 0, 0, 0, 0, 0, "지금은 일에 집중하기로 했다.")));
		ALL.add(new Dec("RETIRE", LIFE, 50, 90, 1, 99, l -> true, "은퇴할까?",
				new String[] { "은퇴할 나이가 되었어요." },
				new Opt("은퇴한다 (여유↑ 소득↓)", 4, 1, 1, 0, 0, "은퇴하기로 했다.").act("RETIRE_NOW"),
				new Opt("일을 줄여 시간제로 한다 (벌이 절반 · 몸이 덜 힘듦)", 1, 0, 0, 0, 0, "일을 줄여 시간제로 몇 해 더 일하기로 했다.").act("RETIRE_LESS"),
				new Opt("계속 일한다 (소득 유지)", 0, -1, 0, 0, 0, "몇 해 더 일하기로 했다.").act("RETIRE_WORK")));

		// ---- 집·가족 (원작 buy · care · oldhome · city · kinask · lend · cosign) ----
		ALL.add(new Dec("BUYHOUSE", LIFE, 25, 60, 3, 5, l -> true, "내 집을 살까?",
				new String[] { "집값과 모은 돈을 따져 보니, 대출을 끼면 집을 살 수 있을 것 같아요." },
				new Opt("산다 (대출을 낄 수도 · 행복↑)", 0, 0, 0, 0, 0, "내 집을 마련했다.").act("BUYHOUSE"),
				new Opt("미룬다 (저축 유지)", 0, 0, 0, 0, 0, "집은 조금 더 모은 뒤에 사기로 했다.")));
		ALL.add(new Dec("CARE", LIFE, 38, 70, 1, 99, EngineChoice::parents, "부모님을 누가 돌볼까?",
				new String[] { "부모님이 부쩍 기력이 쇠하셨어요. 혼자 지내시기 어려워 보여요." },
				new Opt("내가 모시고 산다 (관계↑ · 생활비↑)", 0, -1, 6, 0, 0.05, "부모님을 집으로 모셔 함께 살기 시작했다. 저녁 식탁이 북적였다."),
				new Opt("따로 살며 자주 찾아가 돌본다", 1, -1, 4, 0, 0.02, "주말마다 부모님 댁에 들러 장을 보고 집안일을 챙겼다."),
				new Opt("돌봄 일꾼을 구한다 (비용)", 0, 0, 1, 0, 0.15, "부모님 곁에 낮 동안 돌봐 줄 사람을 구했다."),
				new Opt("형제자매에게 부탁한다 (관계↓)", 0, 0, -4, 0, 0, "형제자매에게 부모님 돌봄을 부탁했다. 한동안 서먹했다.")));
		ALL.add(new Dec("OLDHOME", LIFE, 76, 100, 1, 99, l -> true, "나이 들어 어디에서 지낼까?",
				new String[] { "혼자 살림을 꾸리기가 점점 힘에 부쳐요." },
				new Opt("자녀와 함께 산다 (관계↑ 생활비↓)", 2, 0, 4, 0, 0, "자녀네 집으로 들어가 함께 살기 시작했다. 손주들 소리에 하루가 짧았다.").when(l -> l.getChildCount() > 0),
				new Opt("요양원에 들어간다 (비용 · 돌봄)", -1, 3, -1, 0, 0.4, "요양원에 들어갔다. 끼니와 약을 챙겨 주는 사람이 있어 몸은 편했다."),
				new Opt("지금 집에서 지낸다 (이웃과 친척의 도움으로)", 1, -1, 1, 0, 0, "살던 집에서 그대로 지내기로 했다. 이웃과 친척이 자주 들여다보았다.")));
		ALL.add(new Dec("CITY", LIFE, 16, 40, 1, 99, l -> !"Y".equals(l.getIsUrban()), "도시로 떠날까?",
				new String[] { "도시에 가면 일자리가 많다는 이야기를 자주 들어요. 고향 사람들과는 멀어지겠지요." },
				new Opt("도시로 간다 (기회↑ 관계↓)", 2, 0, -3, 1, 0.03, "짐을 꾸려 도시로 떠났다.").act("CITY"),
				new Opt("고향에 남는다", 1, 0, 2, 0, 0, "고향에 남기로 했다.")));
		ALL.add(new Dec("KINASK", LIFE, 28, 80, 3, 6, l -> l.getAssets() > 0, "가족이 돈을 부탁합니다",
				new String[] { "형제자매가 급한 일이 생겼다며 돈을 좀 보태 달라고 해요." },
				new Opt("부탁한 만큼 보탠다 (관계↑)", 2, 0, 4, 0, 0.08, "부탁한 돈을 보탰다. 고맙다는 말이 오래 남았다."),
				new Opt("형편껏 일부만 보탠다", 0, 0, 1, 0, 0.024, "형편껏 일부를 보탰다."),
				new Opt("이번에는 어렵다고 말한다 (관계↓)", -1, 0, -3, 0, 0, "이번에는 어렵다고 말했다. 한동안 서먹했다.")));
		ALL.add(new Dec("LEND", LIFE, 22, 75, 2, 10, l -> l.getAssets() > 0 && l.getRelation() >= 30, "친구가 돈을 빌려 달라고 합니다",
				new String[] { "오랜 친구가 사정이 급하다며 돈을 빌려 달라고 해요. 꼭 갚겠대요." },
				new Opt("빌려준다", 0, 0, 2, 0, 0, "친구에게 돈을 빌려주었다. 꼭 갚겠다는 말을 믿기로 했다.").act("LEND"),
				new Opt("돌려받지 않아도 될 만큼만 보태 준다", 1, 0, 3, 0, 0.018, "갚지 않아도 된다며 조금 보태 주었다. 친구가 한참 고개를 숙였다."),
				new Opt("형편이 안 된다고 거절한다", 0, 0, -2, 0, 0, "사정을 말하고 거절했다. 마음이 편치 않았다.")));
		ALL.add(new Dec("COSIGN", LIFE, 28, 70, 1, 99, l -> l.getAssets() > 0, "보증을 서 달라는 부탁",
				new String[] { "가까운 사람이 대출을 받으려는데 보증을 서 달라고 해요. 그 사람이 못 갚으면 내가 갚아야 해요." },
				new Opt("보증을 서 준다 (관계↑ · 빚을 떠안을 수도)", 0, 0, 3, 0, 0, "대출 보증을 서 주었다. 고맙다는 말을 몇 번이나 들었다.").act("COSIGN"),
				new Opt("보증 대신 조금 보태 준다", 0, 0, 1, 0, 0.05, "보증은 설 수 없다고 말하고, 대신 조금 보태 주었다."),
				new Opt("보증은 설 수 없다고 말한다 (관계↓)", 0, 0, -3, 0, 0, "보증은 설 수 없다고 말했다. 한동안 서먹했다.")));

		// ---- 습관 (원작 smoke · quit · drink · alcquit · lotto · gamble · gambquit) ----
		ALL.add(new Dec("SMOKE", LIFE, 14, 35, 1, 99, l -> true, "담배 한 대 할래?",
				new String[] { "친구들이 모인 자리에서 누가 담배를 건네요." },
				new Opt("거절한다", 0, 0, 0, 0, 0, "건네받은 담배를 거절했다."),
				new Opt("피워 본다 (친구들과 어울림 · 중독될 수 있음)", 0, 0, 2, 0, 0, "친구가 건넨 담배를 피워 봤다.").act("SMOKE_TRY")));
		ALL.add(new Dec("QUITSMOKE", LIFE, 18, 90, 4, 5, l -> true, "금연할까?",
				new String[] { "계단을 오를 때마다 숨이 차요. 담뱃값도 만만치 않아요." },
				new Opt("금연에 도전한다 (성공하면 건강 회복)", 0, 0, 0, 0, 0, "금연에 도전했다.").act("QUIT_SMOKE"),
				new Opt("그냥 피운다", 0, -1, 0, 0, 0, "담배는 그냥 피우기로 했다.")));
		ALL.add(new Dec("DRINK", LIFE, 20, 75, 2, 8, l -> true, "술잔이 늘었습니다",
				new String[] { "요즘 마음이 무거워 저녁마다 술잔을 기울이게 돼요." },
				new Opt("술을 줄이고 다른 방법을 찾는다 (걷기·사람·도움)", 1, 2, 0, 0, 0, "술잔을 내려놓고 저녁마다 걷기 시작했다."),
				new Opt("술로 버틴다 (당장은 편함 · 의존될 수 있음)", 1, -1, 0, 0, 0, "한동안 술로 버텼다.").act("DRINK_GO")));
		ALL.add(new Dec("QUITDRINK", LIFE, 20, 90, 3, 4, l -> true, "술을 끊을까?",
				new String[] { "술 때문에 몸도 일도 사람도 조금씩 망가지고 있어요." },
				new Opt("도움을 받는다 (치료·자조 모임 · 비용)", 0, 0, 1, 0, 0.015, "술을 끊으려고 도움을 받았다.").act("QUIT_DRINK"),
				new Opt("아직은 괜찮다", 0, -2, -1, 0, 0, "아직은 괜찮다며 술잔을 놓지 않았다.")));
		ALL.add(new Dec("LOTTO", LIFE, 20, 80, 1, 99, l -> true, "복권을 사 볼까?",
				new String[] { "길가 복권 가게 앞에 '1등 당첨 명당'이라는 현수막이 걸려 있어요." },
				new Opt("매주 조금씩 산다 (대부분은 잃음)", 1, 0, 0, 0, 0, "매주 복권을 사기 시작했다. 추첨일 저녁이면 번호를 맞춰 보는 게 작은 낙이 되었다.").act("LOTTO_WEEKLY"),
				new Opt("이번 한 번만 산다", 1, 0, 0, 0, 0, "복권을 한 장 샀다.").act("LOTTO_ONCE"),
				new Opt("사지 않는다", 0, 0, 0, 0, 0, "복권은 사지 않았다.")));
		ALL.add(new Dec("GAMBLE", LIFE, 18, 75, 1, 99, l -> true, "한 판 해 볼까?",
				new String[] { "아는 사람이 돈을 걸고 하는 판에 끼어 보라고 해요." },
				new Opt("하지 않는다", 0, 0, 0, 0, 0, "돈을 거는 판에는 끼지 않았다."),
				new Opt("한번 해 본다 (딸 수도, 잃을 수도 · 빠져들 수 있음)", 0, 0, 0, 0, 0, "한 판 끼어 보았다.").act("GAMBLE")));
		ALL.add(new Dec("QUITGAMBLE", LIFE, 18, 90, 3, 3, l -> true, "도박을 끊을까?",
				new String[] { "잃은 돈을 되찾으려다 빚이 늘고 있어요. 가족은 아직 모르고 있어요." },
				new Opt("가족에게 털어놓고 상담을 받는다", -1, 0, 1, 0, 0.01, "가족에게 털어놓고 상담을 받았다.").act("QUIT_GAMBLE"),
				new Opt("이번에 따면 그만둔다", -1, 0, -2, 0, 0, "이번에 따면 그만두겠다며 다시 판에 앉았다.")));
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
