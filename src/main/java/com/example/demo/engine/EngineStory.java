package com.example.demo.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

// C03 인생 기록 보강: 해마다 그 사람의 처지(나이·학교·일·가족·형편·능력치·집중·나라·시대)에 맞는 일상 몇 줄
// 원작(1in8b)처럼 큰 사건이 없는 해에도 "그해에 무엇을 하며 지냈는지"가 기록에 남게 함
// - 원작 200명 시뮬 기준: 한 해 평균 4~5줄 (0~5세 3.6줄, 6~12세 4.6줄, 13~18세 5.5줄, 어른 5줄, 노년 4줄)
//   → 이벤트(C09·C10, 장윤아님)·진학 기록과 합쳐서 비슷한 양이 되게 일상 줄 수를 정함
// - 문장 대부분은 능력치·돈에 영향 없음. 경조사(결혼식 축의금·부조금·돌잔치 등)만 돈이 나감 (cost)
//   cost = 그 나라 중위 소득 대비 비율, 실제 금액·차감은 EngineService.storyLines 가 계산
//   (원작: 친구 결혼식 = 중위 소득 × 0.8% × (내 소득/중위)^0.3, 벌이가 없거나 빚이 있으면 음식 준비만 거듦)
// - 결혼·출산·사망·이민·실직 같은 사건 문장은 이벤트와 겹치지 않게 넣지 않음
// - 시대 문장(TV·전화·컴퓨터·인터넷·스마트폰)은 그 물건이 흔해진 해부터, 세계 소식은 그해에 한 번
final class EngineStory {

	private EngineStory() {
	}

	// 고른 문장 한 줄 (cost > 0 이면 경조사 등으로 돈이 나감)
	static final class Story {
		final String text;
		final double cost;
		final String id; // 문장 틀 번호 (살림 내역 이름 고르기: cer_wedding → 축의금 …)

		Story(String text, double cost) {
			this(text, cost, null);
		}

		Story(String text, double cost, String id) {
			this.text = text;
			this.cost = cost;
			this.id = id;
		}
	}

	// 한 줄짜리 일상 문장 틀
	private static final class Line {
		final String id;
		final int minAge;
		final int maxAge;
		final int weight;
		final Predicate<LifeDTO> when;
		final String text;
		final double cost;  // 중위 소득 대비 지출 (0이면 돈 영향 없음)
		final int fromYear; // 이 해부터 (0이면 제한 없음)

		Line(String id, int minAge, int maxAge, int weight, Predicate<LifeDTO> when, String text) {
			this(id, minAge, maxAge, weight, when, text, 0, 0);
		}

		Line(String id, int minAge, int maxAge, int weight, Predicate<LifeDTO> when, String text, double cost, int fromYear) {
			this.id = id;
			this.minAge = minAge;
			this.maxAge = maxAge;
			this.weight = weight;
			this.when = when;
			this.text = text;
			this.cost = cost;
			this.fromYear = fromYear;
		}
	}

	// 조건 줄임말
	private static boolean enrolled(LifeDTO l) { return "ENROLLED".equals(l.getSchoolStatus()); }
	private static boolean working(LifeDTO l) { return "EMPLOYED".equals(l.getJobStatus()); }
	// 회사에 다니는 사람 (가게 사장 JOB_OWNER 는 빼고: 윗사람·직장 동료가 없음)
	private static boolean employee(LifeDTO l) { return working(l) && !"JOB_OWNER".equals(l.getJobId()); }
	private static boolean jobless(LifeDTO l) { return "UNEMPLOYED".equals(l.getJobStatus()); }
	private static boolean married(LifeDTO l) { return "Y".equals(l.getMarriedYn()); }
	private static boolean parents(LifeDTO l) { return !"N".equals(l.getParentAlive()); }
	private static boolean rural(LifeDTO l) { return !"Y".equals(l.getIsUrban()); }
	private static boolean poor(LifeDTO l) { return l.getWealthClass() <= 2; }
	private static boolean rich(LifeDTO l) { return l.getWealthClass() >= 4; }
	private static boolean focus(LifeDTO l, String f) { return f.equals(l.getFocusCode()); }
	private static boolean hasMoney(LifeDTO l) { return l.getAssets() > 0 || l.getIncome() > 0; }
	private static boolean owns(LifeDTO l, String id) { return l.getOwns() != null && l.getOwns().contains(id); }
	// 지금 연도 (나이 기준 대략)
	private static int yr(LifeDTO l) { return l.getBirthYear() + l.getCurrentAge(); }
	// 아직 가난한 나라·시대 (1인당 GDP 5천 달러 아래, GDP를 모르면 1980년 전)
	private static boolean lowGdp(LifeDTO l) { return l.getGdpPerCapita() != null ? l.getGdpPerCapita() < 5000 : yr(l) < 1980; }
	// 1인당 GDP (달러, 모르면 시대로 어림)
	private static double gdp(LifeDTO l) { return l.getGdpPerCapita() != null ? l.getGdpPerCapita() : yr(l) < 1980 ? 1000 : 8000; }
	private static final Set<String> CRAM_CC = Set.of("KOR", "CHN", "JPN", "TWN", "HKG", "SGP", "VNM");
	// 병역 중 (SVC-ON 토큰)
	private static boolean serving(LifeDTO l) { return owns(l, "SVC-ON"); }
	// 농사짓는 집: 농사·어업 일을 하거나, 시골에서 일하지 않는 사람(아이·노인)이 가난한 나라·시대에 사는 경우
	private static boolean farmer(LifeDTO l) {
		if (!rural(l)) return false;
		if (working(l)) {
			String n = l.getJobName();
			return n != null && (n.contains("농") || n.contains("어부") || n.contains("어업") || n.contains("목축") || n.contains("과수"));
		}
		return lowGdp(l) && !rich(l);
	}

