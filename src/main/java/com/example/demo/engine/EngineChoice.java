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
	public static final String SCHOOL = "SCHOOL"; // 무작위로 뽑지 않음 (schoolStep 이 붙었을 때만 띄움)

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
			return new Opt(label, h, hl, re, kn, cost, results, badP, badEff, temper, p);
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
				new Opt("집에 가서 집안일을 돕는다", 0, 0, 2, 0, 0, "집에 가서 빨래를 개고 집안일을 도왔다. 부모님이 고맙다고 하셨다.")));
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

		// ---------- 어른 (원작 SMALL) ----------
		// 휴대폰이 있는 사람만 (돈 쓰기로 산 휴대폰, 또는 인생 기록에 '휴대전화·스마트폰이 생겼다'가 나온 사람)
		ALL.add(new Dec("PHONE", ADULT, 13, 60, 1, 99, l -> year(l) >= 2012 && hasPhone(l), "잠들기 전 휴대폰",
				new String[] { "누워서 휴대폰을 보다 보면 어느새 새벽이에요." },
				new Opt("휴대폰을 멀리 두고 일찍 잔다", 0, 2, 0, 1, 0, "휴대폰을 침대 밖에 두고 잤다. 아침이 한결 가벼웠다."),
				new Opt("계속 본다, 이게 쉬는 시간이다", 1, -1, 0, 0, 0, "오늘도 영상 몇 개만 더 보다가 늦게 잠들었다.")));
		ALL.add(new Dec("WEEKEND", ADULT, 18, 75, 2, 8, l -> true, "모처럼 쉬는 주말",
				new String[] { "아무 약속도 없는 주말이에요. 무엇을 할까요?", "늦잠을 자고 일어나니 창밖이 맑아요." },
				new Opt("오랜만에 친구들을 만난다", 2, 0, 3, 0, 0.01, "친구들과 늦게까지 이야기를 나눴다."),
				new Opt("집에서 푹 쉰다", 1, 2, 0, 0, 0, "늦잠을 자고 아무것도 하지 않았다. 그것도 좋았다."),
				new Opt("가족과 나들이를 간다", 2, 0, 3, 0, 0.01, "가족과 도시락을 싸 들고 가까운 공원에 다녀왔다.").when(EngineChoice::family),
				new Opt("혼자 가 보고 싶던 곳에 간다", 3, 0, 0, 0, 0.01, "혼자 기차를 타고 가 보고 싶던 동네를 걸었다.").when(l -> !family(l))));
		ALL.add(new Dec("HOLIDAY", ADULT, 22, 72, 2, 10, EngineChoice::working, "휴가를 어디서 보낼까?",
				new String[] { "며칠 쉴 수 있게 되었어요." },
				new Opt("부모님 댁에 다녀온다", 2, 0, 4, 0, 0, "부모님 댁에서 며칠을 보냈다. 어릴 적 방이 그대로였다.").when(EngineChoice::parents),
				new Opt("가까운 바다나 산으로 떠난다", 4, 1, 0, 0, 0.03, "바다를 보며 오랜만에 숨을 돌렸다."),
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
		ALL.add(new Dec("COOK", ADULT, 18, 35, 1, 99, l -> !married(l), "혼자 사는 저녁",
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
				new String[] { "초등학교를 마쳤어요. 중학교에 갈 수 있게 됐는데, 집안일을 도울 손도 필요해요." },
				new Opt("중학교에 간다", 1, 0, 0, 2, 0, "중학교에 진학했다. 새 교과서 냄새가 좋았다.").act("ENROLL"),
				new Opt("학교를 그만두고 집안일을 돕는다", -2, 0, 2, -1, 0, "학교 대신 집안일을 돕기로 했다. 교복 입은 친구들이 지나갈 때마다 눈길이 갔다.").act("QUIT"));
		m.failText = "집안 형편이 어려워 중학교에 가지 못했다.";
		ALL.add(m);
		Dec h = new Dec("SCHOOL_HIGH", SCHOOL, 0, 99, 1, 99, l -> true, "고등학교에 갈까?",
				new String[] { "중학교를 마쳤어요. 고등학교에 갈 수 있지만, 이제 돈을 벌 수 있는 나이이기도 해요." },
				new Opt("고등학교에 간다", 1, 0, 0, 2, 0, "고등학교에 진학했다. 공부가 한결 어려워졌다.").act("ENROLL"),
				new Opt("학교를 그만두고 일을 시작한다", -1, 0, 1, 0, 0, "고등학교 대신 일을 시작하기로 했다. 첫 월급날이 기다려졌다.").act("QUIT"));
		h.failText = "집안 형편이 어려워 고등학교에 가지 못했다.";
		ALL.add(h);
		Dec u = new Dec("SCHOOL_UNI", SCHOOL, 0, 99, 1, 99, l -> true, "대학에 갈까?",
				new String[] { "대학에 붙었어요. 몇 년 더 공부하면 더 좋은 일자리를 얻을 수 있지만, 그동안은 벌이가 없어요." },
				new Opt("대학에 간다", 2, 0, 0, 2, 0, "대학에 진학했다. 강의실이 생각보다 넓었다.").act("ENROLL"),
				new Opt("대학 대신 바로 일을 시작한다", 0, 0, 1, 0, 0, "대학 대신 바로 일을 시작하기로 했다.").act("QUIT"));
		ALL.add(u);
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
		return -(decIdx * 10 + optIdx + 1);
	}

	public static int decIdxOf(int choiceId) {
		return (-choiceId - 1) / 10;
	}

	public static int optIdxOf(int choiceId) {
		return (-choiceId - 1) % 10;
	}
}
