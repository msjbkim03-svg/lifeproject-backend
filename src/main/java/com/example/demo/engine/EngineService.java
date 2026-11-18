package com.example.demo.engine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.event.EventDAO;
import com.example.demo.event.EventService;
import com.example.demo.event.dto.EventResultDTO;

@Service
public class EngineService {

	private final EngineDao engineDao;
	private final EventService eventService; // C09·C10 이벤트 (장윤아님)
	private final EventDAO eventDao; // 선택지 다시 불러오기용 (장윤아님, 읽기만)

	// 생성자 주입: 스프링이 객체를 만들 때 EngineDao, EventService, EventDAO를 넣어줌
	public EngineService(EngineDao engineDao, EventService eventService, EventDAO eventDao) {
		this.engineDao = engineDao;
		this.eventService = eventService;
		this.eventDao = eventDao;
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
			return incomePct(gdpOf(stat) * life.getIncomeRate(), cc, stat);
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
	public boolean isMyLife(int lifeId, Integer memberId, String guestToken) {
		return loadMyLife(lifeId, memberId, guestToken) != null;
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
		// 같은 해를 두 번 진행하지 않게 이 인생 줄을 잠금 (버튼 연타·자동 진행이 겹쳐도 한 번만 진행)
		//   먼저 온 요청이 끝날 때까지 기다렸다가, 나이가 이미 바뀌었으면 0줄 → 진행 안 함
		if (engineDao.lockLifeYear(life) == 0) {
			return null;
		}

		// 그해 사는 나라 통계 (사망 판정·진학에 사용)
		CountryStatDTO stat = yearStat(life);

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
			engineDao.endLife(life);
			fillRegion(life);
			return life;
		}

		// 2. 살아남으면 나이 +1
		life.setCurrentAge(life.getCurrentAge() + 1);

		// ③ 학교: 새 나이 기준으로 졸업·중퇴·진학 판정 (C06-1)
		schoolStep(life, stat);

		// ③ 직업: 정년 은퇴 판정, 일자리가 없으면 직업 3개 제안 (C06-2)
		jobStep(life, stat);

		// 3. 나이대가 바뀌면 집중을 비우고 선택 창 띄우기 (C04)
		String newStage = stageOf(life);
		if (newStage != null && !newStage.equals(life.getStageCode())) {
			life.setStageCode(newStage);
			life.setFocusCode(null);
			life.setNeedFocus(true);
			life.setFocusOptions(focusOptions(newStage, life));
		}

		// 4. 저장 + 그해 기록
		engineDao.updateLifeYear(life);
		engineDao.insertYearLog(life);

		fillRegion(life);
		return life;
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

	// ================= C06-2 직업 선택 =================

	// 제안받은 직업 중 하나를 선택 (올해 제안받은 3개 안에서만)
	@Transactional
	public LifeDTO chooseJob(LifeDTO life, String jobId) {
		if (!life.getStatus().equals("PLAYING")) {
			return null;
		}
		if (life.getCurrentAge() < 15 || "ENROLLED".equals(life.getSchoolStatus())
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
		fillRegion(life);
		return life;
	}

	// 나이대 판정 (원작 기준)
	private String stageOf(LifeDTO life) {
		int age = life.getCurrentAge();
		String level = life.getEduLevel();
		boolean enrolled = "ENROLLED".equals(life.getSchoolStatus());

		if (age < 6) {
			return null; // 0~5세는 집중 선택 없음
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
		if (age < 15) {
			return "YOUTH"; // 학교 안 다니는 아이 (6~14세)
		}
		return "JOBLESS"; // 일이 없는 15세 이상
	}

	// 나이대별로 고를 수 있는 집중 목록 (원작 기준)
	private List<String> focusOptions(String stage, LifeDTO life) {
		List<String> list = new ArrayList<>();
		if (stage.equals("STUDENT")) {
			list.add("STUDY"); list.add("PLAY"); list.add("HOUSE"); list.add("SPORT"); list.add("HOBBY");
		} else if (stage.equals("YOUTH")) {
			list.add("HOUSE"); list.add("PLAY"); list.add("SPORT"); list.add("HOBBY");
		} else if (stage.equals("UNIV")) {
			list.add("STUDY"); list.add("PLAY"); list.add("PARTTIME"); list.add("JOB_PREP"); list.add("SPORT"); list.add("HOBBY");
		} else if (stage.equals("WORKER")) {
			list.add("WORK"); list.add("BALANCE"); list.add("FAMILY"); list.add("HEALTH"); list.add("SELF_DEV"); list.add("HOBBY");
		} else if (stage.equals("JOBLESS")) {
			list.add("FAMILY"); list.add("HEALTH"); list.add("SELF_DEV"); list.add("HOBBY"); list.add("REST");
		} else if (stage.equals("SENIOR")) {
			if ("EMPLOYED".equals(life.getJobStatus())) {
				list.add("WORK"); list.add("FAMILY"); list.add("HEALTH"); list.add("BRAIN"); list.add("HOBBY");
			} else {
				list.add("FAMILY"); list.add("HEALTH"); list.add("BRAIN"); list.add("HOBBY"); list.add("REST");
			}
		}
		return list;
	}

	// 고르지 않았을 때 기본값 (원작 기준): 목록에 '균형'이 있으면 균형, 없으면 목록 첫 번째
	private String defaultFocus(LifeDTO life) {
		List<String> list = focusOptions(life.getStageCode(), life);
		if (list.contains("BALANCE")) {
			return "BALANCE";
		}
		return list.get(0);
	}

	// 집중별 1년 능력치 효과 (원작 FOCUS 수치: 건강, 행복, 관계)
	// 지식은 여기서 바로 더하지 않고 C05 지식 계산(knowEff)에서 반영 (원작 기준)
	// 건강 관리의 건강 효과는 나이가 들수록 줄어듦 (원작 fitK)
	private void applyFocus(LifeDTO life) {
		String f = life.getFocusCode();
		if (f == null) {
			return; // 0~5세
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

	// 학교 단계: 초등 6년 → 중등 3년 → 고등 3년 → 대학 4년 → 대학원 2년 (6세 입학, 모든 나라 같은 기간)
	private String[] levels = { "ELEM", "MIDDLE", "HIGH", "UNI", "GRAD" };
	private int[] startAges = { 6, 12, 15, 18, 22 };
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
		int idx = levelIndex(life.getEduLevel());
		String status = life.getSchoolStatus();

		// 1. 다니는 중이면: 졸업 나이가 됐는지, 아니면 중퇴하는지
		if ("ENROLLED".equals(status)) {
			if (age >= startAges[idx] + years[idx]) {
				life.setSchoolStatus("GRADUATED");
				addExperience(life, "EDU_" + levels[idx]); // 졸업 경험 (C05)
				status = "GRADUATED";
			} else {
				if (random.nextDouble() < dropRate(life, idx)) {
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

		// 2. 진학: 아직 입학 전(6세)이거나, 졸업한 해에 다음 단계 입학 나이면 판정
		int next = idx + 1;
		boolean canEnter = (idx == -1 && status == null) || "GRADUATED".equals(status);
		if (canEnter && next < levels.length && age == startAges[next]) {
			if (random.nextDouble() < enrollRate(life, stat, next)) {
				life.setEduLevel(levels[next]);
				life.setSchoolStatus("ENROLLED");
				life.setDropReason(null);
			}
		}
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
			return admit * 0.5;
		}
		return eduProbs(life, stat)[next];
	}

	// 1년 중퇴 확률 (원작 lsKeep·usKeep)
	// 중학교·고등학교만 중퇴 있음 (초등·대학·대학원은 원작에도 일반 중퇴 없음)
	//   끝까지 다닐 확률 = 그 단계 진학 확률을 노력 배율만큼 키운 값 (최소 중학교 10%·고등학교 8%)
	//   노력 배율 = 1.15 × (13세 이하 1.7, 그 위 2.05) × e^(0.85×지식 z - 0.36)
	//   1년 중퇴 확률 = 1 - 끝까지 다닐 확률^(1/2) (3년 과정이라 2번 판정)
	private double dropRate(LifeDTO life, int idx) {
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

		// 1. 정년 은퇴 (원작 연금 규칙)
		//    은퇴 나이 전에 시작한 일만 정년 은퇴, 은퇴 후 다시 구한 일은 75세 또는 건강 40 미만까지
		if ("EMPLOYED".equals(life.getJobStatus()) && age >= retireAge(life, stat)) {
			boolean rehired = age - life.getJobYears() >= retireAge(life, stat);
			if (!rehired) {
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

		// 2. 15세 이상, 학교 안 다니고, 일자리가 없으면 매년 직업 3개 제안
		showOffers(life, stat);
	}

	// 직업을 제안받을 수 있는 상태인지: 15세 이상, 학교 안 다니고, 일자리 없고, 은퇴 나이 전
	private boolean canGetJob(LifeDTO life, CountryStatDTO stat) {
		boolean noJob = life.getJobStatus() == null || "UNEMPLOYED".equals(life.getJobStatus());
		return "PLAYING".equals(life.getStatus()) && life.getCurrentAge() >= 15
				&& !"ENROLLED".equals(life.getSchoolStatus()) && noJob && life.getCurrentAge() < retireAge(life, stat);
	}

	// 직업 선택 창 띄우기 (매년 진행 후, 이어하기·새로고침 때)
	private void showOffers(LifeDTO life, CountryStatDTO stat) {
		if (!canGetJob(life, stat)) {
			return;
		}
		List<JobDTO> offers = offerJobs(life);
		if (!offers.isEmpty()) {
			life.setNeedJob(true);
			life.setJobOffers(offers);
		}
	}

	// 필요 학력을 갖춘 직업 중 권역 산업 비율에 따라 3개 제안
	// 취업 준비 점수(0~2)가 높을수록 소득 배율 높은 직업이 나올 확률 증가
	// 제안은 DB에 저장하지 않고, 같은 인생·나이·준비 점수·학력·나라면 항상 같은 3개가 나오게 뽑음
	// → 새로고침해도 같은 제안이 다시 뜨고, 직업 선택 때 제안받은 직업인지 다시 뽑아서 확인 가능
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
			income = gdp * life.getIncomeRate() * (0.8 + random.nextDouble() * 0.4);
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
		} else if ("PARTTIME".equals(focus)) {
			income = gdp * 0.15; // 아르바이트
		}
		life.setIncome(Math.round(income));

		// 18세 미만·학생은 집에서 돌봐 줌 (생활비·저축 없음)
		if (age < 18 || "ENROLLED".equals(life.getSchoolStatus())) {
			return;
		}

		double change = 0;
		if (income > 0) {
			double sr = saveRate(income, med);
			if (retired) {
				// 연금에서 남기는 돈: 저축률(최대 3%) + 연금이 넉넉하면 남는 몫 (원작 retKeep)
				double e = income / med;
				double keep = e > 1.5 ? clamp(1 - (1.5 + 0.25 * (e - 1.5)) / e, 0, 0.65) : 0;
				change = income * (Math.min(sr, 0.03) + keep);
			} else {
				change = income * sr; // 번 돈에서 남긴 돈
				// 아이 키우는 비용 (원작 kidK): 자녀 1명당 소득 × (3% + 저축률 10% 넘는 만큼 × 0.2)
				//   자녀 나이는 저장하지 않아서 55세 전까지 키운다고 봄
				if (age < 55 && life.getChildCount() > 0) {
					change -= life.getChildCount() * income * (0.03 + 0.2 * Math.max(0, sr - 0.1));
				}
				// 배우자도 일하면 배우자 벌이에서 남긴 돈 × 0.85 (배우자 = 그 나라 중위 소득으로 봄)
				if ("Y".equals(life.getMarriedYn()) && age < retireAge(life, stat)) {
					change += med * saveRate(med, med) * 0.85;
				}
				// 자기계발: 배움에 소득의 2% (원작)
				if ("SELF_DEV".equals(focus)) {
					change -= income * 0.02;
				}
				// 최소 생활비보다 적게 벌면 모자란 돈의 절반이 빠짐
				if (income < minLive) {
					change -= (minLive - income) * 0.5;
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
				if (age < 55 && life.getChildCount() > 0) {
					change -= life.getChildCount() * med * (0.03 + 0.2 * Math.max(0, psr - 0.1));
				}
				change -= 0.02 * Math.max(0, life.getAssets());
			} else {
				double k = withKids || withKin || withParents ? 0.3 : married ? 0.6 : 1;
				double live = minLive * k;
				// 실업급여 (고소득국·유럽중앙아시아): 마지막 소득의 절반 (중위 소득의 40~80%)
				if ("UNEMPLOYED".equals(life.getJobStatus()) && (g.equals("hi") || g.equals("eca"))) {
					double last = life.getIncomeRate() != null ? gdp * life.getIncomeRate() : 0;
					change += Math.min(Math.max(med * 0.4, last * 0.5), med * 0.8);
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
				// 돈이 바닥나면 함께 사는 자녀·가족이 생활비를 맡음
				if ((withKids || withKin) && life.getAssets() + change < 0) {
					change = Math.min(0, change + live);
				}
			}
			// 60세 이상은 자녀가 보내 주는 돈 (원작: 자녀 최대 2명 × 중위 소득 × 12%, 관계 40 이하면 절반)
			if (age >= 60 && life.getChildCount() > 0) {
				change += Math.min(2, life.getChildCount()) * med * 0.12 * (life.getRelation() > 40 ? 1 : 0.5);
			}
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
				// 상황 문장만 기록하고, 효과는 고른 선택지로 (PENDING_EVENT_ID에 저장)
				life.setPendingEventId(e.getEventId());
				life.setNeedChoice(true);
				life.setPendingEvent(e);
				saveEventLog(life, life.getCurrentAge(), e.getEventId(), null, e.getLogText());
				continue;
			}
			applyEvent(life, e, life.getCurrentAge(), null);
		}

		fixStats(life);
		life.setEvents(events); // [화면용] 올해 일어난 일
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
		if ("EMPLOYED".equals(life.getJobStatus()) && h.getJobLossAdd() != null
				&& random.nextDouble() < h.getJobLossAdd() * k) {
			life.setJobStatus("UNEMPLOYED");
			life.setJobYears(0);
			saveEventLog(life, age, e.getEventId(), null, why + " 때문에 일자리를 잃었다.");
		}
		if ("ENROLLED".equals(life.getSchoolStatus()) && h.getEduStopAdd() != null
				&& random.nextDouble() < h.getEduStopAdd() * k) {
			life.setSchoolStatus("DROPPED");
			life.setDropReason(why + " 때문에 학교를 그만둠");
			saveEventLog(life, age, e.getEventId(), null, why + " 때문에 학교를 그만두었다.");
		}
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
		if (e.getEffWealthRate() != 0 && life.getAssets() > 0) {
			life.setAssets(life.getAssets() + Math.round(life.getAssets() * e.getEffWealthRate()));
		}

		// 4. 상태 변경 (결혼·자녀·부모 사망·실직·조기 은퇴·이민)
		applyAction(life, e.getResultAction(), e.getMigrateTo(), age);

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

		applyEvent(life, r, age, choiceId);
		fixStats(life);

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
		life.setEvents(list); // [화면용] 고른 결과

		engineDao.updateLifeYear(life);
		fillRegion(life);
		return life;
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

	// 이어하기·새로고침 때 선택 창 다시 띄우기
	private void restoreChoice(LifeDTO life) {
		if (life == null || life.getPendingEventId() == null) {
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
			break;
		case "CHILD": // 자녀 +1
			life.setChildCount(life.getChildCount() + 1);
			break;
		case "MARRIAGE_END": // 이혼·사별 (윤아님 이벤트 값): 결혼 상태 해제 (자녀 수는 그대로)
		case "DIVORCE":
		case "SPOUSE_DEATH":
			life.setMarriedYn("N"); // 다시 연애·재혼할 수 있게, 첫째 출산은 멈춤 (STATE_MARRIED가 빠짐)
			break;
		case "PARENT_DEATH": // 부모 사망
			life.setParentAlive("N");
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
		double r = mortG(g);
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
					return d.getCauseName();
				}
			}
			return list.get(list.size() - 1).getCauseName();
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
}