	// 이 인생에 이 줄(시대 물건 등)이 이미 나왔는지 (엔진 선택지: 휴대폰이 생긴 사람만 '잠들기 전 휴대폰')
	public static boolean had(int lifeId, String id) {
		Set<String> s = ONCE.get(lifeId);
		return s != null && s.contains(id);
	}
	private static boolean level(LifeDTO l, String lv) { return enrolled(l) && lv.equals(l.getEduLevel()); }
	// 학교 밖 아이: 막 졸업한 해(초등 졸업 무렵)는 빼기 - "졸업했다"와 "또래를 부러워했다"가 같은 해에 나오지 않게
	private static boolean outOfSchool(LifeDTO l) { return !enrolled(l) && !("GRADUATED".equals(l.getSchoolStatus()) && l.getCurrentAge() <= 14); }

	private static final Predicate<LifeDTO> ANY = l -> true;

	// {명절}: 나라 명절, {나이}: 나이
	private static final List<Line> LINES = List.of(
			// ── 아기 (0~2세) ──
			new Line("baby_vacc", 0, 0, 3, l -> l.getWealthClass() >= 2 || !rural(l), "보건소에서 기본 예방접종을 맞았다."),
			new Line("baby_novacc", 0, 1, 2, l -> poor(l) && rural(l), "보건소가 너무 멀어 예방접종을 제때 받지 못했다."),
			new Line("baby_cry", 0, 1, 2, ANY, "밤마다 울어 식구들이 번갈아 안고 재웠다."),
			new Line("baby_smile", 0, 0, 3, ANY, "처음으로 사람을 보고 방긋 웃었다. 식구들이 서로 먼저 안아 보겠다고 했다."),
			new Line("baby_tooth", 0, 1, 2, ANY, "첫 이가 났다. 무엇이든 입으로 가져갔다."),
			new Line("baby_word", 1, 1, 3, ANY, "첫 단어를 말했다. ‘엄마’라는 뜻의 말이었다."),
			new Line("baby_walk", 1, 1, 3, ANY, "벽을 짚고 일어서더니 첫걸음을 뗐다. 온 식구가 박수를 쳤다."),
			new Line("baby_bday1", 1, 1, 3, ANY, "첫 생일을 맞아 친척들이 모여 축하해 주었다."),
			new Line("baby_hold", 0, 2, 2, l -> rural(l) && !rich(l) && lowGdp(l), "어른들이 밭일을 하는 동안 할머니 등에 업혀 지냈다."),
			new Line("baby_story", 1, 3, 2, ANY, "잠들기 전마다 부모님이 옛이야기를 들려주셨다."),
			new Line("baby_play", 2, 2, 2, ANY, "흙 한 줌, 나뭇가지 하나로 한나절을 놀았다."),
			new Line("baby_mud", 2, 4, 2, ANY, "흙장난을 하다 옷을 다 버렸다."),
			new Line("baby_climb", 2, 4, 2, ANY, "높은 곳만 보면 기어 올라가 어른들이 가슴을 쓸어내렸다."),
			new Line("baby_why", 2, 4, 2, ANY, "눈에 보이는 것마다 ‘왜?’라고 물었다."),
			new Line("baby_sick", 1, 3, 2, l -> l.getHealth() < 70, "열이 올라 며칠을 앓았다. 어머니가 밤새 곁을 지켰다."),
			new Line("baby_diarrhea", 0, 4, 2, l -> poor(l) && l.getHealth() < 75, "설사병으로 며칠을 앓았다. 끓인 물과 소금물로 겨우 회복했다."),

			// ── 어린 시절 (3~5세) ──
			new Line("kid_holiday", 3, 12, 3, ANY, "{명절}에 온 식구가 모여 음식을 나눠 먹었다."),
			new Line("kid_outside", 3, 9, 3, ANY, "동네 아이들과 해가 질 때까지 뛰어놀았다."),
			new Line("kid_book", 3, 6, 2, l -> l.getWealthClass() >= 3, "그림책 한 권을 닳도록 넘겨 보았다."),
			new Line("kid_stones", 3, 7, 2, l -> poor(l), "장난감이 없어 돌멩이와 빈 병으로 놀이를 만들어 냈다."),
			new Line("kid_snack", 3, 7, 2, ANY, "시장에 따라갔다가 군것질거리를 하나 얻어먹었다."),
			new Line("kid_fall", 3, 9, 2, ANY, "뛰다가 넘어져 무릎이 까졌다. 다음 날 또 뛰어다녔다."),
			new Line("kid_bday5", 5, 5, 3, ANY, "다섯 번째 생일을 맞았다."),
			new Line("kid_dream", 5, 8, 2, ANY, "커서 무엇이 되고 싶냐는 물음에 날마다 다른 대답을 했다."),
			// 유치원 (원작 kinder): 잘사는 나라거나, 집이 넉넉하면 조금 가난한 나라에서도
			new Line("kid_kinder", 3, 5, 3, l -> gdp(l) > 4000 || l.getWealthClass() >= 4 && gdp(l) > 1300 || l.getWealthClass() >= 5 && gdp(l) > 800, "유치원에 다니며 처음으로 단체 생활을 배웠다."),
			// ── 집안 형편 (원작 fam_room · fam_trip · fam_maid · fam_room1 · fam_clothes · hungry · kid_academy) ──
			new Line("fam_maid", 1, 6, 2, l -> l.getWealthClass() >= 5 && gdp(l) < 25000, "집에 살림과 아이 돌보기를 도와주는 사람이 있었다."),
			new Line("fam_room", 6, 11, 2, l -> rich(l) && (gdp(l) > 3000 || l.getWealthClass() >= 5), "처음으로 내 방이 생겼다."),
			new Line("fam_trip", 5, 14, 2, l -> rich(l) && (gdp(l) > 4000 || l.getWealthClass() >= 5), "방학에 가족과 함께 여행을 다녀왔다."),
			new Line("fam_room1", 2, 12, 2, l -> poor(l) && !rural(l) && gdp(l) < 12000, "온 가족이 방 한 칸에서 함께 먹고 잤다."),
			new Line("fam_clothes", 4, 12, 2, l -> poor(l) && gdp(l) < 6000, "새 옷은 명절에나 한 벌 생겼다."),
			new Line("fam_hungry", 0, 17, 2, l -> poor(l) && gdp(l) < 1800, "흉작으로 식구들이 하루 한 끼로 버텼다."),
			new Line("kid_academy", 9, 13, 3, l -> enrolled(l) && CRAM_CC.contains(l.getCurrentCountry()) && l.getWealthClass() >= 2 && gdp(l) > 2500,
					"학교가 끝나면 학원 두 곳을 돌고, 깜깜해져서야 집에 돌아왔다."),
			new Line("kid_grandpa", 3, 8, 2, ANY, "할아버지가 몰래 사탕을 쥐여 주셨다."),
			new Line("kid_draw", 3, 7, 2, ANY, "크레파스를 쥐기만 하면 시간 가는 줄 몰랐다. 벽에도 그림을 그려 혼이 났다."),
			new Line("kid_animal", 3, 8, 2, l -> rural(l), "마당의 닭과 개를 쫓아다니며 놀았다."),
			new Line("kid_dark", 3, 6, 2, ANY, "밤에 혼자 화장실 가는 것이 무서워 형제를 깨웠다."),
			new Line("kid_sibfight", 3, 12, 2, ANY, "형제자매와 장난감을 두고 다퉜다가 금세 다시 같이 놀았다."),
			new Line("kid_help", 4, 9, 2, l -> (poor(l) || rural(l)) && lowGdp(l), "어른들을 따라 물을 길어 오는 일을 거들었다."),
			new Line("kid_rain", 3, 9, 2, ANY, "비 오는 날 웅덩이마다 뛰어들어 신발이 젖었다."),

			// ── 학교 다니는 아이 (6~14세) ──
			new Line("sch_pass", 6, 18, 3, l -> enrolled(l) && l.getKnowledge() >= 35 && l.getKnowledge() < 65, "평범한 성적으로 한 학년을 마쳤다."),
			new Line("sch_good", 6, 18, 3, l -> enrolled(l) && l.getKnowledge() >= 65, "좋은 성적으로 한 학년을 마쳤다. 선생님께 칭찬을 들었다."),
			new Line("sch_hard", 6, 18, 3, l -> enrolled(l) && l.getKnowledge() < 35, "수업을 따라가기가 벅찼다. 숙제를 붙들고 늦게까지 앉아 있었다."),
			new Line("sch_friend", 6, 16, 2, l -> l.getRelation() >= 55, "마음이 잘 맞는 단짝 친구가 생겼다."),
			new Line("sch_fight", 6, 16, 2, ANY, "친구와 크게 다퉜다가 며칠 만에 화해했다."),
			new Line("sch_lonely", 6, 16, 2, l -> l.getRelation() < 40, "반 아이들 사이에 끼지 못해 쉬는 시간이 길게 느껴졌다."),
			new Line("sch_teacher", 7, 17, 2, l -> enrolled(l), "좋은 선생님을 만나 공부에 재미를 붙였다."),
			new Line("sch_chores", 6, 14, 2, l -> enrolled(l) && (focus(l, "HOUSE") || poor(l)), "학교가 끝나면 집으로 달려가 집안일을 거들었다."),
			new Line("sch_sport", 6, 18, 3, l -> focus(l, "SPORT"), "운동장에서 공을 차며 하루를 보냈다. 운동회에서 반 대표로 뛰었다."),
			new Line("sch_hobby", 6, 18, 3, l -> focus(l, "HOBBY"), "좋아하는 일에 푹 빠져 시간 가는 줄 몰랐다."),
			new Line("sch_study", 6, 18, 3, l -> focus(l, "STUDY") && enrolled(l), "책상 앞에 앉아 있는 시간이 부쩍 늘었다."),
			new Line("sch_play", 6, 16, 3, l -> focus(l, "PLAY"), "친구들과 몰려다니며 온 동네를 놀이터로 삼았다."),
			new Line("sch_walk", 6, 14, 2, l -> rural(l) && enrolled(l) && (lowGdp(l) || poor(l)), "먼 길을 걸어 학교에 다녔다. 비 오는 날이면 신발이 다 젖었다."),
			new Line("sch_sportsday", 7, 12, 3, l -> enrolled(l), "운동회 날 이어달리기에서 온 힘을 다해 달렸다."),
			new Line("sch_trip", 9, 17, 2, l -> enrolled(l) && !poor(l), "수학여행을 떠나 처음으로 친구들과 낯선 도시에서 밤을 보냈다."),
			new Line("sch_notrip", 9, 17, 2, l -> enrolled(l) && poor(l), "수학여행비를 낼 형편이 안 돼 혼자 학교에 남았다."),
			new Line("sch_homework", 7, 13, 2, l -> enrolled(l), "방학 숙제를 개학 전날 몰아서 했다."),
			new Line("sch_class_role", 8, 14, 2, l -> enrolled(l) && l.getRelation() >= 50, "반장 선거에 나가 친구들 앞에서 떨리는 목소리로 연설을 했다."),
			new Line("sch_lunch", 6, 12, 2, l -> enrolled(l) && poor(l), "도시락 반찬이 늘 같아 친구들 반찬을 부러워했다."),
			new Line("sch_library", 8, 16, 2, l -> enrolled(l) && l.getKnowledge() >= 55, "학교 도서관에서 빌린 책을 밤늦도록 읽었다."),
			new Line("sch_bully", 8, 15, 1, l -> enrolled(l) && l.getRelation() < 45, "괴롭히는 아이들 때문에 학교 가는 길이 무거웠다."),
			new Line("sch_cram", 10, 18, 2, l -> enrolled(l) && rich(l) && !rural(l), "학교가 끝나면 학원으로 향했다. 집에 오면 밤이었다."),
			new Line("sch_award", 8, 17, 1, l -> enrolled(l) && l.getKnowledge() >= 75, "글짓기 대회에서 상을 받아 집에 상장을 들고 갔다."),
			new Line("sch_vacation", 7, 15, 2, l -> rural(l) && enrolled(l), "방학이면 냇가에서 물고기를 잡으며 하루를 보냈다."),
			new Line("sch_cousins", 7, 15, 2, l -> enrolled(l), "방학에 사촌들이 놀러 와 며칠 내내 붙어 지냈다."),
			new Line("noschool_work", 6, 14, 3, l -> outOfSchool(l) && farmer(l), "학교 대신 밭에 나가 어른들 일을 거들었다."),
			new Line("noschool_care", 6, 14, 3, l -> outOfSchool(l), "어린 동생들을 돌보며 하루를 보냈다."),
			new Line("noschool_envy", 7, 14, 2, l -> outOfSchool(l), "책가방을 메고 가는 또래들을 오래 바라보았다."),
			new Line("noschool_street", 9, 15, 2, l -> !enrolled(l) && !rural(l) && poor(l), "시장 골목에서 물건을 날라 주고 몇 푼을 벌었다."),

			// ── 십대 (12~19세) ──
			new Line("teen_grow", 12, 15, 2, ANY, "키가 부쩍 자라 지난해 옷이 맞지 않았다."),
			new Line("teen_voice", 12, 15, 2, l -> "M".equals(l.getGender()), "목소리가 갈라지기 시작했다. 친구들이 놀렸다."),
			new Line("teen_crush", 13, 19, 2, ANY, "동네 친구에게 설렘을 느꼈다. 아무에게도 말하지 않았다."),
			new Line("teen_letter", 14, 19, 1, ANY, "밤새 고쳐 쓴 편지를 끝내 전하지 못했다."),
			new Line("teen_parent", 13, 19, 2, l -> parents(l), "진로 문제로 부모님과 말다툼을 했다."),
			new Line("teen_door", 13, 17, 2, l -> parents(l), "방문을 닫고 혼자 있는 시간이 늘었다. 부모님은 서운해하셨다."),
			new Line("teen_exam", 15, 19, 3, l -> level(l, "HIGH"), "시험을 앞두고 밤늦게까지 문제집을 풀었다."),
			new Line("teen_entrance", 17, 19, 3, l -> level(l, "HIGH") && l.getKnowledge() >= 45, "입시를 앞두고 1년 내내 책상 앞을 떠나지 않았다."),
			new Line("teen_future", 15, 19, 2, l -> enrolled(l), "앞으로 무엇을 하며 살지 처음으로 진지하게 고민했다."),
			new Line("teen_job", 14, 19, 2, l -> poor(l) && enrolled(l), "방학 동안 일을 해서 처음으로 돈을 벌었다."),
			new Line("teen_music", 13, 19, 2, ANY, "좋아하는 노래를 하루에도 몇 번씩 되풀이해 들었다."),
			new Line("teen_band", 14, 19, 1, l -> focus(l, "HOBBY"), "친구들과 밴드를 만들어 창고에서 연습했다."),
			new Line("teen_star", 13, 18, 2, ANY, "좋아하는 가수의 사진을 책상 앞에 붙여 두었다."),
			new Line("teen_secret", 13, 18, 2, l -> l.getRelation() >= 50, "친구들과 밤새 비밀 이야기를 나눴다."),
			new Line("teen_mirror", 13, 17, 2, ANY, "거울 앞에 서 있는 시간이 부쩍 길어졌다."),
			new Line("teen_sports", 13, 19, 2, l -> focus(l, "SPORT") && enrolled(l), "학교 대표로 경기에 나가 땀을 흘렸다."),
			new Line("teen_chores", 12, 18, 2, l -> rural(l) && outOfSchool(l) && lowGdp(l), "어른 몫의 일을 맡게 되었다. 손이 거칠어졌다."),

			// ── 시대 물건 (그 물건이 흔해진 해부터) ──
			new Line("era_radio", 5, 60, 2, l -> l.getWealthClass() >= 3 && yr(l) < 1975, "집에 라디오가 생겼다. 저녁마다 식구들이 모여 들었다.", 0, 1950),
			new Line("era_tv", 5, 70, 2, l -> l.getWealthClass() >= 3 && !rural(l) && yr(l) < 1992, "집에 텔레비전이 생겼다. 동네 아이들이 구경하러 몰려왔다.", 0, 1970),
			new Line("era_phone", 15, 80, 2, l -> !rural(l) && l.getWealthClass() >= 3 && yr(l) < 1998, "집에 전화가 놓였다. 벨이 울리면 서로 받겠다고 뛰어갔다.", 0, 1980),
			new Line("era_pc", 10, 60, 2, l -> l.getWealthClass() >= 3 && !rural(l) && yr(l) < 2012, "내 컴퓨터가 생겼다. 밤늦도록 화면 앞을 떠나지 않았다.", 0, 1995),
			new Line("era_net", 10, 70, 2, ANY, "처음으로 인터넷에 접속했다. 세상이 넓어졌다.", 0, 2002),
			// 휴대전화는 스마트폰이 흔해지기 전(2013년 전)까지만, 돈 쓰기로 이미 산 사람은 빼고
			new Line("era_mobile", 13, 75, 2, l -> l.getBirthYear() + l.getCurrentAge() < 2013 && !owns(l, "CELL") && !owns(l, "PHONE"), "처음으로 휴대전화가 생겼다.", 0, 2005),
			// 첫 스마트폰은 이야기 한 줄이 아니라 EngineService.phoneYear 에서 흔해진 해에 바로 장만 (원작식)
			new Line("era_video", 20, 90, 2, ANY, "멀리 사는 가족과 영상 통화로 얼굴을 보며 이야기했다.", 0, 2016),

			// ── 대학생 ──
			new Line("uni_life", 18, 30, 3, l -> level(l, "UNI") || level(l, "GRAD"), "강의와 과제에 쫓기면서도 새 친구들과 어울렸다."),
			new Line("uni_club", 18, 26, 2, l -> level(l, "UNI"), "동아리에 들어가 선후배들과 밤늦게까지 어울렸다."),
			new Line("uni_part", 18, 30, 3, l -> focus(l, "PARTTIME"), "아르바이트를 마치고 늦은 밤에 집으로 돌아왔다."),
			new Line("uni_prep", 18, 30, 3, l -> focus(l, "JOB_PREP"), "원서와 자기소개서를 몇 번이고 고쳐 썼다."),

			// ── 일하는 어른 ──
			new Line("work_used", 16, 70, 3, l -> working(l) && l.getJobYears() <= 3, "일에 익숙해져 손이 빨라졌다."),
			new Line("work_tired", 16, 70, 2, l -> working(l) && l.getHealth() < 55, "일이 고되어 몸살을 앓고 며칠을 쉬었다."),
			new Line("work_praise", 16, 70, 3, l -> employee(l) && focus(l, "WORK"), "일을 잘 해내 윗사람에게 칭찬을 들었다."),
			new Line("work_balance", 16, 70, 2, l -> working(l) && focus(l, "BALANCE"), "일은 일대로, 저녁은 저녁대로 챙기며 한 해를 보냈다."),
			new Line("work_parents", 18, 55, 2, l -> working(l) && parents(l), "월급날 부모님께 용돈을 드렸다.", 0.01, 0),
			new Line("work_commute", 18, 64, 2, l -> working(l) && !rural(l), "출퇴근길 버스와 지하철은 늘 붐볐다."),
			new Line("work_farm", 16, 70, 2, l -> working(l) && farmer(l), "날씨를 살피며 한 해 농사를 지었다."),
			new Line("work_harvest", 16, 75, 2, l -> farmer(l), "올해는 풍년이었다. 창고가 그득해 마음이 든든했다."),
			new Line("work_drought", 16, 75, 2, l -> farmer(l), "비가 너무 적어 수확이 줄었다."),
			new Line("work_learn", 18, 64, 3, l -> focus(l, "SELF_DEV"), "퇴근 뒤에 짬을 내어 새로운 것을 배웠다."),
			new Line("work_overtime", 22, 60, 2, l -> working(l) && focus(l, "WORK"), "야근이 이어져 집에는 잠만 자러 들어갔다."),
			new Line("work_dinner", 22, 60, 2, l -> employee(l) && !rural(l), "회식 자리에서 동료들과 늦게까지 이야기를 나눴다."),
			new Line("jobless_search", 16, 64, 3, l -> jobless(l) && !serving(l), "일자리를 찾아 여기저기 다녔지만 쉽지 않았다."),
			new Line("jobless_day", 16, 64, 2, l -> jobless(l) && poor(l) && !serving(l), "그날그날 일거리를 얻어 하루를 버텼다."),

			// ── 경조사 (돈이 나감) ──
			new Line("cer_wedding", 22, 45, 3, l -> hasMoney(l), "친구의 결혼식에 다녀왔다. 축의금을 냈다.", 0.008, 0),
			new Line("cer_wedding2", 25, 65, 2, l -> hasMoney(l) && employee(l), "직장 동료의 결혼식에 다녀왔다.", 0.006, 0),
			new Line("cer_wedding0", 18, 45, 2, l -> !hasMoney(l), "친구의 결혼식에 가서 음식 준비를 거들었다."),
			new Line("cer_funeral", 30, 90, 2, l -> hasMoney(l), "친척 장례에 다녀와 부조금을 냈다.", 0.006, 0),
			new Line("cer_baby", 25, 45, 2, l -> hasMoney(l), "친구 아이의 돌잔치에 다녀왔다.", 0.004, 0),
			new Line("cer_holiday", 25, 70, 2, l -> hasMoney(l) && parents(l), "{명절}에 부모님 댁에 선물과 용돈을 챙겨 갔다.", 0.008, 0),
			new Line("cer_reunion", 30, 80, 2, l -> hasMoney(l), "동창 모임에 나가 회비를 내고 옛이야기로 밤을 새웠다.", 0.003, 0),
			new Line("cer_kidschool", 30, 55, 2, l -> l.getChildCount() > 0 && hasMoney(l), "아이 학원비와 책값이 꽤 들었다.", 0.02, 0),
			// 원작 살림 지출 (원작 500명 기록의 중간값을 그 나라 중위 소득 비율로 맞춤: 치과 −7만, 가전 −37만, 배관 −61만 원 …)
			new Line("exp_dentist", 25, 80, 2, l -> hasMoney(l), "이가 아파 치과에 다녀왔다.", 0.01, 0),
			new Line("exp_appliance", 25, 85, 2, l -> hasMoney(l) && owns(l, "TV"), "냉장고가 고장 나 고치는 데 돈이 들었다.", 0.05, 1965),
			new Line("exp_tv", 25, 85, 1, l -> hasMoney(l) && owns(l, "TV"), "텔레비전이 고장 나 새로 들였다.", 0.05, 1970),
			new Line("exp_pipe", 28, 85, 1, l -> hasMoney(l) && !rural(l), "집 배관이 터져 수리를 맡겼다.", 0.08, 0),
			new Line("exp_roof", 28, 85, 1, l -> hasMoney(l) && rural(l), "비가 새서 지붕을 고쳤다.", 0.06, 0),
			new Line("exp_car", 30, 75, 1, l -> hasMoney(l) && owns(l, "CAR"), "자동차가 고장 나 정비소에 맡겼다.", 0.12, 1980),
			new Line("exp_trip", 28, 70, 2, l -> hasMoney(l) && married(l), "형제 가족과 함께 여행을 다녀왔다.", 0.025, 1975),
			new Line("exp_parent_hosp", 35, 70, 2, l -> hasMoney(l) && parents(l), "부모님 병원비를 형제들과 나눠 냈다.", 0.04, 0),
			new Line("exp_glasses", 45, 80, 1, l -> hasMoney(l), "글씨가 흐려 돋보기를 맞췄다.", 0.002, 0),
			new Line("exp_cane", 72, 99, 1, l -> hasMoney(l), "무릎이 시려 지팡이를 하나 샀다.", 0.03, 0),

			// ── 가족 ──
			new Line("fam_walk", 20, 90, 2, l -> married(l), "배우자와 저녁을 먹으며 하루 있었던 일을 이야기했다."),
			new Line("fam_quarrel", 20, 80, 2, l -> married(l), "부부싸움을 했지만 다음 날 먼저 말을 걸었다."),
			new Line("fam_kid_school", 33, 60, 3, l -> l.getChildCount() > 0, "아이의 학교 행사에 다녀왔다."),
			new Line("fam_kid_sick", 22, 55, 2, l -> l.getChildCount() > 0, "아이가 아파 밤을 새웠다."),
			new Line("fam_kid_laugh", 22, 55, 2, l -> l.getChildCount() > 0, "아이가 처음 해낸 일을 온 식구가 함께 기뻐했다."),
			new Line("fam_focus", 16, 90, 3, l -> focus(l, "FAMILY"), "가족과 친구들 곁에서 보내는 시간을 늘렸다."),
			new Line("fam_oldparents", 35, 65, 2, l -> parents(l), "부모님이 부쩍 늙으신 것 같아 마음이 쓰였다."),
			new Line("fam_holiday", 13, 99, 3, ANY, "{명절}에 식구들이 모여 오랜만에 이야기꽃을 피웠다."),
			new Line("fam_friend", 20, 99, 2, ANY, "한동안 연락이 뜸했던 오랜 친구와 다시 만나 서로 사는 이야기를 나눴다."),
			new Line("fam_sibling", 20, 80, 2, ANY, "형제자매와 크게 다퉜다가 몇 달 만에 화해했다."),
			new Line("fam_neighbor", 25, 95, 3, l -> l.getRelation() >= 45, "형편이 어려운 이웃에게 음식을 나눴다."),
			new Line("fam_neighbor2", 25, 95, 2, l -> l.getRelation() >= 45, "혼자 사는 이웃 어르신을 자주 들여다보았다."),
			new Line("fam_babysit", 25, 75, 2, l -> l.getRelation() >= 50, "갑자기 일이 생긴 이웃의 아이를 하루 맡아 주었다."),
			new Line("fam_party", 16, 70, 2, l -> rural(l), "동네 잔치에서 밤늦도록 노래하고 춤췄다."),

			// ── 건강·쉼 ──
			new Line("hl_check", 30, 90, 2, l -> !rural(l) && l.getWealthClass() >= 3, "정기 건강검진을 받았다. 별 이상은 없었다."),
			new Line("hl_flu", 10, 90, 2, l -> l.getHealth() < 60, "독감에 걸려 일주일을 누워 지냈다."),
			new Line("hl_cold", 6, 90, 2, ANY, "지독한 감기로 며칠을 누워 지냈다."),
			new Line("hl_heat", 15, 95, 2, ANY, "더위가 길어져 열대야에 며칠 밤을 설쳤다.", 0, 2000),
			new Line("hl_exercise", 20, 90, 3, l -> focus(l, "HEALTH"), "아침마다 몸을 움직이는 습관을 들였다."),
			new Line("hl_rest", 16, 99, 3, l -> focus(l, "REST"), "서두르지 않고 쉬어 가며 한 해를 보냈다."),
			new Line("hl_gray", 38, 50, 2, ANY, "거울 속에서 첫 흰머리를 발견했다."),
			new Line("hl_eyes", 42, 55, 2, ANY, "작은 글씨가 잘 안 보여 처음으로 돋보기를 썼다."),

			// ── 살림 ──
			new Line("money_debt", 20, 90, 3, l -> l.getAssets() < 0, "빚 걱정에 밤잠을 설쳤다."),
			new Line("money_save", 20, 70, 2, l -> l.getAssets() > 0 && l.getWealthClass() <= 3, "조금씩 모은 돈으로 살림살이를 하나 장만했다."),
			new Line("money_rent", 22, 70, 2, l -> !rural(l) && l.getAssets() < 0, "집세가 올라 생활비가 빠듯해졌다."),
			new Line("money_market", 10, 99, 2, l -> rural(l), "장날이면 시장 구경을 나갔다."),

			// ── 노년 ──
			new Line("old_bday50", 50, 50, 3, ANY, "쉰 번째 생일을 맞았다."),
			new Line("old_bday60", 60, 60, 3, ANY, "환갑을 맞아 가족들이 상을 차려 주었다."),
			new Line("old_grand", 58, 99, 3, l -> l.getChildCount() > 0, "손주들이 놀러 와 집 안이 시끌벅적했다."),
			new Line("old_grand2", 58, 99, 3, l -> l.getChildCount() > 0, "손주와 함께 시간을 보냈다."),
			new Line("old_knee", 60, 99, 2, l -> l.getHealth() < 50, "무릎이 아파 걷는 일이 예전 같지 않았다."),
			new Line("old_walk", 60, 99, 2, l -> l.getHealth() >= 50, "아침마다 동네를 천천히 한 바퀴 걸었다."),
			new Line("old_memory", 65, 99, 2, l -> l.getKnowledge() < 50, "기억력이 예전 같지 않다."),
			new Line("old_sharp", 65, 99, 2, l -> l.getKnowledge() >= 50, "날마다 걷고 책을 읽는다. 또래보다 기억력이 좋다는 말을 듣는다."),
			new Line("old_center", 62, 95, 2, l -> !rural(l), "복지관 강좌에 다니며 새로운 것을 배웠다."),
			new Line("old_friend", 70, 99, 2, ANY, "오랜 친구의 부고를 들었다. 함께 보낸 시절이 떠올랐다."),
			new Line("old_photo", 60, 99, 2, ANY, "옛 사진을 꺼내 보며 젊은 날을 떠올렸다."),
			new Line("old_thanks", 70, 99, 2, ANY, "살아온 날들을 돌아보니 고마운 얼굴들이 하나씩 떠올랐다."),
			new Line("old_brain", 60, 99, 3, l -> focus(l, "BRAIN"), "신문을 꼼꼼히 읽고 낱말 맞히기를 즐겼다."),
			new Line("old_retired", 55, 99, 2, l -> "RETIRED".equals(l.getJobStatus()), "일을 놓고 나니 하루가 길었다. 텃밭을 가꾸며 시간을 보냈다."));

