package com.example.demo.engine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.event.EventDAO;
import com.example.demo.event.EventService;
import com.example.demo.event.dto.EventChoiceDTO;
import com.example.demo.event.dto.EventResultDTO;
import com.example.demo.quest.QuestJudgeService;

@Service
public class EngineService {

	private final EngineDao engineDao;
	private final EventService eventService; // C09·C10 이벤트 (장윤아님)
	private final EventDAO eventDao; // 선택지 다시 불러오기용 (장윤아님, 읽기만)
	private final QuestJudgeService questJudgeService; // 사망 후 퀘스트 판정·포인트 지급 (quest 패키지, 호출만)

	// 생성자 주입: 스프링이 객체를 만들 때 EngineDao, EventService, EventDAO, QuestJudgeService를 넣어줌
	public EngineService(EngineDao engineDao, EventService eventService, EventDAO eventDao,
			QuestJudgeService questJudgeService) {
		this.engineDao = engineDao;
		this.eventService = eventService;
		this.eventDao = eventDao;
		this.questJudgeService = questJudgeService;
	}

	private Random random = new Random();

	// ================= 원작(1in8b) 공식 도우미 =================
	// 원작 나라 상수 (원작 COUNTRIES 데이터): 나라코드:권역그룹:남녀 기대수명 차이:지니계수:행복 점수(WHR):5~14세 사망 비율
	//   권역그룹: hi 고소득국, eap 동아시아·태평양, sa 남아시아, eca 유럽·중앙아시아, mena 중동·북아프리카, ssa, lac
	private static final String COUNTRY_TAB = "AFG:sa:6:.3:1.72:.12;DZA:mena:2:.28:5.36:.14;AGO:ssa:4:.51:3.79:.23;ARG:lac:6.5:.47:6.19:.18;AUS:hi:4:.34:7.06:.24;AUT:hi:4.8:.31:6.91:.21;AZE:eca:6:.27:4.89:.13;BGD:sa:3.5:.31:3.89:.15;BLR:eca:9.5:.24:5.82:.39;BEL:hi:4.8:.26:6.89:.19;BEN:ssa:4:.34:4.38:.23;BOL:lac:6.5:.41:5.78:.2;BWA:ssa:4:.53:3.38:.17;BRA:lac:6.8:.52:6.27:.18;BGR:eca:7:.38:5.46:.25;BFA:ssa:4:.37:4.55:.1;BDI:ssa:4:.37:3.78:.35;KHM:eap:6:.33:4.34:.2;CMR:ssa:4:.42:4.87:.28;CAN:hi:4.8:.32:6.9:.18;CAF:ssa:4:.43:3.48:.24;TCD:ssa:4:.37:4.47:.2;CHL:lac:6.5:.43:6.36:.2;CHN:eap:5.8:.36:5.97:.25;COL:lac:6.5:.55:5.7:.19;COG:ssa:4:.42:5.22:.15;CRI:lac:6.5:.47:6.96:.19;CIV:ssa:4:.35:5.08:.23;CUB:lac:6.5:.4:null:.28;CZE:hi:5.6:.26:6.82:.3;COD:ssa:4:.45:3.3:.2;DNK:hi:4.8:.29:7.58:.15;DOM:lac:6.5:.37:5.82:.1;ECU:lac:6.5:.45:5.73:.23;EGY:mena:4.5:.28:3.98:.15;SLV:lac:9:.39:6.47:.35;GNQ:ssa:4:.39:null:.2;ERI:ssa:4:.4:null:.17;ETH:ssa:4.5:.31:3.86:.16;FIN:hi:4.8:.28:7.74:.33;FRA:hi:5.8:.31:6.61:.19;GAB:ssa:4:.38:5.11:.25;GMB:ssa:4:.39:4.49:.2;GEO:eca:9:.34:5.19:.19;DEU:hi:4.8:.34:6.72:.22;GHA:ssa:4:.44:4.29:.25;GRC:hi:4.8:.33:5.93:.18;GTM:lac:6.5:.45:6.29:.16;GIN:ssa:4:.3:5.02:.18;GNB:ssa:4:.33:null:.17;HTI:lac:6.5:.47:3.62:.17;HND:lac:5:.47:5.97:.29;HUN:eca:6.5:.31:6.02:.24;IND:sa:3:.26:4.05:.15;IDN:eap:4.3:.36:5.57:.23;IRN:mena:4:.35:4.92:.28;IRQ:mena:4:.3:5.17:.21;IRL:hi:4.8:.3:6.84:.16;ISR:hi:3.7:.38:7.34:.23;ITA:hi:4.8:.34:6.32:.23;JAM:lac:6.5:.4:5.84:.18;JPN:hi:6:.32:6.06:.32;JOR:mena:4:.34:4.19:.09;KAZ:eca:8:.29:6.19:.27;KEN:ssa:4:.39:4.47:.12;KWT:mena:4:.4:6.95:.2;KGZ:eca:8:.26:5.71:.16;LAO:eap:4.5:.35:5.14:.17;LBN:mena:4:.35:2.71:.22;LSO:ssa:6:.45:3.19:.14;LBR:ssa:4:.35:4.27:.18;LBY:mena:4:.35:5.87:.28;MDG:ssa:4:.37:4.23:.28;MWI:ssa:4:.39:3.42:.23;MYS:eap:4.9:.41:5.98:.24;MLI:ssa:4:.36:4.23:.23;MRT:ssa:4:.32:4.51:.18;MEX:lac:6:.43:6.68:.19;MNG:eap:9.5:.31:5.7:.24;MAR:mena:4:.4:4.8:.13;MOZ:ssa:4:.5:5.22:.14;MMR:eap:6.5:.31:4.35:.11;NAM:ssa:4:.59:4.83:.17;NPL:sa:4:.3:5.16:.16;NLD:hi:4.8:.26:7.32:.17;NZL:hi:4.8:.33:7.03:.19;NIC:lac:6.5:.46:6.28:.23;NER:ssa:4:.33:4.56:.27;NGA:ssa:1.4:.34:4.88:.19;PRK:eap:6.5:.35:null:.22;NOR:hi:4.8:.27:7.3:.29;OMN:mena:4:.4:null:.22;PAK:sa:4.5:.34:4.66:.1;PSE:mena:4:.36:4.88:.23;PAN:lac:6.5:.49:6.36:.21;PNG:eap:4:.42:null:.19;PRY:lac:6.5:.45:5.98:.12;PER:lac:6.5:.4:5.84:.2;PHL:eap:6.5:.35:6.05:.16;PRT:hi:4.8:.36:6.03:.26;QAT:mena:4:.35:6.37:.23;ROU:eca:7:.32:6.49:.2;RUS:eca:10.5:.36:5.79:.35;RWA:ssa:4:.39:3.27:.12;SAU:mena:3:.45:6.59:.24;SEN:ssa:4:.36:4.97:.22;SRB:eca:5:.33:6.41:.21;SLE:ssa:4:.36:3.25:.23;SGP:hi:4.8:.45:6.52:.31;SVK:hi:4.8:.24:6.26:.18;SOM:ssa:4:.37:4.67:.22;ZAF:ssa:6.5:.54:5.42:.16;KOR:hi:6.1:.33:6.06:.26;SSD:ssa:3.5:.44:2.82:.22;ESP:hi:5.3:.34:6.42:.25;LKA:sa:7:.38:3.9:.28;SDN:ssa:4.5:.34:4.14:.14;SWE:hi:4.8:.32:7.34:.27;CHE:hi:4.8:.34:7.06:.15;SYR:mena:9:.26:3.46:.25;TWN:hi:6.5:.32:6.5:.2;TJK:eca:4.6:.39:5.28:.07;TZA:ssa:4:.4:3.78:.29;THA:eap:7.4:.34:5.98:.39;TGO:ssa:4:.38:4.21:.18;TUN:mena:4:.34:4.42:.19;TUR:eca:5.5:.44:4.98:.18;TKM:eca:7.5:.35:5.07:.1;UGA:ssa:4:.38:4.37:.23;UKR:eca:10:.26:4.87:.21;ARE:mena:2.5:.26:6.73:.24;GBR:hi:3.8:.32:6.75:.15;USA:hi:5.4:.42:6.73:.21;URY:lac:6.5:.41:6.61:.21;UZB:eca:5:.31:6.2:.22;VEN:lac:8:.45:5.61:.13;VNM:eap:9.4:.36:6.04:.15;YEM:mena:6:.37:3.56:.22;ZMB:ssa:4:.51:3.5:.19;ZWE:ssa:4:.5:3.34:.15;HKG:hi:5.3:.47:5.42:.2;HRV:hi:6.3:.3:6.13:.2;BIH:eca:6.5:.3:5.77:.2;MDA:eca:9:.27:5.86:.2;ARM:eca:8.1:.27:5.46:.2;LTU:hi:9.5:.36:6.45:.2;ALB:eca:3.7:.29:5.2:.2;SVN:hi:5.4:.25:6.63:.2;LVA:hi:8.9:.34:6.18:.2;MKD:eca:4.5:.34:5.2:.2;BHR:mena:1.3:.35:6.65:.2;TTO:lac:6.3:.47:6.19:.2;TLS:eap:3.4:.29:null:.2;EST:hi:8.1:.31:6.34:.2;CYP:hi:4:.32:6.22:.2;MUS:ssa:6.2:.37:6.07:.2;SWZ:ssa:6:.55:4.4:.2;DJI:ssa:5:.42:null:.2;FJI:eap:4:.31:null:.2;COM:ssa:4.1:.3:4.61:.2;GUY:lac:7.4:.47:null:.2;SLB:eap:2.8:.37:null:.2;BTN:sa:3.7:.28:null:.2;MAC:hi:4.4:.32:null:.2;LUX:hi:3.3:.34:7.4:.2;MNE:eca:6.6:.34:5.55:.2;SUR:lac:6.4:.39:null:.2;MLT:hi:4:.32:6.45:.2;MDV:sa:3.1:.29:5.2:.2;CPV:ssa:6.4:.42:null:.2";
	private static final Map<String, String> C_GROUP = new ConcurrentHashMap<>();
	private static final Map<String, double[]> C_NUM = new ConcurrentHashMap<>();
	static {
		for (String row : COUNTRY_TAB.split(";")) {
			String[] a = row.split(":");
			C_GROUP.put(a[0], a[1]);
			double wh = a[4].equals("null") ? 5 : Double.parseDouble(a[4]);
			C_NUM.put(a[0], new double[] { Double.parseDouble(a[2]), Double.parseDouble(a[3]), wh, Double.parseDouble(a[5]) });
		}
	}

	// 권역 그룹: 원작 표에 있으면 그 값, 없으면 우리 권역으로 대신
	private String groupOf(String cc, CountryStatDTO stat) {
		if (cc != null && C_GROUP.containsKey(cc)) {
			return C_GROUP.get(cc);
		}
		String r = stat == null || stat.getRegionCode() == null ? "" : stat.getRegionCode();
		switch (r) {
		case "SOUTH": return "sa";
		case "CENTRAL": return "eca";
		case "WEST": return "mena";
		default: return "eap";
		}
	}

	// 나라 숫자 상수 (없으면 기본값): 0 남녀 기대수명 차이, 1 지니, 2 행복 점수, 3 5~14세 사망 비율
	private static double cNum(String cc, int i) {
		double[] d = cc == null ? null : C_NUM.get(cc);
		if (d != null) {
			return d[i];
		}
		double[] def = { 5, 0.35, 5, 0.2 };
		return def[i];
	}

	private static double clamp(double v, double lo, double hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	// 표준정규분포 누적확률 Φ(x)
	private double normCdf(double x) {
		double t = 1 / (1 + 0.2316419 * Math.abs(x));
		double d = 0.3989422804014327 * Math.exp(-x * x / 2);
		double p = d * t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))));
		return x >= 0 ? 1 - p : p;
	}

	// 표준정규분포 역함수 Φ⁻¹(p) (Acklam 근사)
	private double invNorm(double p) {
		p = clamp(p, 1e-9, 1 - 1e-9);
		double[] a = { -39.69683028665376, 220.9460984245205, -275.9285104469687, 138.3577518672690, -30.66479806614716, 2.506628277459239 };
		double[] b = { -54.47609879822406, 161.5858368580409, -155.6989798598866, 66.80131188771972, -13.28068155288572 };
		double[] c = { -0.007784894002430293, -0.3223964580411365, -2.400758277161838, -2.549732539343734, 4.374664141464968, 2.938163982698783 };
		double[] d = { 0.007784695709041462, 0.3224671290700398, 2.445134137142996, 3.754408661907416 };
		double q, r;
		if (p < 0.02425) {
			q = Math.sqrt(-2 * Math.log(p));
			return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
		}
		if (p > 1 - 0.02425) {
			q = Math.sqrt(-2 * Math.log(1 - p));
			return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
		}
		q = p - 0.5;
		r = q * q;
		return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1);
	}

	private double gdpOf(CountryStatDTO stat) {
		return stat != null && stat.getGdpPerCapita() != null ? stat.getGdpPerCapita() : 5000;
	}

	// 소득 분포 폭 σ (원작 sigmaOf): 지니계수로 계산, 남아시아·중동·동아시아(중국 제외)는 +0.07
	private double sigmaOf(String cc, CountryStatDTO stat) {
		String g = groupOf(cc, stat);
		double t = cNum(cc, 1);
		if (g.equals("ssa") || g.equals("sa") || g.equals("mena") || (g.equals("eap") && !"CHN".equals(cc))) {
			t = t + 0.07;
		}
		t = clamp(t, 0.22, 0.68);
		return Math.sqrt(2) * invNorm((t + 1) / 2);
	}

	// 직업 소득 = 1인당 GDP × 배율을 나라 형편에 맞춤 (11/20 동현님: 소득이 현실과 동떨어짐)
	//   ① 불평등이 큰 나라일수록 좋은 일과 나쁜 일의 소득 차가 큼: 배율^(σ/0.57) (원작 로그정규 소득처럼, 한국 σ≈0.57이 기준이라 한국은 그대로)
	//   ② 가난한 나라는 일하는 사람이 인구에 비해 적어, 일하는 사람 1명의 소득이 1인당 GDP보다 큼:
	//      구매력 1인당 GDP 2만 달러 아래면 최대 ×1.4 (2만 달러 이상은 그대로)
	//   예) 인도 2018년 철도 기관사 월 72만 → 약 110만 원 (한국 체감, 실제 인도 기관사 월급 4.5만 루피 ≈ 한국 체감 200만 원 쪽으로)
	private double jobPay(double gdp, double rate, String cc, CountryStatDTO stat) {
		double k = clamp(sigmaOf(cc, stat) / 0.57, 0.9, 1.5);
		double w = 1 + 0.4 * clamp(1 - toPpp(gdp) / 20000, 0, 1);
		return gdp * Math.pow(Math.max(0.01, rate), k) * w;
	}

	// 그 나라 중위 소득 (원작 incomeAt(나라, 0.5))
	private double medIncome(String cc, CountryStatDTO stat) {
		double s = sigmaOf(cc, stat);
		return gdpOf(stat) * 0.9 * Math.exp(-s * s / 2);
	}

	// 소득이 그 나라에서 몇 % 위치인지 0~1 (원작 pctOfIncome)
	// 원작은 소득 자체를 로그정규 분포에서 뽑는데, 우리는 소득 = 1인당 GDP × 직업 소득 배율이라
	// 배율 1(평균 직업)이 가운데(0.5)가 되도록 1인당 GDP를 중앙에 둠 (시뮬: 원작식 그대로면 평균 위치가 0.77로 쏠림)
	private double incomePct(double income, String cc, CountryStatDTO stat) {
		double s = sigmaOf(cc, stat);
		return normCdf(Math.log(Math.max(1, income) / gdpOf(stat)) / s);
	}

	// 어른의 경제적 위치 0~1 (원작 S.pct)
	//   일하는 중: 올해 소득 위치 / 은퇴·실직: 일하던 때 소득 위치 그대로 (원작은 은퇴해도 S.pct를 유지)
	//   일한 적 없으면: 아르바이트 소득 위치, 그것도 없으면 가정형편
	private double lifePct(LifeDTO life, CountryStatDTO stat) {
		String cc = life.getCurrentCountry();
		if ("EMPLOYED".equals(life.getJobStatus()) && life.getIncome() > 0) {
			return incomePct(life.getIncome(), cc, stat);
		}
		if (life.getIncomeRate() != null) {
			return incomePct(jobPay(gdpOf(stat), life.getIncomeRate(), cc, stat), cc, stat);
		}
		if (life.getIncome() > 0) {
			return incomePct(life.getIncome(), cc, stat);
		}
		return (life.getWealthClass() - 0.5) / 5;
	}

	// 사망 판정용 경제적 위치 (원작 mortSt): 부모와 함께 사는 동안은 집안 형편과 내 위치 중 높은 쪽
	private double mortPct(LifeDTO life, CountryStatDTO stat) {
		double p = lifePct(life, stat);
		boolean withParents = life.getCurrentAge() < 30 && !"Y".equals(life.getMarriedYn()) && "Y".equals(life.getParentAlive());
		if (withParents) {
			p = Math.max(p, (life.getWealthClass() - 0.5) / 5);
		}
		return p;
	}

	// 빚이 무거운지 (원작 debtHeavy): 빚이 1년 벌이(최소 하위 30% 소득)의 25% 이상
	private boolean debtHeavy(LifeDTO life, CountryStatDTO stat) {
		if (life.getAssets() >= 0) {
			return false;
		}
		double s = sigmaOf(life.getCurrentCountry(), stat);
		double low = gdpOf(stat) * 0.9 * Math.exp(s * invNorm(0.3) - s * s / 2);
		return -life.getAssets() >= 0.25 * Math.max(life.getIncome(), low);
	}

	// 달러 소득 → 구매력(PPP) 소득 (원작 plr: 물가 수준 = 0.11 × √(PPP/1000), 0.22 ~ 1.25)
	private double toPpp(double usd) {
		double p = Math.max(1, usd);
		for (int i = 0; i < 30; i++) {
			p = usd / clamp(0.11 * Math.sqrt(p / 1000), 0.22, 1.25);
		}
		return p;
	}

	// 저축률 (원작 saveRate, 나라별·세금 보정 제외)
	// 소득이 높을수록 기본 저축 ↑ (최대 28%), 중위 소득의 0.3배 이하는 저축 못 함,
	// 중위의 1.6배부터 추가 저축 (최대 +20%), 전체 × 0.8, 최대 50%
	private double saveRate(double income, double med) {
		double s = income / med;
		double t = toPpp(income);
		double o = clamp(0.05 + 0.05 * Math.log(Math.max(500, t) / 4000), -0.05, 0.28);
		double r = o > 0 ? o * clamp((s - 0.3) / 0.6, 0, 1) : o;
		double top = 0.2 * clamp(Math.log(Math.max(s, 1e-6) / 1.6) / Math.log(8), 0, 1);
		return 0.8 * Math.min(0.5, Math.max(r, 0.12 * clamp((s - 1.2) / 2, 0, 1)) + top);
	}

	// 원작 fitK: 나이가 들수록 건강 관리 효과가 줄어듦
	private double fitK(int age) {
		return age < 50 ? 1 : age < 65 ? 0.8 : age < 75 ? 0.6 : age < 85 ? 0.45 : 0.3;
	}

	// 아이가 아플 확률 (원작 kidSickP)
	private double kidSickP(double u5) {
		return clamp(0.06 + u5 / 250, 0.06, 0.55);
	}

	private double u5Of(CountryStatDTO stat) {
		if (stat != null && stat.getU5Mortality() != null) {
			return stat.getU5Mortality();
		}
		if (stat != null && stat.getInfantMortality() != null) {
			return stat.getInfantMortality() * 1.35;
		}
		return 40;
	}

	// 타고난 학업 재능 (원작 tal("st")): 인생마다 정해진 값, 평균 0·표준편차 1
	private double talent(LifeDTO life) {
		// SplittableRandom: 번호가 이웃한 인생끼리도 재능이 고르게 퍼지게 (java.util.Random은 비슷한 씨앗끼리 첫 값이 닮음)
		// C13 학업 능력 레벨은 원작의 '타고난 학업 재능'을 올려 주는 방식 (레벨당 +0.25 표준편차)
		//   → 원작 공식 그대로 지식(또래 위치)이 오르고, 지식을 통해 진학·중퇴·대학원 합격이 좋아짐
		return new java.util.SplittableRandom(life.getLifeId() * 1000003L + 17).nextGaussian() + studyEffect(life);
	}

	// 나이별 재능 반영 비중 (원작 talW)
	private double talW(int age) {
		return 0.9 * clamp(0.35 + 0.055 * (age - 3), 0.35, 1);
	}

	// 집중별 지식 효과 (원작 KNOW_EFF)
	private double knowEff(String focus) {
		if (focus == null) {
			return 0;
		}
		switch (focus) {
		case "STUDY": return 1;
		case "SELF_DEV": return 0.8;
		case "BRAIN": return 0.6;
		case "JOB_PREP": return 0.4;
		case "HOUSE": case "REST": case "PARTTIME": return -0.1;
		default: return 0;
		}
	}

	// 또래가 그해 배우는 양 (원작 KNOW_W0)
	private double knowW0(int age) {
		return age < 6 ? 0 : age < 18 ? 0.2 : age < 25 ? 0.25 : 0.05;
	}

	// 원작 생명표 (lifeTable): 기대수명·영아/5세 미만 사망률에 맞춰 나이별 사망 확률 q를 만듦
	// 5세 이상은 c + b·e^(f·나이), b는 기대수명이 맞을 때까지 50번 반씩 좁혀 찾음
	private static final Map<String, double[]> lifeTableCache = new ConcurrentHashMap<>();

	private double[] lifeTable(String cc, CountryStatDTO stat, String gender) {
		double le = stat != null && stat.getLifeExpectancy() != null ? stat.getLifeExpectancy() : 70;
		double im = stat != null && stat.getInfantMortality() != null ? stat.getInfantMortality() : 30;
		return lifeTableOf(cc, le, im, u5Of(stat), gender);
	}

	// 다른 기능에서도 쓰는 "그 나라·시대·성별·나이의 기본 사망 확률" (원작 생명표, 건강·형편 배율 없이)
	// 예: 윤아님 사별 이벤트 = 배우자(반대 성별, 같은 나이)의 올해 사망 확률
	//   le 기대수명, im 영아사망률(‰), u5 5세 미만 사망률(‰, 없으면 영아사망률×1.35)
	public static double baseDeathProb(String cc, Double le, Double im, Double u5, String gender, int age) {
		double l = le == null ? 70 : le;
		double i = im == null ? 30 : im;
		double u = u5 != null ? u5 : i * 1.35;
		return lifeTableOf(cc, l, i, u, gender)[(int) clamp(age, 0, 110)];
	}

	private static double[] lifeTableOf(String cc, double le, double im, double u5, String gender) {
		String key = cc + gender + le + "/" + im + "/" + u5;
		double[] cached = lifeTableCache.get(key);
		if (cached != null) {
			return cached;
		}
		boolean f = "F".equals(gender);
		double gap = cNum(cc, 0);
		double target = f ? le + gap * 0.49 : le - gap * 0.51;
		double sx = f ? 0.92 : 1.08;
		double o = Math.min(0.25, im / 1000 * sx);
		double r = Math.min(0.35, Math.max(o + 5e-4, u5 / 1000 * sx));
		double i = -Math.log((1 - r) / (1 - o));
		double[] share = { 0.4, 0.27, 0.19, 0.14 };
		double c = 25e-5 + 28e-6 * u5;
		double ff = clamp(0.112 - 16e-5 * u5, 0.085, 0.112);
		double u = 1 - Math.pow(1 - Math.min(0.2, cNum(cc, 3) * u5 / 1000 * sx), 0.1);

		double lo = Math.log(1e-8), hi = Math.log(0.05);
		double[] q = new double[111];
		for (int k = 0; k < 50; k++) {
			double mid = (lo + hi) / 2;
			double b = Math.exp(mid);
			q[0] = o;
			for (int a = 1; a <= 4; a++) {
				q[a] = 1 - Math.exp(-i * share[a - 1]);
			}
			for (int a = 5; a <= 110; a++) {
				if (a == 110) {
					q[a] = 1;
				} else if (a < 15) {
					q[a] = u;
				} else {
					double oldest = Math.min(0.7, (f ? 0.39 : 0.5) * Math.exp(0.15 * (a - 100)));
					q[a] = Math.min(1, 1 - Math.exp(-Math.max(c + b * Math.exp(ff * (a + 0.5)), oldest)));
				}
			}
			double alive = 1, years = 0;
			for (int a = 0; a <= 110; a++) {
				double dead = alive * q[a];
				years += a == 0 ? alive - 0.85 * dead : alive - 0.5 * dead;
				alive -= dead;
			}
			if (years > target) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		lifeTableCache.put(key, q.clone());
		return q;
	}

	// 아이(0~14세)의 기준 건강 (원작 kidHlRef)
	private double kidHlRef(double u5, int age) {
		double n = clamp(78 - u5 / 12, 40, 95);
		double[] a = new double[15];
		for (int s = 0; s < 15; s++) {
			n += (expHealth(s + 1) - n) * 0.25 - (s < 5 ? 4 * kidSickP(u5) : 0);
			a[s] = n;
		}
		return a[(int) clamp(age, 0, 14)];
	}

	private static final double WEALTH_MORT_K = 1.0; // 원작 500명 다시 돌려 보니 형편별 수명 차가 거의 없음 (상위 20% 76.3 · 하위 20% 76.5세) → 원작 그대로 1.0

	// 권역별 형편에 따른 사망 차이 (원작 MORT_G)
	private double mortG(String g) {
		switch (g) {
		case "ssa": case "sa": return 0.25;
		case "mena": case "hi": return 0.15;
		default: return 0.2;
		}
	}

	// 어른 사망 보정 (원작 STAND_K: [남, 여])
	private double standK(String g, String gender) {
		double[] k;
		switch (g) {
		case "ssa": k = new double[] { 1.003, 0.996 }; break;
		case "sa": k = new double[] { 1.055, 0.94 }; break;
		case "eap": k = new double[] { 1.011, 0.978 }; break;
		case "lac": k = new double[] { 0.993, 1.009 }; break;
		case "mena": k = new double[] { 0.973, 1.037 }; break;
		case "eca": k = new double[] { 1.035, 0.955 }; break;
		case "hi": k = new double[] { 1.036, 0.952 }; break;
		default: k = new double[] { 1, 1 };
		}
		return "F".equals(gender) ? k[1] : k[0];
	}

	// ================= C02 랜덤 출생 =================
	// memberId: 로그인한 회원 번호 (비회원이면 null)
	// @Transactional: 인생·특성·0세 기록 저장 중 하나라도 실패하면 전부 취소 (반쪽짜리 인생이 남지 않게)
	@Transactional
	public LifeDTO birth(int decade, String oldGuestToken, Integer memberId) {

		// 0. 진행 중이던 인생이 있으면 포기 처리 (회원 1명·토큰 1개당 진행 중 인생 1개)
		if (memberId != null) {
			engineDao.abandonMemberLife(memberId);
		} else if (oldGuestToken != null && !oldGuestToken.equals("")) {
			engineDao.abandonGuestLife(oldGuestToken);
		}

		// 0. 오래된 비회원 인생 정리 (C03): 종료 후 1일 또는 7일 미접속
		//    1) 통계용 숫자만 옮기고 → 2) 삭제 (1이 실패하면 에러로 멈춰서 2는 실행 안 됨)
		engineDao.moveOldGuestStats();
		engineDao.deleteOldGuestLives();

		// ① 출생 연도: 고른 연대 안에서 랜덤 (1940년대 ~ 현재, 현재 연도 이후 X)
		int minYear = decade;
		int maxYear = Math.min(decade + 9, LocalDate.now().getYear());
		if (minYear > maxYear) {
			return null; // 고를 수 없는 연대
		}
		int birthYear = minYear + random.nextInt(maxYear - minYear + 1);

		// ② 나라: 출생아 수가 많을수록 확률 높게
		List<CountryStatDTO> list = engineDao.selectBirthCountries(decade);
		if (list.isEmpty()) {
			return null; // 그 연대 통계 데이터가 없음
		}

		long total = 0;
		for (CountryStatDTO c : list) {
			total += c.getBirths();
		}

		long pick = (long) (random.nextDouble() * total);
		long sum = 0;
		CountryStatDTO country = list.get(list.size() - 1);
		for (CountryStatDTO c : list) {
			sum += c.getBirths();
			if (pick < sum) {
				country = c;
				break;
			}
		}

		// ③ 성별: 출생 성비 (여아 1당 남아), 없으면 1.05
		double ratio = 1.05;
		if (country.getSexRatioBirth() != null) {
			ratio = country.getSexRatioBirth();
		}
		String gender = random.nextDouble() < ratio / (1 + ratio) ? "M" : "F";

		// ④ 가정형편 1~5, 도시·시골 (도시화율 기준, 없으면 50%)
		int wealthClass = random.nextInt(5) + 1;
		double urbanRate = 50;
		if (country.getUrbanRate() != null) {
			urbanRate = country.getUrbanRate();
		}
		String isUrban = random.nextDouble() * 100 < urbanRate ? "Y" : "N";

		// ⑤ 능력치 시작값 (원작 기준)
		//    건강 = 78 + (가정형편-3)×3 - 5세 미만 사망률(‰)÷12 + 랜덤 -6~6 (40~95)
		//    행복 = 60 + 랜덤 -8~8, 지식 = 50 (또래 가운데 가운데), 관계 = 55 + 랜덤 -6~6
		double u5 = u5Of(country);
		double startHealth = clamp(78 + (wealthClass - 3) * 3 - u5 / 12 + (random.nextInt(13) - 6), 40, 95);

		LifeDTO life = new LifeDTO();
		life.setBirthYear(birthYear);
		life.setBirthCountry(country.getCountryCode());
		life.setCurrentCountry(country.getCountryCode());
		life.setGender(gender);
		life.setWealthClass(wealthClass);
		life.setOriginClass(wealthClass);
		life.setIsUrban(isUrban);
		life.setCurrentAge(0);
		life.setStatus("PLAYING");
		life.setEduLevel("NONE");
		life.setHealth(startHealth);
		life.setHappiness(60 + random.nextInt(17) - 8);
		life.setKnowledge(50);
		life.setRelation(55 + random.nextInt(13) - 6);

		// ⑥ 이름 (목록이 비어 있으면 '무명')
		String name = engineDao.selectRandomName(life);
		if (name == null) {
			name = "무명";
		}
		life.setName(name);

		// ⑦ 회원이면 회원 번호, 비회원이면 새 임시 토큰 발급
		if (memberId != null) {
			life.setStartType("MEMBER");
			life.setMemberId(memberId);
		} else {
			life.setStartType("GUEST");
			life.setGuestToken(UUID.randomUUID().toString());
		}

		// ⑧ 번호 받고 저장 + 0세 기록
		life.setLifeId(engineDao.selectNextLifeId());
		engineDao.insertLife(life);
		engineDao.insertLifeTraits(life); // C13 특성 레벨 저장 (회원 특성, 비회원은 모두 1)
		engineDao.insertYearLog(life);

		// 출생 화면 표시용
		life.setCountryName(country.getNameKo());
		life.setHistName(engineDao.selectHistName(life));
		life.setLifeExpectancy(country.getLifeExpectancy());
		life.setInfantMortality(country.getInfantMortality());
		life.setU5Mortality(country.getU5Mortality());
		life.setGdpPerCapita(country.getGdpPerCapita());
		fillRegion(life);

		return life;
	}

	// [화면용] 지금 사는 나라의 권역 (C08 마을 그림 고르기, 이민하면 바뀜)
	// 화면용: 지금 사는 나라의 권역(C08 마을)과 한글 이름 (이어하기·진행 응답에도 나라 이름이 나오게)
	private void fillRegion(LifeDTO life) {
		if (life == null) {
			return;
		}
		CountryStatDTO s = yearStat(life);
		life.setRegionCode(s == null ? null : s.getRegionCode());
		if (s != null && s.getNameKo() != null) {
			life.setCountryName(s.getNameKo());
		}
		// 일하는 중이면 그 일의 산업 (윤아님 마을 일터 그림: 논밭 · 공장 · 사무실·가게)
		JobDTO nowJob = life.getJobId() == null ? null : findJob(life.getJobId());
		life.setJobIndustry("EMPLOYED".equals(life.getJobStatus()) && nowJob != null ? nowJob.getIndustry() : null);
		if (life.getJobName() == null && nowJob != null) {
			life.setJobName(nowJob.getJobName());
		}
		fillView(life, s); // 한국 체감 원·소득 위치·가족과 삶·같은 1초에 태어난 사람들
		fillWorth(life, s); // 재산 합계: 내 집 시세 · 탈것 중고 시세
	}


	// ================= C03 진행 화면: 한국 체감 돈·살림 내역·가족과 삶·같은 1초에 태어난 사람들 =================

	// 달러 → '한국 체감' 원 (원작 KRW): 구매력(PPP) 달러 × 850원
	//   그 나라 물가 수준은 1인당 GDP로 계산 (toPpp(GDP) ÷ GDP) → 같은 달러라도 물가 싼 나라에선 더 큰돈
	private static final double KRW = 850;

	private double krwPerUsd(CountryStatDTO stat) {
		double gdp = gdpOf(stat);
		return toPpp(gdp) / gdp * KRW;
	}

	// 원작 wonStr: 1억 이상 "1.2억 원", 1만 이상 "34만 원", 그 아래 "8,000원"
	static String wonStr(double won) {
		double v = Math.abs(won);
		if (v >= 1e8) {
			double e = v / 1e8;
			return (e >= 10 ? String.format("%,d", Math.round(e)) : String.format("%.1f", e).replace(".0", "")) + "억 원";
		}
		if (v >= 1e4) {
			return String.format("%,d", Math.round(v / 1e4)) + "만 원";
		}
		return String.format("%,d", Math.round(v / 100) * 100) + "원";
	}

	// 달러 금액을 지금 사는 나라 기준 한국 체감 원으로 ("34만 원")
	private String won(LifeDTO life, double usd) {
		return wonStr(usd * krwPerUsd(yearStat(life)));
	}

	// 부호 붙인 한국 체감 원 ("+34만 원", "−4만 원")
	private String wonSigned(LifeDTO life, double usd) {
		return (usd < 0 ? "−" : "+") + won(life, usd);
	}

	// 지난 1년 순자산 내역에 한 줄 더하기 (같은 이름은 합침)
	private void book(LifeDTO life, String label, double amount) {
		long a = Math.round(amount);
		if (a == 0 || life.getMoneyFlow() == null) {
			return;
		}
		for (MoneyLineDTO m : life.getMoneyFlow()) {
			if (m.getLabel().equals(label)) {
				m.setAmount(m.getAmount() + a);
				return;
			}
		}
		life.getMoneyFlow().add(new MoneyLineDTO(label, a));
	}

	// 한 해를 마칠 때: 순자산 변화 - 적어 둔 줄 = '그 밖의 일', 0원 줄은 지우고 큰 순서로
	private void closeBook(LifeDTO life, long startAssets) {
		if (life.getMoneyFlow() == null) {
			return;
		}
		long delta = life.getAssets() - startAssets;
		long sum = 0;
		for (MoneyLineDTO m : life.getMoneyFlow()) {
			sum += m.getAmount();
		}
		if (Math.abs(delta - sum) > 3) { // 반올림 차이(몇 달러)는 버림
			book(life, "그 밖의 일", delta - sum);
		}
		life.getMoneyFlow().removeIf(m -> m.getAmount() == 0);
		life.getMoneyFlow().sort((a, b) -> Long.compare(Math.abs(b.getAmount()), Math.abs(a.getAmount())));
		life.setMoneyDelta(delta);
	}

	// 일상 문장 틀 → 살림 내역 이름
	private static String storyLabel(String id) {
		if (id == null) {
			return "그 밖의 지출";
		}
		if (id.startsWith("cer_wedding")) return "결혼식 축의금";
		if (id.startsWith("cer_funeral")) return "부조금";
		if (id.startsWith("cer_baby")) return "돌잔치 선물";
		if (id.startsWith("cer_holiday")) return "명절 선물·용돈";
		if (id.startsWith("cer_reunion")) return "모임 회비";
		if (id.startsWith("cer_kidschool")) return "아이 학원비";
		if (id.startsWith("work_parents")) return "부모님 용돈";
		if (id.equals("exp_dentist")) return "치과";
		if (id.equals("exp_appliance") || id.equals("exp_tv")) return "가전 고장";
		if (id.equals("exp_pipe") || id.equals("exp_roof")) return "집 수리";
		if (id.equals("exp_car")) return "자동차 수리";
		if (id.equals("exp_trip")) return "가족 여행";
		if (id.equals("exp_parent_hosp")) return "부모님 병원비";
		if (id.equals("exp_glasses")) return "돋보기";
		if (id.equals("exp_cane")) return "지팡이";
		return "그 밖의 지출";
	}

	// 소득 위치: 등록된 나라 전체에서 (그 연대 출생아 수로 가중, 원작 worldPct를 아시아 판으로)
	private final Map<Integer, List<CountryStatDTO>> birthListCache = new ConcurrentHashMap<>();

	private List<CountryStatDTO> birthList(int decade) {
		List<CountryStatDTO> list = birthListCache.get(decade);
		if (list == null) {
			list = engineDao.selectBirthCountries(decade);
			if (list == null) {
				list = new ArrayList<>();
			}
			birthListCache.put(decade, list);
		}
		return list;
	}

	private Double asiaPct(double income, int year) {
		List<CountryStatDTO> list = birthList(Math.min(year, LocalDate.now().getYear()) / 10 * 10);
		double w = 0, sum = 0;
		for (CountryStatDTO c : list) {
			if (c.getBirths() == null || c.getBirths() <= 0) {
				continue;
			}
			double p = normCdf(Math.log(Math.max(1, income) / gdpOf(c)) / sigmaOf(c.getCountryCode(), c));
			sum += p * c.getBirths();
			w += c.getBirths();
		}
		return w > 0 ? sum / w : null;
	}

	// 진행 화면에 함께 보내는 값 (fillRegion 끝에서)
	private void fillView(LifeDTO life, CountryStatDTO stat) {
		life.setKrwPerUsd(krwPerUsd(stat));
		// 직업 이름: DB(LIFE)에는 JOB_ID 만 있어서, 다시 읽은 인생(1년 진행·이어하기)에는 이름이 비어 화면에 '일하는 중'만 떴음 → 직업 목록에서 채움
		if (life.getJobId() != null && life.getJobName() == null) {
			for (JobDTO j : jobs()) {
				if (j.getJobId().equals(life.getJobId())) {
					life.setJobName(j.getJobName());
					break;
				}
			}
		}
		// 이 인생에서 고른 퀘스트 (읽기만, 비회원·퀘스트 없는 인생은 빈 목록)
		try {
			life.setQuests(engineDao.selectLifeQuests(life.getLifeId()));
		} catch (RuntimeException e) {
			// 퀘스트 표가 없어도 진행은 되게
		}
		// 태어난 해 기대수명 (원작 e0): 화면의 '1년 ≈ n초'·인생 지도 기대수명 칸에 씀
		//   DB에서 다시 읽은 인생(1년 진행·이어하기)에는 이 값이 없어서 태어난 나라·연대 통계에서 채움
		if (life.getLifeExpectancy() == null) {
			for (CountryStatDTO c : birthList(life.getBirthYear() / 10 * 10)) {
				if (c.getCountryCode().equals(life.getBirthCountry())) {
					life.setLifeExpectancy(c.getLifeExpectancy());
					life.setInfantMortality(c.getInfantMortality());
					life.setU5Mortality(c.getU5Mortality());
					life.setGdpPerCapita(c.getGdpPerCapita());
					break;
				}
			}
		}
		fillQuestLive(life);
		double med = medIncome(life.getCurrentCountry(), stat);
		// 배우자 소득: 배우자 직업 소득 배율 × 1인당 GDP (원작처럼 직업에 맞춤, 예전엔 누구나 중위 소득)
		//   집안일·육아를 맡았거나 배우자가 은퇴 나이면 없음
		Long spInc = null;
		if ("Y".equals(life.getMarriedYn())) {
			try {
				FamSeed fs = famSeed(life, stat);
				int sa = Math.max(16, life.getCurrentAge() + fs.spouseGap);
				double rate = spouseRate(life, fs);
				if (sa < retireAge(life, stat) && rate > 0) {
					spInc = Math.round(jobPay(gdpOf(stat), rate, life.getCurrentCountry(), stat));
				}
			} catch (RuntimeException e) {
				spInc = Math.round(med); // 통계가 모자라면 예전처럼 중위 소득
			}
		}
		life.setSpouseIncome(spInc);
		boolean partner = spInc != null;
		double inc = life.getIncome() > 0 ? life.getIncome() : partner ? spInc : 0;
		if (inc > 0 && life.getCurrentAge() >= 15) {
			life.setIncomeRankCountry(incomePct(inc, life.getCurrentCountry(), stat));
			life.setIncomeRankAsia(asiaPct(inc, life.getBirthYear() + life.getCurrentAge()));
		}
		try {
			life.setFamily(family(life, stat));
			life.setPeers(peers(life));
		} catch (RuntimeException e) {
			// 이름·통계가 없어도 진행은 되게 (화면에서 칸만 비움)
		}
	}

	// ---------- 이름 ----------
	private final Map<String, List<String>> nameCache = new ConcurrentHashMap<>();

	private List<String> names(String cc, String gender) {
		String key = cc + "_" + gender;
		List<String> list = nameCache.get(key);
		if (list == null) {
			Map<String, Object> cond = new HashMap<>();
			cond.put("countryCode", cc);
			cond.put("gender", gender);
			list = engineDao.selectNameList(cond);
			if (list == null) {
				list = new ArrayList<>();
			}
			nameCache.put(key, list);
		}
		return list;
	}

	private String pickName(String cc, String gender, java.util.SplittableRandom r) {
		List<String> list = names(cc, gender);
		if (list.isEmpty()) {
			return null;
		}
		return list.get(r.nextInt(list.size()));
	}

	// 성을 물려받는 나라 (원작처럼 아버지와 성이 같게)
	private static final java.util.Set<String> SURNAME_CC = java.util.Set.of("KOR", "PRK", "CHN", "TWN", "HKG", "MAC", "VNM", "JPN", "SGP");

	// [성, 이름] 나누기: 띄어 쓴 이름은 첫 낱말, 붙여 쓴 한·중 이름은 첫 글자
	private static String[] splitName(String full) {
		if (full == null) {
			return new String[] { "", "" };
		}
		int sp = full.indexOf(' ');
		if (sp > 0) {
			return new String[] { full.substring(0, sp), full.substring(sp + 1) };
		}
		if (full.length() >= 2) {
			return new String[] { full.substring(0, 1), full.substring(1) };
		}
		return new String[] { "", full };
	}

	private static String joinName(String sur, String given, String sample) {
		boolean spaced = sample != null && sample.indexOf(' ') > 0;
		return spaced ? sur + " " + given : sur + given;
	}

	// ---------- 가족과 삶 ----------
	// 인생 번호로 늘 같게 정해 두는 가족 바탕 (서버 메모리)
	private static final class FamSeed {
		String mother, father, spouse;
		int motherAt, fatherAt;          // 내가 태어날 때 부모 나이
		int motherDie, fatherDie;        // 부모가 떠날 나이 (부모 생존 N일 때 누가 먼저인지)
		String motherJob, fatherJob, spouseJob;
		int spouseGap;                   // 배우자 나이 - 내 나이
		String spouse0, job0;            // 첫 만남 상대 (예전 인생과 같은 사람)
		int gap0;
		int spouseK = 0;                 // 지금 배우자 자리에 있는 만남 번호 (syncSpouse)
		int[] siblingGap;                // 형제자매 나이 - 내 나이 (+ 손위, − 손아래)
		String[] siblingSex;
	}

	private final Map<Integer, FamSeed> famCache = new ConcurrentHashMap<>();

	// 배우자 직업별 소득 배율 (1인당 GDP 기준, 직업 표 INCOME_RATE 와 같은 눈금) · 가사·육아는 0
	private static final Map<String, Double> SPOUSE_RATE = Map.ofEntries(
			Map.entry("가사·육아", 0.0), Map.entry("농사일", 0.45), Map.entry("날품팔이", 0.4), Map.entry("시장 행상", 0.5),
			Map.entry("소작농", 0.4), Map.entry("농업 노동자", 0.45), Map.entry("어부", 0.6), Map.entry("자작농", 0.75),
			Map.entry("시장 상인", 0.75), Map.entry("재봉사", 0.65), Map.entry("목수", 0.8), Map.entry("교사", 1.1),
			Map.entry("상점 주인", 1.2), Map.entry("면사무소 직원", 1.0), Map.entry("지주", 2.0), Map.entry("농장 주인", 1.6), Map.entry("공무원", 1.1),
			Map.entry("공장 노동자", 0.8), Map.entry("건설 노동자", 0.8), Map.entry("노점상", 0.5), Map.entry("청소 노동자", 0.55),
			Map.entry("운전기사", 0.85), Map.entry("사무원", 1.0), Map.entry("간호사", 1.1), Map.entry("미용사", 0.8),
			Map.entry("기술자", 1.1), Map.entry("상인", 0.9), Map.entry("버스 기사", 0.9), Map.entry("회사원", 1.2),
			Map.entry("엔지니어", 1.5), Map.entry("약사", 1.6), Map.entry("의사", 2.5), Map.entry("프로그래머", 1.6),
			Map.entry("사업가", 2.0));

	// 같은 인생은 늘 같은 배율 (직업 배율 × 0.85~1.15)
	private double spouseRate(LifeDTO life, FamSeed f) {
		double k = SPOUSE_RATE.getOrDefault(f.spouseJob, 1.0);
		return k * (0.85 + new java.util.SplittableRandom(life.getLifeId() * 31L + 7 + f.spouseK * 13L).nextDouble() * 0.3);
	}

	// 결혼한 해에 결혼 상대 소개 한 줄 (원작: 상대의 나이·직업·소득) - 한 인생에 한 번
	private void spouseNote(LifeDTO life, int age) {
		if (!"Y".equals(life.getMarriedYn()) || !"PLAYING".equals(life.getStatus())) {
			return;
		}
		EngState s = engState(life);
		if (s.spouseNoted) {
			return;
		}
		s.spouseNoted = true;
		try {
			CountryStatDTO stat = yearStat(life);
			FamSeed f = famSeed(life, stat);
			int sa = Math.max(16, age + f.spouseGap);
			double rate = spouseRate(life, f);
			String t = "결혼 상대 " + f.spouse + ": " + sa + "세 · "
					+ (rate > 0 ? f.spouseJob + " · 월 소득 약 " + won(life, jobPay(gdpOf(stat), rate, life.getCurrentCountry(), stat) / 12) + " (한국 체감)"
							: "집안일과 아이 돌보기를 맡기로 했다");
			saveEventLog(life, age, null, null, t);
			addYearNote(life, t);
		} catch (RuntimeException e) {
			// 이름·통계가 없으면 소개 줄만 건너뜀
		}
	}

	private static String pick(java.util.SplittableRandom r, String... a) {
		return a[r.nextInt(a.length)];
	}

	// 부모 직업 (집안 형편·도시·시대로)
	private static String parentJob(java.util.SplittableRandom r, int wc, boolean urban, int year, boolean mother) {
		if (mother) {
			double home = year < 1965 ? 0.7 : year < 1985 ? 0.55 : year < 2000 ? 0.4 : 0.3;
			if (r.nextDouble() < home) {
				return "가사·육아";
			}
		}
		if (!urban) {
			if (wc <= 2) return mother ? pick(r, "농사일", "날품팔이", "시장 행상") : pick(r, "소작농", "농업 노동자", "날품팔이", "어부");
			if (wc == 3) return mother ? pick(r, "농사일", "시장 상인", "재봉사") : pick(r, "자작농", "시장 상인", "목수", "어부");
			return mother ? pick(r, "교사", "상점 주인", "면사무소 직원")
					: year < 1980 ? pick(r, "지주", "상점 주인", "공무원", "교사") : pick(r, "농장 주인", "상점 주인", "공무원", "교사");
		}
		if (wc <= 2) return mother ? pick(r, "공장 노동자", "재봉사", "노점상", "청소 노동자") : pick(r, "공장 노동자", "건설 노동자", "노점상", "운전기사");
		if (wc == 3) return mother ? pick(r, "사무원", "간호사", "시장 상인", "미용사") : pick(r, "사무원", "기술자", "상인", "버스 기사");
		if (year >= 1990) return mother ? pick(r, "교사", "회사원", "약사", "공무원") : pick(r, "회사원", "엔지니어", "공무원", "의사", "프로그래머");
		return mother ? pick(r, "교사", "간호사", "공무원") : pick(r, "공무원", "교사", "엔지니어", "의사", "사업가");
	}

	// 형제자매 수의 바탕: 시대·권역별 어머니 한 명의 평균 자녀 수
	private double kidsOfEra(String cc, CountryStatDTO stat, int year) {
		double k = year < 1960 ? 5.5 : year < 1975 ? 4.2 : year < 1990 ? 3.0 : 2.3;
		String g = groupOf(cc, stat);
		boolean east = java.util.Set.of("KOR", "CHN", "JPN", "TWN", "HKG", "MAC", "SGP", "PRK").contains(cc);
		if (east && year >= 1975) {
			k = k * 0.6;
		} else if (g.equals("hi")) {
			k = k * 0.7;
		}
		return k;
	}

	private FamSeed famSeed(LifeDTO life, CountryStatDTO stat) {
		FamSeed f = famCache.get(life.getLifeId());
		if (f != null) {
			syncSpouse(life, f);
			return f;
		}
		if (famCache.size() > 20000) {
			famCache.clear();
		}
		java.util.SplittableRandom r = new java.util.SplittableRandom(life.getLifeId() * 7919L + 101);
		String cc = life.getBirthCountry();
		int by = life.getBirthYear();
		boolean urban = "Y".equals(life.getIsUrban());
		f = new FamSeed();

		// 이름: 아버지는 내 성을 물려줌 (성 쓰는 나라), 일본은 어머니도 같은 성
		String me = life.getName();
		String dad = pickName(cc, "M", r);
		String mom = pickName(cc, "F", r);
		if (SURNAME_CC.contains(cc) && me != null) {
			String sur = splitName(me)[0];
			if (dad != null) {
				dad = joinName(sur, splitName(dad)[1], me);
			}
			if ("JPN".equals(cc) && mom != null) {
				mom = joinName(sur, splitName(mom)[1], me);
			}
		} else if (me != null && me.indexOf(' ') > 0 && dad != null && dad.indexOf(' ') > 0) {
			// 띄어 쓰는 이름 (인도·아랍 …): 아버지도 내 집안 이름(마지막 낱말)을 씀 (예전엔 아버지 성이 나와 달랐음)
			dad = dad.substring(0, dad.indexOf(' ')) + me.substring(me.lastIndexOf(' '));
		}
		f.father = dad;
		f.mother = mom;

		// 나이: 원작 500명 평균 어머니 27.8세 (옛날일수록 조금 어리게), 아버지는 2~6세 위
		double mAvg = 25.5 + (by - 1940) * 0.04;
		f.motherAt = (int) Math.round(clamp(mAvg + r.nextGaussian() * 5, 16, 44));
		f.fatherAt = f.motherAt + 2 + r.nextInt(5);
		// 기대수명은 태어난 해·나라 통계로 (예전엔 처음 부른 해의 통계라, 서버를 재시작하면 부모님 수명이 바뀔 수 있었음
		//   → 이제 이 수명으로 별세 해를 정하므로 늘 같게)
		CountryStatDTO bst = null;
		try {
			for (CountryStatDTO c : birthList(by / 10 * 10)) {
				if (c.getCountryCode().equals(cc)) {
					bst = c;
					break;
				}
			}
		} catch (RuntimeException e) {
			bst = null;
		}
		CountryStatDTO lst = bst != null && bst.getLifeExpectancy() != null ? bst : stat;
		double le = lst != null && lst.getLifeExpectancy() != null ? lst.getLifeExpectancy() : 65;
		f.motherDie = (int) Math.round(clamp(le + 8 + r.nextGaussian() * 12, f.motherAt + 1, 100));
		f.fatherDie = (int) Math.round(clamp(le + 4 + r.nextGaussian() * 12, f.fatherAt + 1, 100));

		f.motherJob = parentJob(r, originOf(life), urban, by, true);
		f.fatherJob = parentJob(r, originOf(life), urban, by, false);

		// 형제자매: 어머니 자녀 수 - 나, 나는 그 가운데 아무 자리
		double kids = kidsOfEra(cc, stat, by);
		int n = 0;
		double L = Math.exp(-(kids - 1)), p = 1;
		while ((p = p * r.nextDouble()) > L && n < 9) {
			n++;
		}
		int older = n == 0 ? 0 : r.nextInt(n + 1);
		f.siblingGap = new int[n];
		f.siblingSex = new String[n];
		int gap = 0;
		for (int i = 0; i < older; i++) {
			gap += 2 + r.nextInt(2);
			f.siblingGap[i] = gap;
		}
		gap = 0;
		for (int i = older; i < n; i++) {
			gap += 2 + r.nextInt(2);
			f.siblingGap[i] = -gap;
		}
		for (int i = 0; i < n; i++) {
			f.siblingSex[i] = r.nextDouble() < 0.51 ? "M" : "F";
		}

		// 배우자: 남자는 2~4살 아래, 여자는 2~4살 위인 사람과 (원작 평균)
		String sg = "M".equals(life.getGender()) ? "F" : "M";
		f.spouseGap = ("M".equals(life.getGender()) ? -1 : 1) * (1 + r.nextInt(4));
		// 태어난 나라 이름으로 (사는 나라로 하면 이민 뒤 서버를 다시 켤 때마다 배우자 이름이 바뀌었음)
		f.spouse = pickName(life.getBirthCountry(), sg, r);
		f.spouseJob = parentJob(r, Math.max(2, life.getWealthClass()), urban, by + 25, "F".equals(sg));
		f.spouse0 = f.spouse;
		f.gap0 = f.spouseGap;
		f.job0 = f.spouseJob;

		famCache.put(life.getLifeId(), f);
		syncSpouse(life, f);
		return f;
	}

	private static String sibWord(String meGender, String sibSex, boolean older) {
		boolean meM = "M".equals(meGender);
		if (!older) {
			return "M".equals(sibSex) ? "남동생" : "여동생";
		}
		if ("M".equals(sibSex)) {
			return meM ? "형" : "오빠";
		}
		return meM ? "누나" : "언니";
	}

	// 같은 호칭이 여럿일 때: 1번째 = 큰누나·큰형 (동생은 '큰 남동생'), 2번째부터 둘째 누나 · 셋째 누나 …
	private static final String[] SIB_ORD = { "둘째", "셋째", "넷째", "다섯째", "여섯째", "일곱째", "여덟째", "아홉째" };

	private static String sibOrd(String word, int k) {
		if (k == 1) {
			return word.endsWith("동생") ? "큰 " + word : "큰" + word;
		}
		return (k - 2 < SIB_ORD.length ? SIB_ORD[k - 2] : k + "번째") + " " + word;
	}

	private List<FamilyMemberDTO> family(LifeDTO life, CountryStatDTO stat) {
		FamSeed f = famSeed(life, stat);
		int age = life.getCurrentAge();
		List<FamilyMemberDTO> out = new ArrayList<>();
		// 사이(원작 bondWord)는 사람마다 조금씩 다르게: 인생 번호 + 역할로 늘 같은 흔들림
		java.util.SplittableRandom br = new java.util.SplittableRandom(life.getLifeId() * 31337L + 17);

		// 부모: 떠나신 분은 OWNS 의 PAR-M-내나이 / PAR-F-내나이 (11/23 윤아님과 합의, parentYear · PARENT_DEATH_*)
		//   부모 생존 N이면 두 분 다 떠나신 것 (예전 인생처럼 토큰이 없으면 정해 둔 수명으로 그때 나이)
		java.util.LinkedHashSet<String> pown = ownSet(life);
		boolean allGone = "N".equals(life.getParentAlive());
		int momAt = parAt(pown, "M"), dadAt = parAt(pown, "F");
		boolean momGone = momAt >= 0 || allGone;
		boolean dadGone = dadAt >= 0 || allGone;
		int momDie = momAt >= 0 ? f.motherAt + momAt : f.motherDie;
		int dadDie = dadAt >= 0 ? f.fatherAt + dadAt : f.fatherDie;
		double momB = br.nextDouble(), dadB = br.nextDouble();
		out.add(parentRow("어머니", f.mother, f.motherAt, momDie, f.motherJob, momGone, age).with(null, momGone ? null : bondWord(life, momB, 8)));
		out.add(parentRow("아버지", f.father, f.fatherAt, dadDie, f.fatherJob, dadGone, age).with(null, dadGone ? null : bondWord(life, dadB, 0)));

		// 형제자매: 손위는 늘, 손아래는 태어난 뒤부터 / 24세가 넘으면 집을 떠남 (원작 sibsLeft)
		//   같은 호칭이 여럿이면 나이 많은 순으로 큰누나 · 둘째 누나 · 셋째 누나 (11/23 동현님: '누나 누나 누나'가 어색함)
		List<Object[]> sibs = new ArrayList<>(); // {호칭, 나이}
		int left = 0;
		for (int i = 0; i < f.siblingGap.length; i++) {
			int g = f.siblingGap[i];
			int sAge = age + g;
			if (sAge < 0) {
				continue;
			}
			sibs.add(new Object[] { sibWord(life.getGender(), f.siblingSex[i], g > 0), sAge });
			if (sAge >= 24 && age >= 6) {
				left++;
			}
		}
		sibs.sort((a, b) -> (Integer) b[1] - (Integer) a[1]);
		Map<String, Integer> wordN = new HashMap<>();
		for (Object[] x : sibs) {
			wordN.merge((String) x[0], 1, Integer::sum);
		}
		Map<String, Integer> wordI = new HashMap<>();
		List<String> sib = new ArrayList<>();
		for (Object[] x : sibs) {
			String w = (String) x[0];
			int k = wordI.merge(w, 1, Integer::sum);
			sib.add((wordN.get(w) > 1 ? sibOrd(w, k) : w) + " " + x[1] + "세");
		}
		String sibText = sib.isEmpty() ? "없음" : sib.size() <= 7 ? String.join(" · ", sib) : sib.size() + "명";
		if (left > 0) {
			sibText = sibText + " · " + (left == sib.size() ? "모두 집을 떠남" : left + "명은 집을 떠남");
		}
		out.add(new FamilyMemberDTO("형제자매", null, null, null, true).with(sibText, sib.isEmpty() ? null : bondWord(life, br.nextDouble(), 0)));

		// 배우자: 이름 · 성별 · 나이 · 사이, 아랫줄 직업 (원작처럼 없으면 줄을 안 띄움)
		double spB = br.nextDouble();
		if ("Y".equals(life.getMarriedYn())) {
			int sa = Math.max(16, age + f.spouseGap);
			String job = sa >= retireAge(life, stat) ? "은퇴 · 전 " + f.spouseJob : f.spouseJob;
			if (life.getSpouseIncome() != null && life.getSpouseIncome() > 0) {
				job = job + " · 월 " + won(life, life.getSpouseIncome() / 12.0); // 원작처럼 배우자 소득도
			}
			String sex = "M".equals(life.getGender()) ? "여성" : "남성";
			out.add(new FamilyMemberDTO("배우자", f.spouse, sa, job, true).with(sex, bondWord(life, spB, 10)));
		}

		// 자녀: 한 명씩 아들·딸, 이름(성 쓰는 나라는 아버지 성), 나이 (태어난 해는 CHILD 이벤트 기록에서)
		//   학생이면 학교, 어른이 되면 결혼·아이 수 (원작 kidLine) → 아이들의 아이는 '손주' 줄로 모음
		List<Integer> grand = new ArrayList<>();
		if (life.getChildCount() > 0) {
			List<Integer> births = engineDao.selectChildBirthAges(life.getLifeId());
			java.util.SplittableRandom r = new java.util.SplittableRandom(life.getLifeId() * 104729L + 7);
			// 이름은 태어난 나라(집안) 이름으로: 이민 간 뒤에도 부모 집안 이름을 따름 (배우자 이름도 같은 나라에서 정해짐)
			String cc = life.getBirthCountry();
			String dadName = "M".equals(life.getGender()) ? life.getName() : f.spouse;
			for (int i = 0; i < life.getChildCount(); i++) {
				String sex = r.nextDouble() < 0.51 ? "M" : "F";
				String nm = pickName(cc, sex, r);
				if (nm != null && dadName != null) {
					if (SURNAME_CC.contains(cc)) {
						nm = joinName(splitName(dadName)[0], splitName(nm)[1], nm); // 한·중·일 …: 아버지 성 + 이름
					} else if (dadName.indexOf(' ') > 0 && nm.indexOf(' ') > 0) {
						// 띄어 쓰는 이름 (아랍·서양 …): 아이 이름 + 아버지 집안 이름 (형제끼리 같은 집안 이름)
						nm = nm.substring(0, nm.indexOf(' ')) + dadName.substring(dadName.lastIndexOf(' '));
					}
				}
				Integer born = births != null && i < births.size() ? births.get(i) : null;
				Integer ka = born == null ? null : Math.max(0, age - born - 1); // 그해 기록 나이 = 태어난 해의 내 나이

				// 이 아이의 앞날 (아이 번호로 늘 같게): 결혼 나이, 아이 수, 아이 터울
				java.util.SplittableRandom kr = new java.util.SplittableRandom(life.getLifeId() * 7727L + i * 131L + 3);
				boolean willMarry = kr.nextDouble() < 0.82;
				int marryAt = 24 + kr.nextInt(10);
				double u = kr.nextDouble();
				int gkN = u < 0.12 ? 0 : u < 0.40 ? 1 : u < 0.78 ? 2 : 3;
				List<String> info = new ArrayList<>();
				String school = kidSchool(ka);
				if (school != null) {
					info.add(school);
				}
				if (ka != null && willMarry && ka >= marryAt) {
					info.add("기혼");
					int at = marryAt + 1 + kr.nextInt(2), born2 = 0;
					for (int k = 0; k < gkN; k++) {
						int ga = ka - at;
						if (ga >= 0) {
							grand.add(ga);
							born2++;
						}
						at += 2 + kr.nextInt(2);
					}
					if (born2 > 0) {
						info.add(born2 == 1 ? "아이 한 명" : "아이 " + born2 + "명");
					}
				}
				FamilyMemberDTO row = new FamilyMemberDTO("M".equals(sex) ? "아들" : "딸", nm, ka, childLine(ka), true);
				out.add(row.with(info.isEmpty() ? null : String.join(" · ", info), ka != null && ka >= 6 ? bondWord(life, kr.nextDouble(), 6) : null));
			}
		}

		// 친구 (12세부터, 원작 frNames): 관계가 좋을수록 많고, 70세가 넘으면 하나둘 먼저 떠남
		if (age >= 12) {
			java.util.SplittableRandom fr = new java.util.SplittableRandom(life.getLifeId() * 4421L + 29);
			int n = (int) Math.round(clamp(0.6 + life.getRelation() / 28.0 + fr.nextGaussian() * 0.5, 0, 4));
			List<String> alive = new ArrayList<>();
			int gone = 0;
			for (int i = 0; i < n; i++) {
				String sx = fr.nextDouble() < 0.75 ? life.getGender() : ("M".equals(life.getGender()) ? "F" : "M");
				String nm = pickName(life.getBirthCountry(), sx, fr);
				int dieAt = (int) Math.round(clamp(78 + fr.nextGaussian() * 9, 55, 102));
				if (nm == null) {
					continue;
				}
				// 성이 없는 나라는 첫 낱말(이름)만, 그런데 그게 한 글자이거나 이미 있는 이름이면 전체 이름
				//   (11/23 동현님: 미얀마 같은 나라에서 '예, 예, 예'처럼 첫 낱말만 겹쳐 나왔음)
				String call = SURNAME_CC.contains(life.getBirthCountry()) ? nm : nm.split(" ")[0];
				if (call.length() < 2 || alive.contains(call)) {
					call = nm;
				}
				if (alive.contains(call)) {
					continue; // 같은 이름 친구는 한 번만
				}
				if (age >= dieAt) {
					gone++;
				} else {
					alive.add(call);
				}
			}
			if (n > 0) {
				String text;
				if (alive.isEmpty()) {
					text = "오랜 친구들이 모두 곁을 떠났습니다";
				} else if (gone > 0) {
					text = (alive.size() == 1 ? "곁에 남은 오랜 친구 한 명" : "곁에 남은 오랜 친구 " + alive.size() + "명") + " · " + String.join(", ", alive);
				} else {
					text = String.join(", ", alive);
				}
				out.add(new FamilyMemberDTO("친구", null, null, null, !alive.isEmpty())
						.with(text, alive.isEmpty() ? null : bondWord(life, fr.nextDouble(), 4)));
			}
		}

		// 손주 (원작: n명 · 나이들)
		if (!grand.isEmpty()) {
			grand.sort(null);
			String ages = grand.size() <= 6
					? grand.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining("·")) + "세"
					: grand.get(0) + "~" + grand.get(grand.size() - 1) + "세";
			out.add(new FamilyMemberDTO("손주", null, null, null, true).with(grand.size() + "명 · " + ages, null));
		}
		return out;
	}

	// 나와의 사이 (원작 bondWord): 내 관계 능력치 + 사람마다 흔들림(0~1 → ±18) + 역할 덤
	//   6세 전에는 비움
	private static String bondWord(LifeDTO life, double jitter, double plus) {
		if (life.getCurrentAge() < 6) {
			return null;
		}
		double s = life.getRelation() + (jitter - 0.5) * 36 + plus;
		return s >= 62 ? "가까운 사이" : s >= 38 ? "무난한 사이" : "서먹한 사이";
	}

	// 아이가 다니는 학교 (한국 학제: 7~12 초등, 13~15 중학, 16~18 고등)
	private static String kidSchool(Integer a) {
		if (a == null || a < 7 || a > 18) {
			return null;
		}
		return a <= 12 ? "초등학생" : a <= 15 ? "중학생" : "고등학생";
	}

	private static String childLine(Integer a) {
		if (a == null) {
			return "";
		}
		if (a < 1) {
			return "갓난아기";
		}
		if (a < 7) {
			return "아기";
		}
		if (a < 19) {
			return null; // 학생은 info 에 학교로
		}
		return null;
	}

	private FamilyMemberDTO parentRow(String role, String name, int at, int die, String job, boolean gone, int myAge) {
		if (gone) {
			int d = Math.min(die, at + myAge);
			return new FamilyMemberDTO(role, name, d, d + "세에 세상을 떠남", false);
		}
		int pa = at + myAge;
		// 65세가 넘으면 은퇴 (원작: 은퇴 · 전 직업), 가사·육아는 그대로
		String j = pa >= 65 && job != null && !job.startsWith("가사") ? "은퇴 · 전 " + job : job;
		return new FamilyMemberDTO(role, name, pa, j, true);
	}

	// ---------- 같은 1초에 태어난 사람들 (원작 makeCompanions 4명) ----------
	private static final class PeerSeed {
		String name, cc, countryName, gender, cause;
		int dieAge;        // 이 나이를 못 넘김
		int schoolEnd;     // 학교를 마치는 나이
		int marryAge;      // 결혼하는 나이 (99면 안 함)
		int kids;
		boolean home;      // 가사·육아 전담
		String job;        // 일하는 동안의 직업 이름 (학력·권역 산업 비율로 하나, '일하는 중' 대신 보여 줌)
	}

	private final Map<Integer, List<PeerSeed>> peerCache = new ConcurrentHashMap<>();

	private List<PeerSeed> peerSeeds(LifeDTO life) {
		List<PeerSeed> list = peerCache.get(life.getLifeId());
		if (list != null) {
			return list;
		}
		if (peerCache.size() > 20000) {
			peerCache.clear();
		}
		java.util.SplittableRandom r = new java.util.SplittableRandom(life.getLifeId() * 104729L + 7);
		int by = life.getBirthYear();
		List<CountryStatDTO> cs = birthList(by / 10 * 10);
		long total = 0;
		for (CountryStatDTO c : cs) {
			total += c.getBirths() == null ? 0 : c.getBirths();
		}
		list = new ArrayList<>();
		for (int i = 0; i < 4 && total > 0; i++) {
			long pickN = (long) (r.nextDouble() * total);
			CountryStatDTO c = cs.get(cs.size() - 1);
			long sum = 0;
			for (CountryStatDTO x : cs) {
				sum += x.getBirths() == null ? 0 : x.getBirths();
				if (pickN < sum) {
					c = x;
					break;
				}
			}
			PeerSeed p = new PeerSeed();
			p.cc = c.getCountryCode();
			p.countryName = c.getNameKo();
			p.gender = r.nextDouble() < 0.515 ? "M" : "F";
			p.name = pickName(p.cc, p.gender, r);
			if (p.name == null) {
				p.name = "이름 모를 아이";
			}

			// 죽는 나이: 원작 생명표 (해마다 그 연대 통계로)
			LifeDTO ghost = new LifeDTO();
			ghost.setBirthYear(by);
			ghost.setCurrentCountry(p.cc);
			ghost.setBirthCountry(p.cc);
			ghost.setGender(p.gender);
			ghost.setWealthClass(3);
			p.dieAge = 110;
			for (int a = 0; a < 110; a++) {
				ghost.setCurrentAge(a);
				CountryStatDTO s = yearStat(ghost);
				if (s == null) {
					s = c;
				}
				double q = baseDeathProb(p.cc, s.getLifeExpectancy(), s.getInfantMortality(), s.getU5Mortality(), p.gender, a);
				if (r.nextDouble() < q) {
					p.dieAge = a;
					ghost.setCurrentAge(a);
					p.cause = deathCause(ghost);
					break;
				}
			}

			// 학교·결혼·자녀 (나라 형편으로 대충)
			double gdp = gdpOf(c);
			double edu = clamp(4 + 2 * Math.log(Math.max(300, gdp) / 1000) + r.nextGaussian() * 3, 0, 18);
			p.schoolEnd = edu < 1 ? 0 : 7 + (int) Math.round(edu);
			p.marryAge = r.nextDouble() < 0.93 ? (int) Math.round(clamp(("F".equals(p.gender) ? 22 : 26) + r.nextGaussian() * 4, 16, 45)) : 99;
			p.kids = (int) Math.round(clamp(kidsOfEra(p.cc, c, by + 25) + r.nextGaussian() * 1.2 - 0.3, 0, 9));
			double home = by < 1965 ? 0.6 : by < 1985 ? 0.45 : 0.3;
			p.home = "F".equals(p.gender) && r.nextDouble() < home;
			p.job = peerJob(ghost, edu, r);
			list.add(p);
		}
		peerCache.put(life.getLifeId(), list);
		return list;
	}

	// 또래 직업: 마친 학력으로 할 수 있는 직업 중 그 권역 산업 비율(25세 무렵)대로 하나
	//   학교 다닌 햇수 edu → 마친 단계: 6년 초등, 9년 중학교, 12년 고등학교, 16년 대학, 18년 대학원
	private String peerJob(LifeDTO ghost, double edu, java.util.SplittableRandom r) {
		int finished = edu >= 18 ? 4 : edu >= 16 ? 3 : edu >= 12 ? 2 : edu >= 9 ? 1 : edu >= 6 ? 0 : -1;
		ghost.setCurrentAge(25);
		double agri = 40, industry = 25, service = 35;
		RegionStatDTO rs = regionStat(ghost);
		if (rs != null) {
			if (rs.getAgriRate() != null) {
				agri = rs.getAgriRate();
			}
			if (rs.getIndustryRate() != null) {
				industry = rs.getIndustryRate();
			}
			if (rs.getServiceRate() != null) {
				service = rs.getServiceRate();
			}
		}
		// 산업별로 고를 수 있는 직업 수가 달라도 비율이 맞게: 산업 비율 ÷ 그 산업 직업 수
		Map<String, Integer> cnt = new HashMap<>();
		List<JobDTO> pool = new ArrayList<>();
		for (JobDTO j : jobs()) {
			if (levelIndex(j.getRequiredEdu()) <= finished && !j.getJobId().equals("JOB_SOLDIER") && !j.getJobId().equals("JOB_OFFICER")) {
				pool.add(j);
				cnt.merge(j.getIndustry(), 1, Integer::sum);
			}
		}
		if (pool.isEmpty()) {
			return null;
		}
		double total = 0;
		double[] w = new double[pool.size()];
		for (int i = 0; i < pool.size(); i++) {
			String ind = pool.get(i).getIndustry();
			double rate = "AGRI".equals(ind) ? agri : "INDUSTRY".equals(ind) ? industry : service;
			w[i] = rate / cnt.get(ind);
			total += w[i];
		}
		double pick = r.nextDouble() * total;
		for (int i = 0; i < pool.size(); i++) {
			pick -= w[i];
			if (pick < 0) {
				return pool.get(i).getJobName();
			}
		}
		return pool.get(pool.size() - 1).getJobName();
	}

	private List<PeerDTO> peers(LifeDTO life) {
		int age = life.getCurrentAge();
		List<PeerDTO> out = new ArrayList<>();
		for (PeerSeed p : peerSeeds(life)) {
			PeerDTO d = new PeerDTO();
			d.setName(p.name);
			d.setCountryCode(p.cc);
			d.setCountryName(p.countryName);
			d.setGender(p.gender);
			if (p.dieAge < age || p.dieAge == age && "ENDED".equals(life.getStatus())) {
				d.setAlive(false);
				d.setStatus(p.dieAge + "세에 세상을 떠남" + (p.cause == null ? "" : " (" + p.cause + ")"));
			} else {
				d.setStatus(peerStatus(p, age));
			}
			out.add(d);
		}
		return out;
	}

	private static String peerStatus(PeerSeed p, int age) {
		if (age < 7) {
			return "어린 시절";
		}
		if (age < p.schoolEnd) {
			return age < 13 ? "초등학교에 다니는 중" : age < 16 ? "중학교에 다니는 중" : age < 19 ? "고등학교에 다니는 중" : "대학에 다니는 중";
		}
		if (age < 15) {
			return "집안일과 일을 돕는 중";
		}
		String s;
		if (age >= 65) {
			s = p.job == null ? "은퇴해 노년을 보내는 중" : "은퇴 · 전 " + p.job;
		} else if (p.home && age >= p.marryAge) {
			s = "가정을 돌보는 중";
		} else {
			s = p.job == null ? "일하는 중" : p.job;
		}
		if (age >= p.marryAge && age < 65) {
			int born = Math.min(p.kids, Math.max(0, (int) ((age - p.marryAge) / 2.5) + (age > p.marryAge ? 1 : 0)));
			s = s + " · 결혼" + (born > 0 ? " · 아이 " + born + "명" : "");
		}
		return s;
	}


	// ================= 본인 확인 =================

	// 내 인생 불러오기: 회원 인생은 로그인한 회원 번호, 비회원 인생은 임시 토큰이 맞아야 함
	// 내 인생이 아니거나 없으면 null
	// (DB 왕복 줄이기: 예전엔 본인 확인·선택 대기 확인·진행에서 인생을 3번 읽었는데 → 여기서 1번만 읽고 넘겨줌)
	public LifeDTO loadMyLife(int lifeId, Integer memberId, String guestToken) {
		LifeDTO life = engineDao.selectLife(lifeId);
		if (life == null) {
			return null;
		}
		boolean mine;
		if (life.getMemberId() != null) {
			mine = life.getMemberId().equals(memberId);
		} else {
			mine = life.getGuestToken() != null && life.getGuestToken().equals(guestToken);
		}
		return mine ? life : null;
	}

	// 내 인생인지 확인만 (C18 결과 화면·C19 AI 이야기에서 사용, 예전 이름 유지)
	// C03 이어하기 화면: 내 인생의 지금까지 기록 (내 인생이 아니면 null)
	public List<EventLogDTO> myEventLogs(int lifeId, Integer memberId, String guestToken) {
		if (loadMyLife(lifeId, memberId, guestToken) == null) {
			return null;
		}
		return engineDao.selectEventLogs(lifeId);
	}

	// C03 인생 지도: 지금까지 한 해씩 남긴 기록 (칸에 마우스를 올리면 그해 행복·건강·순자산)
	public List<YearLogDTO> myYearLogs(int lifeId, Integer memberId, String guestToken) {
		if (loadMyLife(lifeId, memberId, guestToken) == null) {
			return null;
		}
		return engineDao.selectYearLogs(lifeId);
	}

	// C03 나라 패널 (원작 나라 패널): 그해 사는 나라, view가 born이면 태어난 나라
	//   통계는 그해와 가장 가까운 연대 값 (나라·연대마다 한 번 읽으면 서버 메모리에 보관)
	private final Map<String, CountryInfoDTO> countryInfoCache = new ConcurrentHashMap<>();

	public CountryInfoDTO countryInfo(int lifeId, Integer memberId, String guestToken, String view) {
		LifeDTO life = loadMyLife(lifeId, memberId, guestToken);
		if (life == null) {
			return null;
		}
		String cc = "born".equals(view) ? life.getBirthCountry() : life.getCurrentCountry();
		int year = life.getBirthYear() + life.getCurrentAge();
		String key = cc + "_" + year / 10 * 10;
		CountryInfoDTO info = countryInfoCache.get(key);
		if (info == null) {
			Map<String, Object> cond = new HashMap<>();
			cond.put("countryCode", cc);
			cond.put("year", year);
			info = engineDao.selectCountryInfo(cond);
			if (info == null) {
				info = new CountryInfoDTO(); // 통계가 없는 나라: 이름 칸만 비워서 보냄
				info.setCountryCode(cc);
			}
			// 권역 진학률·산업 비율 (그해와 가장 가까운 연대, selectRegionStat은 사는 나라 기준이라 나라만 바꿔서 물어봄)
			LifeDTO at = new LifeDTO();
			at.setCurrentCountry(cc);
			at.setBirthYear(life.getBirthYear());
			at.setCurrentAge(life.getCurrentAge());
			RegionStatDTO r = engineDao.selectRegionStat(at);
			if (r != null) {
				info.setEnrollElem(r.getEnrollElem());
				info.setEnrollMiddle(r.getEnrollMiddle());
				info.setEnrollHigh(r.getEnrollHigh());
				info.setEnrollUni(r.getEnrollUni());
				info.setAgriRate(r.getAgriRate());
				info.setIndustryRate(r.getIndustryRate());
				info.setServiceRate(r.getServiceRate());
			}
			countryInfoCache.put(key, info);
		}
		return info;
	}

	public boolean isMyLife(int lifeId, Integer memberId, String guestToken) {
		return loadMyLife(lifeId, memberId, guestToken) != null;
	}

	// ================= C22 엔딩 크레딧 (원작 주마등 필름) =================
	// 끝난 내 인생 하나의 필름 재료 (내 인생이 아니면 null, 아직 안 끝났으면 빈 Map)
	//   life:     세상을 떠난 해의 모습 (가족과 삶 · 가진 것 · 사는 나라 …, 진행 화면과 같은 fillRegion)
	//   industry: 나이 → 그해 하던 일의 산업 AGRI · INDUSTRY · SERVICE (마을 일터 그림)
	//   sources:  이 이야기에 쓰인 자료 (겪은 역사 사건 · 태어난 나라 통계의 출처 이름, 최대 12개)
	public Map<String, Object> filmData(int lifeId, Integer memberId, String guestToken) {
		LifeDTO life = loadMyLife(lifeId, memberId, guestToken);
		if (life == null) {
			return null;
		}
		Map<String, Object> out = new HashMap<>();
		if (!"ENDED".equals(life.getStatus())) {
			return out;
		}
		life.setHistName(engineDao.selectHistName(life));
		fillRegion(life);
		Map<Integer, String> industry = new java.util.TreeMap<>();
		List<YearLogDTO> years = engineDao.selectYearLogs(lifeId);
		if (years != null) {
			for (YearLogDTO y : years) {
				JobDTO j = y.getJobId() == null ? null : findJob(y.getJobId());
				if (j != null && j.getIndustry() != null) {
					industry.put(y.getAge(), j.getIndustry());
				}
			}
		}
		List<String> sources = engineDao.selectFilmSources(lifeId);
		out.put("life", life);
		out.put("industry", industry);
		out.put("sources", sources == null ? new ArrayList<String>() : sources);
		return out;
	}

	// 선택 이벤트를 아직 안 골랐는지 (다음 해로 못 넘어감)
	public boolean isWaitingChoice(LifeDTO life) {
		return life.getPendingEventId() != null;
	}

	// ================= 통계 캐시 (DB 왕복 줄이기) =================
	// 나라·권역 통계와 직업 목록은 게임 중 바뀌지 않는 데이터라 한 번 읽으면 서버 메모리에 보관
	// (데이터를 DB에서 고쳤으면 서버를 재시작해야 반영됨)
	private final Map<String, CountryStatDTO> yearStatCache = new ConcurrentHashMap<>();
	private final Map<String, RegionStatDTO> regionStatCache = new ConcurrentHashMap<>();
	private List<JobDTO> jobCache;

	// 캐시 열쇠: 사는 나라 + 그해의 연대 (예: KOR_1960)
	private String statKey(LifeDTO life) {
		return life.getCurrentCountry() + "_" + (life.getBirthYear() + life.getCurrentAge()) / 10 * 10;
	}

	// 그해 사는 나라 통계
	private CountryStatDTO yearStat(LifeDTO life) {
		String key = statKey(life);
		CountryStatDTO s = yearStatCache.get(key);
		if (s == null) {
			s = engineDao.selectYearStat(life);
			if (s != null) {
				yearStatCache.put(key, s);
			}
		}
		return s;
	}

	// 그해 사는 나라 권역 통계 (진학률·산업 비율)
	private RegionStatDTO regionStat(LifeDTO life) {
		String key = statKey(life);
		RegionStatDTO r = regionStatCache.get(key);
		if (r == null) {
			r = engineDao.selectRegionStat(life);
			if (r != null) {
				regionStatCache.put(key, r);
			}
		}
		return r;
	}

	// 직업 목록 전체
	private List<JobDTO> jobs() {
		if (jobCache == null) {
			jobCache = engineDao.selectJobs();
		}
		return jobCache;
	}

	// 직업 하나 찾기 (없으면 null)
	private JobDTO findJob(String jobId) {
		for (JobDTO job : jobs()) {
			if (job.getJobId().equals(jobId)) {
				return job;
			}
		}
		return null;
	}

	// ================= C03 이어하기 =================

	// 진행 중인 인생 찾기 (회원은 회원 번호, 비회원은 임시 토큰)
	public LifeDTO currentLife(Integer memberId, String guestToken) {
		if (memberId == null && (guestToken == null || guestToken.equals(""))) {
			return null;
		}
		LifeDTO cond = new LifeDTO();
		cond.setMemberId(memberId);
		cond.setGuestToken(guestToken);
		LifeDTO life = engineDao.selectPlayingLife(cond);
		if (life != null && life.getStageCode() != null && life.getFocusCode() == null) {
			// 집중을 아직 안 골랐으면 선택 창 다시 띄우기
			life.setNeedFocus(true);
			life.setFocusOptions(focusOptions(life.getStageCode(), life));
		}
		restoreChoice(life); // 선택 이벤트를 아직 안 골랐으면 선택 창 다시 띄우기
		if (life != null) {
			life.setHistName(engineDao.selectHistName(life)); // 이어하기 화면용 당시 국가명
			showOffers(life, yearStat(life)); // 직업을 아직 안 골랐으면 같은 제안으로 선택 창 다시 띄우기
		}
		fillRegion(life);
		return life;
	}

	// ================= C03 1년 진행 =================
	// life: 컨트롤러에서 loadMyLife로 불러온 인생
	// @Transactional: 한 해 처리(능력치·이벤트·경험·기록 저장) 중 하나라도 실패하면 그해 전체를 취소
	//   (예전엔 중간에 에러가 나면 경험만 저장되고 나이는 그대로라, 다음 진행 때 같은 경험이 또 들어가 막힐 수 있었음)
	@Transactional
	public LifeDTO nextYear(LifeDTO life) {

		// 1. 끝난 인생이면 진행 안 함
		if (!life.getStatus().equals("PLAYING")) {
			return null;
		}
		// 엔진 선택지(C06-4)를 아직 안 골랐으면 진행 안 함 (화면은 409 → 다시 불러오기 → 선택 창이 다시 뜸)
		if (engineChoiceWaiting(life)) {
			return null;
		}
		// 같은 해를 두 번 진행하지 않게 이 인생 줄을 잠금 (버튼 연타·자동 진행이 겹쳐도 한 번만 진행)
		//   먼저 온 요청이 끝날 때까지 기다렸다가, 나이가 이미 바뀌었으면 0줄 → 진행 안 함
		if (engineDao.lockLifeYear(life) == 0) {
			return null;
		}
		engState(life); // 올해 이벤트 기록이 쌓이기 전에 되살려 둠 (만남 번호가 두 번 세지지 않게)

		// 그해 사는 나라 통계 (사망 판정·진학에 사용)
		CountryStatDTO stat = yearStat(life);
		long startAssets = life.getAssets();
		life.setMoneyFlow(new ArrayList<>()); // [화면용] 지난 1년 순자산 내역
		life.setEvents(new ArrayList<>());    // [화면용] 올해 일어난 일 (돈 계산·이벤트·물건이 차례로 덧붙임)
		life.setAlerts(new ArrayList<>());    // [화면용] 올해 '큰 사건' 경고등을 켤 문장

		// ① 집중 효과 (C04): 고르지 않았으면 나이대 기본값
		if (life.getStageCode() != null && life.getFocusCode() == null) {
			life.setFocusCode(defaultFocus(life));
		}
		applyFocus(life);
		fixStats(life);

		// ② 능력치 자연 변화 (C05): 건강·관계·행복·지식이 목표값으로 조금씩 이동
		naturalChange(life, stat);
		fixStats(life);

		// ④ 소득·재산 (C06-2)
		moneyStep(life, stat);

		// ⑤ 인생·역사 이벤트 (C09, C10): 윤아님 rollYear로 올해 일어난 일을 뽑고 → 효과·상태를 인생에 반영
		eventStep(life);
		// ⑤-0 부모님 별세: 정해 둔 수명대로 그해 떠나실 분 (11/23 윤아님과 합의)
		parentYear(life, stat);
		// ⑤-1 전쟁 (원작 ERA_WAR): 그 나라·그해 전쟁이면 확률 없이 반드시 시작·끝 소식 / 결혼 상대 소개 (원작처럼 직업·소득)
		warYear(life);
		spouseNote(life, life.getCurrentAge());
		// ⑤-1-2 집안 형편 변동: 부모님 일이 기울거나 잘 풀림 (11/20)
		familyFortune(life);
		// ⑤-1-3 만난 사람 소개 (새로운 만남마다 새 사람) · 수저별 부모님 도움 (11/23)
		// ⑤-0-2 수감 (윤아님 '위험한 제안' PRISON, 11/23): 안에서 보내는 한 해 (원작: 건강 −3 · 행복 −6 · 관계 −4)
		boolean jailed = inPrison(life);
		if (jailed) {
			prisonYear(life);
		} else {
			loveYear(life, stat);
			parentHelpYear(life, stat);
		}

		// ⑤-2 돈 쓰기·투자 (C06-3): 모은 돈 굴리기 + 가진 물건 유지비
		investStep(life, stat);
		// ⑤-3 스마트폰이 흔해진 해부터 장만 (원작 phoneYear)
		if (!jailed) {
			phoneYear(life, stat);
		}
		// ⑤-4 반려동물 돌봄·떠나보냄 (원작 petYear)
		petYear(life, stat);
		// ⑤-5 병역 복무·전역 (원작 service)
		svcYear(life);
		// ⑤-6 취미 (원작 hobby): 해마다 작은 효과와 비용
		if (!jailed) {
			hobbyYear(life, stat);
		}
		// ⑤-7 습관·돈 약속 (원작 담배·술·도박·복권·빌려준 돈·보증, 11/23)
		if (!jailed) {
			habitYear(life, stat);
		}
		fixStats(life);

		// ⑥ 사망 판정 (C07): 올해(현재 나이)를 못 넘기면 사망
		//    역사 이벤트의 deathAdd(전쟁·기근 등)는 올해 사망 확률에 더함 (윤아님 EventResultDTO.deathAdd)
		double histAdd = 0;
		String histCause = null;
		double histBest = 0;
		if (life.getEvents() != null) {
			for (EventResultDTO ev : life.getEvents()) {
				if (ev.getDeathAdd() > 0) {
					histAdd = histAdd + ev.getDeathAdd();
					if (ev.getDeathCause() != null && ev.getDeathAdd() > histBest) {
						histBest = ev.getDeathAdd();
						histCause = ev.getDeathCause(); // 가산이 가장 큰 사건을 원인 후보로
					}
				}
			}
		}
		// 지난해 선택(참전 등)에서 넘어온 사망 위험
		EngState carry = engStates.get(life.getLifeId());
		if (carry != null && carry.carryDeath > 0) {
			histAdd = histAdd + carry.carryDeath;
			if (carry.carryCause != null && carry.carryDeath > histBest) {
				histBest = carry.carryDeath;
				histCause = carry.carryCause;
			}
			carry.carryDeath = 0;
			carry.carryCause = null;
		}
		double deathP = Math.min(1, deathRate(life, stat) + histAdd);
		if (random.nextDouble() < deathP) {
			// 올해 선택 이벤트가 걸려 있었어도 죽었으니 선택 대기 해제 (끝난 인생에 선택 창이 뜨지 않게)
			life.setPendingEventId(null);
			life.setNeedChoice(false);
			life.setPendingEvent(null);
			engineDao.updateLifeYear(life); // 마지막 해의 능력치·소득·재산 저장
			life.setStatus("ENDED");
			life.setDeathAge(life.getCurrentAge());
			// 사망 원인: 역사 사건 몫(histAdd ÷ 전체 확률)만큼의 확률로 사건 원인, 아니면 평소 원인
			if (histCause != null && random.nextDouble() < histAdd / deathP) {
				life.setDeathCause(histCause);
			} else {
				life.setDeathCause(deathCause(life));
			}
			finalResult(life);
			// 재산이 어떻게 나뉘었는지 (원작 상속): 화면에선 사망 줄 뒤에 (eventType INHERIT)
			String inh = inheritText(life);
			if (inh != null) { // 남길 것도 갚을 빚도 없는 아이는 상속 줄 없음
				saveEventLog(life, life.getCurrentAge(), null, null, inh);
				List<EventResultDTO> evs = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
				EventResultDTO ie = noteEvent(inh);
				ie.setEventType("INHERIT");
				evs.add(ie);
				life.setEvents(evs);
			}
			engineDao.endLife(life);
			// 퀘스트 판정 + 포인트 지급 (끝난 인생만, 비회원은 퀘스트가 없어 그냥 지나감)
			//   같은 트랜잭션이라 방금 저장한 STATUS='ENDED'·사망 나이를 그대로 읽어서 판정
			questJudgeService.judgeAndGive(life.getLifeId());
			closeBook(life, startAssets);
			fillRegion(life);
			return life;
		}

		// 2. 살아남으면 나이 +1
		life.setCurrentAge(life.getCurrentAge() + 1);

		// ③ 학교: 새 나이 기준으로 졸업·중퇴·진학 판정 (C06-1)
		String eduBefore = life.getEduLevel();
		String schoolBefore = life.getSchoolStatus();
		schoolStep(life, stat);

		// ③ 직업: 정년 은퇴 판정, 일자리가 없으면 직업 3개 제안 (C06-2)
		String jobBefore = life.getJobStatus();
		String jobIdBefore = life.getJobId();
		jobStep(life, stat);

		// ⑦ 인생 기록 보강 (C03): 원작처럼 해마다 기록이 있게
		//    입학·진학·졸업·중퇴·은퇴 + 그해 처지에 맞는 일상 1~3줄(EngineStory) + 몇 살 고비마다 같은 해 태어난 또래 생존율
		int lived = life.getCurrentAge() - 1; // 방금 지나간 해 (이벤트와 같은 나이로 기록)
		List<EventResultDTO> yearEvents = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
		if (lived == 0) {
			String home = homeLine(life, stat);
			saveEventLog(life, lived, null, null, home);
			yearEvents.add(0, noteEvent(home)); // 0세 기록은 집안 소개가 맨 앞 (시대 사건보다 먼저)
		}
		for (String note : milestones(life, eduBefore, schoolBefore, jobBefore, jobIdBefore)) {
			saveEventLog(life, lived, null, null, note);
			yearEvents.add(noteEvent(note));
		}
		for (String note : questNotes(life)) {
			saveEventLog(life, lived, null, null, note);
			yearEvents.add(noteEvent(note));
		}
		EngState sn = engStates.get(life.getLifeId());
		if (sn != null && sn.schoolNote != null) {
			saveEventLog(life, lived, null, null, sn.schoolNote);
			yearEvents.add(noteEvent(sn.schoolNote));
			sn.schoolNote = null;
		}
		for (String line : storyLines(life, stat, lived, yearEvents.size())) {
			saveEventLog(life, lived, null, null, line);
			yearEvents.add(noteEvent(line));
		}
		String alive = survivalLine(life, stat, lived);
		if (alive != null) {
			saveEventLog(life, lived, null, null, alive);
			yearEvents.add(noteEvent(alive));
		}
		life.setEvents(yearEvents);

		// 3. 나이대가 바뀌면 집중을 비우고 선택 창 띄우기 (C04)
		String newStage = stageOf(life);
		if (newStage != null && !newStage.equals(life.getStageCode())) {
			life.setStageCode(newStage);
			life.setFocusCode(null);
			life.setNeedFocus(true);
			life.setFocusOptions(focusOptions(newStage, life));
		}

		// 나이가 바뀌며 지금 고른 집중이 목록에서 빠졌으면 (예: 연애에 집중하다 결혼) 다시 고르기
		if (life.getFocusCode() != null && life.getStageCode() != null
				&& !focusOptions(life.getStageCode(), life).contains(life.getFocusCode())) {
			life.setFocusCode(null);
			life.setNeedFocus(true);
			life.setFocusOptions(focusOptions(life.getStageCode(), life));
		}

		// 선택 이벤트 비용은 나이가 바뀐 뒤 기준으로 다시 계산 (고를 때 chooseEvent·choiceBlocked가 보는 나이·연대와 같게)
		//   예전엔 이벤트를 뽑을 때(나이 +1 전) 계산해서, 연대가 바뀌는 해(2009→2010)엔 중위 소득이 달라져
		//   화면엔 고를 수 있다고 나온 가게 차리기가 고르면 "돈이 모자라요"로 막혔음
		if (life.isNeedChoice() && life.getPendingEvent() != null) {
			life.setChoiceCosts(costsOf(life, life.getPendingEvent().getChoices(), life.getPendingEvent().getLogText()));
			// 나이가 바뀌어 다시 계산했더니 '하지 않는다'만 남으면 그 선택은 없던 일로 (드문 경우)
			if (onlyNoLeft(life.getPendingEvent().getChoices(), life.getChoiceCosts())) {
				life.setPendingEventId(null);
				life.setNeedChoice(false);
				life.setPendingEvent(null);
				life.setChoiceCosts(null);
			} else {
				hideUnaffordable(life); // 돈·신용이 모자라 고를 수 없는 선택지는 아예 빼고 보여 줌
			}
		}

		// 윤아님 선택 이벤트·직업 제안이 없는 해엔 엔진 선택지 (C06-4: 기질·어린 시절·주말·사람 사이)
		rollEngineChoice(life, lived);

		// 4. 저장 + 그해 기록
		engineDao.updateLifeYear(life);
		engineDao.insertYearLog(life);
		// 아이(16세 전)에게 뜬 이민·피란 선택은 가족이 대신 정함 (11/23 동현님: 영유아·학생에게 결정권이 있는 게 어색)
		kidFamilyDecides(life, lived);

		closeBook(life, startAssets);
		fillRegion(life);
		return life;
	}


	// ================= 전쟁 (원작 ERA_WAR · eraDue) =================
	// 그 나라·그해 전쟁이면 확률 없이 반드시 겪음 (11/20 동현님: "그 시대 그 나라에 살면 100%")
	//   - 전쟁이 시작한 해: 시작 소식 (화면 경고등)
	//   - 전쟁 중에 태어나면 0세에: 태어난 해에 전쟁이 이어지고 있었다
	//   - 끝난 해: 끝 소식 + 행복 +4 (진 쪽·나라가 갈린 쪽은 빼고), 아직 이어지는 전쟁(2024년 이후 끝)은 없음
	//   원작처럼 그해 강도 0.5 이상일 때만 (강도 0.5 아래는 국지 분쟁이라 소식 없음), 크메르 루주 학살은 따로 다루지 않음
	//   윤아님 역사 이벤트(DB 확률)로 같은 해 전쟁 사건이 이미 떴으면 시작 소식은 겹치지 않게 뺌
	//   사망 위험은 그해 생명표(전쟁 해의 기대수명)와 윤아님 전쟁 이벤트 deathAdd 에 이미 들어 있어 더하지 않음
	//   { 나라, 시작, 끝, 강도, 전쟁 코드 } - 원작 ERA_WAR 중 우리 나라들
	private static final String[][] ERA_WAR = {
			{ "CHN", "1930", "1936", "0.5", "cncw" }, { "CHN", "1937", "1945", "3", "cjw" }, { "CHN", "1946", "1949", "2", "cncw" },
			{ "JPN", "1937", "1940", "0.5", "cjw" }, { "JPN", "1941", "1943", "1.2", "ww2" }, { "JPN", "1944", "1945", "3", "ww2" },
			{ "IND", "1939", "1945", "0.2", "ww2" }, { "PHL", "1941", "1945", "2", "ww2" }, { "IDN", "1942", "1945", "1.5", "ww2" },
			{ "MMR", "1942", "1945", "1.5", "ww2" }, { "MYS", "1941", "1945", "1", "ww2" }, { "SGP", "1942", "1945", "1", "ww2" },
			{ "VNM", "1940", "1945", "0.5", "ww2" }, { "TLS", "1942", "1945", "3", "ww2" }, { "THA", "1941", "1945", "0.3", "ww2" },
			{ "IDN", "1945", "1949", "1", "idrev" }, { "VNM", "1946", "1954", "1.5", "indo" }, { "MYS", "1948", "1960", "0.3", "mye" },
			{ "MMR", "1948", "2020", "0.4", "mmw" }, { "MMR", "2021", "2025", "1.5", "mmw" }, { "IND", "1947", "1948", "0.2", "kash47" },
			{ "PAK", "1947", "1948", "0.3", "kash47" }, { "KOR", "1950", "1953", "5", "kw" }, { "PRK", "1950", "1953", "5", "kw" },
			{ "VNM", "1955", "1964", "1", "vnw" }, { "VNM", "1965", "1975", "3", "vnw" }, { "VNM", "1979", "1979", "0.5", "sinovn" },
			{ "LAO", "1959", "1975", "1.5", "laow" }, { "KHM", "1970", "1975", "2.5", "khw" }, { "KHM", "1979", "1991", "1", "khw2" },
			{ "LKA", "1983", "2009", "1", "lkw" }, { "NPL", "1996", "2006", "0.4", "npw" }, { "AFG", "1978", "1989", "3", "afw1" },
			{ "AFG", "1990", "2001", "2", "afw2" }, { "AFG", "2002", "2021", "1.2", "afw3" }, { "BGD", "1971", "1971", "5", "bdw" },
			{ "PAK", "1971", "1971", "0.5", "bdw" }, { "PHL", "1972", "1990", "0.3", "phm" },
			{ "SYR", "1967", "1967", "0.5", "w67" }, { "SYR", "1973", "1973", "0.5", "w73" }, { "SYR", "2011", "2024", "3", "syw" },
			{ "SYR", "2025", "2025", "1", "syw" }, { "JOR", "1967", "1967", "0.5", "w67" }, { "JOR", "1970", "1971", "0.5", "bsep" },
			{ "ISR", "1956", "1956", "0.3", "suez" }, { "ISR", "1967", "1967", "0.5", "w67" }, { "ISR", "1973", "1973", "1", "w73" },
			{ "ISR", "1982", "1982", "0.3", "lbw82" }, { "ISR", "2006", "2006", "0.3", "lbw06" }, { "ISR", "2023", "2025", "0.6", "gaza23" },
			{ "PSE", "1967", "1967", "1", "w67" }, { "PSE", "1987", "1993", "0.5", "int1" }, { "PSE", "2000", "2005", "1", "int2" },
			{ "PSE", "2008", "2009", "1.5", "gaza08" }, { "PSE", "2014", "2014", "1.5", "gaza14" }, { "PSE", "2023", "2025", "5", "gaza23" },
			{ "LBN", "1975", "1990", "2.5", "lbw" }, { "LBN", "2006", "2006", "1", "lbw06" }, { "IRN", "1980", "1988", "2", "iiw" },
			{ "IRQ", "1961", "1970", "0.5", "kurd" }, { "IRQ", "1974", "1975", "0.5", "kurd" }, { "IRQ", "1980", "1988", "2.5", "iiw" },
			{ "IRQ", "1991", "1991", "2", "gulf" }, { "IRQ", "2003", "2011", "2.5", "iqw" }, { "IRQ", "2014", "2017", "2", "isw" },
			{ "KWT", "1990", "1991", "2", "gulf" }, { "YEM", "1962", "1970", "1.5", "yew1" }, { "YEM", "1986", "1986", "1", "yew86" },
			{ "YEM", "1994", "1994", "0.5", "yew94" }, { "YEM", "2015", "2025", "2", "yew" }, { "OMN", "1963", "1976", "0.3", "dhofar" },
			{ "TUR", "1984", "1999", "0.3", "pkk" }, { "AZE", "1988", "1994", "1.5", "nkw" }, { "AZE", "2020", "2020", "0.5", "nkw2" },
			{ "GEO", "1991", "1993", "1.5", "geow" }, { "GEO", "2008", "2008", "0.5", "ruge" }, { "TJK", "1992", "1997", "2.5", "tjw" },
			{ "ARM", "1988", "1994", "1.5", "nkw" }, { "ARM", "2020", "2020", "1", "nkw2" }, { "CYP", "1974", "1974", "2", "cyw" },
			{ "TLS", "1975", "1999", "1.5", "tlw" } };

	private static final Map<String, String> WAR_NAME = Map.ofEntries(
			Map.entry("ww2", "제2차 세계대전"), Map.entry("cjw", "중일전쟁"), Map.entry("cncw", "국공 내전"),
			Map.entry("idrev", "인도네시아 독립전쟁"), Map.entry("kash47", "제1차 카슈미르 전쟁"), Map.entry("kw", "한국전쟁"),
			Map.entry("indo", "제1차 인도차이나 전쟁"), Map.entry("vnw", "베트남 전쟁"), Map.entry("sinovn", "중국-베트남 전쟁"),
			Map.entry("laow", "라오스 내전"), Map.entry("khw", "캄보디아 내전"), Map.entry("khw2", "캄보디아 내전"),
			Map.entry("lkw", "스리랑카 내전"), Map.entry("npw", "네팔 내전"), Map.entry("afw1", "소련-아프가니스탄 전쟁"),
			Map.entry("afw2", "아프가니스탄 내전"), Map.entry("afw3", "아프가니스탄 전쟁"), Map.entry("bdw", "방글라데시 해방전쟁"),
			Map.entry("mye", "말라야 비상사태"), Map.entry("phm", "민다나오 분쟁"), Map.entry("mmw", "미얀마 내전"),
			Map.entry("suez", "수에즈 전쟁"), Map.entry("w67", "제3차 중동전쟁"), Map.entry("w73", "제4차 중동전쟁"),
			Map.entry("syw", "시리아 내전"), Map.entry("bsep", "요르단 내전"), Map.entry("lbw82", "1982년 레바논 전쟁"),
			Map.entry("lbw06", "2006년 레바논 전쟁"), Map.entry("gaza23", "2023년 가자 전쟁"), Map.entry("int1", "제1차 인티파다"),
			Map.entry("int2", "제2차 인티파다"), Map.entry("gaza08", "2008년 가자 전쟁"), Map.entry("gaza14", "2014년 가자 전쟁"),
			Map.entry("lbw", "레바논 내전"), Map.entry("iiw", "이란-이라크 전쟁"), Map.entry("kurd", "쿠르드 전쟁"),
			Map.entry("gulf", "걸프 전쟁"), Map.entry("iqw", "이라크 전쟁"), Map.entry("isw", "IS와의 전쟁"),
			Map.entry("yew1", "북예멘 내전"), Map.entry("yew86", "남예멘 내전"), Map.entry("yew94", "예멘 내전"),
			Map.entry("yew", "예멘 내전"), Map.entry("dhofar", "도파르 전쟁"), Map.entry("pkk", "쿠르드 분쟁"),
			Map.entry("nkw", "나고르노카라바흐 전쟁"), Map.entry("nkw2", "제2차 나고르노카라바흐 전쟁"), Map.entry("geow", "조지아 내전"),
			Map.entry("ruge", "러시아-조지아 전쟁"), Map.entry("tjw", "타지키스탄 내전"), Map.entry("cyw", "1974년 키프로스 전쟁"),
			Map.entry("tlw", "인도네시아의 동티모르 점령"));

	// 시작 소식 (코드|나라 가 있으면 그것, 없으면 코드, 그것도 없으면 '○○이 일어났다')
	private static final Map<String, String> WAR_START = Map.ofEntries(
			Map.entry("cjw", "7월, 베이징 근처 루거우차오의 충돌이 일본과의 전면 전쟁으로 번졌다. 중일전쟁이 시작되었다."),
			Map.entry("cjw|JPN", "중국과의 전쟁이 시작되었다. 마을마다 출정하는 군인을 깃발을 흔들며 배웅했다."),
			Map.entry("ww2", "일본군이 쳐들어와 이곳을 점령했다. 제2차 세계대전의 전쟁터가 되었다."),
			Map.entry("ww2|JPN", "12월 8일, 일본이 미국·영국과 전쟁을 시작했다는 라디오 방송이 흘러나왔다. 태평양 전쟁이 시작되었다."),
			Map.entry("ww2|IDN", "일본군이 쳐들어와 네덜란드군이 항복했다. 제2차 세계대전의 전쟁터가 되었다."),
			Map.entry("idrev", "돌아온 네덜란드군과 싸움이 시작되었다. 인도네시아 독립전쟁이 일어났다."),
			Map.entry("kw", "6월 25일 새벽, 북한군이 38선을 넘어 쳐내려왔다. 한국전쟁이 시작되어 피란 짐을 진 사람들로 길이 메워졌다."),
			Map.entry("kw|PRK", "6월 25일 새벽, 인민군이 38선을 넘어 남쪽으로 내려가며 한국전쟁이 시작되었다. 머지않아 폭격이 온 나라를 덮쳤다."),
			Map.entry("bdw", "독립을 외치던 동파키스탄에서 군대의 학살이 시작되었다. 방글라데시 해방전쟁이 일어났다."),
			Map.entry("bdw|PAK", "독립을 요구하는 동파키스탄에 군대가 보내졌다. 방글라데시 해방전쟁이 일어났다."),
			Map.entry("syw", "시위가 내전으로 번졌다. 시리아 내전이 시작되어 도시마다 폭격 소리가 들렸다."),
			Map.entry("gulf", "미국이 이끄는 다국적군이 이라크군을 몰아내려 공습을 시작했다. 걸프 전쟁이 일어났다."),
			Map.entry("gulf|KWT", "이라크군이 국경을 넘어 쿠웨이트를 점령했다. 걸프 전쟁이 시작되었다."),
			Map.entry("iqw", "미국이 이끄는 연합군이 이라크를 침공했다. 이라크 전쟁이 시작되었다."),
			Map.entry("isw", "IS가 모술을 비롯한 여러 도시를 점령했다. IS와의 전쟁이 시작되었다."),
			Map.entry("iiw", "이라크와 이란 사이에 전쟁이 일어났다. 이란-이라크 전쟁이 시작되었다."),
			Map.entry("lbw", "종파 사이의 충돌이 내전으로 번졌다. 레바논 내전이 시작되어 베이루트가 동서로 갈렸다."));

	private static final Map<String, String> WAR_END = Map.ofEntries(
			Map.entry("ww2", "일본이 항복하고 제2차 세계대전이 끝났다."),
			Map.entry("ww2|JPN", "8월 15일 정오, 라디오에서 전쟁이 끝났다는 천황의 방송이 흘러나왔다."),
			Map.entry("cjw", "8월, 일본이 항복했다는 소식에 폭죽이 터졌다. 중일전쟁이 끝났다."),
			Map.entry("cncw", "국공 내전이 끝났다."),
			Map.entry("idrev", "네덜란드가 인도네시아의 독립을 인정했다. 독립전쟁이 끝났다."),
			Map.entry("kw", "정전 협정이 맺어져 총성이 멎었다. 나라는 휴전선을 사이에 두고 둘로 나뉘었다."),
			Map.entry("vnw", "사이공이 함락되고 베트남 전쟁이 끝났다."),
			Map.entry("vnw|VNM", "사이공이 함락되고 베트남 전쟁이 끝났다. 나라는 하나가 되었다."),
			Map.entry("bdw|PAK", "동파키스탄이 방글라데시로 독립하며 전쟁이 끝났다."));

	private static final java.util.Set<String> WAR_END_SAD = java.util.Set.of("ww2|JPN", "bdw|PAK", "cyw", "nkw2|ARM");
	private static final java.util.Set<String> WAR_JOIN = java.util.Set.of("cjw|JPN"); // 이어서 더 큰 전쟁이 된 쪽은 끝 소식 없음

	private void warYear(LifeDTO life) {
		if (!"PLAYING".equals(life.getStatus())) {
			return;
		}
		String cc = life.getCurrentCountry();
		int age = life.getCurrentAge();
		int year = life.getBirthYear() + age;
		// 그해 가장 센 전쟁 (원작 warAt)
		String[] now = null;
		for (String[] w : ERA_WAR) {
			if (w[0].equals(cc) && year >= Integer.parseInt(w[1]) && year <= Integer.parseInt(w[2])
					&& (now == null || Double.parseDouble(w[3]) > Double.parseDouble(now[3]))) {
				now = w;
			}
		}
		if (now == null || Double.parseDouble(now[3]) < 0.5) {
			return;
		}
		String id = now[4];
		String key = id + "|" + cc;
		// 같은 전쟁의 처음과 끝 해 (원작 warSpan: 강도와 상관없이 같은 코드 전체)
		int from = 9999, to = 0;
		for (String[] w : ERA_WAR) {
			if (w[0].equals(cc) && w[4].equals(id)) {
				from = Math.min(from, Integer.parseInt(w[1]));
				to = Math.max(to, Integer.parseInt(w[2]));
			}
		}
		String name = WAR_NAME.getOrDefault(id, "전쟁");
		List<String> lines = new ArrayList<>();
		int h = 0;
		if (year == from) {
			if (!histWarThisYear(life)) {
				lines.add(WAR_START.getOrDefault(key, WAR_START.getOrDefault(id, iga(name) + " 일어났다.")));
			}
		} else if (age == 0 && !histWarThisYear(life)) { // 윤아님 전쟁 이벤트가 같은 해에 있으면 생략
			String c = life.getCountryName() == null ? "이 나라" : life.getCountryName();
			lines.add("태어난 해, " + c + "에서는 " + iga(name) + " 이어지고 있었다.");
		}
		if (year == to && to < 2024 && !WAR_JOIN.contains(key)) {
			lines.add(WAR_END.getOrDefault(key, WAR_END.getOrDefault(id, iga(name) + " 끝났다.")));
			if (!WAR_END_SAD.contains(id) && !WAR_END_SAD.contains(key)) {
				h = 4;
			}
		}
		for (int i = 0; i < lines.size(); i++) {
			String t = lines.get(i);
			EventResultDTO e = new EventResultDTO();
			e.setEventType("HIST"); // 화면: 역사 사건 색 · 큰 사건 경고등
			e.setTitle(name);
			e.setLogText(t);
			if (!ENDED_NEWS.matcher(t).find()) {
				life.addAlert(t); // 전쟁이 시작했거나 이어지는 소식은 경고등 (끝난 소식은 빼고)
			}
			if (i == lines.size() - 1 && h != 0) {
				e.setEffHappiness(h);
				life.setHappiness(life.getHappiness() + h);
			}
			saveEventLog(life, age, null, null, t);
			List<EventResultDTO> list = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
			list.add(e);
			life.setEvents(list);
		}
	}

	// 올해 윤아님 역사 이벤트 가운데 전쟁(WAR 유형)이 이미 있는지
	private boolean histWarThisYear(LifeDTO life) {
		if (life.getEvents() == null) {
			return false;
		}
		for (EventResultDTO e : life.getEvents()) {
			// 제목에 전쟁·내전이 들어간 역사 사건도 같은 전쟁 소식으로 봄 (유형 코드가 WAR가 아닐 때)
			if ("HIST".equals(e.getEventType()) && e.getEventId() != null && e.getTitle() != null
					&& (e.getTitle().contains("전쟁") || e.getTitle().contains("내전"))) {
				return true;
			}
			if ("HIST".equals(e.getEventType()) && e.getEventId() != null) {
				HistEffectDTO h = engineDao.selectHistEffect(e.getEventId());
				if (h != null && "WAR".equals(h.getTypeCode())) {
					return true;
				}
			}
		}
		return false;
	}

	// ================= C03 인생 기록 보강 =================

	private static final Map<String, String> SCHOOL_NAME = Map.of(
			"ELEM", "초등학교", "MIDDLE", "중학교", "HIGH", "고등학교", "UNI", "대학교", "GRAD", "대학원");

	// 학교 이름 + 을/를 (대학원만 받침이 있음)
	private static String schoolObj(String level) {
		String n = SCHOOL_NAME.getOrDefault(level, "학교");
		return n + ("GRAD".equals(level) ? "을" : "를");
	}

	// 그해 일상 문장 (EngineStory) + 경조사·살림 비용 차감 (금액은 한국 체감 원으로 붙임)
	//   경조사비 = 중위 소득 × 비율 × (내 소득/중위)^0.3 (0.6~1.8배) - 원작 친구 결혼식 공식
	//   빚이 있거나 벌이·재산이 없으면 EngineStory 가 돈 안 드는 문장(음식 준비를 거듦 등)을 고름
	private List<String> storyLines(LifeDTO life, CountryStatDTO stat, int age, int already) {
		List<String> out = new ArrayList<>();
		int[] jail = prisonSpan(life);
		if (jail != null && age >= jail[0] && age < jail[0] + jail[1]) {
			// 그 해를 교도소에서 보냄: 바깥 일상 줄 대신 안에서의 한 줄 (들어간 해는 이벤트 문장이 있으니 기록이 없을 때만)
			//   마지막 해는 출소 문장이 있으니 빼요 (11/23: '교도소에서 나왔다' 바로 뒤에 '면회를 왔다'가 붙었음)
			if ((age > jail[0] || already == 0) && age < jail[0] + jail[1] - 1) {
				out.add(prisonLine(life));
			}
			return out;
		}
		double med = medIncome(life.getCurrentCountry(), stat);
		// 형제자매 (가족 칸과 같은 씨앗): 외동인데 '형제자매와 다퉜다' 같은 줄이 나오지 않게
		int sibNow = 0, sibYounger = 0;
		for (int g : famSeed(life, stat).siblingGap) {
			if (age + g >= 0) {
				sibNow++;
				if (g < 0) {
					sibYounger++;
				}
			}
		}
		for (EngineStory.Story st : EngineStory.year(life, age, already, random, sibNow, sibYounger)) {
			String text = st.text;
			if (st.cost > 0 && age >= 18) {
				double k = clamp(Math.pow(Math.max(life.getIncome(), 1) / med, 0.3), 0.6, 1.8);
				long pay = Math.round(med * st.cost * k);
				// 한국 체감으로 너무 작은 금액이 나오지 않게 (가난한 나라 축의금이 7,700원으로 나왔음): 문장마다 최소 금액
				int floorWon = COST_FLOOR.getOrDefault(st.id == null ? "" : st.id, 10000);
				pay = Math.max(pay, (long) Math.ceil(floorWon / krwPerUsd(stat))); // 올림 (반올림하면 9,500원처럼 최소 금액 아래로 나왔음)
				// 위로도 한국 체감에 맞게 (300번: 지팡이 78만 원 · 동창 회비 16만 원 · 배관 327만 원이 나왔음)
				Integer capWon = COST_CAP.get(st.id == null ? "" : st.id);
				if (capWon != null) {
					pay = Math.min(pay, Math.max((long) Math.ceil(floorWon / krwPerUsd(stat)), Math.round(capWon / krwPerUsd(stat))));
				}
				if (pay > 0) {
					life.setAssets(life.getAssets() - pay);
					book(life, storyLabel(st.id), -pay);
					text = text + " (−" + won(life, pay) + ")";
				}
			}
			out.add(text);
		}
		return out;
	}

	// 엔진 선택지 비용: 중위 소득 × 비율, 한국 체감 1만 원 아래로는 내려가지 않게
	private double optPay(LifeDTO life, double rate) {
		CountryStatDTO st = yearStat(life);
		return Math.max(medIncome(life.getCurrentCountry(), st) * rate, Math.ceil(10000 / krwPerUsd(st))); // 올림 (달러를 반올림해 9,500원이 나왔음)
	}

	// 일상 지출 최소 금액 (한국 체감 원). 목록에 없는 문장은 1만 원
	private static final Map<String, Integer> COST_FLOOR = Map.ofEntries(Map.entry("work_parents", 50000),
			Map.entry("cer_wedding", 50000), Map.entry("cer_wedding2", 30000), Map.entry("cer_funeral", 30000),
			Map.entry("cer_baby", 30000), Map.entry("cer_holiday", 50000), Map.entry("cer_reunion", 20000),
			Map.entry("cer_kidschool", 100000), Map.entry("exp_dentist", 30000), Map.entry("exp_appliance", 100000),
			Map.entry("exp_tv", 200000), Map.entry("exp_pipe", 200000), Map.entry("exp_roof", 300000),
			Map.entry("exp_trip", 200000), Map.entry("exp_parent_hosp", 100000), Map.entry("exp_glasses", 20000),
			Map.entry("exp_cane", 20000));

	// 일상 지출 최대 금액 (한국 체감 원)
	private static final Map<String, Integer> COST_CAP = Map.ofEntries(Map.entry("work_parents", 500000),
			Map.entry("cer_wedding", 300000), Map.entry("cer_wedding2", 200000), Map.entry("cer_funeral", 300000),
			Map.entry("cer_baby", 200000), Map.entry("cer_holiday", 300000), Map.entry("cer_reunion", 100000),
			Map.entry("exp_dentist", 500000), Map.entry("exp_pipe", 1500000), Map.entry("exp_roof", 1500000),
			Map.entry("exp_glasses", 100000), Map.entry("exp_cane", 100000));

	// 엔진이 만든 기록 한 줄 (화면용: 이벤트와 같은 모양, 효과 없음)
	private EventResultDTO noteEvent(String text) {
		EventResultDTO e = new EventResultDTO();
		e.setLogText(text);
		return e;
	}

	// 올해 바뀐 학교·직업 상태를 문장으로 (입학·진학·졸업·중퇴·은퇴)
	private List<String> milestones(LifeDTO life, String eduBefore, String schoolBefore, String jobBefore, String jobIdBefore) {
		List<String> notes = new ArrayList<>();
		String edu = life.getEduLevel();
		String school = life.getSchoolStatus();
		boolean wasIn = "ENROLLED".equals(schoolBefore);
		boolean isIn = "ENROLLED".equals(school);
		boolean levelChanged = edu != null && !edu.equals(eduBefore);

		if (wasIn && isIn && levelChanged) {
			notes.add(schoolObj(eduBefore) + " 마치고 " + SCHOOL_NAME.getOrDefault(edu, "학교") + "에 진학했다.");
		} else if (!wasIn && isIn) {
			notes.add(SCHOOL_NAME.getOrDefault(edu, "학교") + "에 " + ("ELEM".equals(edu) ? "입학했다." : "진학했다."));
		} else if (wasIn && "GRADUATED".equals(school)) {
			notes.add(schoolObj(edu) + " 졸업했다.");
		} else if (wasIn && "DROPPED".equals(school) && !PRISON_DROP.equals(life.getDropReason())) {
			// 원작 outWhy · dropOwn: 그만둔 까닭과 그 뒤 (예전엔 '고등학교를 그만두었다'만 덩그러니 나왔음)
			boolean poorDrop = life.getDropReason() != null && life.getDropReason().contains("형편");
			notes.add(poorDrop ? "집안 형편이 어려워져 " + schoolObj(edu) + " 그만두었다. 일을 해서 살림을 보태야 했다."
					: "공부에 흥미를 잃고 " + schoolObj(edu) + " 그만두었다. 교실보다 바깥이 더 궁금했다.");
		}

		if ("PRISON".equals(jobBefore) && !inPrison(life)) {
			int[] pr = prisonSpan(life);
			notes.add((pr == null ? "" : pr[1] + "년 만에 ") + "교도소에서 나왔다. 세상은 그사이 많이 바뀌어 있었다."
					+ " 전과가 있어 일자리를 구하기가 쉽지 않을 것 같았다.");
		}
		if ("EMPLOYED".equals(jobBefore) && "RETIRED".equals(life.getJobStatus())) {
			JobDTO job = jobIdBefore == null ? null : engineDao.selectJob(jobIdBefore);
			notes.add((job == null ? "일을 그만두고 은퇴했다." : job.getJobName() + " 일을 마치고 은퇴했다.") + pensionText(life));
		} else if (!"RETIRED".equals(jobBefore) && "RETIRED".equals(life.getJobStatus())) {
			notes.add("은퇴할 나이가 되었다." + pensionText(life)); // 일이 없던 사람도 정년 나이에 연금 판정 (jobStep 1-0)
		}
		return notes;
	}

	// ================= 퀘스트 (C12, 진행 중 달성) =================
	// 판정은 인생이 끝날 때 QuestJudgeService(민승님)가 하지만, 한 번 이루면 되돌릴 수 없는 퀘스트는
	// 진행 중에도 바로 '달성'으로 보여 주고 인생 기록에 한 줄 남김 (11/20 동현님: 대학 졸업했는데 완료 안 됨)
	//   대학·대학원 졸업, 자녀 N명, N세 넘기기, 기대수명보다 N년 더 살기 (같은 조건식은 QuestJudgeService.judgeOne)
	//   마지막 재산·행복·이민·평생 소득은 끝날 때 상태라 진행 중엔 판정하지 않음
	private static final String QUEST_DONE = "퀘스트 달성: ";

	private boolean questLive(LifeDTO life, LifeQuestDTO q) {
		String type = q.getQuestType(), target = q.getTargetValue();
		if (type == null || target == null) {
			return false;
		}
		try {
			String edu = life.getEduLevel();
			boolean grad = "GRADUATED".equals(life.getSchoolStatus());
			switch (type) {
			case "EDU":
				return "UNI".equals(target) ? ("UNI".equals(edu) && grad) || "GRAD".equals(edu) : "GRAD".equals(edu) && grad;
			case "CHILD":
				return life.getChildCount() >= Integer.parseInt(target);
			case "AGE":
				return life.getCurrentAge() >= Integer.parseInt(target);
			case "AGE_EXP":
				double le = life.getLifeExpectancy() != null ? life.getLifeExpectancy() : 70;
				return life.getCurrentAge() - le >= Double.parseDouble(target);
			default:
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
	}

	// 끝날 때 상태로 판정하는 퀘스트 (이민·재산·행복)가 지금 조건을 채우는지 (11/23: 진행 중에도 달성 표시)
	//   판정식은 QuestJudgeService.judgeOne 과 같음 (MIGRATE · ASSETS · HAPPINESS)
	private boolean questNow(LifeDTO life, LifeQuestDTO q) {
		String type = q.getQuestType(), target = q.getTargetValue();
		if (type == null) {
			return false;
		}
		try {
			switch (type) {
			case "MIGRATE":
				return life.getBirthCountry() != null && !life.getBirthCountry().equals(life.getCurrentCountry());
			case "ASSETS":
				return target != null && life.getAssets() >= Double.parseDouble(target);
			case "HAPPINESS":
				return target != null && life.getHappiness() >= Double.parseDouble(target);
			default:
				return false;
			}
		} catch (NumberFormatException e) {
			return false;
		}
	}

	// 화면용: 지금 이미 이룬 퀘스트는 live = Y (끝난 뒤 판정값 achieved 가 있으면 그것을 씀)
	private void fillQuestLive(LifeDTO life) {
		if (life.getQuests() == null) {
			return;
		}
		for (LifeQuestDTO q : life.getQuests()) {
			if (q.getAchieved() == null && questLive(life, q)) {
				q.setLive("Y");
			} else if (q.getAchieved() == null && questNow(life, q)) {
				q.setLive("NOW"); // 지금은 조건을 채움 (끝날 때 상태로 판정하는 퀘스트라 아직 바뀔 수 있음)
			}
		}
	}

	// 올해 새로 이룬 퀘스트 → 인생 기록 한 줄씩 (한 번만)
	private List<String> questNotes(LifeDTO life) {
		List<String> out = new ArrayList<>();
		List<LifeQuestDTO> qs;
		try {
			qs = engineDao.selectLifeQuests(life.getLifeId());
		} catch (RuntimeException e) {
			return out;
		}
		if (qs == null || qs.isEmpty()) {
			return out;
		}
		if (life.getLifeExpectancy() == null) {
			for (CountryStatDTO c : birthList(life.getBirthYear() / 10 * 10)) {
				if (c.getCountryCode().equals(life.getBirthCountry())) {
					life.setLifeExpectancy(c.getLifeExpectancy());
				}
			}
		}
		EngState s = engState(life);
		for (LifeQuestDTO q : qs) {
			String key = "Q:" + q.getQuestName();
			if (!s.done.containsKey(key) && questLive(life, q)) {
				s.done.put(key, life.getCurrentAge());
				out.add(QUEST_DONE + q.getQuestName());
			}
		}
		return out;
	}

	// 은퇴할 때 연금 한 줄 (원작 pensionBase: 정식 일자리였던 사람만, 금액은 한 달 기준 한국 체감)
	private String pensionText(LifeDTO life) {
		Long p = life.getPension();
		if (p != null && p / 12.0 * krwPerUsd(yearStat(life)) >= 10000) { // 한 달 1만 원도 안 되면 없는 것으로 (0원·5,000원 줄이 나왔음)
			return " 연금은 한 달에 약 " + won(life, p / 12.0) + "씩 나온다.";
		}
		return " 받을 연금이 없어 모아 둔 돈과 가족에 기대어 살아야 한다.";
	}

	// 몇 살 고비마다: 같은 해 같은 나라에서 태어난 또래 중 지금도 살아 있는 비율 (원작 생명표로 계산)
	private static final java.util.Set<Integer> SURVIVAL_AGES = java.util.Set.of(1, 5, 15, 30, 45, 60, 75, 90);

	private String survivalLine(LifeDTO life, CountryStatDTO stat, int age) {
		if (stat == null || !SURVIVAL_AGES.contains(age)) {
			return null;
		}
		double alive = 1;
		for (int a = 0; a < age; a++) {
			alive = alive * (1 - baseDeathProb(life.getCurrentCountry(), stat.getLifeExpectancy(),
					stat.getInfantMortality(), stat.getU5Mortality(), life.getGender(), a));
		}
		String country = stat.getNameKo() != null ? stat.getNameKo() : life.getCurrentCountry();
		String kid = "M".equals(life.getGender()) ? "남자아이" : "여자아이";
		long pct = Math.round(alive * 100);
		String part = pct >= 100 ? "거의 모두가" : "약 " + pct + "%가";
		return age + "세. 같은 해 " + country + "에서 태어난 " + kid + " 가운데 " + part + " 아직 살아 있다.";
	}

	// C09 '가게를 차린다'(LE_INVEST_BUSINESS)가 성공하면 자영업 사장이 됨 (예전엔 일자리 상태가 그대로라 다음 해 직업 제안이 떴음)
	private void startBusiness(LifeDTO life, int age, List<EventResultDTO> shown) {
		if ("ENROLLED".equals(life.getSchoolStatus()) || "EMPLOYED".equals(life.getJobStatus()) && "JOB_OWNER".equals(life.getJobId())) {
			return;
		}
		JobDTO job = engineDao.selectJob("JOB_OWNER");
		if (job == null) {
			return;
		}
		life.setJobId(job.getJobId());
		life.setJobStatus("EMPLOYED");
		life.setJobYears(0);
		life.setIncomeRate(job.getIncomeRate());
		life.setPrepScore(0);
		life.setJobName(job.getJobName());
		String note = "가게를 열어 " + job.getJobName() + "이 되었다.";
		saveEventLog(life, age, null, null, note);
		shown.add(noteEvent(note));
	}

	// ================= C04 나이대별 집중 선택 =================

	// 집중 선택 저장 (그 나이대에서 고를 수 있는 것만)
	@Transactional
	public LifeDTO chooseFocus(LifeDTO life, String focusCode) {
		if (!life.getStatus().equals("PLAYING") || life.getStageCode() == null) {
			return null;
		}
		if (!focusOptions(life.getStageCode(), life).contains(focusCode)) {
			return null;
		}
		life.setFocusCode(focusCode);
		engineDao.updateFocus(life);
		fillRegion(life);
		return life;
	}

	// ================= C06-3 돈 쓰기·투자 (원작 '돈 쓰기 · 투자') =================
	// 원작 구조를 따라 우리 엔진 값으로 새로 만듦: 여유 자금 굴리기(8가지) + 물건 사기 + 돈 쓰기(여행·용돈 …)
	//   - 투자 방식은 LIFE.INVEST_TYPE, 가진 물건은 LIFE.OWNS(쉼표 목록)에 저장
	//   - 해마다 investStep: 순자산이 0보다 많으면 그 방식의 수익률(평균 + 출렁임)만큼 불거나 줄어듦
	//   - 원작 장기 실질 수익률: 예금 1~1.5%, 채권 2%, 반반 3.5%, 주식 5%(±17%), 부동산 3.5%, 종목 사고팔기는 대부분 지수보다 못함
	//   - 돈 쓰기는 한 해 한 번씩 (서버 메모리에 그 나이 기록, 재시작하면 다시 할 수 있음)

	private static final String[] INV_IDS = { "CASH", "DEPOSIT", "BOND", "MIX", "STOCK", "WORLD", "PROPERTY", "TRADE" };
	private static final Map<String, String> INV_NAME = Map.of(
			"CASH", "집에 둔 현금", "DEPOSIT", "예금", "BOND", "채권", "MIX", "주식·채권 반반 펀드",
			"STOCK", "주식 (지수)", "WORLD", "해외 지수 (세계 주식)", "PROPERTY", "부동산 펀드", "TRADE", "종목 골라 사고팔기");
	private static final Map<String, String> INV_DESC = Map.of(
			"CASH", "이자 없음",
			"DEPOSIT", "물가 빼고 연 1~1.5%, 거의 안 출렁여요",
			"BOND", "평균 2% 안팎, 출렁임 작음",
			"MIX", "평균 3.5%, 출렁임 중간",
			"STOCK", "평균 5%, 해마다 ±17% 출렁여요",
			"WORLD", "평균 5%, 수수료가 조금 더 들고 환율 따라서도 출렁여요",
			"PROPERTY", "평균 3.5%, 출렁임 중간",
			"TRADE", "자주 사고팔면 수수료·세금 때문에 대부분 지수보다 덜 벌어요. 크게 버는 사람도 드물게 있어요");

	// 물건: 코드, 이름, 값(구매력 달러), 살 수 있는 나이, 나온 해, 행복
	private static final Object[][] ITEMS = {
			{ "PHONE", "스마트폰", 200, 13, 2010, 5 },
			{ "CELL", "휴대전화", 250, 13, 1995, 3 },
			{ "BIKE", "자전거", 150, 10, 1900, 3 },
			{ "MOTO", "오토바이", 1300, 18, 1960, 7 },
			{ "CAR", "자동차", 16000, 20, 1950, 8 },
			{ "TV", "텔레비전·냉장고", 600, 18, 1965, 5 },
			{ "PC", "컴퓨터·노트북", 700, 15, 1990, 4 },
	};
	private static final Map<String, String> OWN_NAME = Map.of(
			"PHONE", "스마트폰", "CELL", "휴대전화", "BIKE", "자전거", "MOTO", "오토바이", "CAR", "자동차",
			"TV", "텔레비전·냉장고", "PC", "컴퓨터·노트북", "HOUSE", "내 집");

	// 돈 쓰기를 올해 이미 했는지 (lifeId → 항목 → 나이)
	private final Map<Integer, Map<String, Integer>> doneThisYear = new ConcurrentHashMap<>();

	private boolean bankOK(LifeDTO life) {
		int year = life.getBirthYear() + life.getCurrentAge();
		return year >= 1990 || "Y".equals(life.getIsUrban()) || life.getWealthClass() >= 3;
	}

	private boolean marketOK(LifeDTO life, CountryStatDTO stat) {
		int year = life.getBirthYear() + life.getCurrentAge();
		return year >= 1975 && gdpOf(stat) >= 4000;
	}

	// 지금 나라·연도에서 고를 수 있는 투자 방식인지 (안 되면 이유)
	private String investWhy(LifeDTO life, CountryStatDTO stat, String id) {
		if ("CASH".equals(id)) {
			return null;
		}
		if (!bankOK(life)) {
			return "은행 계좌가 없어 모은 돈은 집에 현금으로 두고 있어요";
		}
		if ("DEPOSIT".equals(id)) {
			return null;
		}
		if (!marketOK(life, stat)) {
			return "이 나라·이 시절엔 보통 사람이 사고팔 수 있는 증권시장이 사실상 없어요";
		}
		if ("WORLD".equals(id) && life.getBirthYear() + life.getCurrentAge() < 1995) {
			return "1995년쯤부터 해외 지수 펀드를 살 수 있어요";
		}
		return null;
	}

	// 실제로 쓰는 투자 방식 (고른 게 지금은 안 되면 예금 또는 현금)
	private String investNow(LifeDTO life, CountryStatDTO stat) {
		String t = life.getInvestType();
		if (t != null && investWhy(life, stat, t) == null) {
			return t;
		}
		return bankOK(life) ? "DEPOSIT" : "CASH";
	}

	// 한 해 수익률 (원작처럼 평균 + 정규분포 출렁임)
	private double investReturn(String t) {
		double z = random.nextGaussian();
		switch (t) {
		case "DEPOSIT":
			return 0.012 + 0.004 * z;
		case "BOND":
			return clamp(0.02 + 0.04 * z, -0.15, 0.2);
		case "MIX":
			return clamp(0.035 + 0.09 * z, -0.3, 0.35);
		case "STOCK":
			return clamp(0.05 + 0.17 * z, -0.5, 0.6);
		case "WORLD":
			return clamp(0.045 + 0.15 * z + 0.07 * random.nextGaussian(), -0.5, 0.6);
		case "PROPERTY":
			return clamp(0.035 + 0.1 * z, -0.3, 0.35);
		case "TRADE":
			return clamp(0.05 + 0.17 * z - 0.055 + 0.25 * random.nextGaussian(), -0.9, 2);
		default:
			return 0;
		}
	}

	private static java.util.LinkedHashSet<String> ownSet(LifeDTO life) {
		java.util.LinkedHashSet<String> s = new java.util.LinkedHashSet<>();
		if (life.getOwns() != null) {
			for (String o : life.getOwns().split(",")) {
				if (!o.isBlank()) {
					s.add(o.trim());
				}
			}
		}
		return s;
	}

	// ⑤-2 해마다: 모은 돈 굴리기 + 가진 물건 유지비·효과 (eventStep 뒤, 사망 판정 전)
	private void investStep(LifeDTO life, CountryStatDTO stat) {
		int age = life.getCurrentAge();
		java.util.Set<String> owns = ownSet(life);
		double med = medIncome(life.getCurrentCountry(), stat);
		if (owns.contains("CAR")) {
			double c = med * 0.04;
			life.setAssets(life.getAssets() - Math.round(c));
			book(life, "자동차 유지비", -c);
		}
		if (owns.contains("MOTO")) {
			double c = med * 0.01;
			life.setAssets(life.getAssets() - Math.round(c));
			book(life, "오토바이 유지비", -c);
		}
		// 유지비는 돈이 없어도 냄 (모자라면 빚). 빚이 중위 소득의 절반을 넘게 쌓였는데 차·오토바이가 있으면 팔아서 빚을 줄임
		//   중고값 = 지금 새것 값의 35% (오토바이 40%), 판 해엔 행복 −4
		if (life.getAssets() < -med * 0.5 && (owns.contains("CAR") || owns.contains("MOTO"))) {
			String id = owns.contains("CAR") ? "CAR" : "MOTO";
			long resale = 0;
			for (Object[] it : ITEMS) {
				if (id.equals(it[0])) {
					resale = Math.round(((Integer) it[2]) * priceK(stat) * ("CAR".equals(id) ? 0.35 : 0.4));
				}
			}
			owns.remove(id);
			life.setOwns(String.join(",", owns));
			life.setAssets(life.getAssets() + resale);
			life.setHappiness(life.getHappiness() - 4);
			book(life, OWN_NAME.get(id) + " 판 돈", resale);
			String note = "빚이 불어나 " + OWN_NAME.get(id) + "를 팔았다. 손에 쥔 돈은 " + won(life, resale) + "뿐이었다.";
			saveEventLog(life, age, null, null, note);
			List<EventResultDTO> ev = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
			ev.add(noteEvent(note));
			life.setEvents(ev);
		}
		if (owns.contains("BIKE") && age < 70) {
			life.setHealth(life.getHealth() + 0.4); // 타고 다니면 건강 조금
		}
		houseStep(life, stat);
		// 굴리는 돈 = 순자산에서 내 집 시세를 뺀 몫 (집은 houseStep 이 따로 오르내림)
		long money = life.getAssets() - (life.getHouseValue() == null ? 0 : life.getHouseValue());
		if (money <= 0 || age < 15) {
			return;
		}
		String t = investNow(life, stat);
		double r = investReturn(t);
		double gain = money * r;
		if (Math.round(gain) == 0) {
			return;
		}
		life.setAssets(life.getAssets() + Math.round(gain));
		book(life, "DEPOSIT".equals(t) ? "예금 이자" : INV_NAME.get(t) + (gain >= 0 ? " 수익" : " 손실"), gain);
		String note = null;
		int pct = (int) Math.round(Math.abs(r) * 100);
		// 원작 invYear: 큰 오르내림만, 3년 안에는 다시 쓰지 않음 (45% 넘게 오르거나 35% 넘게 떨어지면 예외)
		//   문장도 원작대로 (예전 '주식 (지수)가 크게 올라 맡긴 돈이…'는 어색했고 2년마다 나오기도 했음)
		EngState ms = engState(life);
		boolean extreme = r >= 0.45 || r <= -0.35;
		boolean mktOk = extreme || ms.mktL == null || age - ms.mktL >= 3;
		String nm = "STOCK".equals(t) ? "주식" : "WORLD".equals(t) ? "해외 지수" : INV_NAME.get(t);
		if (r <= -0.18 && mktOk) {
			note = ("STOCK".equals(t) ? "주식시장이 폭락해 투자금의 " : "시장이 크게 떨어져 " + nm + "에 넣어 둔 돈의 ") + pct + "%가 사라졌다. (" + wonSigned(life, gain) + ")";
		} else if (r >= 0.25 && mktOk) {
			note = iga(nm) + " 크게 올라 투자금이 " + pct + "% 불었다. (" + wonSigned(life, gain) + ")";
		}
		if (note != null) {
			ms.mktL = age;
		}
		if (note != null) {
			saveEventLog(life, age, null, null, note);
			List<EventResultDTO> ev = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
			ev.add(noteEvent(note));
			life.setEvents(ev);
		}
	}

	// ================= 내 집 시세 · 재산 합계 (11/20) =================
	// 집값 = 중위 소득 × 5 (도시 ×1.4, 시골 ×0.7) - 원작과 같은 식, 산 뒤에는 HOUSE_VALUE 가 해마다 그 나라 집값 흐름만큼 오르내림
	//   집값 흐름(물가 뺀 실질): 평균 +2%, 해마다 ±6% 출렁임 - 같은 나라·같은 해면 누구에게나 같은 값 (나라·연도로 씨앗)
	//   + 실제 큰 흐름: 한국 1998 외환 위기·2023 하락, 2002·2006·2020·2021 급등 / 일본 1991~1995 거품 붕괴 /
	//     아시아 외환 위기 1997~1998 (태국·인도네시아·말레이시아·홍콩) / 중국 2022~2024 / 세계 금융 위기 2008 (모든 나라)
	private static final Map<String, Double> HOUSE_SHOCK = Map.ofEntries(
			Map.entry("KOR1998", -0.15), Map.entry("KOR2002", 0.12), Map.entry("KOR2006", 0.12),
			Map.entry("KOR2020", 0.10), Map.entry("KOR2021", 0.10), Map.entry("KOR2023", -0.10),
			Map.entry("JPN1991", -0.08), Map.entry("JPN1992", -0.08), Map.entry("JPN1993", -0.08), Map.entry("JPN1994", -0.06),
			Map.entry("JPN1995", -0.06), Map.entry("HKG1998", -0.30), Map.entry("CHN2022", -0.06), Map.entry("CHN2023", -0.06),
			Map.entry("CHN2024", -0.06), Map.entry("THA1997", -0.15), Map.entry("IDN1998", -0.20), Map.entry("MYS1998", -0.12),
			Map.entry("ALL2008", -0.06));

	// 원작 집값 (11/23 동현님: 오르기만 함): 물가 뺀 실질로 해마다 +0.2% + 시장 순환
	//   시장 수준 = 지난해의 85% + 새 충격 ±5% (−60~+60%) → 몇 해 오르면 몇 해 내리는 오르내림, 길게 보면 거의 제자리
	//   그해 변화 = 0.2% + (올해 시장 수준 − 지난해 시장 수준) + 실제 큰 흐름 (−30~+25%)
	private double houseMarket(String cc, int year) {
		double m = 0;
		for (int y = year - 60; y <= year; y++) {
			Random g = new Random(cc.hashCode() * 1000003L + y);
			m = clamp(0.85 * m + 0.05 * g.nextGaussian(), -0.6, 0.6);
		}
		return m;
	}

	private double houseReturn(String cc, int year) {
		double r = 0.002 + houseMarket(cc, year) - houseMarket(cc, year - 1);
		r = r + HOUSE_SHOCK.getOrDefault(cc + year, 0.0) + HOUSE_SHOCK.getOrDefault("ALL" + year, 0.0);
		return clamp(r, -0.3, 0.25);
	}

	private double housePriceOf(LifeDTO life, CountryStatDTO stat) {
		return medIncome(life.getCurrentCountry(), stat) * 5 * ("Y".equals(life.getIsUrban()) ? 1.4 : 0.7);
	}

	// 해마다 내 집 시세: 오르내린 만큼 순자산에도 더함 (주식처럼 '지난 1년 순자산' 내역에 한 줄, 5% 넘게 움직이면 인생 기록에도)
	private void houseStep(LifeDTO life, CountryStatDTO stat) {
		life.setHouseRate(null);
		if (!ownSet(life).contains("HOUSE")) {
			life.setHouseValue(null);
			return;
		}
		if (life.getHouseValue() == null) {
			life.setHouseValue(Math.round(housePriceOf(life, stat))); // 시세를 적기 전에 산 집
		}
		int age = life.getCurrentAge();
		double r = houseReturn(life.getCurrentCountry(), life.getBirthYear() + age);
		long v = life.getHouseValue();
		long d = Math.round(v * r);
		life.setHouseValue(Math.max(0, v + d));
		life.setHouseRate(r);
		if (d == 0) {
			return;
		}
		life.setAssets(life.getAssets() + d);
		book(life, d > 0 ? "내 집 시세 오름" : "내 집 시세 내림", d);
		// 원작처럼 크게(10% 넘게) 움직인 해만, 5년에 한 번까지 기록 (작은 오르내림은 순자산 내역에만)
		int pct = (int) Math.round(Math.abs(r) * 100);
		EngState hs = engState(life);
		Integer lastHp = hs.done.get("HP");
		if (pct < 10 || age < 18 || lastHp != null && age - lastHp < 5) {
			return;
		}
		hs.done.put("HP", age);
		String note = (d > 0 ? "동네 집값이 크게 올라 집값이 " + pct + "% 뛰었다. (" : "집값이 한 해 사이 " + pct + "% 떨어졌다. (")
				+ wonSigned(life, d) + ", 지금 시세 " + won(life, v + d) + ")";
		saveEventLog(life, age, null, null, note);
		List<EventResultDTO> ev = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
		ev.add(noteEvent(note));
		life.setEvents(ev);
	}

	// [화면용] 재산 합계: 내 집 시세(순자산 안에 들어 있음) · 자동차·오토바이 중고 시세(산 값을 이미 써서 순자산 밖)
	private void fillWorth(LifeDTO life, CountryStatDTO stat) {
		java.util.Set<String> owns = ownSet(life);
		if (owns.contains("HOUSE") && life.getHouseValue() == null) {
			life.setHouseValue(Math.round(housePriceOf(life, stat)));
		}
		long car = 0;
		for (Object[] it : ITEMS) {
			if (("CAR".equals(it[0]) || "MOTO".equals(it[0])) && owns.contains((String) it[0])) {
				car = car + Math.round(((Integer) it[2]) * priceK(stat) * ("CAR".equals(it[0]) ? 0.35 : 0.4));
			}
		}
		life.setCarValue(car > 0 ? car : null);
	}

	// 돈 쓰기·투자 창 목록
	public ShopDTO shop(LifeDTO life) {
		CountryStatDTO stat = yearStat(life);
		double med = medIncome(life.getCurrentCountry(), stat);
		int age = life.getCurrentAge();
		int year = life.getBirthYear() + age;
		java.util.Set<String> owns = ownSet(life);
		long housePrice = Math.round(med * 5 * ("Y".equals(life.getIsUrban()) ? 1.4 : 0.7));
		// 쓸 수 있는 돈: 순자산에서 집에 묶인 계약금(집값의 20%)은 빼고 (집을 사도 그 돈을 또 쓰지 못하게)
		long cash = Math.max(0, life.getAssets() - (owns.contains("HOUSE") ? Math.round(housePrice * 0.2) : 0));
		Map<String, Integer> did = doneThisYear.getOrDefault(life.getLifeId(), Map.of());

		List<ShopItemDTO> items = new ArrayList<>();
		// 물건 사기
		for (Object[] it : ITEMS) {
			String id = (String) it[0];
			int minAge = (Integer) it[3];
			int from = (Integer) it[4];
			if (age < minAge || year < from) {
				continue;
			}
			if ("CELL".equals(id) && (owns.contains("PHONE") || year >= 2015)) {
				continue; // 스마트폰 시대엔 휴대전화 대신 스마트폰
			}
			if ("MOTO".equals(id) && owns.contains("CAR")) {
				continue;
			}
			long price = Math.round(((Integer) it[2]) * priceK(stat));
			String desc = "행복 +" + it[5];
			if ("CAR".equals(id)) {
				desc = desc + " · 해마다 유지비가 들어요";
			} else if ("BIKE".equals(id)) {
				desc = desc + " · 타고 다니면 건강 ↑";
			} else if ("PC".equals(id)) {
				desc = desc + " · 지식 +2";
			} else if ("MOTO".equals(id)) {
				desc = desc + " · 유지비가 조금 들어요";
			}
			ShopItemDTO d = new ShopItemDTO("BUY_" + id, "BUY", (String) it[1], desc, price);
			d.setDone(owns.contains(id));
			if (d.isDone()) {
				d.setOk(false);
				d.setWhy("이미 가지고 있어요");
			} else if (cash < price) {
				d.setOk(false);
				d.setWhy("돈이 모자라요");
			}
			items.add(d);
		}
		// 내 집 마련: 집값의 20%가 있으면 나머지는 대출 (이자는 갚는 동안 모두 합쳐 25%)
		if (age >= 26) {
			long price = housePrice;
			ShopItemDTO d = new ShopItemDTO("BUY_HOUSE", "BUY", "내 집 마련", "행복 +10 · 집값의 20%가 있으면 나머지는 대출 · 산 뒤엔 집값 따라 시세가 오르내려요", price);
			d.setDone(owns.contains("HOUSE"));
			if (d.isDone()) {
				d.setOk(false);
				d.setWhy("이미 내 집이 있어요");
			} else if (cash < price * 0.2) {
				d.setOk(false);
				d.setWhy("계약금(집값의 20%)이 모자라요");
			} else if (age >= 65 && cash < price) {
				d.setOk(false);
				d.setWhy("이 나이에는 대출이 어려워 집값 전부가 필요해요");
			} else if (cash < price && price - cash > Math.max(0,
					Math.max(life.getIncome(), spouseIncomeOf(life)) * 5 - Math.max(0, -life.getAssets()))) {
				d.setOk(false); // 신용 관리: 빚 합계가 벌이의 5배를 넘으면 대출이 안 나옴 (이벤트 집 사기와 같은 기준)
				d.setWhy("벌이에 비해 대출이 너무 커서 은행이 빌려주지 않아요");
			}
			items.add(d);
		}
		// 돈 쓰기 (한 해 한 번)
		addDo(items, did, age, cash, "DO_TRIP", "국내 여행", "행복 +4 · 관계 +1", med * 0.04, age >= 15);
		addDo(items, did, age, cash, "DO_ABROAD", "해외 여행", "행복 +7 · 지식 +1", med * 0.2, age >= 18 && year >= 1989);
		addDo(items, did, age, cash, "DO_PARENTS", "부모님 용돈·병원비", "관계 +5 · 행복 +2", med * 0.06,
				age >= 20 && !"N".equals(life.getParentAlive()));
		addDo(items, did, age, cash, "DO_PARTY", "친구들에게 한턱내기", "관계 +3 · 행복 +1", med * 0.015, age >= 15);
		addDo(items, did, age, cash, "DO_KIDS", "자녀 학원·과외", "아이 공부에 보태요 · 관계 +2", med * 0.1,
				life.getChildCount() > 0 && age >= 28);
		addDo(items, did, age, cash, "DO_CHECKUP", "건강검진", "건강 +3", med * 0.02, age >= 30);
		addDo(items, did, age, cash, "DO_COURSE", "강좌·학원 등록", "지식 +4", med * 0.05, age >= 15);
		addDo(items, did, age, cash, "DO_REMODEL", "집 수리·인테리어", "행복 +5", med * 0.25, owns.contains("HOUSE"));

		// 여유 자금 굴리기
		String now = investNow(life, stat);
		List<ShopItemDTO> inv = new ArrayList<>();
		for (String id : INV_IDS) {
			ShopItemDTO d = new ShopItemDTO("INV_" + id, "INVEST", INV_NAME.get(id), INV_DESC.get(id), 0);
			String why = investWhy(life, stat, id);
			d.setOk(why == null);
			d.setWhy(why);
			d.setDone(id.equals(now));
			inv.add(d);
		}

		ShopDTO out = new ShopDTO();
		out.setItems(items);
		out.setInvests(inv);
		out.setInvestType(now);
		out.setOwns(String.join(",", owns));
		out.setAssets(life.getAssets());
		fillWorth(life, stat);
		out.setHouseValue(life.getHouseValue());
		out.setCarValue(life.getCarValue());
		out.setCash(cash);
		out.setHealth(life.getHealth());
		out.setHappiness(life.getHappiness());
		out.setKnowledge(life.getKnowledge());
		out.setRelation(life.getRelation());
		out.setKrwPerUsd(krwPerUsd(stat));
		return out;
	}

	private void addDo(List<ShopItemDTO> items, Map<String, Integer> did, int age, long cash, String id, String name,
			String desc, double cost, boolean show) {
		if (!show) {
			return;
		}
		ShopItemDTO d = new ShopItemDTO(id, "DO", name, desc, Math.max(1, Math.round(cost)));
		Integer at = did.get(id);
		d.setDone(at != null && at == age);
		if (d.isDone()) {
			d.setOk(false);
			d.setWhy("올해는 이미 했어요");
		} else if (cash < d.getPrice()) {
			d.setOk(false);
			d.setWhy("돈이 모자라요");
		}
		items.add(d);
	}

	// 물건값 나라 보정: 잘사는 나라일수록 조금 비쌈 (구매력 달러 기준이라 크게는 안 다름)
	private double priceK(CountryStatDTO stat) {
		return clamp(Math.pow(Math.max(gdpOf(stat), 500) / 15000, 0.15), 0.7, 1.3);
	}

	// 사기·하기 (logAge: 화면이 보고 있는 나이, 서버 나이와 한 살 차이까지만)
	//   돌려주는 문구는 인생 기록에도 남김. 못 하면 null + 이유는 shopBlocked
	@Transactional
	public ShopDTO buy(LifeDTO life, String itemId, Integer logAge) {
		if (!"PLAYING".equals(life.getStatus()) || itemId == null) {
			return null;
		}
		ShopItemDTO it = null;
		ShopDTO list = shop(life);
		for (ShopItemDTO d : list.getItems()) {
			if (d.getId().equals(itemId)) {
				it = d;
			}
		}
		if (it == null || !it.isOk()) {
			return null;
		}
		int age = logAge == null ? life.getCurrentAge() : Math.max(life.getCurrentAge() - 1, Math.min(life.getCurrentAge(), logAge));
		java.util.LinkedHashSet<String> owns = ownSet(life);
		String text;
		if ("BUY_HOUSE".equals(itemId)) {
			EngState hs = engState(life);
			boolean small = hs.houseSm > 0;
			long price = small ? hs.houseSm : it.getPrice(); // 작은 집이면 그 값
			hs.houseSm = 0;
			long cash = list.getCash(); // 쓸 수 있는 돈 (shop 과 같은 기준)
			long help = Math.round(homeHelp(life, yearStat(life), price, cash)); // 수저별 부모님 도움 (원작 homeHelp)
			if (help > 0) {
				life.setAssets(life.getAssets() + help);
				cash = cash + help;
				engState(life).done.put("PAR_HOUSE", life.getCurrentAge());
			}
			long loan = Math.max(0, price - cash);
			long interest = Math.round(loan * 0.25);
			String helpNote = help > 0 ? "부모님 도움 " + won(life, help) + ", " : "";
			life.setAssets(life.getAssets() - interest); // 집값은 재산으로 남고(현금 → 집) 이자만 나감
			life.setHappiness(life.getHappiness() + 10);
			owns.add("HOUSE");
			life.setHouseValue(price); // 이 값에서 해마다 집값 흐름만큼 오르내림 (houseStep)
			text = (help > 0 ? "부모님의 도움을 받아 " : "") + (small ? "작은 집이지만 내 집을 마련했다!" : "내 집을 마련했다!")
					+ " (" + helpNote + "집값 " + won(life, price) + (loan > 0 ? ", 대출 " + won(life, loan) + " · 이자 약 " + won(life, interest) : "") + ")";
		} else if (it.getKind().equals("BUY")) {
			String code = itemId.substring(4);
			life.setAssets(life.getAssets() - it.getPrice());
			for (Object[] row : ITEMS) {
				if (row[0].equals(code)) {
					life.setHappiness(life.getHappiness() + (Integer) row[5]);
				}
			}
			if ("PC".equals(code)) {
				life.setKnowledge(life.getKnowledge() + 2);
			}
			if ("PHONE".equals(code)) {
				owns.remove("CELL");
			}
			owns.add(code);
			text = OWN_NAME.get(code) + josa(OWN_NAME.get(code)) + " 샀다. (−" + won(life, it.getPrice()) + ")";
		} else {
			life.setAssets(life.getAssets() - it.getPrice());
			switch (itemId) {
			case "DO_TRIP":
				life.setHappiness(life.getHappiness() + 4);
				life.setRelation(life.getRelation() + 1);
				text = "국내 여행을 다녀왔다.";
				break;
			case "DO_ABROAD":
				life.setHappiness(life.getHappiness() + 7);
				life.setKnowledge(life.getKnowledge() + 1);
				text = "해외여행을 다녀왔다.";
				break;
			case "DO_PARENTS":
				life.setRelation(life.getRelation() + 5);
				life.setHappiness(life.getHappiness() + 2);
				text = "부모님께 용돈과 병원비를 드렸다.";
				break;
			case "DO_PARTY":
				life.setRelation(life.getRelation() + 3);
				life.setHappiness(life.getHappiness() + 1);
				text = "친구들에게 한턱냈다.";
				break;
			case "DO_KIDS":
				life.setRelation(life.getRelation() + 2);
				text = "아이를 학원에 보냈다.";
				break;
			case "DO_CHECKUP":
				life.setHealth(life.getHealth() + 3);
				text = "건강검진을 받았다.";
				break;
			case "DO_COURSE":
				life.setKnowledge(life.getKnowledge() + 4);
				text = "강좌에 등록해 배웠다.";
				break;
			case "DO_REMODEL":
				life.setHappiness(life.getHappiness() + 5);
				text = "집을 고치고 꾸몄다.";
				break;
			default:
				return null;
			}
			text = text + " (−" + won(life, it.getPrice()) + ")";
			doneThisYear.computeIfAbsent(life.getLifeId(), k -> new ConcurrentHashMap<>()).put(itemId, life.getCurrentAge());
		}
		if (doneThisYear.size() > 20000) {
			doneThisYear.clear();
		}
		life.setOwns(String.join(",", owns));
		fixStats(life);
		engineDao.updateShop(life);
		saveEventLog(life, age, null, null, text);
		ShopDTO out = shop(life);
		out.setMessage(text);
		out.setLogAge(age);
		return out;
	}

	// 못 산 이유 (컨트롤러 400 문구)
	public String shopBlocked(LifeDTO life, String itemId) {
		for (ShopItemDTO d : shop(life).getItems()) {
			if (d.getId().equals(itemId)) {
				return d.isOk() ? "지금은 할 수 없어요." : d.getWhy() + ".";
			}
		}
		return "지금 고를 수 없는 항목이에요.";
	}

	// 여유 자금 굴리기 방식 바꾸기
	@Transactional
	public ShopDTO invest(LifeDTO life, String type, Integer logAge) {
		if (!"PLAYING".equals(life.getStatus()) || type == null || !INV_NAME.containsKey(type)) {
			return null;
		}
		CountryStatDTO stat = yearStat(life);
		if (investWhy(life, stat, type) != null) {
			return null;
		}
		int age = logAge == null ? life.getCurrentAge() : Math.max(life.getCurrentAge() - 1, Math.min(life.getCurrentAge(), logAge));
		life.setInvestType(type);
		engineDao.updateShop(life);
		String text = "모은 돈을 " + INV_NAME.get(type) + "에 맡기기로 했다.";
		saveEventLog(life, age, null, null, text);
		ShopDTO out = shop(life);
		out.setMessage(text);
		out.setLogAge(age);
		return out;
	}

	// 을/를
	private static String josa(String w) {
		if (w == null || w.isEmpty()) {
			return "을";
		}
		char c = w.charAt(w.length() - 1);
		if (c < 0xAC00 || c > 0xD7A3) {
			return "을";
		}
		return (c - 0xAC00) % 28 == 0 ? "를" : "을";
	}

	// ================= C06-2 직업 선택 =================

	// 제안받은 직업 중 하나를 선택 (올해 제안받은 3개 안에서만)
	@Transactional
	public LifeDTO chooseJob(LifeDTO life, String jobId) {
		if (!life.getStatus().equals("PLAYING")) {
			return null;
		}
		if (life.getCurrentAge() < JOB_MIN_AGE || "ENROLLED".equals(life.getSchoolStatus())
				|| "EMPLOYED".equals(life.getJobStatus()) || "RETIRED".equals(life.getJobStatus())) {
			return null; // 지금은 직업을 고를 수 없는 상태
		}
		// 올해 제안받은 3개 안에 있는 직업만 고를 수 있음 (같은 조건이면 같은 제안이 다시 나옴)
		CountryStatDTO stat = yearStat(life);
		if (!canGetJob(life, stat)) {
			return null;
		}
		JobDTO job = null;
		for (JobDTO o : offerJobs(life)) {
			if (o.getJobId().equals(jobId)) {
				job = o;
			}
		}
		if (job == null) {
			return null; // 제안받지 않은 직업
		}

		life.setJobId(job.getJobId());
		life.setJobStatus("EMPLOYED");
		life.setJobYears(0);
		life.setIncomeRate(job.getIncomeRate());
		life.setPrepScore(0);
		addExperience(life, job.getJobId()); // 직업 경험 (C05)

		// 직장인이 됐으니 나이대 변경 → 집중 다시 선택
		String newStage = stageOf(life);
		if (newStage != null && !newStage.equals(life.getStageCode())) {
			life.setStageCode(newStage);
			life.setFocusCode(null);
			life.setNeedFocus(true);
			life.setFocusOptions(focusOptions(newStage, life));
		}
		engineDao.updateLifeYear(life);
		life.setJobName(job.getJobName());
		// 인생 기록: 고른 직업 (선택 이벤트처럼 방금 지나간 해에 기록)
		String note = job.getJobName() + " 일을 시작했다.";
		saveEventLog(life, life.getCurrentAge() - 1, null, null, note);
		List<EventResultDTO> shown = new ArrayList<>();
		shown.add(noteEvent(note));
		String fund = startFund(life, stat); // 수저별 살림 밑천 (원작)
		if (fund != null) {
			saveEventLog(life, life.getCurrentAge() - 1, null, null, fund);
			shown.add(noteEvent(fund));
			engineDao.updateLifeYear(life);
		}
		life.setEvents(shown);
		fillRegion(life);
		return life;
	}

	// 나이대 판정 (원작 기준)
	private String stageOf(LifeDTO life) {
		int age = life.getCurrentAge();
		String level = life.getEduLevel();
		boolean enrolled = "ENROLLED".equals(life.getSchoolStatus());

		if (age < 6 || (age < startAges[0] && !enrolled)) {
			return null; // 0~5세, 초등학교 입학 전에는 집중 선택 없음
		}
		if (enrolled && (level.equals("UNI") || level.equals("GRAD"))) {
			return "UNIV"; // 대학생
		}
		if (enrolled) {
			return "STUDENT"; // 초·중·고 학생
		}
		if (age >= 65 || "RETIRED".equals(life.getJobStatus())) {
			return "SENIOR"; // 노년 (65세 이상 또는 은퇴, 일하고 있으면 '일에 몰두' 추가)
		}
		if ("EMPLOYED".equals(life.getJobStatus())) {
			return "WORKER"; // 직장인
		}
		if (age < 18) {
			return "YOUTH"; // 학교·일 없는 청소년 (6~17세, 명세서 C04-1)
		}
		return "JOBLESS"; // 학교·일 없는 18세 이상 (무직 성인)
	}

	// 나이대별로 고를 수 있는 집중 목록 (원작 기준 + 나이에 맞게 조금씩 바뀜)
	//   학생: 공부·친구·집안 돕기·운동·취미 + 15세부터 진로 준비(취업 준비)
	//   대학생·직장인·무직: 18~39세 미혼이면 연애·만남 (LOVE)
	//   무직 40세 미만: 취업 준비, 노년: 종교·봉사 (원작 faith)
	private List<String> focusOptions(String stage, LifeDTO life) {
		List<String> list = new ArrayList<>();
		int age = life.getCurrentAge();
		boolean single = !"Y".equals(life.getMarriedYn());
		boolean loveAge = age >= 18 && age < 40 && single;
		if (stage.equals("STUDENT")) {
			list.add("STUDY"); list.add("PLAY"); list.add("HOUSE"); list.add("SPORT"); list.add("HOBBY");
			if (age >= 15) {
				list.add("JOB_PREP");
			}
		} else if (stage.equals("YOUTH")) {
			list.add("HOUSE"); list.add("PLAY"); list.add("SPORT"); list.add("HOBBY");
		} else if (stage.equals("UNIV")) {
			list.add("STUDY"); list.add("PLAY"); list.add("PARTTIME"); list.add("JOB_PREP"); list.add("SPORT"); list.add("HOBBY");
			if (loveAge) {
				list.add("LOVE");
			}
		} else if (stage.equals("WORKER")) {
			list.add("WORK"); list.add("BALANCE"); list.add("FAMILY"); list.add("HEALTH"); list.add("SELF_DEV"); list.add("HOBBY");
			if (loveAge) {
				list.add("LOVE");
			}
		} else if (stage.equals("JOBLESS")) {
			list.add("FAMILY"); list.add("HEALTH"); list.add("SELF_DEV"); list.add("HOBBY"); list.add("REST");
			if (age < 40) {
				list.add("JOB_PREP");
			}
			if (loveAge) {
				list.add("LOVE");
			}
		} else if (stage.equals("SENIOR")) {
			if ("EMPLOYED".equals(life.getJobStatus())) {
				list.add("WORK");
			}
			list.add("FAMILY"); list.add("HEALTH"); list.add("BRAIN"); list.add("HOBBY"); list.add("REST"); list.add("FAITH");
		}
		return list;
	}

	// 고르지 않았을 때 기본값 (명세서 C04-5): 학생·대학생 공부, 비학생 청소년 집안 돕기, 직장인 균형,
	//   무직 성인 자기계발, 노년 건강 관리 (목록에 없으면 목록 첫 번째)
	private String defaultFocus(LifeDTO life) {
		String stage = life.getStageCode();
		List<String> list = focusOptions(stage, life);
		String want = "BALANCE";
		if ("STUDENT".equals(stage) || "UNIV".equals(stage)) {
			want = "STUDY";
		} else if ("YOUTH".equals(stage)) {
			want = "HOUSE";
		} else if ("JOBLESS".equals(stage)) {
			want = "SELF_DEV";
		} else if ("SENIOR".equals(stage)) {
			want = "HEALTH";
		}
		return list.contains(want) ? want : list.get(0);
	}

	// 집중별 1년 능력치 효과 (원작 FOCUS 수치: 건강, 행복, 관계)
	// 지식은 여기서 바로 더하지 않고 C05 지식 계산(knowEff)에서 반영 (원작 기준)
	// 건강 관리의 건강 효과는 나이가 들수록 줄어듦 (원작 fitK)
	private void applyFocus(LifeDTO life) {
		String f = life.getFocusCode();
		if (f == null || inPrison(life)) { // 0~5세 · 수감 중
			return;
		}
		double hl = 0, hp = 0, re = 0;
		switch (f) {
		case "STUDY":    hp = -1; re = -1;          break; // 공부 (지식 +, C05)
		case "PLAY":     re = 4; hp = 4;            break; // 친구·놀이
		case "HOUSE":    re = 2;                    break; // 집안 돕기
		case "SPORT":    hl = 4; hp = 2; re = 1;    break; // 운동
		case "HOBBY":    hp = 4;                    break; // 취미
		case "PARTTIME": hp = -1; re = 1;           break; // 아르바이트 (소득은 C06)
		case "JOB_PREP": hp = -2;                   break; // 취업 준비 (준비 점수는 C06)
		case "WORK":     hl = -3; hp = -2; re = -2; break; // 일에 몰두 (소득은 C06)
		case "BALANCE":  hl = 1; hp = 1; re = 1;    break; // 균형
		case "FAMILY":   re = 5; hp = 3;            break; // 가족·친구
		case "HEALTH":   hl = 5 * fitK(life.getCurrentAge()); hp = 1; break; // 건강 관리
		case "SELF_DEV": hp = -1;                   break; // 자기계발 (지식 +, C05)
		case "BRAIN":    hp = 1; re = 1;            break; // 두뇌 활동 (지식 +, C05)
		case "REST":     hl = 2; hp = 2;            break; // 휴식
		case "LOVE":     re = 2; hp = 3;            break; // 연애·만남 (결혼 확률은 윤아님 연애·결혼 템플릿 보정)
		case "FAITH":    re = 3; hp = 3;            break; // 종교·봉사 (원작 faith)
		}
		life.setHealth(life.getHealth() + hl);
		life.setHappiness(life.getHappiness() + hp);
		life.setRelation(life.getRelation() + re);
	}

	// ================= C05 능력치 =================

	// 나이별 기대 건강 (원작 기준): 0세 72, 1~4세 80, 5~29세 90, 30~59세 매년 -0.6, 60세부터 매년 -0.95 (최소 15)
	private double expHealth(int age) {
		if (age < 1) {
			return 72;
		} else if (age < 5) {
			return 80;
		} else if (age < 30) {
			return 90;
		} else if (age < 60) {
			return 90 - (age - 30) * 0.6;
		}
		return Math.max(15, 72 - (age - 60) * 0.95);
	}

	// 자연 변화 (원작 기준)
	private void naturalChange(LifeDTO life, CountryStatDTO stat) {
		int age = life.getCurrentAge();
		String cc = life.getCurrentCountry();
		String focus = life.getFocusCode();
		double c = clamp((age - 14) / 6.0, 0, 1); // 15~20세에 걸쳐 어른 기준으로 바뀜
		boolean student = "ENROLLED".equals(life.getSchoolStatus());

		// 1. 지식 (원작 knowYear): 지식 = 또래 가운데 위치 = 100 × Φ(재능 × 나이 비중 + 쌓은 실력)
		//    쌓은 실력은 해마다 조금씩 빠지고(25세 전 ×0.97, 그 후 ×0.985), 집중·학교에 따라 더해짐
		double tal = talent(life);
		double kE;
		if (age < 6) {
			kE = 0;
		} else {
			double z = invNorm(clamp(life.getKnowledge(), 0.5, 99.9) / 100);
			kE = clamp(z - talW(age - 1) * tal, -1.4, 1.6);
			double d = 0.075 * (knowEff(focus) - knowW0(age));
			if (age <= 17) {
				// 집안 형편: 넉넉하면 책·학원·과외, 어려우면 집안일·일손 돕기로 공부 시간이 줄어듦 (11/20)
				d += 0.025 * (life.getWealthClass() - 3);
			}
			if (age <= 17 && !student) {
				// 학교 밖 아이: 또래보다 뒤처짐 (혼자 공부하면 절반만)
				double self = "STUDY".equals(focus) || "SELF_DEV".equals(focus) ? 1 : 0;
				d -= 0.2 * (1 - 0.5 * self);
			}
			if (student && ("UNI".equals(life.getEduLevel()) || "GRAD".equals(life.getEduLevel()))) {
				d += 0.02; // 대학·대학원
			}
			if (age > 72 && !"BRAIN".equals(focus)) {
				d -= 0.02; // 두뇌 활동을 안 하면 노년에 감소
			}
			kE = clamp(kE * (age < 25 ? 0.97 : 0.985) + d, -1.4, 1.6);
		}
		life.setKnowledge(clamp(100 * normCdf(talW(age) * tal + kE), 0.5, 99.9));

		// 2. 건강: 목표 = 기대 건강 + (가정형편-3)×1.5 + 건강 관리 6×fitK, 매년 차이의 25%만큼 이동
		//    5세 미만은 해마다 아플 위험만큼 건강이 깎임 (원작 kidSickP: 아이 질병 평균값)
		//    빚이 무거우면 -2 (원작 debtHeavy)
		boolean heavyDebt = debtHeavy(life, stat);
		double healthGoal = expHealth(age + 1) + (life.getWealthClass() - 3) * 1.5;
		if ("HEALTH".equals(focus)) {
			healthGoal = healthGoal + 6 * fitK(age);
		}
		if (heavyDebt) {
			healthGoal = healthGoal - 2;
		}
		life.setHealth(clamp(life.getHealth() + (healthGoal - life.getHealth()) * 0.25, 1, 100));
		if (age < 5) {
			life.setHealth(life.getHealth() - 4 * kidSickP(u5Of(stat)));
		}

		// 3. 관계: 목표 = 45 + 결혼 12 + 자녀 수×4(최대 15) + 부모 생존 3 - 이민 6 (어른은 -8)
		double relGoal = 45;
		if ("Y".equals(life.getMarriedYn())) {
			relGoal = relGoal + 12;
		}
		relGoal = relGoal + Math.min(15, life.getChildCount() * 4);
		if ("Y".equals(life.getParentAlive())) {
			relGoal = relGoal + 3;
		}
		if (!life.getBirthCountry().equals(life.getCurrentCountry())) {
			relGoal = relGoal - 6;
		}
		relGoal = relGoal - 8 * c;
		double relRate = life.getRelation() > relGoal ? 0.12 + (0.25 - 0.12) * c : 0.12;
		life.setRelation(life.getRelation() + (relGoal - life.getRelation()) * relRate);

		// 4. 행복: 목표 = 나라 행복 기준(28 + 행복 점수×5.5) + 경제적 위치 + 관계 보정 + 건강 보정 - 빚 (어른은 -12)
		//    경제적 위치: 어른은 (소득 위치 - 0.5)×18, 18세 미만·학생은 (가정형편-3)×3
		double hapGoal = 28 + cNum(cc, 2) * 5.5;
		if (age >= 18 && !student) {
			hapGoal = hapGoal + (lifePct(life, stat) - 0.5) * 18;
		} else {
			hapGoal = hapGoal + (life.getWealthClass() - 3) * 3;
		}
		hapGoal = hapGoal + (life.getRelation() - 50) * 0.25 + (life.getHealth() - expHealth(age + 1)) * 0.2;
		if (heavyDebt) {
			hapGoal = hapGoal - 6; // 무거운 빚 (원작)
		} else if (life.getAssets() < 0) {
			hapGoal = hapGoal - 1;
		}
		hapGoal = hapGoal - 12 * c;
		double hapRate = 0.22 + ((life.getHappiness() > hapGoal ? 0.35 : 0.2) - 0.22) * c;
		life.setHappiness(life.getHappiness() + (hapGoal - life.getHappiness()) * hapRate);
	}

	// 능력치를 0 ~ 100 사이로 맞추기
	private void fixStats(LifeDTO life) {
		life.setHealth(limit(life.getHealth()));
		life.setHappiness(limit(life.getHappiness()));
		life.setKnowledge(limit(life.getKnowledge()));
		life.setRelation(limit(life.getRelation()));
	}

	private double limit(double v) {
		if (v < 0) {
			return 0;
		}
		if (v > 100) {
			return 100;
		}
		return v;
	}

	// ================= C06-1 학교 (진학·졸업·중퇴) =================

	// 학교 단계: 초등 6년 → 중등 3년 → 고등 3년 → 대학 4년 → 대학원 2년 (모든 나라 같은 기간)
	//   startAges 는 '새 나이'로 판정: 8이면 7살을 마치며 입학 → 인생 기록(지난 해 나이)에는 7세 입학으로 보임
	//   한국 학제 기준: 초등 7세 입학 · 13세 졸업 → 중학교 16세 졸업 → 고등학교 19세 졸업 → 대학 23세 졸업
	//   (11/20 동현님 요청: 12세 초졸 → 13세 초졸)
	private String[] levels = { "ELEM", "MIDDLE", "HIGH", "UNI", "GRAD" };
	private int[] startAges = { 8, 14, 17, 20, 24 };
	// 일자리 제안을 받기 시작하는 나이 ('새 나이' 기준 19 = 인생 기록 18세, 성인부터)
	private static final int JOB_MIN_AGE = 19;
	private int[] years = { 6, 3, 3, 4, 2 };

	// 학력 코드 → 몇 번째 단계인지 (NONE은 -1)
	private int levelIndex(String level) {
		for (int i = 0; i < levels.length; i++) {
			if (levels[i].equals(level)) {
				return i;
			}
		}
		return -1;
	}

	// 1년 학교 처리 (새 나이 기준)
	private void schoolStep(LifeDTO life, CountryStatDTO stat) {
		int age = life.getCurrentAge();
		if (inPrison(life)) {
			return; // 수감 중엔 진학 없음 (다니던 학교는 들어갈 때 그만둠)
		}
		int idx = levelIndex(life.getEduLevel());
		String status = life.getSchoolStatus();

		// 1. 다니는 중이면: 졸업 나이가 됐는지, 아니면 중퇴하는지
		if ("ENROLLED".equals(status)) {
			if (age >= startAges[idx] + years[idx]) {
				life.setSchoolStatus("GRADUATED");
				addExperience(life, "EDU_" + levels[idx]); // 졸업 경험 (C05)
				status = "GRADUATED";
			} else {
				// 들어간 바로 다음 해에는 그만두지 않음 (진학을 고르자마자 그만두는 게 뜬금없었음)
				if (age > startAges[idx] + 1 && random.nextDouble() < dropRate(life, idx)) {
					life.setSchoolStatus("DROPPED");
					if (life.getWealthClass() <= 2) {
						life.setDropReason("집안 형편이 어려워 학교를 그만둠");
					} else {
						life.setDropReason("성적이 떨어져 학교를 그만둠");
					}
				}
				return;
			}
		}

		// 2. 진학: 아직 입학 전이거나, 졸업한 해에 다음 단계 입학 나이면 판정
		//   중학교·고등학교·대학(next 1~3)은 붙으면 그해 끝에 갈지 말지 직접 고름 (C06-4 진학 선택, 원작 school2·school3·college)
		//   제때(onTime) 못 붙으면 다음 해 한 번 더 도전 (11/19 일지), 두 번째도 못 붙으면 "형편이 어려워 못 갔다" 한 줄
		//   기다리던 진학 선택을 서버 재시작으로 잃었을 때도 같은 '한 해 늦게' 판정으로 다시 고름
		int next = idx + 1;
		boolean canEnter = (idx == -1 && status == null) || "GRADUATED".equals(status);
		boolean choose = next >= 1 && next <= 4; // 대학원(4)도 붙으면 갈지 고름 (원작 afterUni, 11/23)
		boolean onTime = next < levels.length && age == startAges[next];
		boolean late = choose && age == startAges[next] + 1 && !schoolDecided(life, next);
		if (canEnter && (onTime || late)) {
			if (random.nextDouble() < enrollRate(life, stat, next)) {
				if (choose) {
					engState(life).schoolNext = next; // 그해 끝(rollEngineChoice)에서 선택 창
					return;
				}
				life.setEduLevel(levels[next]);
				life.setSchoolStatus("ENROLLED");
				life.setDropReason(null);
			} else if (next == 0 && onTime) {
				// 초등학교에 못 감 (원작 outWhy start): 까닭 한 줄 + 몇 해 안에 '학교에 가고 싶어요' 선택
				String t = elemWhy(life, stat);
				saveEventLog(life, age - 1, null, null, t);
				addYearNote(life, t);
				engState(life).noschoolAsk = true;
			} else if (next == 4) {
				// 대학원은 붙지 않으면 조용히 넘어감 (지원했는지 모르는 사람에게 '떨어졌다'를 쓰지 않음, 다시 보지도 않음)
				markEngineDone(engState(life), EngineChoice.byKey("SCHOOL_GRAD"), age - 1);
			} else if (next == 3 && onTime) {
				// 대학 첫 낙방: 재수할지 묻기 (원작 retake) - 창이 안 뜨면 예전처럼 다음 해 다시 판정
				engState(life).retakeAsk = true;
			} else if (choose && onTime && next >= 3) {
				// 첫해 낙방: 기록만 남기고 다음 해 다시 판정 (late)
				// 중학교·고등학교는 원작처럼 '재수'가 없음 → 아래에서 바로 못 간 까닭 한 줄 (11/23 300번 점검: 중학교 '내년에 다시 도전'이 어색)
				// 기록은 졸업 문장 뒤에 오도록 nextYear 의 졸업·입학 기록 다음에 저장 (EngState.schoolNote)
				engState(life).schoolNote = SCHOOL_NAME.getOrDefault(levels[next], "학교") + "에 들어가지 못했다. 내년에 다시 도전하기로 했다.";
			} else if (choose) {
				EngineChoice.Dec d = EngineChoice.byKey("SCHOOL_" + levels[next]);
				markEngineDone(engState(life), d, age - 1);
				String ft = d.failText;
				// 형편이 넉넉한 집 아이에게 '형편이 어려워'는 맞지 않음 → 시험·자리 탓으로
				if (ft != null && next <= 2 && life.getWealthClass() >= 3) {
					ft = SCHOOL_NAME.getOrDefault(levels[next], "학교") + " 입학 시험에서 떨어져 진학하지 못했다.";
				}
				if (ft != null) {
					saveEventLog(life, age - 1, null, null, ft);
					addYearNote(life, ft);
				}
			}
		}
	}

	// 초등학교에 못 간 까닭 (원작 outWhy start)
	private String elemWhy(LifeDTO life, CountryStatDTO stat) {
		String g = groupOf(life.getCurrentCountry(), stat);
		boolean rural = !"Y".equals(life.getIsUrban());
		if (life.getWealthClass() >= 4) {
			return rural ? "또래들이 학교에 들어갈 때, 다닐 만한 학교가 너무 멀어 집에서 지냈다."
					: "또래들이 학교에 들어갈 때, 그해 동네 학교에는 자리가 모자라 들어가지 못했다.";
		}
		if ("F".equals(life.getGender()) && rural && (g.equals("sa") || g.equals("mena"))) {
			return "또래들이 학교에 들어갈 때, 집안 어른들은 딸이 멀리 학교에 다니는 것을 반대했다.";
		}
		return rural ? "또래들이 학교에 들어갈 때, 학교는 멀고 집안 일손은 모자라 학교에 가지 못했다."
				: "또래들이 학교에 들어갈 때, 학비를 마련하지 못해 학교에 가지 못했다.";
	}

	// 이 단계 진학을 이미 정했는지 (골랐거나 못 붙었거나: 인생 기록 문장으로 확인)
	private boolean schoolDecided(LifeDTO life, int next) {
		return engState(life).done.containsKey("SCHOOL_" + levels[next]);
	}

	// 원작 odds: 확률 p를 배율 k만큼 "승산"으로 키우거나 줄임 (확률을 그냥 곱하면 1을 넘으니 승산으로 곱함)
	private double odds(double p, double k) {
		p = clamp(p, 1e-4, 1 - 1e-4);
		return p * k / (1 - p + p * k);
	}

	// 단계별 진학 확률 (원작 eduProbs)
	// 권역 진학률(또래 중 그 단계까지 가는 비율)로 "앞 단계를 마친 사람 중 다음 단계로 가는 비율"을 구하고
	// 가정형편·도시/시골 배율을 승산에 곱함
	//   배율 = e^(m×(가정형편-3)) × (구매력 1인당 GDP 15,000 미만이면 도시 1.4 / 시골 0.75), m = 15,000 미만 0.6, 이상 0.45
	//   대학: 졸업까지 가는 비율 = 대학 진학률 × 0.75 (최대 78%), 여자 ×1.2(남아시아·중동 제외)·남자 ×0.9
	//   지식이 아주 높으면(또래 상위 4%) 대학 진학 더 쉬움 (원작 uniTop)
	private double[] eduProbs(LifeDTO life, CountryStatDTO stat) {
		double elem = 80, middle = 60, high = 40, uni = 15;
		RegionStatDTO rs = regionStat(life);
		if (rs != null) {
			if (rs.getEnrollElem() != null) {
				elem = rs.getEnrollElem();
			}
			if (rs.getEnrollMiddle() != null) {
				middle = rs.getEnrollMiddle();
			}
			if (rs.getEnrollHigh() != null) {
				high = rs.getEnrollHigh();
			}
			if (rs.getEnrollUni() != null) {
				uni = rs.getEnrollUni();
			}
		}
		double p1 = clamp(elem / 100, 0.02, 0.999);
		double p2 = Math.min(clamp(middle / 100, 0.01, 0.999), p1 * 0.998);
		double p3 = Math.min(clamp(high / 100, 0.005, 0.999), p2 * 0.998);
		double u = Math.min(Math.min(0.78, uni / 100 * 0.75), p3 * 0.95);

		double ppp = toPpp(gdpOf(stat));
		boolean lowInc = ppp < 15000;
		double d = Math.exp((lowInc ? 0.6 : 0.45) * (life.getWealthClass() - 3));
		if (lowInc) {
			d = d * ("Y".equals(life.getIsUrban()) ? 1.4 : 0.75);
		}
		String g = groupOf(life.getCurrentCountry(), stat);
		double uniK = d;
		if ("F".equals(life.getGender())) {
			uniK = d * (g.equals("sa") || g.equals("mena") || g.equals("ssa") ? 1 : 1.2);
		} else {
			uniK = d * 0.9;
		}
		double t1 = odds(p1, d);
		double t2 = odds(p2 / p1, d);
		double t3 = odds(p3 / p2, d);
		double t4 = odds(Math.min(0.95, u / Math.max(p3, 0.05)), uniK);
		if (kz(life) >= 1.75) {
			t4 = 1 - (1 - t4) * (1 - 0.75 * clamp((t4 - 0.1) / 0.2, 0, 1));
		}
		return new double[] { t1, t2, t3, t4 };
	}

	// 지식을 또래 가운데 위치 점수(z)로 (원작 kzOf)
	private double kz(LifeDTO life) {
		return invNorm(clamp(life.getKnowledge(), 0.5, 99.9) / 100);
	}

	// 진학 확률 (원작 eduProbs, 대학원은 원작 gradAdmit)
	private double enrollRate(LifeDTO life, CountryStatDTO stat, int next) {
		if (next == 4) {
			// 대학원 합격: 0.1 + 대학 진학률÷400 + (지식-40)÷300 + (가정형편-3)×0.03 (5~45%)
			//   원작은 플레이어가 지원할지 고르는데, 우리는 자동 진행이라 대학 졸업자 절반이 지원한다고 봄
			double uni = 15;
			RegionStatDTO rs = regionStat(life);
			if (rs != null && rs.getEnrollUni() != null) {
				uni = rs.getEnrollUni();
			}
			double admit = clamp(0.1 + uni / 400 + (life.getKnowledge() - 40) / 300 + (life.getWealthClass() - 3) * 0.03, 0.05, 0.45);
			return admit; // 붙으면 갈지 직접 고름 (예전엔 절반만 지원한다고 보고 ×0.5)
		}
		return eduProbs(life, stat)[next];
	}

	// 1년 중퇴 확률 (원작 lsKeep·usKeep)
	// 중학교·고등학교는 원작식, 대학은 명세서 C06-3 지식 배율 (초등·대학원은 중퇴 없음)
	//   끝까지 다닐 확률 = 그 단계 진학 확률을 노력 배율만큼 키운 값 (최소 중학교 10%·고등학교 8%)
	//   노력 배율 = 1.15 × (13세 이하 1.7, 그 위 2.05) × e^(0.85×지식 z - 0.36)
	//   1년 중퇴 확률 = 1 - 끝까지 다닐 확률^(1/2) (3년 과정이라 2번 판정)
	private double dropRate(LifeDTO life, int idx) {
		if (idx == 3) {
			// 대학 중퇴 (명세서 C06-3, 원작에는 없음): 1년 2.5% × 형편(1~2분위 ×1.5) × 지식 배율 → 4년 동안 평균 약 10%
			double k = life.getKnowledge();
			double kMul = k >= 85 ? 0.45 : k >= 70 ? 0.78 : k >= 50 ? 1.12 : 1.68;
			return 0.025 * (life.getWealthClass() <= 2 ? 1.5 : 1) * kMul;
		}
		if (idx != 1 && idx != 2) {
			return 0;
		}
		CountryStatDTO stat = yearStat(life);
		double[] t = eduProbs(life, stat);
		double effort = 1.15 * (life.getCurrentAge() <= 13 ? 1.7 : 2.05)
				* Math.exp(0.85 * clamp(kz(life), -3, 3) - 0.85 * 0.85 / 2);
		double keep = idx == 1 ? odds(Math.max(t[1], 0.1), effort) : odds(Math.max(t[2], 0.08), effort);
		keep = clamp(keep, 0.05, 1);
		return 1 - Math.pow(keep, 1.0 / (years[idx] - 1));
	}

	// ================= C06-2 직업·소득·재산 =================

	// 마친 학력 단계 (졸업했으면 그 단계, 재학·중퇴면 그 전 단계, 없으면 -1)
	private int finishedLevel(LifeDTO life) {
		int idx = levelIndex(life.getEduLevel());
		if (idx == -1 || "GRADUATED".equals(life.getSchoolStatus())) {
			return idx; // 학교를 안 다녔으면 -1 (학력 없음)
		}
		return idx - 1;
	}

	// 은퇴 나이 (원작 retireAge): 고소득국 65세, 유럽·중앙아시아 62세, 그 외 60세
	//   한국·일본은 정년 60세 (한국 2016년 전, 일본 1998년 전은 55세)
	//   2030년 이후 고소득국·중앙아시아·중국·한국·일본은 10년마다 +1세 (최대 +5)
	private int retireAge(LifeDTO life, CountryStatDTO stat) {
		String cc = life.getCurrentCountry();
		String g = groupOf(cc, stat);
		int year = life.getBirthYear() + life.getCurrentAge();
		int add = 0;
		if (year >= 2030 && (g.equals("hi") || g.equals("eca") || "CHN".equals(cc))) {
			add = Math.min(5, (year - 2030) / 10 + 1);
		}
		if ("KOR".equals(cc)) {
			return year < 2016 ? 55 : 60 + add;
		}
		if ("JPN".equals(cc)) {
			return year < 1998 ? 55 : 60 + add;
		}
		if ("CHN".equals(cc)) {
			return 60 + Math.max(0, add - 3);
		}
		if (g.equals("hi")) {
			return 65 + add;
		}
		if (g.equals("eca")) {
			return 62 + add;
		}
		return 60;
	}

	// 연금 (원작 pensionBase): 고소득국·유럽중앙아시아는 모두, 그 외는 정식 일자리(비공식 고용 비율만큼 못 받음)만
	//   연금 = 마지막 소득 × 대체율 (한국 31.2%, 일본 32.4%, 고소득국 45%, 그 외 35%), 최소 중위 소득 × 0.25
	private long pensionOf(LifeDTO life, CountryStatDTO stat) {
		String cc = life.getCurrentCountry();
		String g = groupOf(cc, stat);
		boolean formal = g.equals("hi") || g.equals("eca");
		if (!formal) {
			double agri = 40;
			RegionStatDTO rs = regionStat(life);
			if (rs != null && rs.getAgriRate() != null) {
				agri = rs.getAgriRate();
			}
			double informal = clamp(0.15 + 0.75 * agri / 100 + 0.45 * (1 - Math.min(1, toPpp(gdpOf(stat)) / 40000)), 0.08, 0.95);
			formal = life.getJobYears() >= 10 && random.nextDouble() > informal;
		}
		if (!formal) {
			return 0;
		}
		double rate = "KOR".equals(cc) ? 0.312 : "JPN".equals(cc) ? 0.324 : g.equals("hi") ? 0.45 : 0.35;
		return Math.round(Math.max(medIncome(cc, stat) * 0.25, life.getIncome() * rate));
	}

	// 1년 직업 처리 (새 나이 기준)
	private void jobStep(LifeDTO life, CountryStatDTO stat) {
		int age = life.getCurrentAge();

		// 0. 출소 (원작: 형기를 채운 해 끝에 구직 중으로) - 출소 문장은 milestones
		int[] pr = prisonSpan(life);
		if (inPrison(life) && (pr == null || age >= pr[0] + pr[1])) {
			life.setJobStatus("UNEMPLOYED");
			life.setJobYears(0);
		}

		// 1. 정년 은퇴 (원작 연금 규칙)
		//    은퇴 나이 전에 시작한 일만 정년 은퇴, 은퇴 후 다시 구한 일은 75세 또는 건강 40 미만까지
		if ("EMPLOYED".equals(life.getJobStatus()) && age >= retireAge(life, stat)) {
			boolean rehired = age - life.getJobYears() >= retireAge(life, stat);
			if (!rehired) {
				// 원작 retire: 정년이 되면 은퇴할지 묻고, 시간제·계속 일하기를 고르면 5년 더 (건강 40 미만이면 그만둠)
				EngState rs = engState(life);
				if (rs.retUntil != null && age <= rs.retUntil && life.getHealth() >= 40) {
					return;
				}
				if (rs.retUntil == null && !rs.done.containsKey("RETIRE") && life.getHealth() >= 40 && age < 75) {
					rs.retireAsk = true; // 그해 끝에 은퇴할지 물음 (rollEngineChoice)
					return;
				}
				life.setJobStatus("RETIRED");
				life.setPension(pensionOf(life, stat));
				return;
			}
			if (age >= 75 || life.getHealth() < 40) {
				life.setJobStatus("RETIRED"); // 연금은 그대로 (없으면 없음)
				return;
			}
		}

		// 1-0. 은퇴 나이가 지났는데 일이 없는 사람(실직·처음부터 무직)은 은퇴로 처리 (원작 연금 규칙, 소득 0이면 기본 연금만)
		//      (안 하면 실직 상태로 평생 남아 연금도, 다시 일하기도 못 함)
		boolean jobless = life.getJobStatus() == null || "UNEMPLOYED".equals(life.getJobStatus());
		if (jobless && age >= retireAge(life, stat) && !"ENROLLED".equals(life.getSchoolStatus())) {
			life.setJobStatus("RETIRED");
			life.setIncome(0);
			life.setPension(pensionOf(life, stat));
		}

		// 1-1. 연금 없는 은퇴자가 모아 둔 돈이 모자라면 다시 일함 (원작 oldBackYear)
		//      75세 전·건강 40 이상, 재산 < 최소 생활비 3년치면 40% 확률로 일자리를 구함
		if ("RETIRED".equals(life.getJobStatus()) && age < 75 && life.getHealth() >= 40
				&& (life.getPension() == null || life.getPension() == 0)) {
			String cc = life.getCurrentCountry();
			double need = medIncome(cc, stat) * 0.3 * ("Y".equals(life.getMarriedYn()) ? 0.6 : 1)
					* (life.getChildCount() > 0 ? 0.5 : 1);
			if (life.getAssets() < need * 3 && random.nextDouble() < 0.4) {
				List<JobDTO> offers = offerJobs(life);
				if (!offers.isEmpty()) {
					JobDTO job = offers.get(0);
					life.setJobId(job.getJobId());
					life.setJobStatus("EMPLOYED");
					life.setIncomeRate(job.getIncomeRate());
					life.setJobYears(0);
					addExperience(life, job.getJobId()); // 직업 경험 (C05)
					// 인생 기록 (EVENT_ID는 비워 둠: LIFE_EVENT_LOG.EVENT_ID는 빈 값 허용)
					saveEventLog(life, age, null, null, "모아 둔 돈만으로는 살기 어려워 " + job.getJobName() + " 일을 다시 시작했다.");
				}
			}
			return;
		}

		// 1-2. 가게(자영업 사장)는 몇 해에 걸쳐 버티거나 접음 (원작 bizStart · BIZ_Y1)
		bizYear(life, stat);

		// 2. 성인(JOB_MIN_AGE) 이상, 학교 안 다니고, 일자리가 없으면 매년 직업 3개 제안
		showOffers(life, stat);
	}

	// 원작: 새 가게의 약 29%는 첫해를 못 넘기고, 그 뒤로도 해마다 몇 %씩 문을 닫음 (5년 안에 절반쯤)
	//   운영 연수(jobYears, moneyStep 에서 +1)가 적을수록 닫을 확률이 높음. 드물게 크게 자리를 잡아 벌이가 늘어남
	//   접으면 가게 살림을 헐값에 정리하는 손해(중위 소득 × 1.5 × 45%)가 나고, 다음 해부터 다시 일자리를 구함
	private void bizYear(LifeDTO life, CountryStatDTO stat) {
		if (!"EMPLOYED".equals(life.getJobStatus()) || !"JOB_OWNER".equals(life.getJobId())) {
			return;
		}
		int y = life.getJobYears(); // 방금 지난 해까지 운영한 햇수
		if (y > 8) {
			return; // 8년 넘게 버틴 가게는 자리를 잡은 것으로 봄
		}
		int lived = life.getCurrentAge() - 1;
		double close = y <= 1 ? 0.25 : y <= 3 ? 0.12 : 0.07;
		if (random.nextDouble() < close) {
			double med = medIncome(life.getCurrentCountry(), stat);
			long loss = Math.round(med * 1.5 * 0.45);
			life.setAssets(life.getAssets() - loss);
			book(life, "가게 정리 손해", -loss);
			life.setJobStatus("UNEMPLOYED");
			life.setJobYears(0);
			life.setHappiness(life.getHappiness() - 6);
			String text = (y <= 1 ? "손님이 생각만큼 오지 않아 문을 연 지 얼마 안 된 가게를 접었다."
					: "몇 해를 버텼지만 결국 가게를 접었다.") + " 남은 물건을 헐값에 정리했다. (−" + won(life, loss) + ")";
			saveEventLog(life, lived, null, null, text);
			addYearNote(life, text);
			return;
		}
		if (y <= 5 && random.nextDouble() < 0.05 && life.getIncomeRate() != null) {
			life.setIncomeRate(life.getIncomeRate() * 1.6);
			life.setHappiness(life.getHappiness() + 8);
			String text = "단골이 늘면서 가게가 크게 자리를 잡았다! 일손을 더 두게 되었다.";
			saveEventLog(life, lived, null, null, text);
			addYearNote(life, text);
		}
	}

	// 그해 화면 기록에 한 줄 더하기 (jobStep 처럼 이벤트 뒤에 생긴 일)
	private void addYearNote(LifeDTO life, String text) {
		List<EventResultDTO> list = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
		list.add(noteEvent(text));
		life.setEvents(list);
	}

	// 첫 스마트폰 (원작 phoneYear): 그 나라에 스마트폰이 흔해진 해부터 그해 바로 장만
	//   (예전엔 이야기 한 줄로 가끔 뽑혀서 2020년 37세에야 생기기도 함 → 11/20 동현님 요청으로 원작식)
	//   흔해진 해: 구매력 1인당 GDP 3만 달러 이상 2009년, 1.5만 이상 2013년, 8천 이상 2016년, 4천 이상 2019년, 그 아래 2022년
	//     (원작: 스마트폰 시대(고소득 2008년·그 외 2012년) + 인터넷 사용률 80% 이상, 우리 DB엔 사용률이 없어 소득으로 대신)
	//   10~12세 35%, 13~17세는 부모가 사 줌 (학교 안 다니는 가난한 집 아이는 15세부터 직접 벌어서)
	//   어른은 값(구매력 200달러)을 냄, 모자라면 중고(40%), 벌이가 없거나 빚이 무거운데 모자라면 미룸
	//   북한은 도시·넉넉한 어른만 해마다 3% (원작 nkPhone)
	private void phoneYear(LifeDTO life, CountryStatDTO stat) {
		java.util.LinkedHashSet<String> owns = ownSet(life);
		if (owns.contains("PHONE") || !"PLAYING".equals(life.getStatus())) {
			return;
		}
		int age = life.getCurrentAge();
		int year = life.getBirthYear() + age;
		if (age < 10) {
			return;
		}
		if ("PRK".equals(life.getCurrentCountry())) {
			if (year < 2012 || age < 18 || !("Y".equals(life.getIsUrban()) || life.getWealthClass() >= 4) || random.nextDouble() >= 0.03) {
				return;
			}
		} else {
			double ppp = toPpp(gdpOf(stat));
			int common = ppp >= 30000 ? 2009 : ppp >= 15000 ? 2013 : ppp >= 8000 ? 2016 : ppp >= 4000 ? 2019 : 2022;
			if (year < common) {
				return;
			}
		}
		boolean poorOut = age < 18 && !"ENROLLED".equals(life.getSchoolStatus()) && life.getWealthClass() <= 2;
		String text = "첫 스마트폰이 생겼다. 손에서 놓을 줄을 몰랐다.";
		if (age < 18 && !poorOut) {
			if (age < 13 && random.nextDouble() >= 0.35) {
				return; // 10~12세는 집마다 다름
			}
		} else {
			if (poorOut && (age < 15 || life.getIncome() <= 0)) {
				return;
			}
			long price = Math.round(200 * priceK(stat));
			boolean tight = life.getAssets() < price * 1.1;
			if (tight && (life.getIncome() <= 0 || debtHeavy(life, stat))) {
				return; // 형편이 풀릴 때까지 미룸
			}
			if (tight) {
				price = Math.round(price * 0.4);
				text = "형편에 맞춰 중고 스마트폰을 하나 장만했다.";
			}
			life.setAssets(life.getAssets() - price);
			book(life, "스마트폰 구입", -price);
			text = text + " (−" + won(life, price) + ")";
		}
		owns.remove("CELL");
		owns.add("PHONE");
		life.setOwns(String.join(",", owns));
		life.setHappiness(life.getHappiness() + 2);
		saveEventLog(life, age, null, null, text);
		addYearNote(life, text);
	}

	// 취미: 해마다 효과 × 0.25 (원작 값이 한 해 효과로는 커서, 몇 해에 걸쳐 쌓이게), 18세부터 비용 (중위 소득 × 비율)
	private void hobbyYear(LifeDTO life, CountryStatDTO stat) {
		if (life.getOwns() == null || !"PLAYING".equals(life.getStatus())) {
			return;
		}
		String code = null;
		for (String x : life.getOwns().split(",")) {
			if (x.trim().startsWith("HOB-")) {
				code = x.trim().substring(4);
			}
		}
		String[] h = code == null ? null : EngineChoice.hobbyOf(code);
		if (h == null) {
			return;
		}
		life.setHappiness(life.getHappiness() + Double.parseDouble(h[2]) * 0.25);
		life.setHealth(life.getHealth() + Double.parseDouble(h[3]) * 0.25);
		life.setRelation(life.getRelation() + Double.parseDouble(h[4]) * 0.25);
		life.setKnowledge(life.getKnowledge() + Double.parseDouble(h[5]) * 0.25);
		if (life.getCurrentAge() >= 18 && (life.getIncome() > 0 || life.getAssets() > 0)) {
			double cost = medIncome(life.getCurrentCountry(), stat) * Double.parseDouble(h[6]);
			life.setAssets(life.getAssets() - Math.round(cost));
			book(life, "취미 (" + h[1] + ")", -cost);
		}
	}

	// ================= 병역 (원작 service · KR_SVC, 한국 남성 · 북한) =================
	//   한국: 19~28세에 학교를 다니지 않으면 통지서. 판정은 해마다 비율이 다름 (원작 KR_SVC: 보충역·면제 비율)
	//     보충역 비율 ×1.4 (중졸 이하) / ×0.85 (대학) / ×1.3 (건강 45 미만)
	//     현역 복무: 육군 1968년 전 36개월 → 2020년부터 18개월, 해군·공군은 조금 더 김 / 보충역 12~28개월 (원작 krMonths)
	//     복무 중: 일자리 제안 없음, 하던 일은 그만둠, 해마다 행복 −2 · 관계 −1 · 건강 +1 (보충역은 행복 −1만)
	//   북한: 17세에 입대해 약 10년 복무 (창 없이)
	//   상태는 LIFE.OWNS 에 SVC-ON-전역나이-종류-개월 / SVC-DONE / SVC-EX (DB 구조 그대로)
	private static final double[][] KR_SVC = { // {부터, 까지, 보충역 비율, 면제 비율}
			{ 1953, 1968, 0.10, 0.30 }, { 1969, 1973, 0.13, 0.28 }, { 1974, 1975, 0.20, 0.28 }, { 1976, 1991, 0.27, 0.24 },
			{ 1992, 1994, 0.12, 0.08 }, { 1995, 2013, 0.07, 0 }, { 2014, 2014, 0.06, 0 }, { 2015, 9999, 0.11, 0 } };

	private static String svcTok(LifeDTO life) {
		if (life.getOwns() == null) {
			return null;
		}
		for (String x : life.getOwns().split(",")) {
			if (x.trim().startsWith("SVC-")) {
				return x.trim();
			}
		}
		return null;
	}

	// 복무 중 (현역·보충역 모두 일자리 제안 없음)
	private static boolean svcActive(LifeDTO life) {
		String t = svcTok(life);
		return t != null && t.startsWith("SVC-ON-");
	}

	private void setSvcTok(LifeDTO life, String tok) {
		java.util.LinkedHashSet<String> owns = ownSet(life);
		owns.removeIf(x -> x.startsWith("SVC-"));
		if (tok != null) {
			owns.add(tok);
		}
		life.setOwns(String.join(",", owns));
	}

	private static int krArmyMonths(int y) {
		return y < 1977 ? 36 : y < 1984 ? 33 : y < 1993 ? 30 : y < 2003 ? 26 : y < 2011 ? 24 : y < 2020 ? 21 : 18;
	}

	private static int krNavyMonths(int y) {
		return y < 1979 ? 39 : y < 1990 ? 35 : y < 1993 ? 32 : y < 2003 ? 28 : y < 2011 ? 26 : y < 2020 ? 23 : 20;
	}

	private static int krAirMonths(int y) {
		return y < 1979 ? 39 : y < 1993 ? 35 : y < 2003 ? 30 : y < 2011 ? 27 : y < 2020 ? 24 : 21;
	}

	private static int krSupMonths(int y) {
		return y <= 1981 ? 12 : y <= 1985 ? 14 : y <= 1994 ? 18 : y <= 2002 ? 28 : y <= 2010 ? 26 : y <= 2019 ? 24 : 21;
	}

	private static String krSupName(int y) {
		return y <= 1994 ? "방위병" : y <= 2013 ? "공익근무요원" : "사회복무요원";
	}

	// 통지서를 띄울지 (현역·보충역 선택 번호, 아니면 -1). 면제는 여기서 한 줄 남기고 끝
	private int svcPick(LifeDTO life, EngState s, int lived) {
		if (!"KOR".equals(life.getCurrentCountry()) || !"M".equals(life.getGender()) || svcTok(life) != null
				|| lived < 19 || lived > 28 || "ENROLLED".equals(life.getSchoolStatus())
				|| (s.svcDelayAt != null && s.svcDelayAt == lived)) {
			return -1;
		}
		int y = life.getBirthYear() + lived;
		if (y < 1953) {
			return -1;
		}
		if (s.svcKind == null) {
			double sup = 0.07, ex = 0;
			for (double[] k : KR_SVC) {
				if (y >= k[0] && y <= k[1]) {
					sup = k[2];
					ex = k[3];
				}
			}
			int lv = levelIndex(life.getEduLevel());
			sup = clamp(sup * (lv <= 1 ? 1.4 : lv >= 3 ? 0.85 : 1) * (life.getHealth() < 45 ? 1.3 : 1), 0, 0.8);
			double rr = random.nextDouble();
			s.svcKind = rr < ex ? "ex" : rr < ex + sup ? "sup" : "act";
		}
		if ("ex".equals(s.svcKind)) {
			setSvcTok(life, "SVC-EX");
			String t = "신체검사 결과 병역이 면제되었다.";
			saveEventLog(life, lived, null, null, t);
			addYearNote(life, t);
			return -1;
		}
		return EngineChoice.indexOf("sup".equals(s.svcKind) ? "SVC_SUP" : "SVC_ACT");
	}

	private String svcBody(LifeDTO life, String key) {
		int y = life.getBirthYear() + life.getCurrentAge() - 1;
		if ("SVC_SUP".equals(key)) {
			return "병역판정검사에서 보충역 판정을 받아 " + krSupName(y) + "으로 소집돼요. 기초 군사훈련을 받은 뒤 집에서 출퇴근하며 약 "
					+ krSupMonths(y) + "개월 복무해요.";
		}
		return "대한민국에서는 남성에게 병역 의무가 있어요. 육군 복무 기간은 약 " + krArmyMonths(y) + "개월이에요. 해군(약 "
				+ krNavyMonths(y) + "개월)이나 공군(약 " + krAirMonths(y) + "개월)에 지원할 수도 있어요.";
	}

	private String svcChoose(LifeDTO life, EngState s, String act, int age) {
		int y = life.getBirthYear() + age;
		if ("SVC_DELAY".equals(act)) {
			s.svcDelayAt = age; // 그해엔 다시 묻지 않고, 다음 해 끝에 다시 통지서
			return "SVC_SUP".equals(s.svcKind) || "sup".equals(s.svcKind) ? "소집을 1년 미뤘다." : "입대를 1년 미뤘다.";
		}
		String kind;
		int months;
		String text;
		if ("SVC_SUP".equals(act)) {
			kind = "S";
			months = krSupMonths(y);
			text = "보충역으로 소집되어 집에서 출퇴근하며 복무를 시작했다. " + krSupName(y) + "으로 약 " + months + "개월 복무한다.";
		} else {
			kind = "A";
			months = krArmyMonths(y);
			String fail = null;
			if ("SVC_NAVY".equals(act)) {
				double p = y < 2005 ? 0.6 : y < 2020 ? 0.45 : 0.85;
				if (random.nextDouble() < p) {
					kind = "N";
					months = krNavyMonths(y);
				} else {
					fail = "해군";
				}
			} else if ("SVC_AIR".equals(act)) {
				double p = y < 2005 ? 0.5 : y < 2020 ? 0.35 : 0.5;
				if (random.nextDouble() < p) {
					kind = "F";
					months = krAirMonths(y);
				} else {
					fail = "공군";
				}
			}
			String branch = "N".equals(kind) ? "해군" : "F".equals(kind) ? "공군" : "육군";
			text = (fail != null ? fail + "에 지원했지만 떨어져 육군으로 입대했다." : branch + "으로 입대했다.") + " 약 " + months + "개월 복무한다.";
			if ("EMPLOYED".equals(life.getJobStatus())) {
				life.setJobStatus("UNEMPLOYED");
				life.setJobYears(0);
				text = text + " 하던 일은 그만두었다.";
			}
		}
		int end = life.getCurrentAge() + (int) Math.ceil(months / 12.0);
		setSvcTok(life, "SVC-ON-" + end + "-" + kind + "-" + months);
		return text;
	}

	// 해마다: 복무 중 효과, 기간이 끝나면 전역 / 북한은 17세에 입대해 약 10년
	private void svcYear(LifeDTO life) {
		if (!"PLAYING".equals(life.getStatus()) || !"M".equals(life.getGender())) {
			return;
		}
		int age = life.getCurrentAge();
		String t = svcTok(life);
		if (t == null && "PRK".equals(life.getCurrentCountry()) && age == 17) {
			setSvcTok(life, "SVC-ON-" + (age + 10) + "-A-120");
			String n = "군에 입대했다. 10년 가까이 복무하게 된다.";
			saveEventLog(life, age, null, null, n);
			addYearNote(life, n);
			return;
		}
		if (t == null || !t.startsWith("SVC-ON-")) {
			return;
		}
		String[] p = t.split("-");
		int end = Integer.parseInt(p[2]);
		boolean sup = "S".equals(p[3]);
		if (age >= end) {
			setSvcTok(life, "SVC-DONE");
			life.setHappiness(life.getHappiness() + 3);
			String n = sup ? "소집 해제되었다. 약 " + p[4] + "개월의 복무를 마쳤다." : "약 " + p[4] + "개월의 복무를 마치고 전역했다.";
			saveEventLog(life, age, null, null, n);
			addYearNote(life, n);
			return;
		}
		if (sup) {
			life.setHappiness(life.getHappiness() - 1);
		} else {
			life.setHappiness(life.getHappiness() - 2);
			life.setRelation(life.getRelation() - 1);
			life.setHealth(life.getHealth() + 1);
		}
	}

	// ================= 대학 등록금 (원작 TUI · tuiStart · tuiYear 그대로, 11/23) =================
	//   등록금을 따로 계산하는 나라는 원작처럼 한국·중국·일본·베트남만 (그 밖의 나라는 원작에도 등록금 계산이 없음)
	//   한 해 등록금 = 중위 소득 × {국공립, 사립} 비율 × 그해 배율 (사립에 갈 확률은 나라마다, 입학 때 한 번 정함)
	//   장학금 (학부만): 한국 2012년~ 국가장학금 (형편별 확률·금액, 1분위는 전액) · 중국 2007년~ 국가 조학금 ·
	//                    베트남 2020년~ 성적 장학금 (지식 65 넘으면 학기마다 기회, 사립은 1/4)
	//   부모님 몫 = (등록금 − 장학금) × 형편별 비율 (원작 FAM_PAY 0·5·20·45·75·100%, 중국은 60·85·100·100·100%)
	//               부모님이 안 계시거나 결혼했거나 30세 넘으면 0, 다이아수저 중 아주 부유한 집(원작 rich)은 100%
	//   나머지는 스스로 마련 (모자라면 빚, 대출이 있는 나라·해는 '학자금 대출'로 기록)
	private static final double[] FAM_PAY = { 0, 0.05, 0.2, 0.45, 0.75, 1 };
	private static final double[] CHN_FP = { 0, 0.6, 0.85, 1, 1, 1 };
	private static final double[] KR_P = { 0, 0.8, 0.75, 0.6, 0.3, 0.02 };
	private static final double[] KR_A = { 0, 0.17, 0.125, 0.105, 0.1, 0.03 };
	private static final double[] CN_P = { 0, 0.7, 0.5, 0.2, 0.03, 0 };
	// {국공립 비율, 사립 비율, 사립에 갈 확률}
	private static final Map<String, double[]> TUI_F = Map.of(
			"KOR", new double[] { 0.125, 0.225, 0.78 }, "CHN", new double[] { 0.107, 0.41, 0.22 },
			"JPN", new double[] { 0.155, 0.28, 0.73 }, "VNM", new double[] { 0.25, 0.55, 0.25 });
	// 연도별 배율 {연도, 배율} (없으면 1)
	private static final Map<String, double[][]> TUI_Y = Map.of(
			"CHN", new double[][] { { 0, 0 }, { 1989, 2.7 }, { 1994, 4.5 }, { 1997, 6.7 }, { 2000, 11 }, { 2005, 5.7 }, { 2010, 2.8 }, { 2015, 1.7 }, { 2020, 1.15 }, { 2023, 1 } },
			"JPN", new double[][] { { 0, 0.15 }, { 1976, 0.35 }, { 1980, 0.5 }, { 1990, 0.67 }, { 2000, 0.87 }, { 2005, 1 } },
			"VNM", new double[][] { { 0, 0.6 }, { 2023, 1 } });

	private static final class Tui {
		double fee, schol, fam, rest, famShare;
		String scholName;
		boolean priv;
	}

	private double tuiStep(String cc, int y) {
		double[][] t = TUI_Y.get(cc);
		if (t == null) {
			return 1;
		}
		double k = 1;
		for (double[] row : t) {
			if (y >= row[0]) {
				k = row[1];
			}
		}
		return k;
	}

	private boolean freeUni(LifeDTO life) {
		int y = life.getBirthYear() + life.getCurrentAge();
		String cc = life.getCurrentCountry();
		return "PRK".equals(cc) || "CHN".equals(cc) && y < 1989 || "VNM".equals(cc) && y < 1990;
	}

	private boolean stuLoanOK(LifeDTO life) {
		int y = life.getBirthYear() + life.getCurrentAge();
		String cc = life.getCurrentCountry();
		return "KOR".equals(cc) && y >= 2009 || "JPN".equals(cc) && y >= 1953 || "CHN".equals(cc) && y >= 1999;
	}

	// 이번 등록금 (grad: 대학원이면 장학금 없음) - 등록금을 계산하지 않는 나라면 null
	private Tui tuition(LifeDTO life, CountryStatDTO stat, double med, boolean grad) {
		String cc = life.getCurrentCountry();
		double[] f = TUI_F.get(cc);
		if (f == null || freeUni(life)) {
			return null;
		}
		int y = life.getBirthYear() + life.getCurrentAge();
		int q = (int) clamp(life.getWealthClass(), 1, 5);
		// 국공립·사립과 국가장학금 여부는 입학 때 한 번 정한 것처럼 인생마다 늘 같게
		java.util.SplittableRandom r = new java.util.SplittableRandom(life.getLifeId() * 131L + (grad ? 7 : 3));
		Tui t = new Tui();
		t.priv = r.nextDouble() < f[2];
		t.fee = med * (t.priv ? f[1] : f[0]) * tuiStep(cc, y);
		if (t.fee <= 0) {
			return null;
		}
		double sr = r.nextDouble();
		if (!grad && "KOR".equals(cc) && y >= 2012 && sr < KR_P[q] + (q == 4 && y >= 2025 ? 0.2 : 0)) {
			t.schol = Math.min(t.fee, q == 1 ? t.fee : med * KR_A[q]);
			t.scholName = "국가장학금";
		} else if (!grad && "CHN".equals(cc) && y >= 2007 && sr < CN_P[q]) {
			t.schol = Math.min(t.fee, med * 0.055 * tuiStep(cc, y));
			t.scholName = "국가 조학금";
		} else if (!grad && "VNM".equals(cc) && y >= 2020) {
			int n = 0;
			for (int i = 0; i < 2; i++) {
				if (random.nextDouble() < clamp((life.getKnowledge() - 65) / 170, 0, 0.2) * (t.priv ? 0.25 : 1)) {
					n++;
				}
			}
			if (n > 0) {
				t.schol = Math.min(t.fee, t.fee / 2 * n);
				t.scholName = "학업 장려 장학금";
			}
		}
		double m = 0;
		if (!"N".equals(life.getParentAlive()) && !"Y".equals(life.getMarriedYn()) && life.getCurrentAge() < 30) {
			m = richLevel(life) > 0 ? 1 : "CHN".equals(cc) ? CHN_FP[q] : FAM_PAY[q];
		}
		t.famShare = m;
		t.fam = (t.fee - t.schol) * m;
		t.rest = Math.max(0, t.fee - t.schol - t.fam);
		return t;
	}

	// 대학 진학 선택 창에 붙일 등록금 안내 (원작 tuiYear 첫해 문장)
	private String uniFeeNote(LifeDTO life, CountryStatDTO stat, boolean grad) {
		if (freeUni(life)) {
			return "등록금은 없고, 나라에서 주는 장학금을 받으며 기숙사에서 지내요.";
		}
		Tui t = tuition(life, stat, medIncome(life.getCurrentCountry(), stat), grad);
		if (t == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder((grad ? "대학원" : "대학") + " 등록금은 한 해 약 " + won(life, t.fee) + (t.priv ? " (사립)" : " (국공립)") + "이에요.");
		if (t.schol > 0) {
			sb.append(" ").append(t.scholName).append("으로 한 해 ").append(won(life, t.schol)).append("을 받아요.");
		}
		sb.append(" ").append(feeWho(life, t, true));
		return sb.toString();
	}

	// 등록금을 누가 내는지 (원작 문장)
	private String feeWho(LifeDTO life, Tui t, boolean future) {
		if (t.rest <= t.fee * 0.02) {
			return t.fam > 0 ? (future ? "나머지는 부모님이 내 주세요." : "나머지는 부모님이 내 주셨다.") : "";
		}
		String self = stuLoanOK(life) ? (future ? "학자금 대출로 내요" : "학자금 대출로 냈다") : (future ? "스스로 마련해야 해요" : "스스로 마련했다");
		if (t.fam > 0) {
			int pct = (int) Math.round(t.famShare * 100);
			return future ? "부모님이 " + pct + "%를 보태 주시고, 나머지는 " + self + "."
					: "부모님이 " + pct + "%를 보태 주셨고, 나머지는 " + self + ".";
		}
		return (future ? "부모님 도움은 어려워, 등록금은 " : "부모님 도움 없이 등록금은 ") + self + ".";
	}

	private void uniFees(LifeDTO life, CountryStatDTO stat, double med) {
		EngState es = engState(life);
		boolean grad = "GRAD".equals(life.getEduLevel());
		String key = "FEE_" + life.getEduLevel();
		boolean first = !es.done.containsKey(key);
		es.done.put(key, life.getCurrentAge());
		if (freeUni(life)) {
			if (first) {
				String t = (grad ? "대학원" : "대학") + " 등록금은 없었다. 나라에서 주는 장학금을 받으며 기숙사에서 지냈다.";
				saveEventLog(life, life.getCurrentAge(), null, null, t);
				addYearNote(life, t);
			}
			return;
		}
		Tui t = tuition(life, stat, med, grad);
		if (t == null) {
			return; // 원작처럼 이 나라는 등록금을 따로 계산하지 않음
		}
		book(life, grad ? "대학원 등록금" : "대학 등록금", -t.fee);
		if (t.schol > 0) {
			book(life, t.scholName, t.schol);
		}
		if (t.fam > 0) {
			book(life, "부모님이 내 준 등록금", t.fam);
		}
		life.setAssets(life.getAssets() - Math.round(t.rest));
		if (first) {
			StringBuilder sb = new StringBuilder((grad ? "대학원" : "대학") + " 등록금은 한 해 약 " + won(life, t.fee)
					+ (t.priv ? " (사립)" : " (국공립)") + "이었다.");
			if (t.schol > 0) {
				sb.append(" ").append(t.scholName).append("으로 한 해 ").append(won(life, t.schol)).append("을 받았다.");
			}
			String who = feeWho(life, t, false);
			if (!who.isEmpty()) {
				sb.append(" ").append(who);
			}
			saveEventLog(life, life.getCurrentAge(), null, null, sb.toString());
			addYearNote(life, sb.toString());
		}
	}

	// ================= 반려동물 (원작 petq · petYear) =================
	//   LIFE.OWNS 에 "PET-종류-이름-들인나이-수명" 한 개 (DB 구조 그대로) · 종류 d 강아지 / c 고양이
	//   수명: 강아지 10~15년, 고양이 12~18년 (원작 PET_LIFE) · 이름은 나라별 흔한 이름
	//   돌봄 비용: 그 나라 중위 소득 × (강아지 3% · 고양이 2%) × (고소득국 1, 그 외 0.6) (원작 petCost), 18세부터 내가 냄
	private static final Map<String, String[]> PET_NAMES = Map.of(
			"KOR", new String[] { "보리", "콩이", "초코", "두부", "누리" },
			"JPN", new String[] { "하나", "모모", "소라", "무기", "코코" },
			"CHN", new String[] { "더우더우", "추추", "마오마오", "화화" },
			"_", new String[] { "루나", "코코", "밀로", "벨라", "맥스" });

	// 그 나라에서 개·고양이를 키우는 집 비율 (원작 petOwn 대략값: 고소득국 높고 남아시아 낮음)
	private double petOwnRate(LifeDTO life) {
		String g = groupOf(life.getCurrentCountry(), yearStat(life));
		switch (g) {
		case "hi": return 0.45;
		case "eca": return 0.35;
		case "sa": return 0.08;
		case "mena": return 0.12;
		default: return 0.22;
		}
	}

	// 원작 PET_LIFE · PET_COSTK · PET_SM (종류: d 강아지 · c 고양이 · h 햄스터 · r 토끼 · b 앵무새 · f 물고기)
	private static final Map<String, int[]> PET_LIFE = Map.of("d", new int[] { 10, 15 }, "c", new int[] { 12, 18 },
			"h", new int[] { 2, 3 }, "r", new int[] { 8, 12 }, "b", new int[] { 6, 12 }, "f", new int[] { 4, 9 });
	private static final Map<String, Double> PET_COSTK = Map.of("d", 0.03, "c", 0.02, "h", 0.006, "r", 0.01, "b", 0.008, "f", 0.008);
	private static final Map<String, String> PET_WORD = Map.of("d", "강아지", "c", "고양이", "h", "햄스터", "r", "토끼", "b", "앵무새", "f", "물고기");

	// 그 나라에서 흔한 작은 동물 하나 (원작 PET_SM: 권역별 비율)
	private String petSmallKind(LifeDTO life) {
		String g = groupOf(life.getCurrentCountry(), yearStat(life));
		String[] k;
		double[] w;
		if ("sa".equals(g) || "mena".equals(g)) {
			k = new String[] { "b", "f", "r" };
			w = new double[] { 0.55, 0.3, 0.15 };
		} else if ("hi".equals(g)) {
			k = new String[] { "f", "h", "b", "r" };
			w = new double[] { 0.4, 0.25, 0.2, 0.15 };
		} else if ("eca".equals(g)) {
			k = new String[] { "f", "h", "b", "r" };
			w = new double[] { 0.35, 0.25, 0.25, 0.15 };
		} else {
			k = new String[] { "f", "b", "h", "r" };
			w = new double[] { 0.45, 0.25, 0.2, 0.1 };
		}
		double x = random.nextDouble(), sum = 0;
		for (int i = 0; i < k.length; i++) {
			sum += w[i];
			if (x < sum) {
				return k[i];
			}
		}
		return k[0];
	}

	// 한 해 돌봄 비용 (원작 petCost): 중위 소득 × 종류별 비율 × (고소득국 1, 그 외 0.6)
	private double petCost(LifeDTO life, String kind) {
		CountryStatDTO stat = yearStat(life);
		double c = medIncome(life.getCurrentCountry(), stat) * PET_COSTK.getOrDefault(kind, 0.02)
				* ("hi".equals(groupOf(life.getCurrentCountry(), stat)) ? 1 : 0.6);
		// 한 해 비용 최소 (한국 체감): 개 20만 · 고양이 15만 · 작은 동물 5만 (가난한 나라에서 '한 해 약 4,500원'이 나왔음)
		int floor = "d".equals(kind) ? 200000 : "c".equals(kind) ? 150000 : 50000;
		return Math.max(c, Math.ceil(floor / krwPerUsd(stat)));
	}

	// 이름 다듬기 (원작 petNameClean): 12자까지, OWNS 구분자(쉼표·하이픈)와 따옴표·꺾쇠 빼고
	private static String petNameClean(String n) {
		if (n == null) {
			return "";
		}
		String t = java.text.Normalizer.normalize(n, java.text.Normalizer.Form.NFC)
				.replaceAll("[\\p{Cntrl}\\u200b-\\u200f\\u202a-\\u202e,\\-<>\"'`&\\\\]", "").replaceAll("\\s+", " ").trim();
		return t.length() > 12 ? t.substring(0, 12).trim() : t;
	}

	private static String petEul(String w) {
		return EngineChoice.eul(w);
	}

	// 들이기 (원작 petAdopt): 적은 이름이 없으면 그 나라 흔한 이름, 수명은 종류별 범위에서 하나 (언제 떠날지는 미리 알 수 없음)
	private String petAdopt(LifeDTO life, String kind, int age, String typed) {
		java.util.LinkedHashSet<String> owns = ownSet(life);
		owns.removeIf(x -> x.startsWith("PET-"));
		String name = petNameClean(typed);
		if (name.isEmpty()) {
			String[] pool = PET_NAMES.getOrDefault(life.getCurrentCountry(), PET_NAMES.get("_"));
			name = pool[random.nextInt(pool.length)];
		}
		int[] r = PET_LIFE.getOrDefault(kind, PET_LIFE.get("c"));
		int span = r[0] + random.nextInt(r[1] - r[0] + 1);
		owns.add("PET-" + kind + "-" + name + "-" + age + "-" + span);
		life.setOwns(String.join(",", owns));
		boolean big = "d".equals(kind) || "c".equals(kind);
		life.setHappiness(life.getHappiness() + (big ? 5 : 3));
		life.setRelation(life.getRelation() + (big ? 2 : 1));
		if ("f".equals(kind)) {
			return "어항을 들였다. 물고기 이름은 " + ro(name) + " 지었다.";
		}
		return PET_WORD.getOrDefault(kind, "반려동물") + " " + iga(name) + " 가족이 되었다.";
	}

	// [이름, 종류(d·c), 들인 나이, 수명] 또는 null
	private static String[] petOf(LifeDTO life) {
		if (life.getOwns() == null) {
			return null;
		}
		for (String x : life.getOwns().split(",")) {
			String[] p = x.trim().split("-");
			if (p.length == 5 && "PET".equals(p[0])) {
				return new String[] { p[2], p[1], p[3], p[4] };
			}
		}
		return null;
	}

	// 해마다: 돌봄 비용 · 작은 기쁨, 수명이 다하면 떠나보냄 (원작 petYear · petGone)
	private void petYear(LifeDTO life, CountryStatDTO stat) {
		String[] p = petOf(life);
		if (p == null || !"PLAYING".equals(life.getStatus())) {
			return;
		}
		int age = life.getCurrentAge();
		int years = age - Integer.parseInt(p[2]);
		String kindWord = PET_WORD.getOrDefault(p[1], "반려동물");
		if (years >= Integer.parseInt(p[3])) {
			java.util.LinkedHashSet<String> owns = ownSet(life);
			owns.removeIf(x -> x.startsWith("PET-"));
			life.setOwns(String.join(",", owns));
			life.setHappiness(life.getHappiness() - 7);
			life.setRelation(life.getRelation() - 1);
			String t = iga(p[0]) + " " + years + "살로 무지개다리를 건넜다. 함께한 " + kindWord + "의 빈자리가 오래 남았다.";
			saveEventLog(life, age, null, null, t);
			addYearNote(life, t);
			return;
		}
		boolean big = "d".equals(p[1]) || "c".equals(p[1]);
		life.setHappiness(life.getHappiness() + (big ? 0.6 : 0.35)); // 원작 petYear
		life.setRelation(life.getRelation() + (big ? 0.3 : 0.1));
		if (age >= 18 && (life.getIncome() > 0 || life.getAssets() > 0)) {
			double cost = petCost(life, p[1]);
			life.setAssets(life.getAssets() - Math.round(cost));
			book(life, "반려동물 돌봄 비용", -cost);
		}
	}

	// 받침에 따라 이/가, (으)로
	private static String iga(String w) {
		// 끝의 괄호·기호는 건너뛰고 마지막 한글로 판단: '주식 (지수)' → '주식 (지수)가'
		int i = w.length() - 1;
		while (i > 0 && (w.charAt(i) == ')' || w.charAt(i) == ' ')) i--;
		char ch = w.charAt(i);
		if (ch < 0xAC00 || ch > 0xD7A3) {
			return w + "이(가)";
		}
		return w + ((ch - 0xAC00) % 28 == 0 ? "가" : "이");
	}

	private static String ro(String w) {
		char ch = w.charAt(w.length() - 1);
		if (ch < 0xAC00 || ch > 0xD7A3) {
			return w + "(으)로";
		}
		int jong = (ch - 0xAC00) % 28;
		return w + (jong == 0 || jong == 8 ? "로" : "으로");
	}

	// 잠시 멈춤에 적은 한 줄 (원작 reflect): 200자까지, 따옴표로 감싸 인생 기록에 남김 (DB 구조 그대로, LIFE_EVENT_LOG 한 줄)
	//   atAge: 화면이 보고 있는 나이 (서버 나이와 한 살 차이까지만, 없으면 지금 나이)
	public String saveNote(LifeDTO life, String text, Integer atAge) {
		if (text == null || !"PLAYING".equals(life.getStatus())) {
			return null;
		}
		String t = text.replaceAll("\\s+", " ").trim();
		if (t.isEmpty()) {
			return null;
		}
		if (t.length() > 200) {
			t = t.substring(0, 200);
		}
		int age = life.getCurrentAge();
		if (atAge != null && Math.abs(atAge - age) <= 1) {
			age = atAge;
		}
		String line = "“" + t + "”";
		saveEventLog(life, age, null, null, line);
		return line;
	}

	// 직업을 제안받을 수 있는 상태인지: 성인(JOB_MIN_AGE) 이상, 학교 안 다니고, 일자리 없고, 은퇴 나이 전
	private boolean canGetJob(LifeDTO life, CountryStatDTO stat) {
		boolean noJob = life.getJobStatus() == null || "UNEMPLOYED".equals(life.getJobStatus());
		return "PLAYING".equals(life.getStatus()) && life.getCurrentAge() >= JOB_MIN_AGE && !svcActive(life)
				&& !"ENROLLED".equals(life.getSchoolStatus()) && noJob && life.getCurrentAge() < retireAge(life, stat);
	}

	// 직업 선택 창 띄우기 (매년 진행 후, 이어하기·새로고침 때)
	private void showOffers(LifeDTO life, CountryStatDTO stat) {
		if (!canGetJob(life, stat)) {
			return;
		}
		// 범죄 기록 (원작 recW · jobSure): 출소 뒤 6년까지는 그대로, 그 뒤 5년마다 절반 → 0.5 × 무게 확률로 그해 일자리 없음
		//   새로고침·이어하기에도 같은 결과가 나오게 인생·나이로 정한 난수
		double rw = recordWeight(life);
		if (rw > 0 && new java.util.SplittableRandom(life.getLifeId() * 7919L + life.getCurrentAge() * 31L).nextDouble() < 0.5 * rw) {
			return;
		}
		EngState es = engStates.get(life.getLifeId());
		if (es != null && es.schoolNext != null) {
			return; // 진학할지 고르는 해 (고른 뒤 안 가면 chooseEngine 에서 제안)
		}
		List<JobDTO> offers = new ArrayList<>();
		double gdp = gdpOf(stat);
		for (JobDTO j : offerJobs(life)) {
			JobDTO c = j.copy();
			c.setYearIncome(Math.round(jobPay(gdp, j.getIncomeRate(), life.getCurrentCountry(), stat))); // moneyStep 소득과 같은 식 (±20%는 빼고 가운데 값)
			offers.add(c);
		}
		if (!offers.isEmpty()) {
			life.setNeedJob(true);
			life.setJobOffers(offers);
		}
	}

	// 필요 학력을 갖춘 직업 중 권역 산업 비율에 따라 3개 제안
	// 취업 준비 점수(0~2)가 높을수록 소득 배율 높은 직업이 나올 확률 증가
	// 제안은 DB에 저장하지 않고, 같은 인생·나이·준비 점수·학력·나라면 항상 같은 3개가 나오게 뽑음
	// → 새로고침해도 같은 제안이 다시 뜨고, 직업 선택 때 제안받은 직업인지 다시 뽑아서 확인 가능
	// 전공 → 잘 맞는 직업 이름 (정규식)
	private static final Map<String, String> MAJOR_JOB = Map.of(
			"hum", ".*(작가|기자|사무|번역|공무원|교사).*", "law", ".*(변호사|판사|검사|공무원|경찰|법).*",
			"biz", ".*(회사원|사무|금융|은행|회계|영업|경영|마케팅|사업).*", "eng", ".*(엔지니어|개발|프로그래머|기술|연구|설계).*",
			"med", ".*(의사|간호|약사|보건|치료|의료).*", "edu", ".*(교사|교수|강사|보육).*",
			"art", ".*(예술|크리에이터|디자이너|운동|코치|배우|음악).*");

	private List<JobDTO> offerJobs(LifeDTO life) {
		long seed = life.getLifeId() * 1000003L + life.getCurrentAge() * 101L + life.getPrepScore() * 7L
				+ finishedLevel(life) * 13L + String.valueOf(life.getCurrentCountry()).hashCode();
		java.util.SplittableRandom rnd = new java.util.SplittableRandom(seed);
		// 권역 산업 비율 (%) - 데이터가 없으면 기본값
		double agri = 40, industry = 25, service = 35;
		RegionStatDTO rs = regionStat(life);
		if (rs != null) {
			if (rs.getAgriRate() != null) {
				agri = rs.getAgriRate();
			}
			if (rs.getIndustryRate() != null) {
				industry = rs.getIndustryRate();
			}
			if (rs.getServiceRate() != null) {
				service = rs.getServiceRate();
			}
		}

		// 고를 수 있는 직업과 뽑힐 무게
		List<JobDTO> pool = new ArrayList<>();
		List<Double> weights = new ArrayList<>();
		int finished = finishedLevel(life);
		for (JobDTO job : jobs()) {
			if (levelIndex(job.getRequiredEdu()) > finished) {
				continue; // 학력 부족
			}
			double w = service;
			if (job.getIndustry().equals("AGRI")) {
				w = agri;
			} else if (job.getIndustry().equals("INDUSTRY")) {
				w = industry;
			}
			w = w * (1 + 0.5 * life.getPrepScore() * (job.getIncomeRate() - 1));
			// 전공에 맞는 일은 4배 잘 들어옴 (원작 major: 전공이 일자리를 정함)
			String mj = engState(life).major;
			if (mj != null && MAJOR_JOB.containsKey(mj) && job.getJobName() != null && job.getJobName().matches(MAJOR_JOB.get(mj))) {
				w = w * 4;
			}
			if (w < 0.1) {
				w = 0.1;
			}
			pool.add(job);
			weights.add(w);
		}

		// 무게 비율대로 겹치지 않게 3개 뽑기
		List<JobDTO> offers = new ArrayList<>();
		while (offers.size() < 3 && !pool.isEmpty()) {
			double total = 0;
			for (double w : weights) {
				total += w;
			}
			double pick = rnd.nextDouble() * total;
			double sum = 0;
			int chosen = pool.size() - 1;
			for (int i = 0; i < pool.size(); i++) {
				sum += weights.get(i);
				if (pick < sum) {
					chosen = i;
					break;
				}
			}
			offers.add(pool.remove(chosen));
			weights.remove(chosen);
		}
		return offers;
	}

	// 1년 소득·재산 처리 (원작 finances 기준, 단순화)
	private static final double DEBT_RATE = 0.06; // 빚 이자 (연)

	private void moneyStep(LifeDTO life, CountryStatDTO stat) {
		double gdp = gdpOf(stat);
		String cc = life.getCurrentCountry();
		double med = medIncome(cc, stat); // 그 나라 중위 소득
		double minLive = med * 0.3; // 최소 생활비 (원작 c)
		int age = life.getCurrentAge();
		String focus = life.getFocusCode();

		// 취업 준비 집중: 1년마다 준비 점수 +1 (최대 2)
		if ("JOB_PREP".equals(focus) && life.getPrepScore() < 2) {
			life.setPrepScore(life.getPrepScore() + 1);
		}

		// 1. 소득
		double income = 0;
		boolean retired = "RETIRED".equals(life.getJobStatus());
		if ("EMPLOYED".equals(life.getJobStatus())) {
			// 소득 = 1인당 GDP × 소득 배율 × 랜덤(±20%), 일에 몰두하면 ×1.1 (원작)
			income = jobPay(gdp, life.getIncomeRate(), cc, stat) * (0.8 + random.nextDouble() * 0.4);
			if ("WORK".equals(focus)) {
				income = income * 1.1;
			}

			// 소득 배율 상승: 근속 10년까지 +1.5%, 20년까지 +0.7%, 그 후 +0.1%, 55세 이후 -0.4%
			//   일에 몰두 +1.2%p, 자기계발 +0.6%p (원작)
			int y = life.getJobYears();
			double up = y < 10 ? 1.5 : y < 20 ? 0.7 : 0.1;
			if (age >= 55) {
				up = up - 0.4;
			}
			if ("WORK".equals(focus)) {
				up = up + 1.2;
			} else if ("SELF_DEV".equals(focus)) {
				up = up + 0.6;
			}
			life.setIncomeRate(life.getIncomeRate() * (1 + up / 100));
			life.setJobYears(y + 1);
		} else if (retired && life.getPension() != null) {
			income = life.getPension(); // 연금
		} else if ("PARTTIME".equals(focus) && !inPrison(life)) {
			income = gdp * 0.15; // 아르바이트
		}
		life.setIncome(Math.round(income));

		// 18세 미만·학생은 집에서 돌봐 줌 (생활비·저축 없음), 대학·대학원생은 학비만 (원작 학비·부모 도움·학자금 대출)
		if (age < 18 || "ENROLLED".equals(life.getSchoolStatus())) {
			if ("ENROLLED".equals(life.getSchoolStatus()) && ("UNI".equals(life.getEduLevel()) || "GRAD".equals(life.getEduLevel()))) {
				uniFees(life, stat, med);
			}
			return;
		}

		double change = 0;
		if (inPrison(life)) {
			// 수감 중: 먹고 자는 건 교도소에서, 벌이도 생활비도 없음 → 배우자 벌이에서 남긴 돈과 빚 이자만
			if ("Y".equals(life.getMarriedYn()) && age < retireAge(life, stat)) {
				change = med * saveRate(med, med) * 0.7;
				book(life, "배우자 벌이에서 남긴 돈", change);
			}
			if (life.getAssets() + change < 0) {
				double interest = (life.getAssets() + change) * DEBT_RATE;
				change += interest;
				book(life, "빚 이자", interest);
			}
			life.setAssets(life.getAssets() + Math.round(change));
			return;
		}
		if (income > 0) {
			double sr = saveRate(income, med);
			if (retired) {
				// 연금에서 남기는 돈: 저축률(최대 3%) + 연금이 넉넉하면 남는 몫 (원작 retKeep)
				double e = income / med;
				double keep = e > 1.5 ? clamp(1 - (1.5 + 0.25 * (e - 1.5)) / e, 0, 0.65) : 0;
				change = income * (Math.min(sr, 0.03) + keep);
				book(life, "연금에서 남긴 돈", change);
			} else {
				change = income * sr; // 번 돈에서 남긴 돈
				book(life, "벌이에서 남긴 돈", change);
				// 아이 키우는 비용 (원작 kidK): 자녀 1명당 소득 × (3% + 저축률 10% 넘는 만큼 × 0.2)
				//   자녀 나이는 저장하지 않아서 55세 전까지 키운다고 봄
				if (age < 55 && life.getChildCount() > 0) {
					double kid = life.getChildCount() * income * (0.03 + 0.2 * Math.max(0, sr - 0.1));
					change -= kid;
					book(life, "아이 키우는 비용", -kid);
				}
				// 배우자도 일하면 배우자 벌이에서 남긴 돈 × 0.85 (배우자 = 그 나라 중위 소득으로 봄)
				EngState bs = engState(life);
				if ("Y".equals(life.getMarriedYn()) && age < retireAge(life, stat) && !(bs.spOffUntil != null && age <= bs.spOffUntil)) {
					double sp = med * saveRate(med, med) * 0.85;
					change += sp;
					book(life, "배우자 벌이에서 남긴 돈", sp);
				}
				// 자기계발: 배움에 소득의 2% (원작)
				if ("SELF_DEV".equals(focus)) {
					change -= income * 0.02;
					book(life, "자기계발 비용", -income * 0.02);
				}
				// 건강 관리: 운동·검진에 소득의 2% (명세서 C04-4)
				if ("HEALTH".equals(focus)) {
					change -= income * 0.02;
					book(life, "건강 관리 비용", -income * 0.02);
				}
				// 최소 생활비보다 적게 벌면 모자란 돈의 절반이 빠짐
				if (income < minLive) {
					change -= (minLive - income) * 0.5;
					book(life, "모자란 생활비", -(minLive - income) * 0.5);
				}
			}
		} else {
			// 벌이 없음 (원작 finances): 사는 형편에 따라 생활비가 다름
			String g = groupOf(cc, stat);
			boolean married = "Y".equals(life.getMarriedYn());
			// 부모 집: 30세 전·미혼·부모 생존 → 생활비 ×0.3
			boolean withParents = age < 30 && !married && "Y".equals(life.getParentAlive());
			// 자녀 집: 60세 이상·자녀 있음 → 생활비 ×0.3, 돈이 바닥나면 자녀가 생활비를 맡음
			boolean withKids = age >= 60 && life.getChildCount() > 0;
			// 친정·가족 집 (남아시아·중동 미혼 여성) → 생활비 ×0.3, 돈이 바닥나면 가족이 맡음
			boolean withKin = "F".equals(life.getGender()) && !married && (g.equals("sa") || g.equals("mena"))
					&& ("Y".equals(life.getParentAlive()) || life.getChildCount() > 0);
			// 배우자 벌이 (은퇴 전): 우리 엔진엔 배우자 소득이 없어서 그 나라 중위 소득을 버는 배우자로 봄
			boolean partnerWorks = married && age < retireAge(life, stat);

			if (partnerWorks) {
				// 배우자 벌이에서 남긴 돈 × 0.7 - 양육비 - 모아 둔 돈에서 2%
				double psr = saveRate(med, med);
				change = med * psr * 0.7;
				book(life, "배우자 벌이에서 남긴 돈", change);
				if (age < 55 && life.getChildCount() > 0) {
					double kid = life.getChildCount() * med * (0.03 + 0.2 * Math.max(0, psr - 0.1));
					change -= kid;
					book(life, "아이 키우는 비용", -kid);
				}
				change -= 0.02 * Math.max(0, life.getAssets());
				book(life, "모은 돈에서 쓴 생활비", -0.02 * Math.max(0, life.getAssets()));
			} else {
				double k = withKids || withKin || withParents ? 0.3 : married ? 0.6 : 1;
				double live = minLive * k;
				// 실업급여 (고소득국·유럽중앙아시아): 마지막 소득의 절반 (중위 소득의 40~80%)
				if ("UNEMPLOYED".equals(life.getJobStatus()) && (g.equals("hi") || g.equals("eca"))) {
					double last = life.getIncomeRate() != null ? gdp * life.getIncomeRate() : 0;
					double ub = Math.min(Math.max(med * 0.4, last * 0.5), med * 0.8);
					change += ub;
					book(life, "실업급여", ub);
				}
				// 모아 둔 돈에서 꺼내 쓰기: 4% (은퇴 후 75세부터 6%, 85세부터 8%), 중위 소득 20년치 넘는 부분은 1.5%
				double draw = retired ? (age < 75 ? 0.04 : age < 85 ? 0.06 : 0.08) : 0.04;
				double money = Math.max(0, life.getAssets());
				double cap = med * 20;
				double fromSave = draw * Math.min(money, cap) + 0.015 * Math.max(0, money - cap);
				if (withKids) {
					fromSave = fromSave * 0.5; // 자녀와 함께 살면 절반
				}
				if (retired && !married) {
					fromSave = fromSave * 0.8; // 혼자 사는 집
				}
				change += -live - fromSave;
				book(life, "생활비", -live);
				book(life, "모은 돈에서 쓴 생활비", -fromSave);
				// 돈이 바닥나면 함께 사는 자녀·가족이 생활비를 맡음
				if ((withKids || withKin) && life.getAssets() + change < 0) {
					double before = change;
					change = Math.min(0, change + live);
					book(life, "가족이 맡아 준 생활비", change - before);
				}
			}
			// 60세 이상은 자녀가 보내 주는 돈 (원작: 자녀 최대 2명 × 중위 소득 × 12%, 관계 40 이하면 절반)
			if (age >= 60 && life.getChildCount() > 0) {
				double kidSend = Math.min(2, life.getChildCount()) * med * 0.12 * (life.getRelation() > 40 ? 1 : 0.5);
				change += kidSend;
				book(life, "자녀가 보내 준 돈", kidSend);
			}
		}

		// 빚 이자: 빚(마이너스 재산)에 연 6% (원작은 대출마다 따로 계산 - 우리는 빚 전체에 한 번에)
		if (life.getAssets() + change < 0) {
			double interest = (life.getAssets() + change) * DEBT_RATE;
			change += interest;
			book(life, "빚 이자", interest);
		}

		// 남은 돈은 재산에 (마이너스면 빚)
		life.setAssets(life.getAssets() + Math.round(change));
	}

	// ================= C09 이벤트 반영 =================

	// 올해 일어난 이벤트(윤아님 EventService.rollYear)를 인생에 반영
	// 선택 이벤트(한 해 최대 1개)는 바로 반영하지 않고 선택을 기다림 → chooseEvent
	private void eventStep(LifeDTO life) {
		List<EventResultDTO> events = eventService.rollYear(life);

		for (EventResultDTO e : events) {
			applyHistEffect(life, e); // 역사 사건이면 실직·학업 중단 (선택 이벤트도 사건은 이미 일어남)
			if (e.isChoice()) {
				// 돈·신용이 모자라 큰돈 드는 선택지가 다 빠지고 '하지 않는다' 하나만 남으면, 기회 자체를 띄우지 않음
				//   (예전엔 '부동산 투자' 창에 '하지 않는다' 하나만 떠서 누를 수밖에 없었음)
				List<ChoiceCostDTO> cs = costsOf(life, e.getChoices(), e.getLogText());
				if (onlyNoLeft(e.getChoices(), cs)) {
					continue;
				}
				// 상황 문장만 기록하고, 효과는 고른 선택지로 (PENDING_EVENT_ID에 저장)
				life.setPendingEventId(e.getEventId());
				life.setNeedChoice(true);
				if (isProposal(e) && !"Y".equals(life.getMarriedYn())) {
					e.setLogText(e.getLogText() + " " + candLine(life, yearStat(life), true)); // 원작 wedtalk: 상대의 나이·일·형편·돈
				}
				life.setPendingEvent(e);
				life.setChoiceCosts(cs);
				saveEventLog(life, life.getCurrentAge(), e.getEventId(), null, e.getLogText());
				continue;
			}
			applyEvent(life, e, life.getCurrentAge(), null);
		}

		for (EventResultDTO e : events) {
			if (bigHist(e)) {
				life.addAlert(e.getLogText());
			}
		}

		fixStats(life);
		// [화면용] 올해 일어난 일: ④ 돈 계산에서 먼저 남긴 줄(대학 학비 등)을 앞에 두고 이어 붙임
		List<EventResultDTO> all = new ArrayList<>();
		if (life.getEvents() != null) {
			all.addAll(life.getEvents());
		}
		all.addAll(events);
		life.setEvents(all);
	}

	// ================= 수저별 부모님 도움 (원작 famCanHelp · richGiftYear · homeHelp · ESTATE, 11/23) =================
	//   ① 첫 직장 살림 밑천: 30세 전 첫 일자리, 동수저 이상 → 형편 위치 소득 × 15% × (형편−1)
	//   ② 학창 시절 빚 갚아 줌: 32세 전·빚이 있고·대학을 다녔고·금수저 이상 → 해마다 금 25% · 다이아 50% 확률, 한 번
	//      (갚아 주는 돈: 중위 소득 × 금 0.6 · 다이아 1.5까지)
	//   ③ 일자리를 찾는 동안 생활비: 35세 전·미혼·금수저 이상 → 중위 소득 × 원작 FAM_PAY × 0.6 (아주 부유하면 ×1)
	//   ④ 아주 부유한 집 (원작 rich): 다이아수저 가운데 5% (그중 10%는 더 부유) →
	//      18~25세 해마다 중위 소득 × 0.35 (더 부유 1.3), 28~45세에 한 번 재산 일부를 미리 물려줌 (해마다 12%)
	//   ⑤ 집 살 때 도움: 45세 전·은수저 이상·한 번 → 중위 소득 × (형편−2) × (동아시아 1.5 · 그 밖 0.5)
	//   ⑥ 부모님이 돌아가시면 남긴 재산 (원작 ESTATE): 중위 소득 × {0, 0.5, 1.5, 4, 8, 18}[형편] (+ 은수저 이상은 부모님 집)
	//      × 0.5~1.5 ÷ (1 + 0.8 × 형제자매 수(최대 4)), 흙·동수저는 장례를 치르고 나면 거의 남지 않음
	private static final double[] ESTATE = { 0, 0.5, 1.5, 4, 8, 18 };
	private static final double[] PHOME_K = { 0, 0.5, 0.65, 0.8, 1, 1.4 };
	private static final java.util.Set<String> HOME_HELP_CC = java.util.Set.of("CHN", "KOR", "JPN", "TWN", "HKG", "SGP", "VNM");
	private static final Map<String, Double> EST_TAX = Map.of("KOR", 0.35, "JPN", 0.35, "TWN", 0.1, "PHL", 0.06, "THA", 0.05);

	// 아주 부유한 집 (원작 S.flags.rich): 0 아님, 1 부유, 2 더 부유 - 다이아수저로 태어난 사람의 5%
	private static int richLevel(LifeDTO life) {
		if (originOf(life) < 5) {
			return 0;
		}
		java.util.SplittableRandom r = new java.util.SplittableRandom(life.getLifeId() * 977L + 13);
		if (r.nextDouble() >= 0.05) {
			return 0;
		}
		return r.nextDouble() < 0.1 ? 2 : 1;
	}

	// 그 나라 소득 분포에서 p 위치의 소득 (원작 incomeAt)
	private double incomeAt(String cc, CountryStatDTO stat, double p) {
		double sg = sigmaOf(cc, stat);
		return gdpOf(stat) * 0.9 * Math.exp(sg * invNorm(clamp(p, 0.01, 0.99)) - sg * sg / 2);
	}

	private boolean parentsHelp(LifeDTO life) {
		return !"N".equals(life.getParentAlive());
	}

	// 부모님 도움 한 줄 (인생 기록 + 올해 화면)
	private void parentNote(LifeDTO life, String text) {
		saveEventLog(life, life.getCurrentAge(), null, null, text);
		addYearNote(life, text);
	}

	// ① 첫 직장 살림 밑천 (chooseJob 에서)
	private String startFund(LifeDTO life, CountryStatDTO stat) {
		int q = (int) clamp(life.getWealthClass(), 1, 5);
		EngState s = engState(life);
		if (s.done.containsKey("PAR_SEED") || q < 2 || life.getCurrentAge() >= 30 || !parentsHelp(life)
				|| life.getAssets() > medIncome(life.getCurrentCountry(), stat) * 0.5) {
			return null;
		}
		s.done.put("PAR_SEED", life.getCurrentAge());
		double a = incomeAt(life.getCurrentCountry(), stat, q / 5.0 - 0.1) * 0.15 * (q - 1);
		life.setAssets(life.getAssets() + Math.round(a));
		return "일을 시작하자 부모님이 살림 밑천으로 " + won(life, a) + "을 보태 주셨다.";
	}

	// ②③④ 해마다 (moneyStep 다음)
	private void parentHelpYear(LifeDTO life, CountryStatDTO stat) {
		if (!"PLAYING".equals(life.getStatus()) || !parentsHelp(life)) {
			return;
		}
		int age = life.getCurrentAge();
		int q = (int) clamp(life.getWealthClass(), 1, 5);
		int rich = richLevel(life);
		boolean married = "Y".equals(life.getMarriedYn());
		boolean student = "ENROLLED".equals(life.getSchoolStatus());
		double med = medIncome(life.getCurrentCountry(), stat);
		EngState s = engState(life);
		boolean uniDone = "UNI".equals(life.getEduLevel()) || "GRAD".equals(life.getEduLevel());

		// ② 학창 시절 빚
		if (!student && uniDone && age < 32 && life.getAssets() < 0 && q >= 4 && !s.done.containsKey("PAR_DEBT")
				&& random.nextDouble() < (q == 5 ? 0.5 : 0.25)) {
			double pay = Math.min(-life.getAssets(), med * (q == 5 ? 1.5 : 0.6));
			life.setAssets(life.getAssets() + Math.round(pay));
			book(life, "부모님이 갚아 준 빚", pay);
			s.done.put("PAR_DEBT", age);
			parentNote(life, "부모님이 학창 시절의 빚 " + won(life, pay) + "을 갚아 주셨다.");
		}
		// ③ 일자리를 찾는 동안 생활비
		if (!student && !married && age < 35 && age >= 18 && q >= 4 && "UNEMPLOYED".equals(life.getJobStatus())) {
			double a = med * FAM_PAY[q] * (rich > 0 ? 1 : 0.6) * 0.3; // 한 해 생활비 몫 (원작 q × FAM_PAY, q = 최소 생활비 중위 30%)
			life.setAssets(life.getAssets() + Math.round(a));
			book(life, "부모님이 보내 준 생활비", a);
			if (!s.done.containsKey("PAR_LIVE")) {
				s.done.put("PAR_LIVE", age);
				parentNote(life, "일자리를 찾는 동안 부모님이 생활비를 보내 주셨다.");
			}
		}
		// ④ 아주 부유한 집
		if (rich > 0 && age >= 18 && age <= 25) {
			double a = med * (rich == 2 ? 1.3 : 0.35);
			life.setAssets(life.getAssets() + Math.round(a));
			book(life, "부모님이 보태 준 살림 밑천", a);
			if (!s.done.containsKey("PAR_RICH")) {
				s.done.put("PAR_RICH", age);
				parentNote(life, "부모님이 스물다섯이 될 때까지 해마다 " + won(life, a) + "씩 보내 주시기로 했다.");
			}
		}
		if (rich > 0 && age >= 28 && age <= 45 && !s.done.containsKey("PAR_GIFT") && random.nextDouble() < 0.12) {
			int sib = famSeed(life, stat).siblingGap.length;
			double gift = med * (rich == 2 ? 300 : 60) * 0.15 * (0.6 + 0.6 * random.nextDouble()) / (1 + Math.min(6, sib));
			double tax = gift * EST_TAX.getOrDefault(life.getCurrentCountry(), 0.0);
			life.setAssets(life.getAssets() + Math.round(gift - tax));
			book(life, "부모님이 미리 물려준 재산", gift - tax);
			s.done.put("PAR_GIFT", age);
			parentNote(life, "부모님이 재산의 일부를 미리 물려주셨다. (+" + won(life, gift - tax) + ")"
					+ (tax > 0 ? " 증여세로 " + won(life, tax) + "을 냈다." : ""));
		}
	}

	// ⑤ 집 살 때 부모님 도움 (돈이 모자랄 때만)
	private double homeHelp(LifeDTO life, CountryStatDTO stat, double price, double cash) {
		int q = (int) clamp(life.getWealthClass(), 1, 5);
		if (!parentsHelp(life) || life.getCurrentAge() > 45 || q < 3 || engState(life).done.containsKey("PAR_HOUSE")) {
			return 0;
		}
		double need = price - Math.max(0, cash);
		if (need <= 0) {
			return 0;
		}
		int rich = richLevel(life);
		if (rich > 0) {
			return Math.min(need, price * (rich == 2 ? 1 : 0.6));
		}
		double med = medIncome(life.getCurrentCountry(), stat);
		return Math.min(need, med * (q - 2) * (HOME_HELP_CC.contains(life.getCurrentCountry()) ? 1.5 : 0.5));
	}

	// ⑥ 부모님이 남긴 재산 (부모님 별세 때)
	private void parentEstate(LifeDTO life) {
		CountryStatDTO stat = yearStat(life);
		double med = medIncome(life.getCurrentCountry(), stat);
		int q = (int) clamp(life.getWealthClass(), 1, 5);
		if (q < 3) {
			engState(life).estateNote = "부모님이 남긴 재산은 거의 없었다."; // 별세 문장 뒤에 붙음
			return;
		}
		boolean urban = "Y".equals(life.getIsUrban());
		double o = med * ESTATE[q] * (urban ? 1 : 0.6);
		double home = med * ("hi".equals(groupOf(life.getCurrentCountry(), stat)) ? 5.5 : 5) * (urban ? 1.4 : 0.7) * PHOME_K[q];
		int sib = famSeed(life, stat).siblingGap.length;
		double share = (home + o * 0.4) * (0.5 + random.nextDouble()) / (1 + 0.8 * Math.min(4, sib));
		double tax = share > med * 10 ? (share - med * 10) * EST_TAX.getOrDefault(life.getCurrentCountry(), 0.0) : 0;
		double got = share - tax;
		life.setAssets(life.getAssets() + Math.round(got));
		book(life, "부모님이 남긴 재산", got);
		String t = "부모님이 남긴 재산 가운데 " + (sib > 0 ? "형제자매와 나누고 " : "") + "내 몫으로 " + won(life, got) + "을 물려받았다."
				+ (tax > 0 ? " 상속세로 " + won(life, tax) + "을 냈다." : "");
		EngState es = engState(life);
		es.estateNote = t; // 별세 문장 뒤에 붙음 (applyEvent)
		es.estateAmt = Math.round(got);
	}

	// ================= 만난 사람 · 결혼 상대 (원작 partner · wedtalk, 11/23) =================
	//   윤아님 '새로운 만남' 이벤트가 나올 때마다 새 사람 (k번째 만남, 인생 번호와 k로 늘 같게)
	//   이름·나이·성격·하는 일·집안 형편(내 형편 ±1)을 바로 알려 주고, '청혼' 창에는 모아 둔 돈·빚까지
	//   결혼하면 그 사람이 배우자 (가족과 삶 · 결혼 상대 소개 줄 · 배우자 소득이 모두 같은 사람)
	private static final String[] PERSONA = { "다정한", "성실한", "유머 있는", "야무진", "조용한", "활달한" };

	private static final class Cand {
		String name, job, persona;
		int gap, q;
		double saveK;
		final java.util.Set<String> before = new java.util.HashSet<>(); // 이 사람 전에 만난 사람들 이름
	}

	private Cand cand(LifeDTO life, FamSeed f, int k) {
		Cand c = new Cand();
		java.util.SplittableRandom r = new java.util.SplittableRandom(life.getLifeId() * 7919L + 503 + k * 1009L);
		boolean meM = "M".equals(life.getGender());
		String sg = meM ? "F" : "M";
		c.q = (int) clamp(life.getWealthClass() + r.nextInt(3) - 1, 1, 5);
		c.persona = PERSONA[r.nextInt(PERSONA.length)];
		c.saveK = r.nextDouble();
		if (k == 0) {
			c.name = f.spouse0;
			c.gap = f.gap0;
			c.job = f.job0;
		} else {
			// 앞서 만난 사람들과 이름이 겹치지 않게 (300번 돌려 보니 몇 해 뒤 같은 이름이 다시 나왔음)
			java.util.Set<String> prev = new java.util.HashSet<>();
			Cand p = cand(life, f, k - 1);
			prev.add(p.name);
			prev.addAll(p.before);
			c.before.addAll(prev);
			c.name = pickName(life.getBirthCountry(), sg, r);
			for (int t = 0; t < 12 && c.name != null && prev.contains(c.name); t++) {
				c.name = pickName(life.getBirthCountry(), sg, r);
			}
			c.gap = (meM ? -1 : 1) * (1 + r.nextInt(4)) + (r.nextDouble() < 0.25 ? (meM ? 3 : -3) : 0);
			c.job = parentJob(r, Math.max(2, c.q), "Y".equals(life.getIsUrban()), life.getBirthYear() + 25, "F".equals(sg));
		}
		return c;
	}

	// 지금 만나는 사람을 배우자 자리에 (famSeed 를 쓰는 곳 모두 같은 사람)
	private void syncSpouse(LifeDTO life, FamSeed f) {
		int k = engState(life).candK;
		if (f.spouseK == k) {
			return;
		}
		Cand c = cand(life, f, k);
		f.spouse = c.name;
		f.spouseGap = c.gap;
		f.spouseJob = c.job;
		f.spouseK = k;
	}

	// 그 사람이 모아 온 돈 (마이너스면 빚): 집안 형편과 나이로
	private double candSaved(Cand c, double med, int age) {
		double years = clamp((age - 20) / 10.0, 0, 3);
		if (c.q <= 2 && c.saveK < 0.4) {
			return -med * (0.1 + 0.5 * c.saveK);
		}
		return med * Math.max(0, c.q - 1.8) * 0.25 * years * (0.5 + c.saveK);
	}

	// 소개 문장 (proposal: 청혼 창이면 돈 이야기까지)
	private String candLine(LifeDTO life, CountryStatDTO stat, boolean proposal) {
		FamSeed f = famSeed(life, stat);
		Cand c = cand(life, f, engState(life).candK);
		int sa = Math.max(18, life.getCurrentAge() + c.gap);
		String sex = "M".equals(life.getGender()) ? "여성" : "남성";
		double rate = SPOUSE_RATE.getOrDefault(c.job, 1.0)
				* (0.85 + new java.util.SplittableRandom(life.getLifeId() * 31L + 7 + engState(life).candK * 13L).nextDouble() * 0.3);
		String job = sa < 20 ? "학생" // 스무 살 전이면 학생 (18세 상대가 '집안일'로 나왔음)
				: rate > 0 ? c.job + " (월 소득 약 " + won(life, jobPay(gdpOf(stat), rate, life.getCurrentCountry(), stat) / 12) + ")" : "집안일";
		String t = (proposal ? "청혼한 사람은 " : "만난 사람은 ") + c.name + ", " + sa + "세 " + c.persona + " " + sex + ". 하는 일: " + job
				+ ". 집안 형편: " + SPOON[c.q] + ".";
		if (proposal) {
			double sv = candSaved(c, medIncome(life.getCurrentCountry(), stat), sa);
			double med = medIncome(life.getCurrentCountry(), stat);
			t += Math.abs(sv) < med * 0.05 ? " 모아 둔 돈도 빚도 거의 없다."
					: sv > 0 ? " 모아 둔 돈은 약 " + won(life, sv) + "." : " 학자금이나 할부 같은 빚이 약 " + won(life, -sv) + " 있다.";
		}
		return t;
	}

	private static boolean isMeet(EventResultDTO e) {
		return e.getEventId() != null && e.getLogText() != null && e.getLogText().contains("마음이 통하는 사람을 만났다");
	}

	private static boolean isProposal(EventResultDTO e) {
		return e.getEventId() != null && (e.getEventId().contains("PROPOSAL") || e.getTitle() != null && e.getTitle().contains("청혼"));
	}

	// 올해 '새로운 만남'이 있으면 다음 사람으로 바꾸고 그 사람 소개 줄을 바로 뒤에
	private void loveYear(LifeDTO life, CountryStatDTO stat) {
		if (life.getEvents() == null || "Y".equals(life.getMarriedYn())) {
			return;
		}
		EngState s = engState(life);
		List<EventResultDTO> out = new ArrayList<>();
		for (EventResultDTO e : life.getEvents()) {
			out.add(e);
			if (isMeet(e) && !s.wedFrozen) {
				s.candK = s.meets;
				s.meets++;
				syncSpouse(life, famSeed(life, stat));
				String t = candLine(life, stat, false);
				saveEventLog(life, life.getCurrentAge(), null, null, t);
				out.add(noteEvent(t));
			}
		}
		life.setEvents(out);
	}

	// ================= 집안 형편 (출신 가정 · 부모님 형편 변동, 11/20) =================

	private static final String[] SPOON = { "", "흙수저", "동수저", "은수저", "금수저", "다이아수저" };

	// 태어날 때 형편 (ORIGIN_CLASS, 예전 인생은 지금 형편)
	private static int originOf(LifeDTO life) {
		return life.getOriginClass() == null ? life.getWealthClass() : life.getOriginClass();
	}

	// 0세 기록 첫 줄: 부모님 일 + 집안 형편 (출신 가정이 화면에서 바로 느껴지게)
	private String homeLine(LifeDTO life, CountryStatDTO stat) {
		FamSeed f = famSeed(life, stat);
		String jobs = "";
		if (f.fatherJob != null && f.motherJob != null) {
			jobs = "아버지는 " + f.fatherJob + ", 어머니는 " + f.motherJob + ieotda(f.motherJob) + " ";
		}
		boolean rural = !"Y".equals(life.getIsUrban());
		switch (originOf(life)) {
		case 1:
			return jobs + (rural ? "끼니를 걱정하는 가난한 집이었다. 아이도 걸음마를 떼면 일손을 보태야 했다."
					: "단칸방에 온 식구가 모여 사는 가난한 집이었다. 부모님은 새벽부터 밤까지 일했다.");
		case 2:
			return jobs + "넉넉하지 않은 집이었다. 부모님은 아껴 쓰고 또 아껴 썼다.";
		case 4:
			return jobs + "살림이 넉넉한 집이었다. 집에는 책과 장난감이 여럿 있었다.";
		case 5:
			return jobs + "손꼽히게 부유한 집이었다. 아이에게 필요한 것은 무엇이든 마련되었다.";
		default:
			return jobs + "그 나라에서 흔한, 살림이 그럭저럭 꾸려지는 집이었다.";
		}
	}

	// 받침 있으면 '이었다.', 없으면 '였다.'
	private static String ieotda(String w) {
		char ch = w.charAt(w.length() - 1);
		return ch >= 0xAC00 && ch <= 0xD7A3 && (ch - 0xAC00) % 28 != 0 ? "이었다." : "였다.";
	}

	// 부모님 형편 변동: 부모님이 살아 계시고 내가 40세 전이면 해마다
	//   기울 확률 2.5% · 잘 풀릴 확률 2.5% (그해 경제 위기 사건이 있으면 기울 확률 ×4, 잘 풀릴 확률 0.5%)
	//   가정형편(wealthClass)이 한 칸 바뀌면 진학 확률·대학 학비 도움·결혼 도움·지식·행복 목표·상속이 모두 따라 바뀜
	//   태어날 때 형편은 ORIGIN_CLASS 에 그대로 남음 (신원 기록 '출신 가정')
	private void familyFortune(LifeDTO life) {
		int age = life.getCurrentAge();
		if (!"PLAYING".equals(life.getStatus()) || age < 1 || age >= 40 || "N".equals(life.getParentAlive())) {
			return;
		}
		if (life.getOriginClass() == null) {
			life.setOriginClass(life.getWealthClass());
		}
		boolean crisis = econThisYear(life);
		double down = 0.025 * (crisis ? 4 : 1);
		double up = crisis ? 0.005 : 0.025;
		int wc = life.getWealthClass();
		double r = random.nextDouble();
		int nw = wc;
		if (r < down && wc > 1) {
			nw = wc - 1;
		} else if (r >= down && r < down + up && wc < 5) {
			nw = wc + 1;
		}
		if (nw == wc) {
			return;
		}
		String text;
		boolean kid = age < 20;
		if (nw < wc) {
			if (crisis) {
				text = "경제 위기로 부모님 일이 크게 흔들렸다. 집안 형편이 어려워졌다.";
			} else if (wc >= 4) {
				text = kid ? "부모님 사업이 기울어 큰 집을 팔고 작은 집으로 이사했다." : "부모님 사업이 기울어 고향 집 살림이 어려워졌다.";
			} else {
				text = kid ? "부모님이 하시던 일이 잘 안 풀려 집안 살림이 빠듯해졌다." : "부모님 가게가 문을 닫아 고향 집 살림이 빠듯해졌다.";
			}
			life.setHappiness(life.getHappiness() - (kid ? 4 : 2));
		} else {
			if (wc <= 2) {
				text = kid ? "부모님이 차린 가게가 자리를 잡아 살림이 한결 펴졌다." : "부모님 장사가 잘되어 고향 집 살림이 펴졌다.";
			} else {
				text = kid ? "부모님 사업이 잘되어 집안 형편이 넉넉해졌다." : "부모님 사업이 크게 잘되어 집안 형편이 넉넉해졌다.";
			}
			life.setHappiness(life.getHappiness() + (kid ? 3 : 1));
		}
		text = text + " (" + SPOON[wc] + " → " + SPOON[nw] + ")";
		life.setWealthClass(nw);
		saveEventLog(life, age, null, null, text);
		List<EventResultDTO> list = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
		list.add(noteEvent(text));
		life.setEvents(list);
	}

	// 올해 경제 위기(ECON 유형) 역사 사건이 있었는지
	private boolean econThisYear(LifeDTO life) {
		if (life.getEvents() == null) {
			return false;
		}
		for (EventResultDTO e : life.getEvents()) {
			if ("HIST".equals(e.getEventType()) && e.getEventId() != null) {
				HistEffectDTO h = engineDao.selectHistEffect(e.getEventId());
				if (h != null && "ECON".equals(h.getTypeCode())) {
					return true;
				}
			}
		}
		return false;
	}

	// 큰 사건 경고등 기준 (화면 빨간 줄 + '큰 사건', 새로고침 때는 engine-mapper selectEventLogs 가 같은 기준)
	//   역사 사건(HIST) 가운데 강도(GAME_EVENT.INTENSITY) 2 이상이거나, 목숨이 걸린 전쟁·재난 유형
	//   전쟁이 끝났다·총성이 멎었다 같은 끝난 소식은 빼고, 엔진 전쟁 소식(warYear)은 시작·이어짐만
	private static final java.util.regex.Pattern ENDED_NEWS = java.util.regex.Pattern.compile("끝났|멎었");

	private boolean bigHist(EventResultDTO e) {
		if (!"HIST".equals(e.getEventType()) || e.getEventId() == null || e.getLogText() == null
				|| ENDED_NEWS.matcher(e.getLogText()).find()) {
			return false;
		}
		HistEffectDTO h = engineDao.selectHistEffect(e.getEventId());
		if (h == null) {
			return false;
		}
		int k = h.getIntensity() == null ? 1 : h.getIntensity();
		return k >= 2 || "WAR".equals(h.getTypeCode()) || "DISASTER".equals(h.getTypeCode());
	}

	// 역사 사건의 실직·학업 중단 (C10 → C06, 명세서 C06 3·8번)
	// 확률 = 유형별 보정값(HIST_EVENT_TYPE) × 사건 강도: 예) 전쟁 강도 2면 실직 10%, 학업 중단 16%
	// (피란 이민은 갈 나라 데이터가 없어서 아직 반영 안 함)
	private void applyHistEffect(LifeDTO life, EventResultDTO e) {
		if (!"HIST".equals(e.getEventType())) {
			return;
		}
		HistEffectDTO h = engineDao.selectHistEffect(e.getEventId());
		if (h == null) {
			return;
		}
		int k = h.getIntensity() == null ? 1 : h.getIntensity();
		String why = h.getTypeName() == null ? "역사적 사건" : h.getTypeName();
		int age = life.getCurrentAge();
		histForPerson(life, e, h, k, age);
		if ("EMPLOYED".equals(life.getJobStatus()) && h.getJobLossAdd() != null
				&& random.nextDouble() < h.getJobLossAdd() * k) {
			life.setJobStatus("UNEMPLOYED");
			life.setJobYears(0);
			saveEventLog(life, age, null, null, why + " 때문에 일자리를 잃었다.");
		}
		if ("ENROLLED".equals(life.getSchoolStatus()) && h.getEduStopAdd() != null
				&& random.nextDouble() < h.getEduStopAdd() * k) {
			life.setSchoolStatus("DROPPED");
			life.setDropReason(why + " 때문에 학교를 그만둠");
			saveEventLog(life, age, null, null, why + " 때문에 학교를 그만두었다.");
		}
	}

	// 같은 역사 사건도 나이·성별·형편에 따라 다르게 (11/20 동현님 요청: 코로나·대기근·IMF·전쟁)
	//   사망 확률 가산(윤아님 유형값 × 강도)에 배율을 곱하고, 사건별로 몇 가지를 더함
	//   - 대기근: 5세 미만 ×3, 65세 이상 ×2 / 가난한 집 ×1.6, 넉넉한 집 ×0.3
	//   - 코로나: 50세 미만 거의 0, 65세 이상 ×1.5, 80세 이상 ×4 / 학생은 온라인 수업(지식 −2, 관계 −2)
	//   - 전쟁: 18~45세 남자 ×1.6, 넉넉한 집 ×0.8
	//   - 경제 위기(IMF 등): 40세 이상 직장인은 명예퇴직 8% × 강도, 가게(자영업)는 문 닫을 확률 10% × 강도
	private void histForPerson(LifeDTO life, EventResultDTO e, HistEffectDTO h, int k, int age) {
		String id = e.getEventId() == null ? "" : e.getEventId();
		String type = h.getTypeCode() == null ? "" : h.getTypeCode();
		int wc = life.getWealthClass();
		boolean famine = id.contains("FAMINE") || id.contains("BENGAL") || id.contains("HUNGER");
		boolean covid = id.contains("COVID");
		double m = 1;
		if (famine) {
			m = (age < 5 ? 3 : age >= 65 ? 2 : 1) * (wc <= 2 ? 1.6 : wc == 3 ? 1 : 0.3);
		} else if (covid) {
			m = age < 50 ? 0.05 : age < 65 ? 0.4 : age < 80 ? 1.5 : 4;
		} else if ("WAR".equals(type)) {
			m = ("M".equals(life.getGender()) && age >= 18 && age <= 45 ? 1.6 : 1) * (wc >= 4 ? 0.8 : 1);
			if (soldier(life)) {
				m = m * 2; // 직업군인·장교는 전선에 나감
				saveEventLog(life, age, null, null, "군인으로서 전선에 배치되었다.");
			}
		} else if ("DISASTER".equals(type)) {
			m = wc <= 2 ? 1.3 : wc >= 4 ? 0.7 : 1;
		}
		if (!e.isChoice() && e.getDeathAdd() > 0) {
			e.setDeathAdd(e.getDeathAdd() * m);
		}
		if (covid && "ENROLLED".equals(life.getSchoolStatus())) {
			life.setKnowledge(life.getKnowledge() - 2);
			life.setRelation(life.getRelation() - 2);
			saveEventLog(life, age, null, null, "학교 수업이 온라인으로 바뀌어 친구들을 화면으로만 만났다.");
		}
		if ("ECON".equals(type) && "EMPLOYED".equals(life.getJobStatus())) {
			boolean shop = life.getJobId() != null && life.getJobId().contains("BIZ");
			double p = shop ? 0.10 * k : age >= 40 ? 0.08 * k : 0;
			if (p > 0 && random.nextDouble() < p) {
				life.setJobStatus("UNEMPLOYED");
				life.setJobYears(0);
				saveEventLog(life, age, null, null, shop ? "손님이 끊겨 결국 가게 문을 닫았다."
						: "회사가 구조조정을 하며 명예퇴직을 권했다. 오래 다닌 직장을 떠났다.");
			}
		}
	}

	private static boolean soldier(LifeDTO l) {
		return "EMPLOYED".equals(l.getJobStatus()) && ("JOB_SOLDIER".equals(l.getJobId()) || "JOB_OFFICER".equals(l.getJobId()));
	}

	// C13 학업 능력 효과값 = 타고난 재능에 더하는 값 (표준편차 단위)
	//   TRAIT_LEVEL_EFFECT에 값이 있으면 그 값, 없으면 (레벨-1)×0.25 (Lv1 0 ~ Lv5 +1.0), 특성 기록이 없으면 0
	// 한 인생 안에서는 바뀌지 않아서 서버 메모리에 보관 (매년 DB를 읽지 않게)
	private final Map<Integer, Double> studyCache = new ConcurrentHashMap<>();

	private double studyEffect(LifeDTO life) {
		Double v = studyCache.get(life.getLifeId());
		if (v == null) {
			if (studyCache.size() > 20000) {
				studyCache.clear();
			}
			v = engineDao.selectStudyEffect(life.getLifeId());
			if (v == null) {
				v = 0.0;
			}
			studyCache.put(life.getLifeId(), v);
		}
		return v;
	}

	// 이벤트 결과 하나를 인생에 반영 (일반 이벤트, 고른 선택지 공통)
	// age: 이벤트가 일어난 나이, choiceId: 선택지 번호 (일반 이벤트는 null)
	private void applyEvent(LifeDTO life, EventResultDTO e, int age, Integer choiceId) {
		// 5세 이하 아이가 겪은 역사 사건(전쟁·기근 …): 무슨 일인지 다 알지 못해 행복은 1/3만,
		//   피난·굶주림으로 몸은 상하니 건강은 70% (11/23 동현님: 아기 행복·건강이 너무 크게 떨어짐)
		if ("HIST".equals(e.getEventType()) && age <= 5) {
			e.setEffHappiness((int) Math.round(e.getEffHappiness() * 0.3));
			e.setEffHealth((int) Math.round(e.getEffHealth() * 0.7));
			e.setEffKnowledge(0);
		}
		// 1. 능력치 변화 (C05)
		life.setHealth(life.getHealth() + e.getEffHealth());
		life.setHappiness(life.getHappiness() + e.getEffHappiness());
		life.setKnowledge(life.getKnowledge() + e.getEffKnowledge());
		life.setRelation(life.getRelation() + e.getEffRelation());

		// 2. 소득 배율 변화 (C06): 일하는 중일 때만, 0.1 = +10%
		if (e.getEffIncomeRate() != 0 && "EMPLOYED".equals(life.getJobStatus()) && life.getIncomeRate() != null) {
			life.setIncomeRate(life.getIncomeRate() * (1 + e.getEffIncomeRate()));
		}

		// 3. 재산 변화율 (C06 재산 반영): 재산 × 비율 (0.3 = +30%, -0.5 = -50%)
		//    재산이 0 이하(빚)면 변화 없음 → 빚이 있을 때 손실로 빚이 줄어드는 일 방지
		long before = life.getAssets();
		// 출산은 재산 비율(−5%) 대신 실제 출산 비용으로 (11/23 동현님: 재산이 많으면 1,100만 원이 한 번에 나갔음)
		boolean birth = "CHILD".equals(e.getResultAction());
		EngState es0 = engState(life);
		es0.estateNote = null;
		es0.estateAmt = 0;
		if (e.getEffWealthRate() != 0 && life.getAssets() > 0 && !birth) {
			double rate = e.getEffWealthRate();
			if (rate < 0 && "HIST".equals(e.getEventType())) {
				rate = Math.max(-0.7, rate * crisisK(life)); // 큰 재난·위기는 사람마다 다르게 (11/23)
			}
			long d = Math.round(life.getAssets() * rate);
			if (d < 0 && !"HIST".equals(e.getEventType())) {
				// 병·사고 같은 개인 사건의 손실은 재산 비율만 쓰면 부자는 터무니없이 큼 (300번: 간염 치료비 약 9,400만 원,
				//   11/23 동현님: 결핵 진단만으로 5천만 원) → 그 나라 중위 소득 기준으로 무거운 정도에 따라
				//   큰 병·큰 사고 1배 · 그 밖의 병(진단·앓음) 0.2배 · 나머지 개인 사건 0.5배 (역사 사건·재난은 위 crisisK 그대로)
				String lt = e.getLogText() == null ? "" : e.getLogText();
				double capK = SERIOUS_W.matcher(lt).find() ? 1.0 : ILL_W.matcher(lt).find() ? 0.2 : 0.5;
				d = Math.max(d, -Math.round(medIncome(life.getCurrentCountry(), yearStat(life)) * capK));
			}
			life.setAssets(life.getAssets() + d);
			String t = e.getLogText() == null ? "" : e.getLogText();
			book(life, t.contains("집") || t.contains("땅") ? (d > 0 ? "집값 상승" : "집값 하락")
					: d > 0 ? "뜻밖에 생긴 돈" : "사건으로 잃은 돈", d);
		}

		// 4. 상태 변경 (결혼·자녀·부모 사망·실직·조기 은퇴·이민)
		if ("PRISON".equals(e.getResultAction())) {
			String closed = startPrison(life, prisonYearsOf(e)); // 윤아님 '위험한 제안' 붙잡힘 (11/23)
			if (closed != null && e.getLogText() != null) {
				e.setLogText(e.getLogText() + " " + closed);
			}
		}
		applyAction(life, e.getResultAction(), e.getMigrateTo(), age);
		if (birth) {
			birthCost(life, age);
		}

		// 돈이 오갔으면 기록 끝에 한국 체감 금액 (원작처럼 "(−40만 원)")
		//   부모님이 남긴 재산은 따로 뒤에 붙임 (장례비와 섞이지 않게, 별세 문장 뒤에 오게)
		long moved = life.getAssets() - before - es0.estateAmt;
		// 한국 체감 1만 원도 안 되는 움직임은 금액을 적지 않음 (코로나 −2,900원 같은 어색한 숫자)
		boolean bigMove = Math.abs(moved) * krwPerUsd(yearStat(life)) >= 10000;
		if (es0.estateNote != null && e.getLogText() != null) {
			// 부모님 별세: 금액이 문장 가운데 끼지 않게 장례비는 말로 ('떠나보냈다. (−50만 원) 부모님이 남긴…'이 나왔음)
			e.setLogText(e.getLogText() + (bigMove && moved < 0 ? " 장례를 치르는 데 " + won(life, -moved) + "이 들었다." : ""));
		} else if (bigMove && e.getLogText() != null) {
			e.setLogText(e.getLogText() + " (" + wonSigned(life, moved) + ")");
		}
		if (es0.estateNote != null && e.getLogText() != null) {
			e.setLogText(e.getLogText() + " " + es0.estateNote);
			es0.estateNote = null;
			es0.estateAmt = 0;
		}

		// 5. 경험 기록: 윤아님 pickEvent의 '한 번만'·'필요 경험'·'제외 경험' 판정에 쓰임
		//    (선택지는 이벤트ID_순서, 예: LE_MIGRATE_WORK_1)
		String expId = e.getExpId() != null ? e.getExpId() : e.getEventId();
		ExperienceDTO exp = new ExperienceDTO();
		exp.setLifeId(life.getLifeId());
		exp.setExpId(expId);
		exp.setAge(age);
		exp.setYearNo(life.getBirthYear() + age);
		engineDao.insertExperience(exp);

		// 6. 인생 기록 문장 저장 (C03 기록 화면, C11 AI 서술 대상 표시)
		saveEventLog(life, age, e.getEventId(), choiceId, e.getLogText());
	}

	private static final java.util.regex.Pattern SERIOUS_W = java.util.regex.Pattern.compile("암|심장|뇌졸중|수술|중상|크게 다쳤|치매|큰 병");
	private static final java.util.regex.Pattern ILL_W = java.util.regex.Pattern.compile("진단|병원을 찾|앓|걸렸|병에");

	// 역사 사건(전쟁·재난·경제 위기)으로 잃는 재산 배율 (11/23 동현님: 같은 위기라도 사람마다 피해가 달라야)
	//   운 0.5~1.5배 × 형편 (가난할수록 1.3배, 넉넉하면 0.8배) × 장사·실직 1.3배 × 주식에 넣었으면 1.3배 · 예금이면 0.85배
	private double crisisK(LifeDTO life) {
		double k = 0.5 + random.nextDouble();
		int q = life.getWealthClass();
		k *= q <= 2 ? 1.3 : q >= 4 ? 0.8 : 1;
		if ("JOB_OWNER".equals(life.getJobId()) || "UNEMPLOYED".equals(life.getJobStatus())) {
			k *= 1.3;
		}
		String inv = life.getInvestType();
		if ("STOCK".equals(inv) || "TRADE".equals(inv) || "WORLD".equals(inv)) {
			k *= 1.3;
		} else if ("DEPOSIT".equals(inv) || "CASH".equals(inv)) {
			k *= 0.85;
		}
		return k;
	}

	// 출산 비용: 병원비 중위 소득 3% + 한국 2010년~ 산후조리원 10% (2023년 평균 약 286만 원, 보건복지부)
	//   내 벌이에 따라 0.6~1.8배, 벌이도 재산도 없으면 안 냄 (아이 키우는 비용은 해마다 따로)
	private void birthCost(LifeDTO life, int age) {
		if (age < 18 || life.getIncome() <= 0 && life.getAssets() <= 0) {
			return;
		}
		CountryStatDTO st = yearStat(life);
		double med = medIncome(life.getCurrentCountry(), st);
		boolean pp = "KOR".equals(life.getCurrentCountry()) && life.getBirthYear() + age >= 2010;
		double k = clamp(Math.pow(Math.max(life.getIncome(), 1) / med, 0.3), 0.6, 1.8);
		long pay = Math.round(med * (0.03 + (pp ? 0.1 : 0)) * k);
		life.setAssets(life.getAssets() - pay);
		book(life, pp ? "출산·산후조리 비용" : "출산 비용", -pay);
	}

	// LIFE_EVENT_LOG 한 줄 저장
	private void saveEventLog(LifeDTO life, int age, String eventId, Integer choiceId, String text) {
		EventLogDTO log = new EventLogDTO();
		log.setLifeId(life.getLifeId());
		log.setAge(age);
		log.setYearNo(life.getBirthYear() + age);
		log.setEventId(eventId);
		log.setChoiceId(choiceId);
		log.setLogText(text);
		engineDao.insertEventLog(log);
	}

	// ================= C09 선택 이벤트: 선택지 고르기 =================
	// 선택 이벤트는 작년(나이+1 하기 전)에 일어났으므로 그 나이로 기록
	@Transactional
	public LifeDTO chooseEvent(LifeDTO life, int choiceId) {
		return chooseEvent(life, choiceId, null);
	}

	// name: 반려동물 이름처럼 창에서 적은 한 줄 (원작 petNameAsk, 없으면 null)
	public LifeDTO chooseEvent(LifeDTO life, int choiceId, String name) {
		if (choiceId < 0) {
			return chooseEngine(life, choiceId, name); // 엔진 선택지 (C06-4, 음수 번호)
		}
		if (!life.getStatus().equals("PLAYING") || life.getPendingEventId() == null) {
			return null; // 고를 선택이 없음
		}
		if (engineDao.countPendingChosen(life) > 0) {
			return null; // 이미 '떠난다'를 골랐고 목적지를 기다리는 중 → /api/engine/destination 으로
		}
		// 같은 선택을 두 번 반영하지 않게 잠금 (버튼 연타 시 두 번째는 이미 골라져서 0줄 → 무시)
		if (engineDao.lockPending(life) == 0) {
			return null;
		}
		int age = life.getCurrentAge() - 1;

		EventResultDTO r = eventService.applyChoice(life, choiceId, age);
		if (r == null || !life.getPendingEventId().equals(r.getEventId())) {
			return null; // 없는 선택지이거나 지금 이벤트의 선택지가 아님
		}
		// 선택 결과의 사망 위험 (윤아님 applyFail: 참전 중상 +0.05 '전쟁', 시위 진압 +0.02)
		//   선택은 한 해가 끝난 뒤에 고르니, 그 위험은 다음 해 사망 판정에 더함 (예전엔 버려져서 참전해도 죽지 않았음)
		if (r.getDeathAdd() > 0) {
			EngState es = engState(life);
			es.carryDeath = es.carryDeath + r.getDeathAdd();
			es.carryCause = r.getDeathCause();
		}
		// 전쟁 한가운데서 '남는다'(떠나지도 참전하지도 않음)를 고르면, 선택 없는 전쟁과 같은 위험 (강도 × 1%, 원인 전쟁)
		//   예전엔 남아도 위험이 0이라, 선택 없는 전쟁(+3%)보다 오히려 안전했음
		//   r.getResultAction() 은 참전(성공) 뒤엔 null 이 되므로, 고른 선택지 원래 값으로 판단
		EventChoiceDTO picked = eventDao.selectChoice(choiceId);
		if (picked != null && picked.getResultAction() == null && r.getDeathAdd() <= 0) {
			HistEffectDTO he = engineDao.selectHistEffect(r.getEventId());
			if (he != null && "WAR".equals(he.getTypeCode())) {
				int k = he.getIntensity() == null ? 1 : he.getIntensity();
				double m = ("M".equals(life.getGender()) && age >= 18 && age <= 45 ? 1.6 : 1) * (life.getWealthClass() >= 4 ? 0.8 : 1)
						* (soldier(life) ? 2 : 1);
				EngState es = engState(life);
				es.carryDeath = es.carryDeath + 0.01 * k * m;
				es.carryCause = "전쟁";
			}
		}

		// 큰돈이 드는 선택지(집·땅·가게·투자): 실제 비용·대출·이자로 계산 (재산 비율 대신)
		//   투자 결과(윤아님 applyChoice의 성공·실패)는 넣은 돈 × 변화율로 반영
		ChoiceCostDTO cost = costOf(life, eventDao.selectChoice(choiceId), engineDao.selectPendingText(life));
		long moneyChange = 0;
		boolean openBiz = false;
		if (cost != null && "BUSINESS".equals(cost.getKind())) {
			if (!cost.isOk()) {
				return null;
			}
			// 가게는 고른 해에 바로 성패가 갈리지 않음 (원작 bizStart): 문을 열어 자영업 사장이 되고,
			//   잘될지 접을지는 몇 해에 걸쳐 jobStep(bizYear)에서 정해짐. 윤아님 결과의 성공·실패(소득·재산 변화)는 쓰지 않음
			//   드는 돈 가운데 40%는 인테리어·권리금처럼 못 돌려받는 돈 (나머지는 가게 살림으로 남음, 원작 사업체 가치)
			openBiz = true;
			moneyChange = -Math.round(cost.getCost() * 0.4) - cost.getInterest();
			r.setEffWealthRate(0);
			r.setEffIncomeRate(0);
			// 윤아님 결과의 성공·실패 능력치도 쓰지 않음: 문을 연 설렘만 (원작 bizStart eff h +4)
			r.setEffHealth(0);
			r.setEffHappiness(4);
			r.setEffKnowledge(0);
			r.setEffRelation(0);
			r.setResultAction(null);
			String note = cost.getLabel() + " " + won(life, cost.getCost()) + "을 들여 가게를 열었다";
			if (cost.getLoan() > 0) {
				note = note + " (대출 " + won(life, cost.getLoan()) + " · 이자 약 " + won(life, cost.getInterest()) + ")";
			}
			r.setLogText(note + ". 잘될지는 몇 해 지나 봐야 안다.");
			cost = null; // 아래 일반 비용 처리는 건너뜀 (돈은 moneyChange 로)
		}
		if (cost != null) {
			if (!cost.isOk()) {
				return null; // 돈이 모자람 (컨트롤러가 먼저 막지만 한 번 더)
			}
			double w = r.getEffWealthRate();
			if ("HOUSE".equals(cost.getKind())) {
				w = Math.min(w, 0.3); // 집값 오름은 많아도 30%
			}
			moneyChange = Math.round(cost.getCost() * w) - cost.getInterest();
			r.setEffWealthRate(0); // 재산 비율 변화는 쓰지 않음
			String note = " (" + cost.getLabel() + " " + won(life, cost.getCost());
			if (cost.getLoan() > 0) {
				note = note + ", 대출 " + won(life, cost.getLoan()) + " · 이자 약 " + won(life, cost.getInterest());
			}
			if (Math.round(cost.getCost() * w) != 0) {
				note = note + (w > 0 ? ", 불어난 돈 +" : ", 잃은 돈 −") + won(life, cost.getCost() * w);
			}
			r.setLogText(r.getLogText() + note + ")");
		}

		long startAssets = life.getAssets();
		life.setMoneyFlow(new ArrayList<>());
		applyEvent(life, r, age, choiceId);
		life.setAssets(life.getAssets() + moneyChange);
		if (cost != null) {
			book(life, cost.getLabel() + (moneyChange >= 0 ? " (불어난 돈)" : " (잃은 돈·이자)"), moneyChange);
		} else if (openBiz) {
			book(life, "가게 차리는 비용", moneyChange);
		}
		closeBook(life, startAssets);
		fixStats(life);
		life.setChoiceCosts(null);
		List<EventResultDTO> extra = new ArrayList<>();
		if (openBiz) {
			startBusiness(life, age, extra); // 가게를 열면 자영업 사장 (성패는 몇 해 뒤에)
		}

		// C10 '떠난다'인데 갈 나라가 안 정해진 선택지 → 목적지 고르기 창 (선택 이벤트는 목적지를 고를 때까지 계속 기다림)
		//   기다리는 동안 한 해 진행은 409로 막힘 (PENDING_EVENT_ID 그대로)
		if (r.isNeedDestination() && r.getDestinations() != null && !r.getDestinations().isEmpty()) {
			life.setNeedDestination(true);
			life.setDestinations(r.getDestinations());
			List<EventResultDTO> list = new ArrayList<>();
			list.add(r);
			life.setEvents(list);
			engineDao.updateLifeYear(life);
			fillRegion(life);
			return life;
		}
		life.setPendingEventId(null);

		// 고른 결과로 실직·조기 은퇴 등이 생기면 나이대·직업 제안을 바로 갱신 (다음 해까지 기다리지 않게)
		String newStage = stageOf(life);
		if (newStage != null && !newStage.equals(life.getStageCode())) {
			life.setStageCode(newStage);
			life.setFocusCode(null);
			life.setNeedFocus(true);
			life.setFocusOptions(focusOptions(newStage, life));
		}
		showOffers(life, yearStat(life));

		List<EventResultDTO> list = new ArrayList<>();
		list.add(r);
		list.addAll(extra);
		life.setEvents(list); // [화면용] 고른 결과
		spouseNote(life, age); // 청혼을 받아들이는 등 고른 결과로 결혼했으면 결혼 상대 소개

		engineDao.updateLifeYear(life);
		fillRegion(life);
		return life;
	}

	// 아이가 받은 '떠난다/남는다' 선택 (윤아님 이민·피란 이벤트, 선택지에 MIGRATE 가 있는 것): 가족이 대신 정함
	//   반반으로 고르고, 떠나면 갈 나라도 후보 가운데 하나로 (선택 창·목적지 창을 띄우지 않음)
	private void kidFamilyDecides(LifeDTO life, int lived) {
		if (lived >= 16 || !life.isNeedChoice() || life.getPendingEvent() == null || life.getPendingEventId() == null
				|| life.getPendingEventId().startsWith("ENG_") || life.getPendingEvent().getChoices() == null) {
			return;
		}
		EventChoiceDTO go = null, stay = null;
		for (EventChoiceDTO c : life.getPendingEvent().getChoices()) {
			if ("MIGRATE".equals(c.getResultAction())) {
				go = c;
			} else if (stay == null) {
				stay = c;
			}
		}
		if (go == null || stay == null || go.getChoiceId() == null || stay.getChoiceId() == null) {
			return;
		}
		List<EventResultDTO> before = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
		List<MoneyLineDTO> flow = life.getMoneyFlow() == null ? new ArrayList<>() : new ArrayList<>(life.getMoneyFlow());
		String note = lived < 7 ? "아직 어린아이라, 떠날지 남을지는 가족이 정했다." : "떠날지 남을지는 가족이 정했다. 나는 따라갈 수밖에 없었다.";
		saveEventLog(life, lived, null, null, note);
		EventChoiceDTO pick = random.nextDouble() < 0.5 ? go : stay;
		if (chooseEvent(life, pick.getChoiceId(), null) == null) {
			life.setEvents(before);
			life.setMoneyFlow(flow);
			return; // 못 고르면 예전처럼 창을 띄움
		}
		List<EventResultDTO> added = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
		if (life.isNeedDestination() && life.getDestinations() != null && !life.getDestinations().isEmpty()) {
			String cc = life.getDestinations().get(random.nextInt(life.getDestinations().size())).getCountryCode();
			if (chooseDestination(life, cc) != null && life.getEvents() != null) {
				added.addAll(life.getEvents());
			}
			life.setNeedDestination(false);
			life.setDestinations(null);
		}
		before.add(noteEvent(note));
		before.addAll(added);
		life.setEvents(before);
		if (life.getMoneyFlow() != null) {
			flow.addAll(life.getMoneyFlow());
		}
		life.setMoneyFlow(flow);
		life.setNeedChoice(false);
		life.setPendingEvent(null);
		life.setChoiceCosts(null);
	}

	// ================= C10 이민 목적지 고르기 =================
	// '떠난다'를 고른 뒤 후보 나라 중 하나를 고르면: 구역 효과(윤아님 applyDestination) + 사는 나라 변경
	@Transactional
	public LifeDTO chooseDestination(LifeDTO life, String countryCode) {
		if (!life.getStatus().equals("PLAYING") || life.getPendingEventId() == null || countryCode == null) {
			return null;
		}
		if (engineDao.countPendingChosen(life) == 0) {
			return null; // 아직 '떠난다'를 안 골랐음
		}
		if (engineDao.lockPending(life) == 0) {
			return null; // 연타: 이미 처리됨
		}
		int age = life.getCurrentAge() - 1;
		EventResultDTO r = eventService.applyDestination(life, countryCode, age, life.getPendingEventId());
		if (r == null) {
			return null; // 후보가 아닌 나라
		}
		applyEvent(life, r, age, null); // applyAction MIGRATE → 사는 나라 변경, 경험 MIGRATE_나라, 기록
		fixStats(life);
		life.setPendingEventId(null);

		// 나라가 바뀌면 직업 제안도 새 나라 기준으로
		String newStage = stageOf(life);
		if (newStage != null && !newStage.equals(life.getStageCode())) {
			life.setStageCode(newStage);
			life.setFocusCode(null);
			life.setNeedFocus(true);
			life.setFocusOptions(focusOptions(newStage, life));
		}
		showOffers(life, yearStat(life));

		List<EventResultDTO> list = new ArrayList<>();
		list.add(r);
		life.setEvents(list);

		engineDao.updateLifeYear(life);
		fillRegion(life);
		return life;
	}

	// ================= C09 큰돈 드는 선택지 (집·땅·가게·투자) =================
	// 원작: 집값 = 중위 소득 × 5 (도시 ×1.4, 시골 ×0.7), 대출은 집값의 80%까지 (계약금 20%는 내 돈)
	//       가게 = 중위 소득 × 1.5, 주식 = 모아 둔 돈의 일부 (빚으로는 못 함)
	// 내 돈(재산)이 모자라면: 대출이 되는 것은 대출 + 이자, 안 되면 고를 수 없음
	private static final java.util.regex.Pattern NOT_DO = java.util.regex.Pattern.compile("않|안 |미룬|미루|포기|거절|그대로|계속|다음에|그만두");
	private static final java.util.regex.Pattern HOUSE_W = java.util.regex.Pattern.compile("집|땅|아파트|부동산|주택|토지|논밭");
	private static final java.util.regex.Pattern BIZ_W = java.util.regex.Pattern.compile("가게|창업|장사|사업|식당|점포");
	private static final java.util.regex.Pattern STOCK_W = java.util.regex.Pattern.compile("주식|코인|펀드|증권|투자");

	private ChoiceCostDTO costOf(LifeDTO life, EventChoiceDTO c, String eventText) {
		if (c == null || c.getChoiceText() == null) {
			return null;
		}
		String choice = c.getChoiceText();
		String all = choice + " " + (eventText == null ? "" : eventText);
		boolean invest = "INVEST".equals(c.getResultAction());
		Number wn = c.getEffWealthRate();
		double w = wn == null ? 0 : wn.doubleValue();
		if (NOT_DO.matcher(choice).find() || (!invest && w == 0)) {
			return null; // 안 하는 쪽 선택지, 돈과 상관없는 선택지
		}
		// 사는 쪽 선택지만 비용 계산: 투자 이벤트(LE_INVEST_…)나 INVEST 선택지
		//   피란·이민·역사 사건의 '재산이 줄어드는' 선택지는 사는 게 아니라 잃는 것이라 비용이 아님
		//   (예전엔 문구에 '집·땅'이 있으면 '남는다'까지 집값을 요구해서, 돈이 없으면 두 선택지가 다 막혀 진행이 멈췄음)
		String eventId = c.getEventId();
		if ("MIGRATE".equals(c.getResultAction()) || (!invest && (eventId == null || !eventId.startsWith("LE_INVEST")))) {
			return null;
		}
		String kind = HOUSE_W.matcher(all).find() ? "HOUSE" : BIZ_W.matcher(all).find() ? "BUSINESS"
				: STOCK_W.matcher(all).find() || invest ? "STOCK" : null;
		if (kind == null || (!invest && w > 0 && !"HOUSE".equals(kind))) {
			return null; // 돈이 들어오기만 하는 선택지
		}
		CountryStatDTO stat = yearStat(life);
		double med = medIncome(life.getCurrentCountry(), stat);
		double cash = Math.max(0, life.getAssets());
		double cost;
		double down; // 내 돈으로 내야 하는 몫
		double rate; // 대출 이자 (갚는 동안 모두 합친 비율)
		String label;
		if ("HOUSE".equals(kind)) {
			cost = med * 5 * ("Y".equals(life.getIsUrban()) ? 1.4 : 0.7);
			down = 0.2;
			rate = 0.25;
			label = "집·땅값";
		} else if ("BUSINESS".equals(kind)) {
			cost = med * 1.5;
			down = 0.4;
			rate = 0.15;
			label = "차리는 비용";
		} else {
			cost = clamp(cash * 0.3, med * 0.2, med * 2);
			down = 1;
			rate = 0;
			label = "넣은 돈";
		}
		ChoiceCostDTO d = new ChoiceCostDTO();
		d.setChoiceId(c.getChoiceId());
		d.setKind(kind);
		d.setLabel(label);
		d.setCost(Math.round(cost));
		d.setMine(Math.round(cash));
		// 대출은 신용 한도 안에서만 (원작 creditLimit): 이미 빚이 많거나 벌이가 없으면 못 빌림
		//   집은 집을 담보로 빌리지만, 갚을 벌이가 있어야 함 (빚 합계가 벌이의 5배까지)
		double room = "HOUSE".equals(kind)
				? Math.max(0, Math.max(life.getIncome(), spouseIncomeOf(life)) * 5 - Math.max(0, -life.getAssets()))
				: creditRoom(life, stat, med);
		if (cash >= cost) {
			d.setOk(true);
		} else if (down < 1 && cash >= cost * down && age18(life) && cost - cash <= room) {
			d.setOk(true);
			d.setLoan(Math.round(cost - cash));
			d.setInterest(Math.round((cost - cash) * rate));
		} else {
			d.setOk(false);
			d.setNeed(Math.round(cost * down));
		}
		return d;
	}

	private boolean age18(LifeDTO life) {
		return life.getCurrentAge() >= 18;
	}

	private List<ChoiceCostDTO> costsOf(LifeDTO life, List<EventChoiceDTO> choices, String eventText) {
		List<ChoiceCostDTO> list = new ArrayList<>();
		if (choices == null) {
			return list;
		}
		for (EventChoiceDTO c : choices) {
			ChoiceCostDTO d = costOf(life, c, eventText);
			if (d != null) {
				list.add(d);
			}
		}
		return list;
	}

	// 고를 수 없는 선택지면 이유 (컨트롤러 400 문구), 고를 수 있으면 null
	public String choiceBlocked(LifeDTO life, int choiceId) {
		if (life.getPendingEventId() == null) {
			return null;
		}
		ChoiceCostDTO d = costOf(life, eventDao.selectChoice(choiceId), engineDao.selectPendingText(life));
		if (d == null || d.isOk()) {
			return null;
		}
		return "돈이 모자라요. " + d.getLabel() + " " + won(life, d.getCost()) + " 가운데 내 돈으로 " + won(life, d.getNeed())
				+ "이 필요한데, 지금 가진 돈은 " + won(life, d.getMine()) + "이에요. (한국 체감)";
	}

	// 이어하기·새로고침 때 선택 창 다시 띄우기
	private void restoreChoice(LifeDTO life) {
		if (life == null) {
			return;
		}
		if (life.getPendingEventId() == null) {
			restoreEngineChoice(life); // 엔진 선택지 (서버 메모리에 기다리는 게 있으면)
			return;
		}
		// '떠난다'는 이미 골랐고 목적지만 남았으면 → 목적지 창 (후보는 다시 뽑음, 고를 때는 전체 후보 안에서 확인)
		if (engineDao.countPendingChosen(life) > 0) {
			life.setNeedDestination(true);
			life.setDestinations(eventService.pickDestinations(life.getCurrentCountry(),
					life.getBirthYear() + life.getCurrentAge() - 1));
			return;
		}
		EventResultDTO e = new EventResultDTO();
		e.setEventId(life.getPendingEventId());
		e.setChoice(true);
		e.setLogText(engineDao.selectPendingText(life));
		e.setChoices(eventDao.selectChoices(life.getPendingEventId())); // 윤아님 DAO (읽기만)
		life.setNeedChoice(true);
		life.setPendingEvent(e);
		life.setChoiceCosts(costsOf(life, e.getChoices(), e.getLogText()));
		hideUnaffordable(life);
	}

	// 장례비 (어른이고 벌이나 재산이 있을 때만, 중위 소득 × 비율 × 내 벌이 보정)
	// 부모님 중 떠나신 분 (OWNS PAR-M-내나이 / PAR-F-내나이): 그때 내 나이, 아직 살아 계시면 -1
	private static int parAt(java.util.LinkedHashSet<String> owns, String who) {
		for (String x : owns) {
			if (x.startsWith("PAR-" + who + "-")) {
				try {
					return Integer.parseInt(x.substring(6));
				} catch (NumberFormatException e) {
					return 0;
				}
			}
		}
		return -1;
	}

	private void parMark(LifeDTO life, String who, int age) {
		java.util.LinkedHashSet<String> owns = ownSet(life);
		if (parAt(owns, who) < 0) {
			owns.add("PAR-" + who + "-" + age);
			life.setOwns(String.join(",", owns));
		}
	}

	// 부모님 별세 (11/23 윤아님과 합의): famSeed 에 정해 둔 수명대로 그해 떠나실 분을 엔진이 정함
	//   어머니·아버지 따로, 두 분 수명이 같은 해면 '같은 해 다른 날'로 한 번에 (사고로 함께 떠나시는 건 윤아님 이벤트)
	//   문장·효과는 윤아님 EventService.parentDeathEvent (LE_PARENT_DEATH_MOTHER / FATHER / SAMEYEAR) 결과를 applyEvent 로,
	//   그 이벤트가 DB에 없으면 아래 엔진 문장으로
	private void parentYear(LifeDTO life, CountryStatDTO stat) {
		if ("N".equals(life.getParentAlive()) || !"PLAYING".equals(life.getStatus())) {
			return; // 윤아님 예전 별세 이벤트로 이미 두 분 다 떠나셨으면 다시 띄우지 않음
		}
		int age = life.getCurrentAge();
		FamSeed f = famSeed(life, stat);
		java.util.LinkedHashSet<String> owns = ownSet(life);
		boolean momGone = parAt(owns, "M") >= 0, dadGone = parAt(owns, "F") >= 0;
		boolean momDue = !momGone && age >= f.motherDie - f.motherAt;
		boolean dadDue = !dadGone && age >= f.fatherDie - f.fatherAt;
		if (!momDue && !dadDue) {
			return;
		}
		String action, text;
		if (momDue && dadDue) {
			action = "PARENT_DEATH_BOTH";
			text = "같은 해, 어머니와 아버지가 다른 날 차례로 세상을 떠나셨다.";
		} else if (momDue) {
			action = "PARENT_DEATH_MOTHER";
			text = dadGone ? "아버지에 이어 어머니마저 세상을 떠나셨다." : "어머니가 세상을 떠나셨다.";
		} else {
			action = "PARENT_DEATH_FATHER";
			text = momGone ? "어머니에 이어 아버지마저 세상을 떠나셨다." : "아버지가 세상을 떠나셨다.";
		}
		String evId = "PARENT_DEATH_BOTH".equals(action) ? "LE_PARENT_DEATH_SAMEYEAR"
				: "PARENT_DEATH_MOTHER".equals(action) ? "LE_PARENT_DEATH_MOTHER" : "LE_PARENT_DEATH_FATHER";
		EventResultDTO pe = eventService.parentDeathEvent(life, evId);
		if (pe != null && action.equals(pe.getResultAction())) {
			// 슬픔은 엔진 문장 때와 같게 최소 −8 (두 분 같은 해 −12)
			pe.setEffHappiness(Math.min(pe.getEffHappiness(), "PARENT_DEATH_BOTH".equals(action) ? -12 : -8));
			applyEvent(life, pe, age, null); // 장례비·유산 문장도 applyEvent 가 붙임
			addExperience(life, "LE_PARENT_DEATH");
			fixStats(life);
			List<EventResultDTO> list = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
			list.add(pe);
			life.setEvents(list);
			return;
		}
		long before = life.getAssets();
		EngState es = engState(life);
		es.estateNote = null;
		es.estateAmt = 0;
		applyAction(life, action, null, age);
		// 윤아님 예전 별세 이벤트(LE_PARENT_DEATH, '한 번만')가 그 뒤에 또 '부모님을 떠나보냈다'를 띄우지 않게 경험으로 남김
		//   (200번: 엔진이 47세에 두 분을 보내드린 뒤 62세에 그 이벤트가 또 나왔음)
		addExperience(life, "LE_PARENT_DEATH");
		life.setHappiness(life.getHappiness() - ("PARENT_DEATH_BOTH".equals(action) ? 12 : 8));
		long moved = life.getAssets() - before - es.estateAmt;
		if (moved < 0 && -moved * krwPerUsd(stat) >= 10000) {
			text = text + " 장례를 치르는 데 " + won(life, -moved) + "이 들었다.";
		}
		if (es.estateNote != null) {
			text = text + " " + es.estateNote;
			es.estateNote = null;
			es.estateAmt = 0;
		}
		fixStats(life);
		saveEventLog(life, age, null, null, text);
		addYearNote(life, text);
	}

	private void payFuneral(LifeDTO life, double ratio, String label) {
		if (life.getCurrentAge() < 18 || life.getAssets() <= 0 && life.getIncome() <= 0) {
			return;
		}
		double med = medIncome(life.getCurrentCountry(), yearStat(life));
		double k = clamp(Math.pow(Math.max(life.getIncome(), 1) / med, 0.3), 0.6, 1.8);
		long pay = Math.round(med * ratio * k);
		life.setAssets(life.getAssets() - pay);
		book(life, label, -pay);
	}

	// ================= 수감·범죄 기록 (윤아님 '위험한 제안', 원작 goPrison · recW, 11/23) =================
	//   OWNS 에 PRI-들어간 나이-형기 (마지막 한 번만): 그 나이부터 형기만큼의 해를 교도소에서 보냄
	//   들어가면 일·학교 중단, 엔진 선택지·취업 제안 없음 (윤아님 rollYear 도 PRISON 이면 이벤트 없음)
	private static final String PRISON_DROP = "수감으로 학교를 그만둠";

	private static boolean inPrison(LifeDTO l) {
		return "PRISON".equals(l.getJobStatus());
	}

	// {들어간 나이, 형기} - 수감 기록이 없으면 null
	private static int[] prisonSpan(LifeDTO life) {
		for (String o : ownSet(life)) {
			if (o.startsWith("PRI-")) {
				String[] p = o.split("-");
				try {
					return new int[] { Integer.parseInt(p[1]), Integer.parseInt(p[2]) };
				} catch (RuntimeException ex) {
					return null;
				}
			}
		}
		return null;
	}

	// 윤아님 EventResultDTO.prisonYears (1~4, 11/23 추가), 값이 없으면 원작 기본 2년
	private static int prisonYearsOf(EventResultDTO e) {
		Object v = e.getPrisonYears();
		if (v instanceof Number && ((Number) v).intValue() > 0) {
			return Math.max(1, Math.min(4, ((Number) v).intValue()));
		}
		return 2;
	}

	// 교도소에 들어감: 지금 나이(다음에 살 해)부터 형기만큼, 하던 일·학교는 그만둠
	//   반환: 가게를 하던 사람이면 붙일 문장 (없으면 null)
	private String startPrison(LifeDTO life, int years) {
		String note = null;
		if ("EMPLOYED".equals(life.getJobStatus()) && life.getJobId() != null
				&& (life.getJobId().contains("BIZ") || "JOB_OWNER".equals(life.getJobId()))) {
			note = "안에서는 가게를 이어 갈 수 없어 문을 닫았다.";
		}
		if ("ENROLLED".equals(life.getSchoolStatus())) {
			life.setSchoolStatus("DROPPED");
			life.setDropReason(PRISON_DROP);
		}
		life.setJobStatus("PRISON");
		life.setJobYears(0);
		life.setIncome(0);
		life.setNeedJob(false);
		life.setJobOffers(null);
		engState(life).schoolNext = null;
		java.util.LinkedHashSet<String> owns = ownSet(life);
		owns.removeIf(x -> x.startsWith("PRI-"));
		owns.add("PRI-" + life.getCurrentAge() + "-" + years);
		life.setOwns(String.join(",", owns));
		return note;
	}

	// 수감 중 한 해 (원작: 건강 −3 · 행복 −6 · 관계 −4)
	private void prisonYear(LifeDTO life) {
		life.setHealth(life.getHealth() - 3);
		life.setHappiness(life.getHappiness() - 6);
		life.setRelation(life.getRelation() - 4);
	}

	// 범죄 기록 무게 (원작 recW): 수감 중 1, 출소 뒤 6년까지 1, 그 뒤 5년마다 절반, 기록 없으면 0
	private static double recordWeight(LifeDTO life) {
		if (inPrison(life)) {
			return 1;
		}
		int[] pr = prisonSpan(life);
		if (pr == null) {
			return 0;
		}
		int t = life.getCurrentAge() - (pr[0] + pr[1]); // 출소 뒤 지난 햇수
		return t <= 6 ? 1 : Math.pow(0.5, (t - 6) / 5.0);
	}

	// 교도소 안에서의 한 해 (원작 pr_visit · pr_novisit 갈래를 우리 말로)
	private String prisonLine(LifeDTO life) {
		List<String> c = new ArrayList<>();
		c.add("교도소에서 또 한 해를 보냈다. 같은 시간에 일어나고 같은 시간에 잠들었다.");
		c.add("작업장에서 하루 종일 일했다. 손에 남는 돈은 얼마 되지 않았다.");
		c.add("안에서 다시 공부를 시작했다. 나가면 무엇을 할지 자주 생각했다.");
		boolean married = "Y".equals(life.getMarriedYn());
		boolean family = married || !"N".equals(life.getParentAlive()) || life.getChildCount() > 0;
		if (married) {
			c.add("배우자가 면회를 왔다. 혼자 살림을 꾸리느라 많이 지쳐 보였다.");
		}
		if (family) {
			c.add("가족이 면회를 왔다. 짧은 시간이 금방 지나갔다.");
		} else {
			c.add("면회 오는 사람 없이 한 해가 지나갔다.");
		}
		return c.get(random.nextInt(c.size()));
	}

	// 이벤트 결과의 상태 변경 처리 (RESULT_ACTION)
	// 결혼 상태(IS_MARRIED)는 윤아님 rollYear가 이벤트 조건용 STATE_MARRIED로 바꿔서 씀 (DB 저장 없음)
	private void applyAction(LifeDTO life, String action, String migrateTo, int age) {
		if (action == null) {
			return;
		}
		switch (action) {
		case "MARRY": // 결혼
			life.setMarriedYn("Y");
			// 원작 살림: 배우자가 가져온 돈 + / 결혼식 비용 − (중위 소득 기준, 내 벌이에 따라 0.6~1.8배)
			// 원작 marry(): 결혼식 비용 = 중위 소득 × 0.4 (남아시아는 혼례가 커서 × 1.2)
			//   부모님이 살아 계시고 45세 전이면 형편(1~5)에 따라 5·20·40·60·80%를 보태 주심 (중위 소득의 3%가 안 되면 없음)
			if (age >= 18) {
				CountryStatDTO wst = yearStat(life);
				double med = medIncome(life.getCurrentCountry(), wst);
				// 배우자가 가져온 돈 = 청혼 창에 보인 그 사람의 모은 돈 (빚이면 마이너스)
				FamSeed wf = famSeed(life, wst);
				Cand wc = cand(life, wf, engState(life).candK);
				long brought = Math.round(candSaved(wc, med, Math.max(18, age + wc.gap)));
				engState(life).wedFrozen = true;
				long wedding = Math.round(med * ("sa".equals(groupOf(life.getCurrentCountry(), wst)) ? 1.2 : 0.4));
				long help = 0;
				if (!"N".equals(life.getParentAlive()) && age < 45) {
					double[] share = { 0.05, 0.2, 0.4, 0.6, 0.8 };
					help = Math.round(wedding * share[Math.max(1, Math.min(5, life.getWealthClass())) - 1]);
					if (help < med * 0.03) {
						help = 0;
					}
				}
				life.setAssets(life.getAssets() + brought - wedding + help);
				book(life, brought >= 0 ? "배우자가 가져온 돈" : "배우자의 빚", brought);
				book(life, "결혼식 비용", -wedding);
				if (help > 0) {
					book(life, "부모님이 보태 준 혼례 비용", help);
				}
			}
			break;
		case "CHILD": // 자녀 +1
			life.setChildCount(life.getChildCount() + 1);
			break;
		case "MARRIAGE_END": // 이혼·사별 (윤아님 이벤트 값): 결혼 상태 해제 (자녀 수는 그대로)
		case "DIVORCE":
			life.setMarriedYn("N");
			engState(life).wedFrozen = false; // 다시 연애·재혼할 수 있게, 첫째 출산은 멈춤 (STATE_MARRIED가 빠짐)
			break;
		case "SPOUSE_DEATH":
			life.setMarriedYn("N");
			engState(life).wedFrozen = false;
			payFuneral(life, 0.3, "배우자 장례비"); // 원작 중간값 −208만 원
			break;
		case "PARENT_DEATH": // 부모 사망 (예전 이벤트: 두 분 모두로 처리)
			if (!"N".equals(life.getParentAlive())) {
				parMark(life, "M", age);
				parMark(life, "F", age);
				life.setParentAlive("N");
				payFuneral(life, 0.06, "부모 장례비 나눠 냄"); // 원작 중간값 −40만 원
				parentEstate(life); // 원작 ESTATE: 형편에 따라 남긴 재산
			}
			break;
		case "PARENT_DEATH_MOTHER": // 어머니만 (11/23): 한 분 장례비, 마지막 분이면 유산 + 부모 생존 N
		case "PARENT_DEATH_FATHER": {
			String who = action.endsWith("MOTHER") ? "M" : "F";
			if ("N".equals(life.getParentAlive()) || parAt(ownSet(life), who) >= 0) {
				break;
			}
			parMark(life, who, age);
			payFuneral(life, 0.06, "부모 장례비 나눠 냄");
			if (parAt(ownSet(life), "M".equals(who) ? "F" : "M") >= 0) {
				life.setParentAlive("N");
				parentEstate(life);
			}
			break;
		}
		case "PARENT_DEATH_BOTH": // 두 분 같은 해 (11/23): 남아 계신 분마다 장례비, 유산 + 부모 생존 N
			if (!"N".equals(life.getParentAlive())) {
				for (String who : new String[] { "M", "F" }) {
					if (parAt(ownSet(life), who) < 0) {
						parMark(life, who, age);
						payFuneral(life, 0.06, "부모 장례비 나눠 냄");
					}
				}
				life.setParentAlive("N");
				parentEstate(life);
			}
			break;
		case "JOB_LOSS": // 실직: 다음 해부터 다시 직업 제안 (jobStep)
			if ("EMPLOYED".equals(life.getJobStatus())) {
				life.setJobStatus("UNEMPLOYED");
				life.setJobYears(0);
			}
			break;
		case "EARLY_RETIRE": // 조기 은퇴: 정년 은퇴와 같은 연금 규칙 (원작 pensionBase)
			if ("EMPLOYED".equals(life.getJobStatus())) {
				life.setJobStatus("RETIRED");
				life.setPension(pensionOf(life, yearStat(life)));
			}
			break;
		case "MIGRATE": // 이민: 갈 나라가 정해져 있을 때만
			if (migrateTo != null && !migrateTo.equals(life.getCurrentCountry())) {
				life.setCurrentCountry(migrateTo);
			}
			break;
		case "LOAN": // 빚을 내서 치료 (윤아님 11/20): 재산이 0 이하면 변화율이 안 먹으니, 그 나라 중위 소득 1년치를 빚으로
			{
				double med = medIncome(life.getCurrentCountry(), yearStat(life));
				long debt = Math.round(med);
				life.setAssets(life.getAssets() - debt);
				book(life, "치료비로 진 빚", -debt);
			}
			break;
		case "PRISON": // 수감: applyEvent 에서 startPrison (형기 prisonYears 가 필요해서)
			break;
		case "CRIME_PAY": // 위험한 제안의 대가 (원작: 약속한 큰돈은 안 들어오고 몇 주 치 벌이만) - 중위 소득 × 4~12%
			{
				double med = medIncome(life.getCurrentCountry(), yearStat(life));
				long pay = Math.round(med * (0.04 + random.nextDouble() * 0.08));
				life.setAssets(life.getAssets() + pay);
				book(life, "위험한 일로 받은 돈", pay);
			}
			break;
		default: // INVEST 등: 재산 변화율만 반영 (위 3번)
			break;
		}
	}

	// ================= C05 경험 기록 =================
	private void addExperience(LifeDTO life, String expId) {
		ExperienceDTO exp = new ExperienceDTO();
		exp.setLifeId(life.getLifeId());
		exp.setExpId(expId);
		exp.setAge(life.getCurrentAge());
		exp.setYearNo(life.getBirthYear() + life.getCurrentAge());
		engineDao.insertExperience(exp);
	}

	// ================= C07 사망 판정 =================

	// 올해 사망 확률 (0 ~ 1) - 원작 deathParts 기준
	// 1. 원작 생명표로 나이·성별 기본 확률 q
	// 2. 배율: 건강(기준 건강보다 낮을수록 ↑) × 경제적 위치(낮을수록 ↑) × 권역 보정
	// 3. 확률 = 1 - (1 - q)^배율 (배율 0.25 ~ 9, 85세부터 배율 효과 줄임)
	private double deathRate(LifeDTO life, CountryStatDTO stat) {
		int age = life.getCurrentAge();
		if (age >= 110) {
			return 1;
		}
		String cc = life.getCurrentCountry();
		String g = groupOf(cc, stat);
		double q = lifeTable(cc, stat, life.getGender())[age];

		// 건강: e^((기준 건강 - 내 건강) × 0.03), 5세 미만 0.02, 최소 0.5
		//   기준 건강: 15세 미만은 아이 기준 건강, 그 외 기대 건강 - 50세 이후 (나이-50)×0.08
		//   (원작은 여기서 6을 더 빼는데, 흡연·만성질환으로 평균 건강이 6쯤 낮은 걸 맞추는 값이라
		//    그 기능이 없는 우리 엔진에선 빼야 기대수명이 통계와 맞음 - 시뮬 확인)
		double ref;
		if (age < 15) {
			ref = kidHlRef(u5Of(stat), age);
		} else {
			ref = expHealth(age) - (age > 50 ? (age - 50) * 0.08 : 0);
		}
		double s = Math.max(0.5, Math.exp((ref - life.getHealth()) * (age < 5 ? 0.02 : 0.03)));

		// 경제적 위치: 18세 미만은 가정형편(1~5), 어른은 소득 위치×5+0.5 (0.5~5.5, 부모와 살면 집안 형편 반영)
		// 어른 형편 효과 배율 WEALTH_MORT_K (200명 때 1.5로 올렸다가, 500명 결과에 맞춰 1.0으로 되돌림)
		double r = mortG(g) * (age < 18 ? 1 : WEALTH_MORT_K);
		double level = age < 18 ? life.getWealthClass() : clamp(mortPct(life, stat) * 5 + 0.5, 0.5, 5.5);
		double avg = 0;
		for (int m = 1; m <= 5; m++) {
			avg += Math.exp(-r * (m - 3));
		}
		avg = avg / 5;
		s = s * Math.exp(-r * (level - 3)) / avg / (age < 18 ? 1 : standK(g, life.getGender()));

		// 85세부터는 배율 효과를 점점 줄임
		if (age >= 85) {
			s = Math.pow(s, clamp(1 - (age - 85) / 20.0, 0.3, 1));
		}
		return 1 - Math.pow(1 - q, clamp(s, 0.25, 9));
	}

	// 사망 원인: 권역·나이대별 사망 원인 비율로 결정 (데이터가 없으면 임시 원인)
	private String deathCause(LifeDTO life) {
		int age = life.getCurrentAge();
		String group = "70+";
		if (age < 5) {
			group = "0-4";
		} else if (age < 15) {
			group = "5-14";
		} else if (age < 50) {
			group = "15-49";
		} else if (age < 70) {
			group = "50-69";
		}

		DeathCauseDTO cond = new DeathCauseDTO();
		cond.setCountryCode(life.getCurrentCountry());
		cond.setAgeGroup(group);
		cond.setYearNo(life.getBirthYear() + age);
		List<DeathCauseDTO> list = engineDao.selectDeathCauses(cond);

		if (!list.isEmpty()) {
			// 비율대로 하나 뽑기
			double total = 0;
			for (DeathCauseDTO d : list) {
				total += d.getRatio();
			}
			double pick = random.nextDouble() * total;
			double sum = 0;
			for (DeathCauseDTO d : list) {
				sum += d.getRatio();
				if (pick < sum) {
					return causeLabel(d.getCauseName(), age);
				}
			}
			return causeLabel(list.get(list.size() - 1).getCauseName(), age);
		}

		// 데이터가 없을 때 임시 원인
		if (age < 5) {
			return "감염병";
		} else if (age < 15) {
			return "사고";
		} else if (age < 50) {
			return random.nextBoolean() ? "사고" : "질병";
		} else if (age < 70) {
			return "질병";
		}
		return "노환";
	}

	// 어린이(15세 미만) 사망 원인 이름 다듬기: 통계 분류 "에이즈 등 성매개 감염"은 아이에겐 모자 감염
	private String causeLabel(String name, int age) {
		if (name != null && age < 15 && name.contains("성매개")) {
			return "에이즈(모자 감염)";
		}
		return name;
	}

	// 최종 결과값 (C07): 평균 소득, 인생 점수
	// 인생 점수 = 마지막 능력치 평균 + 수명 10년당 1점 + 대학 졸업 5점 / 대학원 졸업 8점
	private void finalResult(LifeDTO life) {
		Long avg = engineDao.selectAvgIncome(life); // 죽은 해 소득(life.getIncome)도 포함
		if (avg == null) {
			avg = 0L;
		}
		life.setAvgIncome(avg);

		double score = (life.getHealth() + life.getHappiness() + life.getKnowledge() + life.getRelation()) / 4;
		score = score + life.getDeathAge() / 10;
		if ("GRADUATED".equals(life.getSchoolStatus())) {
			if (life.getEduLevel().equals("UNI")) {
				score = score + 5;
			} else if (life.getEduLevel().equals("GRAD")) {
				score = score + 8;
			}
		} else if (life.getEduLevel().equals("GRAD")) {
			score = score + 5; // 대학원 재학·중퇴여도 대학은 졸업
		}
		life.setLifeScore(Math.round(score * 10) / 10.0);

		// 소득 상위 % = 100 - (내 평균 소득보다 1인당 GDP가 낮은 나라에 사는 인구 비율)
		Double poorer = engineDao.selectPoorerPct(life);
		if (poorer != null) {
			double top = 100 - poorer;
			if (top < 0.1) {
				top = 0.1; // 상위 0.1% 미만은 0.1%로 표시
			}
			life.setIncomeTopPct(Math.round(top * 10) / 10.0);
		}
	}
	// ================= C06-4 엔진 선택지 (원작 temper · SMALL · HUM) =================
	// 윤아님 선택 이벤트·직업 제안이 없는 해에 엔진이 그해 처지에 맞는 작은 선택을 띄움 (목록은 EngineChoice)
	//   - 2세: 기질 고르기 (한 번) → 특성 칸에 '기질'로 표시
	//   - 7~15세: 2년 넘게 비면 35% (원작 아이 SMALL + 진학 결정 몫까지 합쳐 조금 자주)
	//   - 16~64세: 4년 넘게 비면 15%, 65세부터 5년 넘게 비면 8%
	//   - 사람 사이(HUM): 16세부터 6년 넘게 비면 30%, 한 인생에 4번까지 (원작 HUM_GAP 6 · HUM_MAX 4)
	// 기다리는 선택은 DB에 저장하지 않고 서버 메모리에만 둠 (PENDING_EVENT_ID 는 윤아님 이벤트 전용)
	//   → 새로고침하면 다시 뜨고, 서버를 다시 켜면 그 선택은 없던 일이 됨
	// 이미 겪은 선택·기질은 인생 기록의 결과 문장으로 다시 찾음 (서버를 다시 켜도 같은 선택이 또 나오지 않게)
	private static class EngState {
		final Map<String, Integer> done = new HashMap<>();
		int meets;          // 결혼 전 '새로운 만남' 횟수 (인생 기록으로 되살림)
		// 습관·돈 약속 (원작 smoker · alc · gamb · lotto · lent · cosign) - 인생 기록 문장으로 되살림 (OWNS 400바이트, C06-6)
		boolean smoker, alc, gamb, lotto;
		Integer lottoAt;
		double lendAmt;
		Integer lendAt;
		boolean lendOk;
		double csAmt;
		Integer csEnd;
		Integer retUntil;   // 시간제·계속 일하기로 한 마지막 나이 (원작 retire)
		boolean retireAsk;  // 올해 끝에 은퇴할지 물음
		boolean retakeAsk;  // 올해 끝에 재수할지 물음 (원작 retake)
		boolean majorAsk;   // 올해 끝에 전공을 물음 (원작 major)
		boolean noschoolAsk; // 초등학교에 못 간 아이에게 '학교에 가고 싶어요' (원작 noschool)
		String major;       // 고른 전공 (hum law biz eng med edu art)
		String jcJobId;     // 이직 제안 직업 (원작 jobchange)
		double jcSame;      // 같은 일·다른 직장 제안의 벌이 배율 (1.05~1.25, 원작: 지금보다 5% 넘게 나은 자리만)
		double petVet;      // 이번 반려동물 치료비 (원작 petsick)
		long houseSm;       // '작은 집을 대출 없이 산다' 집값 (원작 houseDeal("s"), 0이면 보통 집)
		Integer mktL;       // 투자 큰 오르내림을 마지막으로 기록한 나이 (원작 flags.mktL)
		// 자녀 결정 (원작 babycare · kidschool · discipline · kiduni · kidwed, 11/23)
		String kidNm;       // 지금 묻는 아이 이름
		int kidAge;         // 그 아이 나이
		boolean kidPriv;    // 사립학교에 보냄 (학교 다니는 아이마다 해마다 학비)
		double ksFee;       // 사립 학비 (한 해, 아이 한 명)
		double kuAmt;       // 대학 4년 등록금·생활비
		double kwAmt;       // 자녀 결혼 비용
		Integer ptBack;     // 시간제로 줄인 일을 다시 늘리는 나이
		String ptJob;       // 그때 하던 일
		Integer spOffUntil; // 배우자가 아이를 돌보느라 일을 쉬는 마지막 나이
		Integer careUntil;  // 어린이집 비용을 내는 마지막 나이
		double careCost;    // 어린이집 비용 (한 해)
		Integer lastLife;   // 마지막 삶의 갈림길 나이 (2년에 하나까지)
		int candK;          // 지금 만나는(결혼한) 사람 번호
		boolean wedFrozen;  // 결혼 중이면 true (만남이 나와도 배우자가 바뀌지 않게)
		String estateNote;  // 부모님 별세 문장 뒤에 붙일 유산 문장 (applyEvent)
		long estateAmt;
		Integer lastSmall;
		Integer lastHum;
		int humCount;
		String temper;
		EngPend pend;
		Integer schoolNext; // 이번 해 붙은 진학 단계 (그해 끝에 선택 창)
		double carryDeath;  // 선택 결과의 사망 확률 가산 (참전 중상 0.05 · 시위 진압 0.02 …), 다음 해 사망 판정에 더함
		String carryCause;  // 그때의 사망 원인 (전쟁, 시위 진압)
		String schoolNote;  // 진학 첫해 낙방 기록 (졸업 문장 뒤에 남기려고 잠시 보관)
		String svcKind;     // 병역 판정 (act 현역 · sup 보충역 · ex 면제), 처음 통지서 받을 때 정함
		Integer svcDelayAt; // 입대를 미룬 해 (그해엔 다시 묻지 않음)
		boolean spouseNoted; // 결혼 상대 소개 줄을 남겼는지 (인생 기록 '결혼 상대 ' 로 되살림)
		String petSmall;     // 이번 반려동물 창의 '작은 동물' 종류 (h 햄스터 · r 토끼 · b 앵무새 · f 물고기)
	}

	private static class EngPend {
		int decIdx;
		int age;
		EventResultDTO view;
	}

	private final Map<Integer, EngState> engStates = new ConcurrentHashMap<>();

	private EngState engState(LifeDTO life) {
		EngState s = engStates.get(life.getLifeId());
		if (s != null) {
			return s;
		}
		if (engStates.size() > 20000) {
			engStates.clear();
		}
		s = new EngState();
		List<EventLogDTO> logs = engineDao.selectEventLogs(life.getLifeId());
		if (logs != null) {
			for (EventLogDTO g : logs) {
				if (g.getLogText() == null) {
					continue;
				}
				if (g.getEventId() != null) {
					// 윤아님 연애 이벤트 기록으로 만남 번호 되살림
					String lt = g.getLogText();
					if (lt.contains("마음이 통하는 사람을 만났다") && !s.wedFrozen) {
						s.candK = s.meets;
						s.meets++;
					}
					if (lt.contains("결혼식을 올렸다")) {
						s.wedFrozen = true;
					}
					if (lt.contains("각자의 길을 가기로") || lt.contains("이혼") || lt.contains("배우자를 먼저 떠나보내")) {
						s.wedFrozen = false;
					}
					continue;
				}
				String t = g.getLogText();
				// 전공 (원작 major)
				if (t.startsWith("전공으로 ")) {
					for (EngineChoice.Opt mo : EngineChoice.byKey("MAJOR").opts) {
						if (t.startsWith(mo.results[0])) {
							s.major = mo.act.substring(6);
						}
					}
				}
				// 습관 (원작 smoker · alc · gamb · lotto) · 은퇴 미루기
				if (t.contains("담배를 피우기 시작했다")) {
					s.smoker = true;
				}
				if (t.contains("담배를 끊었다")) {
					s.smoker = false;
				}
				if (t.startsWith("금연에 성공했다") || t.startsWith("금연에 실패했다")) {
					markEngineDone(s, EngineChoice.byKey("QUITSMOKE"), g.getAge());
				}
				if (t.contains("사립학교에 보냈다")) {
					s.kidPriv = true;
				}
				if (t.contains("술 없이는 잠들지 못하는")) {
					s.alc = true;
				}
				if (t.contains("술을 끊었다")) {
					s.alc = false;
				}
				if (t.contains("계속하게 되었다") || t.contains("또다시 손을 댔다")) {
					s.gamb = true;
				}
				if (t.contains("도박을 끊었다")) {
					s.gamb = false;
				}
				if (t.startsWith("매주 복권을 사기 시작했다")) {
					s.lotto = true;
					s.lottoAt = g.getAge();
				}
				if (t.contains("복권은 그만 사기로")) {
					s.lotto = false;
				}
				if (t.startsWith("일을 줄여 시간제로 몇 해 더") || t.startsWith("몇 해 더 일하기로 했다") || t.startsWith("은퇴하지 않고 몇 해 더")
						|| t.startsWith("정년 뒤 계약직으로")) {
					s.retUntil = g.getAge() + 5;
				}
				if (t.contains("내 집을 마련했다")) {
					markEngineDone(s, EngineChoice.byKey("BUYHOUSE"), g.getAge());
				}
				// 유언장 (남길 곳 이름이 문장에 들어가서 결과 문장 앞부분 비교로는 못 찾음)
				if (t.startsWith("유언장을 썼다") || t.startsWith("유언장은 쓰지 않기로")) {
					markEngineDone(s, EngineChoice.byKey("WILL"), g.getAge());
				}
				// 집값이 크게 움직인 해 (5년에 한 번만 기록)
				if (t.startsWith("동네 집값이 크게") || t.startsWith("집값이 한 해 사이")) {
					s.done.put("HP", g.getAge());
				}
				// 부모님 도움 (한 번만 받는 것들)
				if (t.startsWith("일을 시작하자 부모님이 살림 밑천")) {
					s.done.put("PAR_SEED", g.getAge());
				}
				if (t.startsWith("부모님이 학창 시절의 빚")) {
					s.done.put("PAR_DEBT", g.getAge());
				}
				if (t.startsWith("일자리를 찾는 동안 부모님이")) {
					s.done.put("PAR_LIVE", g.getAge());
				}
				if (t.startsWith("부모님이 스물다섯")) {
					s.done.put("PAR_RICH", g.getAge());
				}
				if (t.startsWith("부모님이 재산의 일부를 미리")) {
					s.done.put("PAR_GIFT", g.getAge());
				}
				if (t.contains("부모님의 도움") && t.contains("내 집을 마련")) {
					s.done.put("PAR_HOUSE", g.getAge());
				}
				if (t.startsWith("결혼 상대 ")) {
					s.spouseNoted = true;
				}
				if (t.startsWith("반려동물은 들이지 않기로 했다")) {
					s.done.put("PET_ADOPT", 99);
				}
				if (t.startsWith(QUEST_DONE)) {
					s.done.put("Q:" + t.substring(QUEST_DONE.length()).trim(), g.getAge());
				}
				// 학비 안내는 대학·대학원마다 한 번 (uniFees)
				if ((t.startsWith("대학 ") || t.startsWith("대학원 ")) && (t.contains("학비") || t.contains("학자금") || t.contains("등록금"))) {
					s.done.put(t.startsWith("대학원 ") ? "FEE_GRAD" : "FEE_UNI", g.getAge());
				}
				for (EngineChoice.Dec d : EngineChoice.ALL) {
					if (d.failText != null && t.startsWith(d.failText)) {
						markEngineDone(s, d, g.getAge());
					}
					for (EngineChoice.Opt o : d.opts) {
						for (String res : o.results) {
							if (t.startsWith(res)) {
								markEngineDone(s, d, g.getAge());
								if (o.temper != null) {
									s.temper = o.temper;
								}
							}
						}
					}
				}
			}
		}
		engStates.put(life.getLifeId(), s);
		return s;
	}

	private void markEngineDone(EngState s, EngineChoice.Dec d, int age) {
		s.done.merge(d.key, 1, Integer::sum);
		s.done.put(d.key + "@", age); // 마지막으로 고른 나이
		if (EngineChoice.HUM.equals(d.group)) {
			s.humCount++;
			s.lastHum = s.lastHum == null ? age : Math.max(s.lastHum, age);
		} else if (EngineChoice.KID.equals(d.group) || EngineChoice.ADULT.equals(d.group)) {
			s.lastSmall = s.lastSmall == null ? age : Math.max(s.lastSmall, age);
		}
	}

	// 이 선택을 지금 띄울 수 있는지 (나이·횟수·간격·처지)
	private boolean engineOk(LifeDTO life, EngState s, EngineChoice.Dec d, int age) {
		if (age < d.minAge || age > d.maxAge) {
			return false;
		}
		int n = s.done.getOrDefault(d.key, 0);
		if (n >= d.max) {
			return false;
		}
		Integer at = s.done.get(d.key + "@");
		if (at != null && age - at < d.gap) {
			return false;
		}
		return d.ok.test(life);
	}

	// 그룹 안에서 띄울 수 있는 선택 하나를 무작위로 (없으면 -1)
	private int pickEngine(LifeDTO life, EngState s, int age, String... groups) {
		List<Integer> cand = new ArrayList<>();
		for (int i = 0; i < EngineChoice.ALL.size(); i++) {
			EngineChoice.Dec d = EngineChoice.ALL.get(i);
			for (String g : groups) {
				if (g.equals(d.group) && engineOk(life, s, d, age)) {
					cand.add(i);
				}
			}
		}
		return cand.isEmpty() ? -1 : cand.get(random.nextInt(cand.size()));
	}

	// 한 해가 끝날 때 (nextYear 마지막): 다른 결정 창이 없으면 엔진 선택지를 굴림
	private void rollEngineChoice(LifeDTO life, int lived) {
		EngState s = engState(life);
		if (inPrison(life)) {
			s.schoolNext = null;
			return; // 수감 중엔 엔진 선택지(집·자녀·취미 …) 없음
		}
		if (s.schoolNext != null) {
			// 진학 선택 (붙은 해): 다른 결정 창이 이미 떠 있으면 예전처럼 바로 진학
			int nx = s.schoolNext;
			s.schoolNext = null;
			int di = EngineChoice.indexOf("SCHOOL_" + levels[nx]);
			boolean free = s.pend == null && !life.isNeedChoice() && !life.isNeedDestination() && life.getPendingEventId() == null;
			// 그해에만 뜨는 역사 현장(4·19 등)이 겹치면 역사 현장을 띄우고 진학은 바로 (1944년생 1960년 고교 진학과 겹쳐 4·19가 안 뜨던 것)
			int hist = free ? pickEngine(life, s, lived, EngineChoice.HIST) : -1;
			if (free && hist < 0) {
				openEngineChoice(life, s, di, lived);
				if (s.pend != null) {
					return;
				}
			}
			life.setEduLevel(levels[nx]);
			life.setSchoolStatus("ENROLLED");
			life.setDropReason(null);
			EngineChoice.Dec d = EngineChoice.ALL.get(di);
			markEngineDone(s, d, lived);
			String t = d.opts[0].results[0];
			saveEventLog(life, lived, null, null, t);
			addYearNote(life, t);
			if (hist >= 0) {
				openEngineChoice(life, s, hist, lived);
			}
			return;
		}
		if (!"PLAYING".equals(life.getStatus()) || life.isNeedChoice() || life.isNeedDestination()
				|| life.getPendingEventId() != null) {
			return;
		}
		if (s.pend != null) {
			return;
		}
		// 4·19 · 서울의 봄 · 6월 항쟁 (그해·한국에서만): 일자리 제안 창이 있어도 먼저 띄움
		//   (화면은 선택 창 → 일자리 창 순서로 보여 줌, 18세 첫 제안과 1960년이 겹쳐 4·19가 안 뜨던 것)
		int pick = pickEngine(life, s, lived, EngineChoice.HIST);
		if (pick >= 0) {
			openEngineChoice(life, s, pick, lived);
			return;
		}
		// 병역 (한국 남성): 일자리 제안이 있어도 먼저 (입대하면 그 일은 못 함)
		pick = svcPick(life, s, lived);
		if (pick >= 0) {
			openEngineChoice(life, s, pick, lived);
			return;
		}
		// 취미 고르기 (원작 hobby: 7·15·27·50세 후보. 원작은 그해 다른 결정과 겨뤄 인생에 약 2.4번만 뜸)
		//   → 7세는 늘, 15세 35%, 27·50세 65% (300번 비교: 우리 4.3번 → 원작 2.4번에 맞춤)
		if ((lived == 7 || lived == 15 || lived == 27 || lived == 50) && !s.done.containsKey("HOBBY@" + lived)) {
			s.done.put("HOBBY@" + lived, 1);
		}
		if ((lived == 7 || lived == 15 || lived == 27 || lived == 50) && s.done.get("HOBBY@" + lived) == 1) {
			boolean ask = random.nextDouble() < (lived == 7 ? 1.0 : lived == 15 ? 0.35 : 0.65);
			s.done.put("HOBBY@" + lived, 2); // 한 번만 굴림
			if (ask) {
				openEngineChoice(life, s, EngineChoice.indexOf("HOBBY"), lived);
				if (s.pend != null) {
					return;
				}
			}
		}
		// 학교에 못 간 아이 (원작 noschool): 8~12세에 한 번
		if (s.noschoolAsk && lived >= 8 && lived <= 12 && levelIndex(life.getEduLevel()) < 0 && !"ENROLLED".equals(life.getSchoolStatus())) {
			s.noschoolAsk = false;
			openEngineChoice(life, s, EngineChoice.indexOf("NOSCHOOL"), lived);
			if (s.pend != null) {
				return;
			}
		}
		// 재수 (원작 retake) · 전공 (원작 major)
		if (s.retakeAsk) {
			s.retakeAsk = false;
			openEngineChoice(life, s, EngineChoice.indexOf("RETAKE"), lived);
			if (s.pend != null) {
				return;
			}
		}
		if (s.majorAsk && "UNI".equals(life.getEduLevel()) && "ENROLLED".equals(life.getSchoolStatus())) {
			s.majorAsk = false;
			openEngineChoice(life, s, EngineChoice.indexOf("MAJOR"), lived);
			if (s.pend != null) {
				return;
			}
		}
		// 은퇴할지 (원작 retire): 정년이 된 해
		if (s.retireAsk) {
			s.retireAsk = false;
			openEngineChoice(life, s, EngineChoice.indexOf("RETIRE"), lived);
			if (s.pend != null) {
				return;
			}
		}
		if (life.isNeedJob()) {
			return;
		}
		// 자녀 (원작 babycare · kidwed · kiduni · kidschool · discipline): 아이 나이에 맞춰
		pick = famPick(life, s, lived);
		if (pick >= 0) {
			openEngineChoice(life, s, pick, lived);
			if (s.pend != null) {
				return;
			}
		}
		// 돈 굴리기 · 배우자 주식 권유 · 유언장 (원작 invest · pstock · will)
		pick = moneyPick(life, s, lived);
		if (pick >= 0) {
			openEngineChoice(life, s, pick, lived);
			return;
		}
		// 삶의 갈림길 (원작 jobchange · rankup · buy · care · smoke · drink · lotto …, 11/23)
		pick = lifePick(life, s, lived);
		if (pick >= 0) {
			openEngineChoice(life, s, pick, lived);
			if (s.pend != null) {
				return;
			}
			pick = -1;
		}
		if (lived == 2) {
			pick = pickEngine(life, s, lived, EngineChoice.TEMPER);
		}
		if (pick < 0 && lived >= 16 && s.humCount < 4 && (s.lastHum == null || lived - s.lastHum >= 6)
				&& random.nextDouble() < 0.3) {
			pick = pickEngine(life, s, lived, EngineChoice.HUM);
			if (pick >= 0) {
				s.lastHum = lived; // 고르지 않고 넘어가도 간격은 셈
			}
		}
		if (pick < 0 && lived >= 7) {
			int gap = lived <= 15 ? 2 : lived < 65 ? 4 : 5;
			double p = lived <= 15 ? 0.35 : lived < 65 ? 0.15 : 0.08;
			if ((s.lastSmall == null || lived - s.lastSmall >= gap) && random.nextDouble() < p) {
				pick = pickEngine(life, s, lived, EngineChoice.KID, EngineChoice.ADULT);
				if (pick >= 0) {
					s.lastSmall = lived;
				}
			}
		}
		if (pick >= 0) {
			openEngineChoice(life, s, pick, lived);
		}
	}

	// 자녀 결정 고르기 (원작 babycare · kidwed · kiduni · kidschool · discipline)
	//   아이 이름·나이는 가족 칸(family)과 같은 것, 결혼 나이도 가족 칸의 '기혼'과 같은 씨앗
	private int famPick(LifeDTO life, EngState s, int lived) {
		if (life.getChildCount() <= 0) {
			return -1;
		}
		List<FamilyMemberDTO> rows;
		try {
			rows = family(life, yearStat(life));
		} catch (RuntimeException e) {
			return -1;
		}
		String key = null;
		List<int[]> small = new ArrayList<>(); // 훈육할 만한 아이 {번호, 나이}
		List<String> smallNm = new ArrayList<>();
		int i = -1;
		for (FamilyMemberDTO m : rows) {
			if (!"아들".equals(m.getRole()) && !"딸".equals(m.getRole())) {
				continue;
			}
			i++;
			if (!m.isAlive() || m.getAge() == null || m.getName() == null) {
				continue;
			}
			int ka = m.getAge();
			java.util.SplittableRandom kr = new java.util.SplittableRandom(life.getLifeId() * 7727L + i * 131L + 3);
			boolean willMarry = kr.nextDouble() < 0.82;
			int marryAt = 24 + kr.nextInt(10);
			String k = null;
			if (ka == 0 && "EMPLOYED".equals(life.getJobStatus()) && !s.done.containsKey("BC#" + i)) {
				k = "BABYCARE";
			} else if (willMarry && ka == marryAt && !s.done.containsKey("KW#" + i)) {
				k = "KIDWED";
			} else if (ka == 19 && !s.done.containsKey("KU#" + i)) {
				s.done.put("KU#" + i, 1); // 합격 여부는 한 번만 굴림
				double p = clamp(0.1 + 0.1 * life.getWealthClass() + (s.kidPriv ? 0.1 : 0), 0.1, 0.75)
						* (life.getBirthYear() + lived < 1970 ? 0.5 : 1);
				if (random.nextDouble() < p) {
					k = "KIDUNI";
				}
			} else if (ka == 5 && !s.done.containsKey("KS#" + i)) {
				s.done.put("KS#" + i, 1);
				k = "KIDSCHOOL";
			}
			if (ka >= 3 && ka <= 11) {
				small.add(new int[] { i, ka });
				smallNm.add(kidCall(m.getName(), life.getBirthCountry()));
			}
			if (k != null && engineOk(life, s, EngineChoice.byKey(k), lived)) {
				s.kidNm = kidCall(m.getName(), life.getBirthCountry());
				s.kidAge = ka;
				s.done.put(("BABYCARE".equals(k) ? "BC#" : "KIDWED".equals(k) ? "KW#" : "KIDUNI".equals(k) ? "KU#" : "KS#") + i, 1);
				key = k;
				break;
			}
		}
		// 훈육 (원작 discipline: 3~11세 아이, 한 해 12%, 5년 간격, 두 번까지)
		if (key == null && !small.isEmpty() && engineOk(life, s, EngineChoice.byKey("DISCIPLINE"), lived) && random.nextDouble() < 0.12) {
			int j = random.nextInt(small.size());
			s.kidNm = smallNm.get(j);
			s.kidAge = small.get(j)[1];
			key = "DISCIPLINE";
		}
		return key == null ? -1 : EngineChoice.indexOf(key);
	}

	// 아이를 부르는 이름 (원작 callName): 성 쓰는 나라는 성을 빼고, 띄어 쓰는 이름은 앞 낱말만 ('유리아 쿠르니아완' → '유리아')
	private static String kidCall(String nm, String cc) {
		if (nm == null || nm.isBlank()) {
			return "아이";
		}
		if (SURNAME_CC.contains(cc)) {
			String g = splitName(nm)[1];
			return g.isBlank() ? nm : g;
		}
		int sp = nm.indexOf(' ');
		return sp > 0 ? nm.substring(0, sp) : nm;
	}

	// 받침에 따라 조사 (이름 뒤): kj("민준", "을", "를")
	private static String kj(String w, String withB, String noB) {
		char c = w.charAt(w.length() - 1);
		if (c < 0xAC00 || c > 0xD7A3) {
			return w + withB;
		}
		return w + ((c - 0xAC00) % 28 == 0 ? noB : withB);
	}

	// 아이 체벌을 법으로 금지한 나라·해 (원작 cpBan, 아시아 일부)
	private boolean cpBan(LifeDTO life) {
		int y = life.getBirthYear() + life.getCurrentAge();
		String cc = life.getCurrentCountry();
		return "KOR".equals(cc) && y >= 2021 || "JPN".equals(cc) && y >= 2020 || "MNG".equals(cc) && y >= 2016
				|| "NPL".equals(cc) && y >= 2018 || "ISR".equals(cc) && y >= 2000 || "TKM".equals(cc) && y >= 2002;
	}

	// 모아 둔 돈 문장 ('약 0원'이 나오지 않게)
	private String savedLine(LifeDTO life) {
		return life.getAssets() > 0 ? "모아 둔 돈은 약 " + won(life, life.getAssets()) + "이에요." : "모아 둔 돈은 없어요.";
	}

	// 삶의 갈림길 고르기 (원작에서 자주 나오는 결정, 아시아 303번 기준 나오는 비율에 맞춘 해마다 확률)
	//   2년에 하나까지, 조건이 맞는 것 가운데 확률을 넘긴 것 하나를 무작위로
	private int lifePick(LifeDTO life, EngState s, int lived) {
		if (s.lastLife != null && lived - s.lastLife < 2) {
			return -1;
		}
		boolean work = "EMPLOYED".equals(life.getJobStatus()) && !"ENROLLED".equals(life.getSchoolStatus());
		boolean employee = work && !"JOB_OWNER".equals(life.getJobId());
		String edu = life.getEduLevel();
		List<String> keys = new ArrayList<>();
		java.util.function.BiConsumer<String, Double> add = (k, p) -> {
			EngineChoice.Dec d = EngineChoice.byKey(k);
			if (d != null && engineOk(life, s, d, lived) && random.nextDouble() < p) {
				keys.add(k);
			}
		};
		if (employee) {
			add.accept("MENTOR", 0.05);
			add.accept("HARASS", 0.015);
			if (life.getJobYears() >= 5) {
				add.accept("RANKUP", 0.08);
			}
			if (life.getJobYears() >= 1) {
				add.accept("JOBCHANGE", 0.13); // 원작 jobchange 13% (다른 일이 없어도 같은 일·다른 직장 제안은 옴)
			}
			if (!"UNI".equals(edu) && !"GRAD".equals(edu)) {
				add.accept("ADULTEDU", 0.03);
			}
		}
		if (!ownSet(life).contains("HOUSE") && ("Y".equals(life.getMarriedYn()) || lived >= 30) && houseOk(life)) {
			add.accept("BUYHOUSE", 0.12);
		}
		if (lived >= 45) {
			add.accept("CARE", 0.05);
		}
		add.accept("OLDHOME", 0.12);
		add.accept("CITY", 0.04);
		if (famSeed(life, yearStat(life)).siblingGap.length > 0) {
			add.accept("KINASK", 0.04);
		}
		add.accept("LEND", 0.025);
		add.accept("COSIGN", 0.012);
		add.accept("SMOKE", s.smoker ? 0 : 0.06);
		if (s.smoker) {
			add.accept("QUITSMOKE", 0.15);
		}
		if (life.getHappiness() < 50 && !s.alc) {
			add.accept("DRINK", 0.06);
		}
		if (s.alc) {
			add.accept("QUITDRINK", 0.2);
		}
		if (!s.lotto) {
			add.accept("LOTTO", 0.025);
		}
		if (!s.gamb) {
			add.accept("GAMBLE", 0.012);
		} else {
			add.accept("QUITGAMBLE", 0.25);
		}
		// 원작 14% 이상 나머지 (loan · hmwork · petsick · mind · applpar)
		if (life.getAssets() < 0 && debtHeavy(life, yearStat(life))) {
			add.accept("LOAN", 0.15);
			if ("Y".equals(life.getMarriedYn()) && !work && !"ENROLLED".equals(life.getSchoolStatus())) {
				add.accept("HMWORK", 0.15);
			}
		}
		if (petOf(life) != null) {
			add.accept("PETSICK", 0.06);
		} else {
			add.accept("PET_ADOPT", 0.12); // 원작 petq: 한 인생 약 2번 (예전엔 작은 선택 사이에 섞여 0.3번)
		}
		if (life.getHappiness() < 40) {
			add.accept("MIND", 0.1);
		}
		if (!"N".equals(life.getParentAlive()) && !"Y".equals(life.getMarriedYn()) && lived < 35 && life.getIncome() > 0) {
			add.accept("APPLPAR", 0.05);
		}
		if (keys.isEmpty()) {
			return -1;
		}
		s.lastLife = lived;
		return EngineChoice.indexOf(keys.get(random.nextInt(keys.size())));
	}

	// 지금 집을 살 수 있는지 (돈 쓰기 '내 집 마련'이 열려 있는지)
	private boolean houseOk(LifeDTO life) {
		try {
			for (ShopItemDTO it : shop(life).getItems()) {
				if ("BUY_HOUSE".equals(it.getId())) {
					return it.isOk();
				}
			}
		} catch (RuntimeException e) {
			// 목록을 못 만들면 묻지 않음
		}
		return false;
	}

	// 이직 제안 직업 (원작 jobchange): 학력이 되는 일 가운데 지금보다 벌이가 15~100% 나은 것 하나 (그해엔 늘 같게)
	private JobDTO jcCandidate(LifeDTO life, int lived) {
		if (life.getIncomeRate() == null) {
			return null;
		}
		double now = life.getIncomeRate();
		int fin = finishedLevel(life);
		List<JobDTO> pool = new ArrayList<>();
		for (JobDTO j : jobs()) {
			if (levelIndex(j.getRequiredEdu()) <= fin && !j.getJobId().equals(life.getJobId()) && !"JOB_OWNER".equals(j.getJobId())
					&& j.getIncomeRate() > now * 1.05 && j.getIncomeRate() < now * 2) {
				pool.add(j);
			}
		}
		if (pool.isEmpty()) {
			return null;
		}
		return pool.get(new java.util.SplittableRandom(life.getLifeId() * 613L + lived).nextInt(pool.size()));
	}

	// 습관·돈 약속의 해마다 일 (원작 담뱃값 · 술 · 도박 · lottoYear · 빌려준 돈 · 보증)
	private void habitYear(LifeDTO life, CountryStatDTO stat) {
		if (!"PLAYING".equals(life.getStatus())) {
			return;
		}
		EngState s = engState(life);
		int age = life.getCurrentAge();
		double med = medIncome(life.getCurrentCountry(), stat);
		// 자녀 (원작 babycare · kidschool): 어린이집 비용 · 사립 학비 · 시간제 끝
		if (s.careUntil != null) {
			if (age <= s.careUntil) {
				life.setAssets(life.getAssets() - Math.round(s.careCost));
				book(life, "아이 돌봄 비용", -s.careCost);
			} else {
				s.careUntil = null;
			}
		}
		if (s.kidPriv && age < 75) {
			int n = 0;
			List<Integer> births = engineDao.selectChildBirthAges(life.getLifeId());
			if (births != null) {
				for (Integer b : births) {
					if (b != null && age - b - 1 >= 6 && age - b - 1 <= 17) {
						n++;
					}
				}
			}
			if (n > 0) {
				life.setAssets(life.getAssets() - Math.round(s.ksFee * n));
				book(life, "사립 학비", -s.ksFee * n);
			}
		}
		if (s.ptBack != null && age >= s.ptBack) {
			if ("EMPLOYED".equals(life.getJobStatus()) && s.ptJob != null && s.ptJob.equals(life.getJobId()) && life.getIncomeRate() != null) {
				life.setIncomeRate(life.getIncomeRate() / 0.6);
				parentNote(life, "아이가 자라 다시 하루 종일 일하기 시작했다.");
			}
			s.ptBack = null;
		}
		if (s.smoker && age >= 18) {
			double c = Math.min(med * 0.025, Math.max(life.getIncome(), med * 0.3) * 0.06);
			life.setAssets(life.getAssets() - Math.round(c));
			book(life, "담뱃값", -c);
			life.setHealth(life.getHealth() - 0.5);
		}
		if (s.alc) {
			double c = med * 0.02;
			life.setAssets(life.getAssets() - Math.round(c));
			book(life, "술값", -c);
			life.setHealth(life.getHealth() - 0.8);
			life.setHappiness(life.getHappiness() - 0.5);
			life.setRelation(life.getRelation() - 0.3);
		}
		if (s.gamb) {
			double c = med * 0.05 * (0.5 + random.nextDouble());
			life.setAssets(life.getAssets() - Math.round(c));
			book(life, "도박으로 잃은 돈", -c);
			life.setHappiness(life.getHappiness() - 0.6);
			life.setRelation(life.getRelation() - 0.4);
		}
		if (s.lotto) {
			double c = med * ("hi".equals(groupOf(life.getCurrentCountry(), stat)) ? 0.009 : 0.006) * 0.82; // 원작 lottoCost
			life.setAssets(life.getAssets() - Math.round(c));
			book(life, "복권 값", -c);
			double r = random.nextDouble(), n = "KOR".equals(life.getCurrentCountry()) ? 32e-6 : 15e-6;
			if (r < n) {
				double a = med * 45 * (0.6 + random.nextDouble() * 0.8);
				life.setAssets(life.getAssets() + Math.round(a));
				book(life, "복권 당첨금", a);
				life.setHappiness(life.getHappiness() + 12);
				parentNote(life, "복권 1등에 당첨되었다! 세금을 떼고 " + won(life, a) + "이 들어왔다. 몇 번이고 번호를 다시 맞춰 보았다.");
			} else if (r < n + 19e-5) {
				double a = med * 1.2;
				life.setAssets(life.getAssets() + Math.round(a));
				book(life, "복권 당첨금", a);
				life.setHappiness(life.getHappiness() + 6);
				parentNote(life, "복권 2등에 당첨되었다. " + won(life, a) + ", 큰돈이었지만 삶이 바뀔 만큼은 아니었다.");
			} else if (r < n + 19e-5 + 0.0073) {
				double a = med * 0.045;
				life.setAssets(life.getAssets() + Math.round(a));
				book(life, "복권 당첨금", a);
				life.setHappiness(life.getHappiness() + 3);
				parentNote(life, "복권 3등에 당첨되었다. " + won(life, a) + "으로 가족과 근사한 저녁을 먹었다.");
			} else if (s.lottoAt != null && age - s.lottoAt >= 5 && random.nextDouble() < 0.04) {
				s.lotto = false;
				parentNote(life, "몇 해째 꽝만 이어져 복권은 그만 사기로 했다.");
			}
		}
		if (s.lendAt != null && age - s.lendAt >= 2) {
			if (s.lendOk) {
				life.setAssets(life.getAssets() + Math.round(s.lendAmt));
				book(life, "돌려받은 돈", s.lendAmt);
				parentNote(life, "친구가 빌려 간 돈 " + won(life, s.lendAmt) + "을 갚았다. 고맙다며 밥을 샀다.");
				s.lendAt = null;
			} else if (age - s.lendAt >= 4) {
				life.setRelation(life.getRelation() - 2);
				parentNote(life, "친구에게 빌려준 돈은 끝내 돌려받지 못했다.");
				s.lendAt = null;
			}
		}
		if (s.csEnd != null && age >= s.csEnd) {
			if (random.nextDouble() < 0.25) {
				life.setAssets(life.getAssets() - Math.round(s.csAmt));
				book(life, "대신 갚은 보증 빚", -s.csAmt);
				life.setHappiness(life.getHappiness() - 6);
				parentNote(life, "보증을 서 준 대출을 그 사람이 갚지 못해, " + won(life, s.csAmt) + "을 대신 갚았다.");
			} else {
				parentNote(life, "보증을 서 준 대출이 무사히 끝났다. 마음의 짐을 하나 내려놓았다.");
			}
			s.csEnd = null;
		}
	}

	// 돈 결정 (원작 조건)
	//   돈 굴리기: 굴리는 방식을 정한 적 없고 모은 돈이 중위 소득의 30%를 넘으면 한 번 (고를 수 있는 곳이 2개 이상일 때)
	//   유언장: 50세부터, 재산이 중위 소득의 절반을 넘으면 해마다 20%로 한 번
	//   배우자 주식 권유: 결혼했고 모은 돈이 중위 소득의 30%를 넘으면 해마다 3%로 한 번
	private int moneyPick(LifeDTO life, EngState s, int lived) {
		CountryStatDTO stat = yearStat(life);
		double med = medIncome(life.getCurrentCountry(), stat);
		long a = life.getAssets();
		EngineChoice.Dec inv = EngineChoice.byKey("INVEST");
		if (lived >= 20 && life.getInvestType() == null && a > med * 0.3 && engineOk(life, s, inv, lived)) {
			int n = 0;
			for (String t : new String[] { "DEPOSIT", "MIX", "STOCK", "PROPERTY" }) {
				if (investWhy(life, stat, t) == null) {
					n++;
				}
			}
			if (n >= 2) {
				return EngineChoice.indexOf("INVEST");
			}
		}
		EngineChoice.Dec will = EngineChoice.byKey("WILL");
		if (a > med * 0.5 && engineOk(life, s, will, lived) && random.nextDouble() < 0.2) {
			return EngineChoice.indexOf("WILL");
		}
		EngineChoice.Dec ps = EngineChoice.byKey("PSTOCK");
		if (a > med * 0.3 && engineOk(life, s, ps, lived) && investWhy(life, stat, "STOCK") == null && random.nextDouble() < 0.03) {
			return EngineChoice.indexOf("PSTOCK");
		}
		return -1;
	}

	// 선택지 금액 붙이기: 괄호가 두 번 나오지 않게 ('돌봄 일꾼을 구한다 (비용) (약 213만 원)' → '(약 213만 원)',
	//   '내가 모시고 산다 (관계↑ · 생활비↑) (약 71만 원)' → '(관계↑ · 생활비↑ · 약 71만 원)')
	private static String costLabel(String label, String price) {
		int p = label.lastIndexOf(" (");
		if (p < 0 || !label.endsWith(")")) {
			return label + " (약 " + price + ")";
		}
		String head = label.substring(0, p);
		java.util.List<String> parts = new java.util.ArrayList<>();
		for (String x : label.substring(p + 2, label.length() - 1).split(" · ")) {
			if (!"비용".equals(x.trim())) {
				parts.add(x.trim().replace(" · 비용", ""));
			}
		}
		parts.add("약 " + price);
		return head + " (" + String.join(" · ", parts) + ")";
	}

	// 선택 창 만들기: 윤아님 선택 이벤트와 같은 모양 (eventId ENG_키, eventType SMALL, 선택지 번호는 음수)
	private void openEngineChoice(LifeDTO life, EngState s, int decIdx, int lived) {
		EngineChoice.Dec d = EngineChoice.ALL.get(decIdx);
		EventResultDTO e = new EventResultDTO();
		e.setEventId("ENG_" + d.key);
		e.setEventType("SMALL");
		e.setTitle(d.title);
		e.setChoice(true);
		e.setLogText(d.bodies[random.nextInt(d.bodies.length)]);
		if (d.key.startsWith("SVC_")) {
			e.setLogText(svcBody(life, d.key));
		}
		if ("SCHOOL_UNI".equals(d.key) || "SCHOOL_GRAD".equals(d.key)) {
			// 원작처럼 등록금 · 장학금 · 부모님 도움 · 대출 안내
			e.setLogText((e.getLogText() + " " + uniFeeNote(life, yearStat(life), "SCHOOL_GRAD".equals(d.key))).trim());
		}
		// 돈 결정은 지금 금액을 넣어 (원작 invest · will · pstock 본문)
		if ("INVEST".equals(d.key)) {
			e.setLogText("모은 돈이 약 " + won(life, life.getAssets()) + "이 되었어요 (한국 체감 금액). 여유 자금을 어디에 둘까요? 나중에 '돈 쓰기'에서 언제든 바꿀 수 있어요.");
		} else if ("WILL".equals(d.key)) {
			e.setLogText("남길 것을 생각할 나이가 되었어요. 지금 재산은 약 " + won(life, life.getAssets()) + "이에요. 유언장이 없으면 법이 정한 순서대로 나뉘어요."
					+ ("KOR".equals(life.getCurrentCountry()) ? " 한국에서는 유언 없이 세상을 떠나면 배우자가 자녀 몫의 1.5배를 받고 자녀들은 똑같이 나눠요 (민법 1009조)." : ""));
		} else if ("PSTOCK".equals(d.key)) {
			e.setLogText("배우자가 아는 사람에게 확실한 종목이라는 이야기를 들었다며, 모아 둔 돈의 절반인 " + won(life, Math.max(0, life.getAssets()) * 0.5) + "을 넣자고 해요. 개인 투자자는 대개 시장 평균보다 낮은 수익을 내요.");
		}
		boolean pays = engineCostPays(life, lived);
		double med = medIncome(life.getCurrentCountry(), yearStat(life));
		List<EventChoiceDTO> cs = new ArrayList<>();
		for (int i = 0; i < d.opts.length; i++) {
			EngineChoice.Opt o = d.opts[i];
			if (o.show != null && !o.show.test(life)) {
				continue;
			}
			// 돈 굴리기: 그 나라·그해에 못 하는 곳(증권시장 없음 등)은 빼고
			if (o.act != null && o.act.startsWith("INV_") && investWhy(life, yearStat(life), o.act.substring(4)) != null) {
				continue;
			}
			// 반려동물 (원작 petq): '한 해 약 ○○ · 10~15년 함께' / 작은 동물은 그 나라에서 흔한 것 하나 (원작 petSmall)
			if ("PET_DOG".equals(o.act) || "PET_CAT".equals(o.act) || "PET_SMALL".equals(o.act)) {
				String k = "PET_DOG".equals(o.act) ? "d" : "PET_CAT".equals(o.act) ? "c" : (s.petSmall = petSmallKind(life));
				int[] span = PET_LIFE.get(k);
				String head = "PET_SMALL".equals(o.act) ? ("f".equals(k) ? "어항을 들여 물고기를 키운다" : petEul(PET_WORD.get(k)) + " 들인다") : o.label;
				EventChoiceDTO c = new EventChoiceDTO();
				c.setChoiceId(EngineChoice.choiceId(decIdx, i));
				c.setEventId(e.getEventId());
				c.setChoiceOrder(cs.size() + 1);
				c.setChoiceText(head + " (한 해 약 " + won(life, petCost(life, k)) + " · " + span[0] + "~" + span[1] + "년 함께)");
				cs.add(c);
				continue;
			}
			EventChoiceDTO c = new EventChoiceDTO();
			c.setChoiceId(EngineChoice.choiceId(decIdx, i));
			c.setEventId(e.getEventId());
			c.setChoiceOrder(cs.size() + 1);
			c.setChoiceText(pays && o.cost > 0 ? costLabel(o.label, won(life, optPay(life, o.cost))) : o.label);
			c.setEffHealth(o.hl);
			c.setEffHappiness(o.h);
			c.setEffKnowledge(o.kn);
			c.setEffRelation(o.re);
			cs.add(c);
		}
		if ("HOBBY".equals(d.key) && cs.size() > 4) {
			java.util.Collections.shuffle(cs, random);
			cs = new ArrayList<>(cs.subList(0, 4));
			for (int i = 0; i < cs.size(); i++) {
				cs.get(i).setChoiceOrder(i + 1);
			}
		}
		if ("WILL".equals(d.key)) {
			for (EventChoiceDTO c : cs) {
				c.setChoiceText(willWords(life, c.getChoiceText()));
			}
		}
		// 이직 제안 (원작 jobchange): 그 직업과 벌이
		if ("JOBCHANGE".equals(d.key)) {
			// 원작 jobchange: 다른 일(있으면) + 같은 일·다른 직장(지금보다 5~25% 더) + 남기
			JobDTO j = jcCandidate(life, lived);
			CountryStatDTO st = yearStat(life);
			double gdp = gdpOf(st);
			double nowRate = life.getIncomeRate() == null ? 1 : life.getIncomeRate();
			s.jcJobId = j == null ? null : j.getJobId();
			s.jcSame = 1.05 + 0.2 * new java.util.SplittableRandom(life.getLifeId() * 911L + lived).nextDouble();
			JobDTO cur = life.getJobId() == null ? null : findJob(life.getJobId()); // 결정 창을 만들 땐 jobName 이 비어 있음
			String jobNow = cur != null ? cur.getJobName() : life.getJobName() == null ? "지금 일" : life.getJobName();
			e.setLogText("다른 곳에서 함께 일하자는 제안이 왔어요. 지금은 " + ro(jobNow) + " " + Math.max(1, life.getJobYears()) + "년째 일하고 있어요."
					+ " 지금 벌이는 월 약 " + won(life, jobPay(gdp, nowRate, life.getCurrentCountry(), st) / 12) + "이에요.");
			for (EventChoiceDTO c : cs) {
				if (c.getChoiceText().startsWith("새 일로")) {
					c.setChoiceText(j == null ? null
							: ro(j.getJobName()) + " 옮긴다 (월 약 " + won(life, jobPay(gdp, j.getIncomeRate(), life.getCurrentCountry(), st) / 12) + ")");
				} else if (c.getChoiceText().startsWith("같은 일")) {
					c.setChoiceText("같은 일, 다른 직장으로 옮긴다 (" + jobNow + " · 월 약 "
							+ won(life, jobPay(gdp, nowRate * s.jcSame, life.getCurrentCountry(), st) / 12) + " · 지금보다 "
							+ Math.round((s.jcSame - 1) * 100) + "% 더)");
				}
			}
			cs.removeIf(c -> c.getChoiceText() == null);
			for (int i = 0; i < cs.size(); i++) {
				cs.get(i).setChoiceOrder(i + 1);
			}
		}
		// 은퇴 (원작 retire): 연금 안내
		if ("RETIRE".equals(d.key)) {
			CountryStatDTO st = yearStat(life);
			long pen = pensionOf(life, st);
			// 원작 retire: 연금이 없으면 '연금이 없어요' (예전엔 '연금은 한 달에 약 0원'으로 나왔음) + 모은 돈으로 몇 해를 지낼 수 있는지
			double need = medIncome(life.getCurrentCountry(), st) * 0.3 * ("Y".equals(life.getMarriedYn()) ? 0.6 : 1) - pen;
			long cash = life.getAssets();
			String money;
			if (need <= 0) {
				money = "연금으로 아껴 쓰면 생활비는 꾸릴 수 있어요.";
			} else if (cash > 0) {
				long yrs = (long) Math.floor(cash / need);
				money = "모아 둔 돈은 약 " + won(life, cash) + "이에요. "
						+ (yrs >= 30 ? "남은 생활비를 꾸리기에 넉넉해요." : yrs >= 1 ? "아껴 쓰면 약 " + yrs + "년을 지낼 수 있어요." : "한 해를 버티기에도 모자라요.");
			} else {
				money = "모아 둔 돈이 없어 일을 그만두면 생활비가 모자라요.";
			}
			e.setLogText("은퇴할 나이가 되었어요. " + (pen >= 12 * 10000 / krwPerUsd(st) ? "은퇴하면 연금으로 한 달에 약 " + won(life, pen / 12.0) + "을 받아요. " : "연금이 없어요. ")
					+ money + (life.getHealth() < 45 ? " 몸이 예전 같지 않아요." : ""));
		}
		// 내 집 (원작 buy): 가격 · 모은 돈 · 부모님 도움 · 대출, 선택지는 '모은 돈으로 산다' 또는 '대출을 끼고 산다' / '미룬다'
		if ("BUYHOUSE".equals(d.key)) {
			boolean found = false;
			ShopDTO sh = shop(life);
			for (ShopItemDTO it : sh.getItems()) {
				if ("BUY_HOUSE".equals(it.getId())) {
					long price = it.getPrice();
					long cash = Math.max(0, sh.getCash());
					long help = Math.round(homeHelp(life, yearStat(life), price, cash));
					long loan = Math.max(0, price - cash - help);
					String have = cash > 0
							? (help > 0 ? "모은 돈 " + won(life, cash) + "에 부모님께서 " + won(life, help) + "을 보태 주시기로 했어요." : "모은 돈 " + won(life, cash) + "이 있어요.")
							: (help > 0 ? "모은 돈은 없지만 부모님께서 " + won(life, help) + "을 보태 주시기로 했어요." : "모은 돈은 없어요.");
					e.setLogText("가격은 약 " + won(life, price) + " (한국 체감 금액). " + have + " "
							+ (loan > 0 ? "나머지 " + won(life, loan) + "은 대출로 갚아야 해요." : "대출 없이 살 수 있어요."));
					// 선택지에도 집값을 (11/23 동현님: 집값이 안 보임) - 원작 sub 는 '행복 +10' 뿐이지만 본문을 안 읽어도 알게
					cs.get(0).setChoiceText(loan > 0 ? "대출을 끼고 산다 (약 " + won(life, price) + " · 대출 " + won(life, loan) + " · 행복 +10)"
							: "모은 돈으로 산다 (약 " + won(life, price) + " · 행복 +10)");
					// 원작: 대출이 필요하면 '작은 집을 대출 없이 산다'(집값 55%급)를 가운데에 (살 수 있을 때만)
					String smId = String.valueOf(EngineChoice.choiceId(decIdx, 2));
					EventChoiceDTO sm = null;
					for (EventChoiceDTO c : cs) {
						if (smId.equals(String.valueOf(c.getChoiceId()))) {
							sm = c;
						}
					}
					if (sm != null) {
						cs.remove(sm);
						long smPrice = Math.round(price * 0.69);
						long smHelp = Math.round(homeHelp(life, yearStat(life), smPrice, cash));
						if (loan > 0 && cash + smHelp >= smPrice) {
							s.houseSm = smPrice;
							sm.setChoiceText("작은 집을 대출 없이 산다 (약 " + won(life, smPrice) + " · 행복 +10)");
							cs.add(1, sm);
						}
						for (int ci = 0; ci < cs.size(); ci++) {
							cs.get(ci).setChoiceOrder(ci + 1);
						}
					}
					found = true;
				}
			}
			if (!found) {
				return; // 집값을 못 구하면 묻지 않음
			}
		}
		// 빚 (원작 loan): 지금 빚
		if ("LOAN".equals(d.key)) {
			e.setLogText("빚이 " + won(life, -life.getAssets()) + "으로 불어나 이자만 갚기도 버거워요.");
		}
		// 반려동물이 아파요 (원작 petsick): 이름과 치료비
		if ("PETSICK".equals(d.key)) {
			String[] pp = petOf(life);
			if (pp == null) {
				return;
			}
			s.petVet = petCost(life, pp[1]) * 1.5;
			e.setLogText(iga(pp[0]) + " 며칠째 밥을 먹지 않고 축 처져 있어요.");
			cs.get(0).setChoiceText("치료한다 (약 " + won(life, s.petVet) + " · 나을 수도, 아닐 수도)");
		}
		// 자녀 결정 (원작 babycare · kidschool · discipline · kiduni · kidwed): 이름·나이·금액
		if (EngineChoice.FAM.equals(d.group)) {
			String nm = s.kidNm == null ? "아이" : s.kidNm;
			CountryStatDTO fst = yearStat(life);
			double fmed = medIncome(life.getCurrentCountry(), fst);
			String fg = groupOf(life.getCurrentCountry(), fst);
			final long fcash = Math.max(0, life.getAssets());
			switch (d.key) {
			case "BABYCARE":
				s.careCost = fmed * 0.12;
				e.setLogText(kj(nm, "이", "가") + " 태어났어요. 일을 이어 가려면 아이를 돌볼 사람이 필요해요.");
				for (EventChoiceDTO c : cs) {
					if (c.getChoiceText().startsWith("어린이집")) {
						c.setChoiceText("어린이집이나 돌보미에게 맡긴다 (해마다 약 " + won(life, s.careCost) + ")");
					}
				}
				break;
			case "KIDSCHOOL": {
				s.ksFee = fmed * ("hi".equals(fg) ? 0.25 : 0.15);
				double hh = Math.max(0, life.getIncome()) + spouseIncomeOf(life);
				long pc = hh > 0 ? Math.round(s.ksFee / hh * 100) : 0;
				e.setTitle(nm + "의 학교를 정할 때예요");
				e.setLogText(kj(nm, "이", "가") + " 내년이면 학교에 가요. 가까운 공립학교는 학비가 거의 들지 않고, 사립학교는 아이 한 명에 한 해 약 "
						+ won(life, s.ksFee) + "이 들어요."
						+ (hh <= 0 ? " 지금 집에 들어오는 벌이가 없어요." : pc >= 100 ? " 집안의 한 해 벌이보다 많아요." : pc >= 15 ? " 집안 벌이의 약 " + pc + "%예요." : ""));
				for (EventChoiceDTO c : cs) {
					if (c.getChoiceText().startsWith("사립학교에")) {
						c.setChoiceText("사립학교에 보낸다 (아이마다 해마다 약 " + won(life, s.ksFee) + " · 대학 진학 가능성 조금↑)");
					}
				}
				break;
			}
			case "DISCIPLINE":
				e.setLogText(s.kidAge + "살 " + kj(nm, "이", "가") + (s.kidAge <= 6 ? " 몇 번을 말해도 듣지 않고 떼를 써요." : " 몇 번을 말해도 말대꾸를 하며 고집을 부려요.")
						+ " 오늘따라 지치고 화가 나요.");
				if (cpBan(life)) {
					for (EventChoiceDTO c : cs) {
						if (c.getChoiceText().startsWith("매를 든다")) {
							c.setChoiceText("매를 든다 (법으로 금지된 일 · 아이와 멀어질 수 있다)");
						}
					}
				}
				break;
			case "KIDUNI": {
				s.kuAmt = fmed * ("hi".equals(fg) ? 1.6 : "eca".equals(fg) ? 0.8 : 1.0);
				final double ku = s.kuAmt;
				e.setTitle(kj(nm, "이", "가") + " 대학에 합격했어요");
				e.setLogText("4년 동안 등록금과 생활비로 약 " + won(life, ku) + " (한국 체감)이 들어요. " + savedLine(life));
				cs.removeIf(c -> c.getChoiceText().startsWith("내가 다 대 준다") && fcash < ku || c.getChoiceText().startsWith("반은 내가") && fcash < ku / 2);
				for (EventChoiceDTO c : cs) {
					if (c.getChoiceText().startsWith("내가 다 대 준다")) {
						c.setChoiceText("내가 다 대 준다 (약 " + won(life, ku) + ")");
					} else if (c.getChoiceText().startsWith("반은 내가")) {
						c.setChoiceText("반은 내가, 반은 학자금 대출로 (약 " + won(life, ku / 2) + ")");
					}
				}
				break;
			}
			case "KIDWED": {
				String cc = life.getCurrentCountry();
				boolean trad = "sa".equals(fg) || "mena".equals(fg) || "CHN".equals(cc) || "KOR".equals(cc) || "VNM".equals(cc);
				s.kwAmt = fmed * (trad ? 1 : 0.3);
				final double kw = s.kwAmt;
				e.setTitle(kj(nm, "이", "가") + " 결혼해요");
				e.setLogText(s.kidAge + "살 " + kj(nm, "이", "가") + " 평생을 함께할 사람을 데려왔어요."
						+ (trad ? " 이 나라에서는 부모가 결혼 비용을 크게 보태는 일이 흔해요." : "")
						+ " 결혼식과 새 살림에 약 " + won(life, kw) + " (한국 체감)이 들어요. " + savedLine(life));
				cs.removeIf(c -> c.getChoiceText().startsWith("결혼 비용을 넉넉히") && fcash < kw || c.getChoiceText().startsWith("형편껏 조금") && fcash < kw * 0.25);
				for (EventChoiceDTO c : cs) {
					if (c.getChoiceText().startsWith("결혼 비용을 넉넉히")) {
						c.setChoiceText("결혼 비용을 넉넉히 보탠다 (약 " + won(life, kw) + ")");
					} else if (c.getChoiceText().startsWith("형편껏 조금")) {
						c.setChoiceText("형편껏 조금 보탠다 (약 " + won(life, kw * 0.25) + ")");
					}
				}
				break;
			}
			default:
				break;
			}
			for (int ci = 0; ci < cs.size(); ci++) {
				cs.get(ci).setChoiceOrder(ci + 1);
			}
		}
		// 가족이 돈 부탁·친구 돈·보증: 금액
		double askMed = medIncome(life.getCurrentCountry(), yearStat(life));
		if ("LEND".equals(d.key)) {
			cs.get(0).setChoiceText("빌려준다 (약 " + won(life, askMed * 0.08) + ")");
		}
		if ("COSIGN".equals(d.key)) {
			e.setLogText("가까운 사람이 대출을 받으려는데 보증을 서 달라고 해요. 그 사람이 못 갚으면 약 " + won(life, askMed * 0.6) + "을 내가 갚아야 해요.");
		}
		if (cs.size() < 2) {
			return; // 고를 게 하나뿐이면 띄우지 않음
		}
		e.setChoices(cs);
		EngPend p = new EngPend();
		p.decIdx = decIdx;
		p.age = lived;
		p.view = e;
		s.pend = p;
		life.setNeedChoice(true);
		life.setPendingEvent(e);
		life.setChoiceCosts(new ArrayList<>());
	}

	// 엔진 선택지의 돈은 어른이고 벌이나 재산이 있을 때만 냄 (아이·빈털터리는 시간만 씀)
	private boolean engineCostPays(LifeDTO life, int age) {
		return age >= 18 && (life.getIncome() > 0 || life.getAssets() > 0);
	}

	// 아직 안 고른 엔진 선택지가 있는지 (나이가 맞지 않는 오래된 것은 버림)
	private boolean engineChoiceWaiting(LifeDTO life) {
		EngState s = engStates.get(life.getLifeId());
		if (s == null || s.pend == null) {
			return false;
		}
		if (s.pend.age != life.getCurrentAge() - 1) {
			s.pend = null;
			return false;
		}
		return true;
	}

	// 이어하기·새로고침: 기다리는 엔진 선택지가 있으면 선택 창 다시 띄우기
	private void restoreEngineChoice(LifeDTO life) {
		if (!engineChoiceWaiting(life)) {
			return;
		}
		EngState s = engStates.get(life.getLifeId());
		life.setNeedChoice(true);
		life.setPendingEvent(s.pend.view);
		life.setChoiceCosts(new ArrayList<>());
	}

	// 엔진 선택지 고르기 (POST /api/engine/choice 에 음수 choiceId)
	private LifeDTO chooseEngine(LifeDTO life, int choiceId, String petName) {
		if (!"PLAYING".equals(life.getStatus()) || !engineChoiceWaiting(life)) {
			return null;
		}
		EngState s = engStates.get(life.getLifeId());
		EngPend p;
		synchronized (s) {
			p = s.pend;
			if (p == null || p.decIdx != EngineChoice.decIdxOf(choiceId)) {
				return null;
			}
			boolean offered = false;
			for (EventChoiceDTO c : p.view.getChoices()) {
				if (c.getChoiceId() != null && c.getChoiceId() == choiceId) {
					offered = true;
				}
			}
			if (!offered) {
				return null;
			}
			s.pend = null; // 연타해도 한 번만
		}
		EngineChoice.Dec d = EngineChoice.ALL.get(p.decIdx);
		EngineChoice.Opt o = d.opts[EngineChoice.optIdxOf(choiceId)];
		if (!"BUYHOUSE_S".equals(o.act)) {
			s.houseSm = 0; // 작은 집 값은 그 선택을 고를 때만 (나중에 '돈 쓰기'로 집을 살 때 작은 값이 쓰이지 않게)
		}
		int age = p.age;
		long startAssets = life.getAssets();
		life.setMoneyFlow(new ArrayList<>());
		// 고른 뒤 능력치가 얼마나 바뀌었는지 (원작처럼 결과 줄에 효과 칩)
		double hl0 = life.getHealth(), h0 = life.getHappiness(), kn0 = life.getKnowledge(), re0 = life.getRelation();
		boolean invested = false;

		boolean bad = o.badP > 0 && random.nextDouble() < o.badP;
		if (bad && d.badDeath > 0) {
			s.carryDeath = s.carryDeath + d.badDeath;
			s.carryCause = d.badCause;
		}
		int[] eff = bad && o.badEff != null ? o.badEff : new int[] { o.h, o.hl, o.re, o.kn };
		life.setHappiness(life.getHappiness() + eff[0]);
		life.setHealth(life.getHealth() + eff[1]);
		life.setRelation(life.getRelation() + eff[2]);
		life.setKnowledge(life.getKnowledge() + eff[3]);
		String text = o.results[bad && o.results.length > 1 ? 1 : 0];
		String paidNote = ""; // 든 돈은 문장 맨 끝에 (결과 문장 가운데 끼지 않게)
		if (o.cost > 0 && engineCostPays(life, age)) {
			long pay = Math.round(optPay(life, o.cost));
			if (pay > 0) {
				life.setAssets(life.getAssets() - pay);
				book(life, d.title, -pay);
				paidNote = " (−" + won(life, pay) + ")";
			}
		}
		if (o.temper != null) {
			s.temper = o.temper;
		}
		// 취미 (원작 hobby): 고른 취미는 OWNS 에 HOB-코드 하나 (새로 고르면 바뀜)
		if (o.act != null && o.act.startsWith("HOB_")) {
			java.util.LinkedHashSet<String> owns = ownSet(life);
			owns.removeIf(x -> x.startsWith("HOB-"));
			owns.add("HOB-" + o.act.substring(4));
			life.setOwns(String.join(",", owns));
		}
		// 병역 (원작 service)
		if (o.act != null && o.act.startsWith("SVC_")) {
			text = svcChoose(life, s, o.act, age);
		}
		// 반려동물 (원작 petq): 조르기는 허락받을 확률, 어른은 바로 들임
		if ("PET_BEG".equals(o.act)) {
			if (random.nextDouble() < clamp(0.25 + 0.08 * (life.getWealthClass() - 3) + petOwnRate(life) * 0.6, 0.1, 0.8)) {
				text = text + " " + petAdopt(life, "d", age, petName);
			} else {
				life.setHappiness(life.getHappiness() - 2);
				text = text + " 부모님은 아직은 안 된다고 하셨다.";
			}
		} else if ("PET_DOG".equals(o.act) || "PET_CAT".equals(o.act)) {
			text = petAdopt(life, "PET_DOG".equals(o.act) ? "d" : "c", age, petName);
		} else if ("PET_SMALL".equals(o.act)) {
			text = petAdopt(life, s.petSmall != null ? s.petSmall : petSmallKind(life), age, petName);
		} else if ("PET_NEVER".equals(o.act)) {
			s.done.put("PET_ADOPT", 99); // 다시 묻지 않음 (원작 never)
		}
		// 돈 굴리기 (원작 invest): 고른 곳에 맡김 ('돈 쓰기'의 굴리기와 같은 값)
		if (o.act != null && o.act.startsWith("INV_")) {
			life.setInvestType(o.act.substring(4));
			invested = true;
		}
		// 배우자 주식 권유 (원작 pstock): 모은 돈의 절반, 45% 크게 잃음 · 30% 제자리 · 25% 꽤 벎
		if ("PSTOCK_GO".equals(o.act)) {
			text = text + " " + pstock(life);
		}
		// ---- 빚·집안일·반려동물 (원작 loan · hmwork · petsick · mandret) ----
		if ("LOAN_FAMILY".equals(o.act) && life.getAssets() < 0) {
			int q = (int) clamp(life.getWealthClass(), 1, 5);
			double lm = medIncome(life.getCurrentCountry(), yearStat(life));
			double fam = !"N".equals(life.getParentAlive()) ? lm * (0.2 + 0.15 * q) : lm * 0.15;
			double got = Math.min(-life.getAssets(), fam);
			life.setAssets(life.getAssets() + Math.round(got));
			book(life, "가족이 모아 준 돈", got);
			text = text + " 가족과 친척이 형편껏 모아 준 " + won(life, got) + (life.getAssets() >= 0 ? "으로 빚을 모두 갚았다." // '남은 빚은 0원이다'가 나왔음
					: "으로 빚 일부를 갚았다. 남은 빚은 " + won(life, -life.getAssets()) + "이다.");
		} else if ("LOAN_SHARK".equals(o.act) && life.getAssets() < 0) {
			double more = -life.getAssets() * 0.1;
			life.setAssets(life.getAssets() - Math.round(more));
			book(life, "고리대금 이자", -more);
			text = text + " 이자로 빚이 " + won(life, more) + " 늘었다. 이자가 한 해 20%가 넘어, 다 갚을 때까지 빚이 빠르게 붇는다.";
		} else if ("HMWORK".equals(o.act)) {
			if (life.getJobStatus() == null) {
				life.setJobStatus("UNEMPLOYED");
			}
			life.setPrepScore(2); // 마음먹고 찾으니 조금 나은 자리가 들어옴
		} else if ("PET_TREAT".equals(o.act) || "PET_LETGO".equals(o.act)) {
			String[] pp = petOf(life);
			java.util.LinkedHashSet<String> owns = ownSet(life);
			if (pp != null) {
				boolean live = "PET_TREAT".equals(o.act) && random.nextDouble() < 0.65;
				if ("PET_TREAT".equals(o.act)) {
					life.setAssets(life.getAssets() - Math.round(s.petVet));
					book(life, "반려동물 치료비", -s.petVet);
				}
				owns.removeIf(x -> x.startsWith("PET-"));
				if (live) {
					int at = Integer.parseInt(pp[2]);
					int span = Math.max(Integer.parseInt(pp[3]), age - at + 1 + random.nextInt(3));
					owns.add("PET-" + pp[1] + "-" + pp[0] + "-" + at + "-" + span);
					life.setHappiness(life.getHappiness() + 2);
					text = text + " 치료를 받은 " + iga(pp[0]) + " 다시 기운을 차렸다. (−" + won(life, s.petVet) + ")";
				} else {
					life.setHappiness(life.getHappiness() - ("PET_TREAT".equals(o.act) ? 7 : 6));
					text = "PET_TREAT".equals(o.act) ? text + " 치료를 받았지만 " + iga(pp[0]) + " 끝내 떠났다. (−" + won(life, s.petVet) + ")"
							: text + " " + pp[0] + "의 빈자리가 오래 남았다.";
				}
				life.setOwns(String.join(",", owns));
			}
		} else if ("RETIRE_REHIRE".equals(o.act)) {
			if (life.getIncomeRate() != null) {
				life.setIncomeRate(life.getIncomeRate() * 0.7);
			}
			s.retUntil = age + 5;
		}
		// ---- 학교에 보내 달라고 조름 (원작 noschool): 형편이 나을수록, 도시일수록 허락받기 쉬움 ----
		if ("NOSCHOOL_BEG".equals(o.act)) {
			double ok = clamp(0.25 + 0.12 * (life.getWealthClass() - 1) + ("Y".equals(life.getIsUrban()) ? 0.1 : 0), 0.1, 0.8);
			if (random.nextDouble() < ok) {
				life.setEduLevel("ELEM");
				life.setSchoolStatus("ENROLLED");
				life.setDropReason(null);
				life.setHappiness(life.getHappiness() + 6);
				text = text + " 부모님이 허락하셨다. 늦게나마 초등학교에 입학했다!";
			} else {
				life.setHappiness(life.getHappiness() - 5);
				text = text + " 학비와 일손 문제로 끝내 학교에 가지 못했다.";
			}
		}
		// ---- 대학 갈림길 (원작 major · retake) ----
		if (o.act != null && o.act.startsWith("MAJOR_")) {
			s.major = o.act.substring(6);
		}
		if ("RETAKE_NO".equals(o.act)) {
			markEngineDone(s, EngineChoice.byKey("SCHOOL_UNI"), age); // 다음 해 다시 보지 않음
		}
		// ---- 삶의 갈림길 (11/23, 원작 결정) ----
		boolean logged = false; // 이미 기록을 남긴 결정 (집 사기는 돈 쓰기가 기록)
		double am = medIncome(life.getCurrentCountry(), yearStat(life));
		if ("RANKUP".equals(o.act) && life.getIncomeRate() != null) {
			life.setIncomeRate(life.getIncomeRate() * 1.2);
			text = text + " 벌이는 늘었지만 퇴근이 늦어졌다.";
		} else if ("JOBCHANGE".equals(o.act)) {
			JobDTO j = s.jcJobId == null ? null : findJob(s.jcJobId);
			if (j != null) {
				life.setJobId(j.getJobId());
				life.setIncomeRate(j.getIncomeRate());
				life.setJobYears(0);
				life.setJobName(j.getJobName());
				addExperience(life, j.getJobId());
				text = text + " " + j.getJobName() + " 일을 시작했다.";
			}
		} else if ("JOBCHANGE_SAME".equals(o.act) && life.getIncomeRate() != null) {
			// 같은 일·다른 직장 (원작): 벌이만 오르고 경력은 그대로
			life.setIncomeRate(life.getIncomeRate() * (s.jcSame > 1 ? s.jcSame : 1.1));
			JobDTO cur = life.getJobId() == null ? null : findJob(life.getJobId());
			if (cur != null) {
				text = text + " 하는 일은 " + cur.getJobName() + " 그대로다.";
			}
		} else if ("JOBCHANGE_STAY".equals(o.act) && life.getIncomeRate() != null) {
			life.setIncomeRate(life.getIncomeRate() * 1.01); // 원작: 남으면 직급 조금 (pct +0.01)
		} else if ("QUIT_JOB".equals(o.act) && "EMPLOYED".equals(life.getJobStatus())) {
			life.setJobStatus("UNEMPLOYED");
			life.setJobYears(0);
		} else if ("RETIRE_NOW".equals(o.act)) {
			life.setJobStatus("RETIRED");
			life.setPension(pensionOf(life, yearStat(life)));
			text = text + (life.getJobName() == null ? "" : " " + life.getJobName() + " 일을 마쳤다.") + pensionText(life);
		} else if ("RETIRE_LESS".equals(o.act)) {
			if (life.getIncomeRate() != null) {
				life.setIncomeRate(life.getIncomeRate() * 0.5);
			}
			s.retUntil = age + 5;
		} else if ("RETIRE_WORK".equals(o.act)) {
			s.retUntil = age + 5;
		} else if ("BUYHOUSE".equals(o.act) || "BUYHOUSE_S".equals(o.act)) {
			ShopDTO bought = buy(life, "BUY_HOUSE", age);
			if (bought != null && bought.getMessage() != null) {
				text = bought.getMessage();
				logged = true; // buy 가 기록을 남김
			} else {
				text = "집을 알아봤지만 이번에는 사지 못했다.";
			}
		} else if ("CITY".equals(o.act)) {
			life.setIsUrban("Y");
		} else if ("CARE_HOME".equals(o.act)) {
			EngineStory.mark(life.getLifeId(), "par_live"); // 모시고 사니 '명절에 부모님 댁에 선물'이 나오지 않게
		} else if ("LEND".equals(o.act)) {
			s.lendAmt = am * 0.08;
			s.lendAt = age;
			s.lendOk = random.nextDouble() < 0.55;
			life.setAssets(life.getAssets() - Math.round(s.lendAmt));
			book(life, "친구에게 빌려준 돈", -s.lendAmt);
			text = text + " (−" + won(life, s.lendAmt) + ")";
		} else if ("COSIGN".equals(o.act)) {
			s.csAmt = am * 0.6;
			s.csEnd = age + 3 + random.nextInt(4);
		} else if ("SMOKE_TRY".equals(o.act)) {
			if (random.nextDouble() < 0.6) {
				s.smoker = true;
				text = text + " 친구들과 어울려 담배를 피우기 시작했다. 한 대 피우면 마음이 놓이는 것 같았다.";
			} else {
				text = text + " 몇 번 피우다 그만두었다.";
			}
		} else if ("QUIT_SMOKE".equals(o.act)) {
			// 원작 quit: 13%만 성공 (6개월 넘게 끊는 사람 약 13%, CDC 2015)
			if (random.nextDouble() < 0.13) {
				s.smoker = false;
				life.setHealth(life.getHealth() + 4);
				life.setHappiness(life.getHappiness() + 2);
				text = "금연에 성공했다! 담배를 끊었다.";
			} else {
				life.setHappiness(life.getHappiness() - 3);
				text = "금연에 실패했다.";
			}
		} else if ("DRINK_GO".equals(o.act)) {
			if (random.nextDouble() < 0.5) {
				s.alc = true;
				text = text + " 술 없이는 잠들지 못하는 날이 늘었다.";
			} else {
				text = text + " 술이 늘었다가, 스스로 줄였다.";
			}
		} else if ("QUIT_DRINK".equals(o.act)) {
			if (random.nextDouble() < 0.5) {
				s.alc = false;
				life.setHealth(life.getHealth() + 3);
				text = text + " 몇 달을 버틴 끝에 술을 끊었다.";
			} else {
				text = text + " 몇 달 뒤 다시 잔을 들었다.";
			}
		} else if ("LOTTO_WEEKLY".equals(o.act)) {
			s.lotto = true;
			s.lottoAt = age;
		} else if ("LOTTO_ONCE".equals(o.act)) {
			text = text + (random.nextDouble() < 0.05 ? " 가장 작은 금액에 당첨됐다. 그 돈으로 한 장을 더 샀다가 꽝이었다."
					: " 역시 꽝이었다. 그래도 추첨까지 일주일 동안은 즐거웠다.");
		} else if ("GAMBLE".equals(o.act)) {
			boolean win = random.nextDouble() < 0.3;
			double g = win ? am * 0.08 : -am * 0.06;
			life.setAssets(life.getAssets() + Math.round(g));
			book(life, win ? "도박으로 딴 돈" : "도박으로 잃은 돈", g);
			if (random.nextDouble() < (win ? 0.2 : 0.13)) {
				s.gamb = true;
				text = text + (win ? " 처음엔 땄다. 그 기분을 잊을 수 없어 계속하게 되었다." : " 잃은 돈을 되찾고 싶어 다시, 또다시 손을 댔다.");
			} else {
				text = text + (win ? " 조금 땄지만, 그걸로 그만두었다." : " 조금 잃고 나서 그만두었다.");
			}
			text = text + " (" + wonSigned(life, g) + ")";
		} else if ("QUIT_GAMBLE".equals(o.act)) {
			if (random.nextDouble() < 0.6) {
				s.gamb = false;
				text = text + " 몇 달 동안 상담을 받으며 도박을 끊었다.";
			} else {
				text = text + " 상담을 받았지만 다시 판에 앉았다.";
			}
		}
		// 유언장 (원작 will): OWNS 에 WILL-방식-나이 하나 (세상을 떠날 때 inheritText 가 읽음)
		if (o.act != null && o.act.startsWith("WILL_") && !"WILL_LAW".equals(o.act)) {
			java.util.LinkedHashSet<String> owns = ownSet(life);
			owns.removeIf(x -> x.startsWith("WILL-"));
			owns.add("WILL-" + o.act.substring(5).toLowerCase() + "-" + age); // 남길 곳은 세상을 떠날 때 마지막 일·학력으로 (OWNS 400바이트지만 짧게)
			life.setOwns(String.join(",", owns));
			text = willWords(life, text);
			// 유언장에 직접 적은 말 (화면 '남길 말' 칸, 비우면 없음)
			String words = petName == null ? "" : petName.replaceAll("[\\r\\n\"“”]", " ").trim();
			if (!words.isEmpty()) {
				text = text + " 유언장 끝에 이렇게 적었다. “" + (words.length() > 100 ? words.substring(0, 100) : words) + "”";
			}
		}
		if (o.act != null && d.key.startsWith("SCHOOL_")) {
			int nx = java.util.Arrays.asList(levels).indexOf(d.key.substring("SCHOOL_".length()));
			if ("ENROLL".equals(o.act) && nx == 3 && s.major == null) {
				s.majorAsk = true; // 대학 첫해 끝에 전공 고르기
			}
			if ("ENROLL".equals(o.act) && nx >= 0) {
				life.setEduLevel(levels[nx]);
				life.setSchoolStatus("ENROLLED");
				life.setDropReason(null);
			}
		}
		// 자녀 결정 결과 (원작 문장, 아이 이름으로)
		if (o.act != null && EngineChoice.FAM.equals(d.group)) {
			String nm = s.kidNm == null ? "아이" : s.kidNm;
			long fpay = 0;
			switch (o.act) {
			case "BC_ME":
				if ("EMPLOYED".equals(life.getJobStatus())) {
					life.setJobStatus("UNEMPLOYED");
					life.setJobYears(0);
				}
				text = "일을 그만두고 " + kj(nm, "을", "를") + " 돌보기로 했다. 아이와 보내는 하루가 길고도 짧았다.";
				break;
			case "BC_PT":
				if (life.getIncomeRate() != null) {
					life.setIncomeRate(life.getIncomeRate() * 0.6);
					s.ptBack = age + 3;
					s.ptJob = life.getJobId();
				}
				text = "일을 줄여 시간제로 일하며 " + kj(nm, "을", "를") + " 돌보기로 했다.";
				break;
			case "BC_PARTNER":
				s.spOffUntil = age + 3;
				text = "배우자가 몇 해 일을 쉬고 " + kj(nm, "을", "를") + " 돌보기로 했다.";
				break;
			case "BC_GP":
				text = "부모님께 " + kj(nm, "을", "를") + " 맡기고 일을 이어 갔다. 부모님께 신세를 졌다.";
				break;
			case "BC_PAY":
				s.careUntil = age + 3;
				text = kj(nm, "을", "를") + " 어린이집에 맡기고 일을 이어 갔다.";
				break;
			case "KS_PUB":
				text = kj(nm, "을", "를") + " 동네 공립학교에 보냈다.";
				break;
			case "KS_PRIV":
				s.kidPriv = true;
				text = kj(nm, "을", "를") + " 사립학교에 보냈다.";
				break;
			case "DS_HIT":
				text = "매를 들었다. " + kj(nm, "이", "가") + (s.kidAge <= 6 ? " 울음을 그쳤지만, 한동안 내 눈을 피했다." : " 아무 말도 하지 않았지만, 한동안 내 눈을 피했다.");
				break;
			case "DS_CALM":
				text = s.kidAge <= 6
						? ("Y".equals(life.getIsUrban()) ? "잠시 베란다에 나가 숨을 골랐다." : "잠시 마당에 나가 숨을 골랐다.") + " 돌아와 보니 " + nm + "도 울음을 그쳐 있었다."
						: "잠시 밖에 나가 숨을 골랐다. 돌아와 보니 " + nm + "도 한결 누그러져 있었다.";
				break;
			case "DS_TALK":
				if (random.nextDouble() < clamp(0.6 + (life.getRelation() - 50) / 300, 0.5, 0.85)) {
					life.setRelation(life.getRelation() + 2);
					life.setHappiness(life.getHappiness() + 1);
					text = kj(nm, "과", "와") + " 마주 앉아 이야기했다. 서툴러도 약속을 하나 정했다.";
				} else {
					life.setRelation(life.getRelation() + 1);
					text = "타일렀지만 " + kj(nm, "이", "가") + " 한동안 또 " + (s.kidAge <= 6 ? "떼를 썼다" : "고집을 부렸다") + ". 그래도 날마다 조금씩 나아졌다.";
				}
				break;
			case "KU_PAY":
				fpay = Math.round(s.kuAmt);
				text = nm + "의 등록금을 내주었다. 입학식 날 오래 손을 흔들었다.";
				break;
			case "KU_HALF":
				fpay = Math.round(s.kuAmt / 2);
				text = kj(nm, "과", "와") + " 등록금을 반씩 나눠 내기로 했다.";
				break;
			case "KU_SELF":
				if (random.nextDouble() < 0.25) {
					long o2 = Math.round(s.kuAmt * 0.4);
					if (life.getAssets() >= o2 && random.nextDouble() < clamp(life.getAssets() / Math.max(1, s.kuAmt), 0, 1)) {
						fpay = o2;
						life.setHappiness(life.getHappiness() + 1);
						text = kj(nm, "이", "가") + " 학비가 모자라다며 도움을 청해 와, 남은 학비를 보태 주었다.";
					} else {
						life.setHappiness(life.getHappiness() - 6);
						life.setRelation(life.getRelation() - 2);
						text = kj(nm, "은", "는") + " 학비를 감당하지 못해 한 학기 만에 학교를 그만두었다.";
					}
				} else {
					life.setHappiness(life.getHappiness() + 2);
					text = kj(nm, "은", "는") + " 아르바이트를 하며 스스로 학비를 벌었다.";
				}
				break;
			case "KW_HELP":
				fpay = Math.round(s.kwAmt);
				text = nm + "의 결혼식에서 하객들에게 인사하느라 목이 쉬었다.";
				break;
			case "KW_LITTLE":
				fpay = Math.round(s.kwAmt * 0.25);
				text = kj(nm, "이", "가") + " 결혼했다. 작은 정성을 보탰다.";
				break;
			case "KW_NONE":
				text = kj(nm, "이", "가") + " 결혼했다. 손을 꼭 잡고 축복해 주었다.";
				break;
			default:
				break;
			}
			if (fpay > 0) {
				life.setAssets(life.getAssets() - fpay);
				book(life, o.act.startsWith("KU") ? "자녀 대학 학비" : "자녀 결혼 비용", -fpay);
				if (fpay * krwPerUsd(yearStat(life)) >= 10000) {
					paidNote = " (−" + won(life, fpay) + ")";
				}
			}
		}
		markEngineDone(s, d, age);
		fixStats(life);
		if (!logged) {
			saveEventLog(life, age, null, null, text + paidNote);
		}
		closeBook(life, startAssets);

		life.setNeedChoice(false);
		life.setPendingEvent(null);
		life.setChoiceCosts(null);
		List<EventResultDTO> list = new ArrayList<>();
		EventResultDTO res = noteEvent(text + paidNote);
		res.setEffHealth((int) Math.round(life.getHealth() - hl0));
		res.setEffHappiness((int) Math.round(life.getHappiness() - h0));
		res.setEffKnowledge((int) Math.round(life.getKnowledge() - kn0));
		res.setEffRelation((int) Math.round(life.getRelation() - re0));
		list.add(res);
		life.setEvents(list);
		if ("QUIT".equals(o.act)) {
			showOffers(life, yearStat(life)); // 학교 대신 일을 고르면 바로 일자리 제안 (JOB_MIN_AGE 이상일 때만)
		}
		engineDao.updateLifeYear(life);
		if (invested) {
			engineDao.updateShop(life); // INVEST_TYPE 은 updateShop 이 저장
		}
		fillRegion(life);
		return life;
	}

	// 배우자 주식 권유 결과 (원작 pstock 확률·크기 그대로)
	private String pstock(LifeDTO life) {
		double t = Math.max(0, life.getAssets()) * 0.5;
		double n = random.nextDouble();
		if (n < 0.45) {
			long loss = Math.round(t * (0.3 + random.nextDouble() * 0.4));
			life.setAssets(life.getAssets() - loss);
			book(life, "주식 (배우자 권유)", -loss);
			life.setHappiness(life.getHappiness() - 8);
			life.setRelation(life.getRelation() - 4);
			fixStats(life);
			return "주가가 곤두박질쳤다. 넣은 돈의 상당 부분을 잃었다. (−" + won(life, loss) + ")";
		}
		if (n < 0.75) {
			life.setHappiness(life.getHappiness() - 1);
			fixStats(life);
			return "주가는 오르락내리락하다 제자리로 돌아왔다. 마음고생만 했다.";
		}
		long gain = Math.round(t * (0.15 + random.nextDouble() * 0.35));
		life.setAssets(life.getAssets() + gain);
		book(life, "주식 (배우자 권유)", gain);
		life.setHappiness(life.getHappiness() + 5);
		life.setRelation(life.getRelation() + 2);
		fixStats(life);
		return "주가가 올라 꽤 벌었다. (+" + won(life, gain) + ")";
	}

	// 세상을 떠날 때 재산이 어떻게 나뉘었는지 (원작 상속: 유언장 · 나라별 법 · 빚)
	// 유언으로 남길 곳 (원작 giveList · GIVE_N): 의료 일 → 병원비 기금, 가르친 일·대학 → 장학금, 그 밖 → 어린이 단체
	private static final Map<String, String> GIVE_N = Map.of("med", "아픈 이웃의 병원비를 돕는 기금", "edu", "형편이 어려운 학생들의 장학금",
			"child", "어린이를 돕는 단체");

	private String giveKey(LifeDTO life) {
		String j = life.getJobName() == null ? "" : life.getJobName();
		if (j.matches(".*(의사|간호|약사|요양|의료|치과|병원).*")) {
			return "med";
		}
		if (j.matches(".*(교사|교수|강사).*") || "UNI".equals(life.getEduLevel()) || "GRAD".equals(life.getEduLevel())) {
			return "edu";
		}
		return "child";
	}

	// 첫째 자녀 이름 (살아 있는 아이 가운데 맨 처음)
	private String firstKidName(LifeDTO life) {
		try {
			for (FamilyMemberDTO m : family(life, yearStat(life))) {
				if (("아들".equals(m.getRole()) || "딸".equals(m.getRole())) && m.isAlive() && m.getName() != null) {
					return m.getName();
				}
			}
		} catch (RuntimeException e) {
			// 이름을 못 찾으면 '첫째'
		}
		return "첫째";
	}

	// 유언장 문구의 빈칸: '구호 단체' → 남길 곳 이름, '{첫째}' → 첫째 이름
	private String willWords(LifeDTO life, String t) {
		if (t == null) {
			return null;
		}
		if (t.contains("{첫째}")) {
			t = t.replace("{첫째}", firstKidName(life));
		}
		return t.replace("구호 단체", GIVE_N.get(giveKey(life)));
	}

	private String inheritText(LifeDTO life) {
		long a = life.getAssets();
		boolean sp = "Y".equals(life.getMarriedYn());
		int kids = life.getChildCount();
		if (a == 0 || a < 0 && life.getCurrentAge() < 18) {
			return null; // 아기·아이: 남긴 것도 갚을 빚도 없음 (0세에 '빚이 많아' 줄이 나왔음)
		}
		if (a <= 0) {
			return kids > 0 ? "남긴 재산보다 빚이 많아 자녀들은 상속을 포기했다. 빚은 대물림되지 않았다."
					: "남긴 재산보다 빚이 많아, 남은 것은 빚을 갚는 데 쓰였다.";
		}
		if (life.getCurrentAge() < 18 && !sp && kids == 0) {
			// 아이·청소년: '배우자도 자녀도 없어'는 어색해서 (15세 사망에 나왔음)
			return "모아 둔 돈 약 " + won(life, a) + "은 " + ("Y".equals(life.getParentAlive()) ? "부모님께" : "형제자매와 친척에게") + " 돌아갔다.";
		}
		String amt = "남긴 재산은 약 " + won(life, a) + ".";
		String will = null;
		String to = GIVE_N.get(giveKey(life));
		int at = 0;
		for (String x : ownSet(life)) {
			if (x.startsWith("WILL-")) {
				String[] p = x.split("-");
				will = p[1];
				at = p.length > 2 ? Integer.parseInt(p[2]) : 0;
				if (p.length > 3 && GIVE_N.containsKey(p[3])) {
					to = GIVE_N.get(p[3]);
				}
			}
		}
		boolean heirs = sp || kids > 0;
		if (will != null) {
			String how;
			switch (will) {
			case "spouse":
				how = sp ? "배우자가 먼저 물려받고, 나머지는 자녀들에게 갔다." : "배우자가 먼저 떠나, 자녀들이 물려받았다.";
				break;
			case "fam":
				how = "가족이 똑같이 나눠 가졌다.";
				break;
			case "give":
				how = "5분의 1은 " + to + "에, 나머지는 " + (heirs ? "가족에게 갔다." : "형제자매와 친척에게 갔다.");
				break;
			case "home":
				how = "집은 " + firstKidName(life) + "에게 가고, 나머지는 법이 정한 몫대로 나뉘었다.";
				break;
			default:
				how = "모두 " + to + "에 남겼다.";
			}
			return amt + " " + at + "세에 쓴 유언장대로 나뉘었다. " + how;
		}
		String cc = life.getCurrentCountry();
		if (!heirs) {
			// 배우자·자녀가 없으면 부모님이 먼저 (민법 1000조 순위), 부모님도 없으면 형제자매
			return amt + ("Y".equals(life.getParentAlive()) ? " 배우자도 자녀도 없어, 재산은 부모님께 돌아갔다."
					: " 물려받을 배우자도 자녀도 없어, 재산은 형제자매와 친척에게 돌아갔다.");
		}
		if (sp && kids > 0) {
			if ("KOR".equals(cc)) {
				return amt + " 유언장이 없어 민법에 따라 나뉘었다. 배우자는 자녀 몫의 1.5배를 받았다 (민법 1009조).";
			}
			if ("JPN".equals(cc)) {
				return amt + " 유언장이 없어 일본 민법에 따라 배우자가 절반, 자녀들이 나머지 절반을 똑같이 나눴다.";
			}
			return amt + " 유언장이 없어 법이 정한 몫대로 배우자와 자녀들에게 나뉘었다.";
		}
		if (sp) {
			return amt + " 유언장이 없어 법에 따라 배우자가 물려받았다.";
		}
		return amt + (kids > 1 ? " 유언장이 없어 자녀들이 똑같이 나눴다." : " 유언장이 없어 자녀가 모두 물려받았다.");
	}

	// 돈·신용이 모자라 고를 수 없는 선택지는 화면에서 빼기 (고를 수 있는 게 하나도 안 남으면 그대로 둠)
	// 고를 수 없는 선택지(돈 모자람)를 빼면 하나 이하만 남는지
	private static boolean onlyNoLeft(List<EventChoiceDTO> choices, List<ChoiceCostDTO> costs) {
		if (choices == null || costs == null || costs.isEmpty()) {
			return false;
		}
		int left = 0, hidden = 0;
		for (EventChoiceDTO c : choices) {
			boolean blocked = false;
			for (ChoiceCostDTO x : costs) {
				if (c.getChoiceId() != null && x.getChoiceId() == c.getChoiceId() && !x.isOk()) {
					blocked = true;
				}
			}
			if (blocked) {
				hidden++;
			} else {
				left++;
			}
		}
		return hidden > 0 && left <= 1;
	}

	private void hideUnaffordable(LifeDTO life) {
		EventResultDTO e = life.getPendingEvent();
		List<ChoiceCostDTO> costs = life.getChoiceCosts();
		if (e == null || e.getChoices() == null || costs == null || costs.isEmpty()) {
			return;
		}
		List<EventChoiceDTO> keep = new ArrayList<>();
		List<ChoiceCostDTO> keepCost = new ArrayList<>();
		for (EventChoiceDTO c : e.getChoices()) {
			ChoiceCostDTO k = null;
			for (ChoiceCostDTO x : costs) {
				if (c.getChoiceId() != null && x.getChoiceId() == c.getChoiceId()) {
					k = x;
				}
			}
			if (k == null || k.isOk()) {
				keep.add(c);
				if (k != null) {
					keepCost.add(k);
				}
			}
		}
		if (!keep.isEmpty() && keep.size() < e.getChoices().size()) {
			e.setChoices(keep);
			life.setChoiceCosts(keepCost);
		}
	}

	// 원작 creditLimit: 빌릴 수 있는 돈 = 중위 소득 20% + 벌이(제도권 금융이면 전부, 아니면 30%) + 집값 80%
	//   + 대학을 나왔고 제도권 금융이면 중위 소득 80% + 차가 있으면 차값 ($8,000) − 이미 진 빚
	//   제도권 금융: 고소득·유럽 권역, 또는 1인당 GDP $15,000 넘는 나라에서 일하는 사람
	//   (원작은 제도권 금융 나라면 가게 대출 한도가 없지만, 우리는 어디서나 한도를 둠: 빚이 많으면 더 못 빌림)
	private double creditRoom(LifeDTO life, CountryStatDTO stat, double med) {
		String g = groupOf(life.getCurrentCountry(), stat);
		Double gdp = stat == null ? null : stat.getGdpPerCapita();
		boolean formal = "hi".equals(g) || "eca".equals(g)
				|| (gdp != null && gdp > 15000 && "EMPLOYED".equals(life.getJobStatus()));
		double pay = Math.max(life.getIncome(), spouseIncomeOf(life));
		String owns = life.getOwns() == null ? "" : life.getOwns();
		double house = owns.contains("HOUSE") ? med * 5 * ("Y".equals(life.getIsUrban()) ? 1.4 : 0.7) : 0;
		boolean uni = "UNI".equals(life.getEduLevel()) || "GRAD".equals(life.getEduLevel());
		double limit = med * 0.2 + pay * (formal ? 1 : 0.3) + house * 0.8 + (formal && uni ? med * 0.8 : 0)
				+ (owns.contains("CAR") ? 8000 : 0);
		return Math.max(0, limit - Math.max(0, -life.getAssets()));
	}

	private double spouseIncomeOf(LifeDTO life) {
		if (!"Y".equals(life.getMarriedYn())) {
			return 0;
		}
		return life.getSpouseIncome() != null ? life.getSpouseIncome() : 0;
	}

	// ================= C13 특성 칸 (진행 화면) =================
	// 이 인생의 특성 4종 레벨과 효과값 (LIFE_TRAIT, 효과 표에 값이 없으면 0) + 어릴 때 고른 기질
	public Map<String, Object> myTraits(int lifeId, Integer memberId, String guestToken) {
		LifeDTO life = loadMyLife(lifeId, memberId, guestToken);
		if (life == null) {
			return null;
		}
		Map<String, Object> out = new HashMap<>();
		out.put("traits", engineDao.selectLifeTraits(lifeId));
		out.put("temper", engState(life).temper);
		return out;
	}
}