	// 세계 소식 (나라와 상관없이 그해 한 번, 5세 이상) - [연도, 문장]
	// 사실: 세계 인구는 유엔 세계인구전망 기준
	private static final Object[][] WORLD = {
			{ 1945, "전쟁이 끝났다는 소식에 어른들이 거리로 나왔다." },
			{ 1957, "사람이 만든 첫 인공위성이 지구 둘레를 돌기 시작했다는 소식을 들었다." },
			{ 1960, "세계 인구가 30억 명을 넘었다." },
			{ 1969, "사람이 처음으로 달에 발을 디뎠다는 소식이 전해졌다." },
			{ 1973, "기름값이 치솟아 물가가 크게 올랐다." },
			{ 1974, "세계 인구가 40억 명을 넘었다." },
			{ 1987, "세계 인구가 50억 명을 넘었다." },
			{ 1999, "세계 인구가 60억 명을 넘었다." },
			{ 2008, "금융 위기가 터졌다. 은행들이 흔들리고 회사들이 사람을 줄였다." },
			{ 2011, "세계 인구가 70억 명을 넘었다." },
			{ 2020, "새로운 감염병이 온 세계로 번졌다. 사람들은 마스크를 쓰고 서로 거리를 두었다." },
			{ 2022, "세계 인구가 80억 명을 넘었다." },
			{ 2023, "기록적인 폭염이 이어졌다. 한낮에는 밖에서 일하기 어려웠다." },
			{ 2025, "AI가 사무실과 공장, 병원에서 일하는 방식을 빠르게 바꾸고 있다." } };

	// 나라별 대표 명절 (없으면 '새해 명절')
	private static final Map<String, String[]> HOLIDAY = Map.ofEntries(
			Map.entry("KOR", new String[] { "설날", "추석" }), Map.entry("PRK", new String[] { "설날", "추석" }),
			Map.entry("CHN", new String[] { "춘절", "중추절" }), Map.entry("SGP", new String[] { "춘절" }),
			Map.entry("JPN", new String[] { "설", "오봉" }), Map.entry("MNG", new String[] { "차강사르" }),
			Map.entry("VNM", new String[] { "뗏", "중추절" }), Map.entry("THA", new String[] { "송끄란" }),
			Map.entry("LAO", new String[] { "삐마이" }), Map.entry("KHM", new String[] { "쫄츠남 트마이" }),
			Map.entry("MMR", new String[] { "띤잔" }), Map.entry("IND", new String[] { "디왈리", "홀리" }),
			Map.entry("NPL", new String[] { "다샤인", "티하르" }), Map.entry("LKA", new String[] { "새해 축제" }),
			Map.entry("BTN", new String[] { "로사르" }), Map.entry("PHL", new String[] { "크리스마스" }),
			Map.entry("TLS", new String[] { "크리스마스" }), Map.entry("ARM", new String[] { "새해" }),
			Map.entry("GEO", new String[] { "새해" }), Map.entry("CYP", new String[] { "부활절" }),
			Map.entry("ISR", new String[] { "유월절" }), Map.entry("IRN", new String[] { "노루즈" }),
			Map.entry("AFG", new String[] { "노루즈", "이드" }), Map.entry("TJK", new String[] { "노루즈" }),
			Map.entry("UZB", new String[] { "노루즈" }), Map.entry("KAZ", new String[] { "나우르즈" }),
			Map.entry("KGZ", new String[] { "노오루즈" }), Map.entry("TKM", new String[] { "노루즈" }),
			Map.entry("AZE", new String[] { "노루즈" }));
	// 그 밖에 이드(라마단을 마치는 명절)를 크게 쇠는 나라
	private static final Set<String> EID = Set.of("IDN", "MYS", "BRN", "BGD", "PAK", "MDV", "IRQ", "SAU", "ARE", "QAT",
			"KWT", "BHR", "OMN", "YEM", "JOR", "SYR", "PSE", "TUR", "LBN");

	private static String holiday(String cc, Random rnd) {
		String[] list = HOLIDAY.get(cc);
		if (list == null) {
			return EID.contains(cc) ? "이드 명절" : "새해 명절";
		}
		return list[rnd.nextInt(list.length)];
	}

	// 인생마다 최근에 쓴 문장 (같은 문장이 해마다 반복되지 않게, 서버 메모리)
	// 시대 물건(era_)은 평생 한 번만
	private static final Map<Integer, Deque<String>> RECENT = new ConcurrentHashMap<>();
	private static final Map<Integer, Set<String>> ONCE = new ConcurrentHashMap<>();
	private static final int RECENT_SIZE = 14;
	// 평생 한 번만 나오는 줄 (원작 플래그)
	private static final Set<String> ONCE_IDS = Set.of("fam_maid", "fam_room", "fam_room1", "fam_clothes", "kid_academy", "kid_kinder");
	// 살림 지출(exp_)은 같은 일이 8년 안에 다시 나오지 않게 (TV가 몇 해마다 고장 나지 않게)
	private static final Map<Integer, Map<String, Integer>> LAST = new ConcurrentHashMap<>();
	private static final int EXP_GAP = 8;

	// 나이대별 일상 줄 수 (원작 한 해 기록 양에 맞춤, 이벤트·진학 기록과 합쳐서)
	private static int wantLines(int age, Random rnd) {
		if (age <= 5) {
			return 3 + rnd.nextInt(2); // 3~4줄
		}
		if (age <= 18) {
			return 3 + rnd.nextInt(3); // 3~5줄
		}
		if (age <= 64) {
			return 2 + rnd.nextInt(3); // 2~4줄
		}
		return 2 + rnd.nextInt(2); // 2~3줄
	}

	// 그해 일상 문장 고르기
	// age: 방금 지나간 해의 나이, already: 그해 이미 기록된 사건 수 (많으면 일상은 덜 넣음)
	static List<Story> year(LifeDTO life, int age, int already, Random rnd) {
		int year = life.getBirthYear() + age;
		int want = Math.max(age <= 18 ? 2 : 1, wantLines(age, rnd) - already / 2); // 사건 2개당 일상 1줄 덜

		Deque<String> recent = RECENT.computeIfAbsent(life.getLifeId(), k -> new ArrayDeque<>());
		Set<String> once = ONCE.computeIfAbsent(life.getLifeId(), k -> ConcurrentHashMap.newKeySet());
		Map<String, Integer> last = LAST.computeIfAbsent(life.getLifeId(), k -> new ConcurrentHashMap<>());
		List<Story> out = new ArrayList<>();

		// 세계 소식 (그해에 있으면 먼저, 5세 이상)
		if (age >= 5) {
			for (Object[] w : WORLD) {
				if ((int) w[0] == year) {
					out.add(new Story((String) w[1], 0));
				}
			}
		}

		List<Line> pool = new ArrayList<>();
		for (Line line : LINES) {
			if (age >= line.minAge && age <= line.maxAge && year >= line.fromYear
					&& !recent.contains(line.id) && !once.contains(line.id) && line.when.test(life)
					&& !(line.id.startsWith("exp_") && last.containsKey(line.id) && age - last.get(line.id) < EXP_GAP)) {
				pool.add(line);
			}
		}

		int picked = 0;
		while (picked < want && !pool.isEmpty()) {
			int total = 0;
			for (Line line : pool) {
				total += line.weight;
			}
			int pick = rnd.nextInt(total);
			Line chosen = pool.get(pool.size() - 1);
			for (Line line : pool) {
				pick -= line.weight;
				if (pick < 0) {
					chosen = line;
					break;
				}
			}
			pool.remove(chosen);
			// 같은 무리(cer_wedding / cer_wedding0 …)는 한 해에 하나만
			String group = chosen.id.replaceAll("\\d+$", "");
			pool.removeIf(l -> l.id.replaceAll("\\d+$", "").equals(group));

			out.add(new Story(chosen.text.replace("{명절}", holiday(life.getCurrentCountry(), rnd))
					.replace("{나이}", String.valueOf(age)), chosen.cost, chosen.id));
			picked++;
			if (chosen.id.startsWith("era_") || chosen.id.equals("exp_glasses") || chosen.id.equals("exp_cane")
					|| chosen.id.equals("hl_eyes") || ONCE_IDS.contains(chosen.id)) {
				once.add(chosen.id);
			}
			// 돋보기는 '처음 썼다'와 '맞췄다' 중 한 번만
			if (chosen.id.equals("hl_eyes") || chosen.id.equals("exp_glasses")) {
				once.add("hl_eyes");
				once.add("exp_glasses");
				pool.removeIf(l -> l.id.equals("hl_eyes") || l.id.equals("exp_glasses"));
			}
			if (chosen.id.startsWith("exp_")) {
				last.put(chosen.id, age);
			}
			recent.addLast(chosen.id);
			while (recent.size() > RECENT_SIZE) {
				recent.removeFirst();
			}
		}
		if (RECENT.size() > 5000) {
			RECENT.clear(); // 서버 메모리 보호 (오래 켜 두면 끝난 인생 기록이 쌓이므로)
			ONCE.clear();
			LAST.clear();
		}
		return out;
	}
}
