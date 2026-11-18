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
		fillView(life, s); // 한국 체감 원·소득 위치·가족과 삶·같은 1초에 태어난 사람들
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
		// 태어난 해 기대수명 (원작 e0): 화면의 '1년 ≈ n초'·인생 지도 기대수명 칸에 씀
		//   DB에서 다시 읽은 인생(1년 진행·이어하기)에는 이 값이 없어서 태어난 나라·연대 통계에서 채움
		if (life.getLifeExpectancy() == null) {
			for (CountryStatDTO c : birthList(life.getBirthYear() / 10 * 10)) {
				if (c.getCountryCode().equals(life.getBirthCountry())) {
					life.setLifeExpectancy(c.getLifeExpectancy());
					life.setInfantMortality(c.getInfantMortality());
					life.setGdpPerCapita(c.getGdpPerCapita());
					break;
				}
			}
		}
		double med = medIncome(life.getCurrentCountry(), stat);
		boolean partner = "Y".equals(life.getMarriedYn()) && life.getCurrentAge() < retireAge(life, stat);
		life.setSpouseIncome(partner ? Math.round(med) : null);
		double inc = life.getIncome() > 0 ? life.getIncome() : partner ? med : 0;
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
		int[] siblingGap;                // 형제자매 나이 - 내 나이 (+ 손위, − 손아래)
		String[] siblingSex;
	}

	private final Map<Integer, FamSeed> famCache = new ConcurrentHashMap<>();

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
			return mother ? pick(r, "교사", "상점 주인", "면사무소 직원") : pick(r, "지주", "상점 주인", "공무원", "교사");
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
		}
		f.father = dad;
		f.mother = mom;

		// 나이: 원작 500명 평균 어머니 27.8세 (옛날일수록 조금 어리게), 아버지는 2~6세 위
		double mAvg = 25.5 + (by - 1940) * 0.04;
		f.motherAt = (int) Math.round(clamp(mAvg + r.nextGaussian() * 5, 16, 44));
		f.fatherAt = f.motherAt + 2 + r.nextInt(5);
		double le = stat != null && stat.getLifeExpectancy() != null ? stat.getLifeExpectancy() : 65;
		f.motherDie = (int) Math.round(clamp(le + 8 + r.nextGaussian() * 12, f.motherAt + 1, 100));
		f.fatherDie = (int) Math.round(clamp(le + 4 + r.nextGaussian() * 12, f.fatherAt + 1, 100));

		f.motherJob = parentJob(r, life.getWealthClass(), urban, by, true);
		f.fatherJob = parentJob(r, life.getWealthClass(), urban, by, false);

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
		f.spouse = pickName(life.getCurrentCountry(), sg, r);
		f.spouseJob = parentJob(r, Math.max(2, life.getWealthClass()), urban, by + 25, false);

		famCache.put(life.getLifeId(), f);
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

	private List<FamilyMemberDTO> family(LifeDTO life, CountryStatDTO stat) {
		FamSeed f = famSeed(life, stat);
		int age = life.getCurrentAge();
		List<FamilyMemberDTO> out = new ArrayList<>();

		// 부모: 부모 생존이 N이면 먼저 떠날 사람부터 (나이가 많이 들었으면 둘 다)
		boolean bothAlive = !"N".equals(life.getParentAlive());
		boolean dadGone = false, momGone = false;
		if (!bothAlive) {
			int dadEnd = f.fatherDie - f.fatherAt; // 내 나이 몇 살 때 떠나는지
			int momEnd = f.motherDie - f.motherAt;
			if (dadEnd <= momEnd) {
				dadGone = true;
				momGone = momEnd <= age;
			} else {
				momGone = true;
				dadGone = dadEnd <= age;
			}
		}
		out.add(parentRow("어머니", f.mother, f.motherAt, f.motherDie, f.motherJob, momGone, age));
		out.add(parentRow("아버지", f.father, f.fatherAt, f.fatherDie, f.fatherJob, dadGone, age));

		// 형제자매: 손위는 늘, 손아래는 태어난 뒤부터
		List<String> sib = new ArrayList<>();
		for (int i = 0; i < f.siblingGap.length; i++) {
			int g = f.siblingGap[i];
			int sAge = age + g;
			if (sAge < 0) {
				continue;
			}
			sib.add(sibWord(life.getGender(), f.siblingSex[i], g > 0) + " " + sAge + "세");
		}
		out.add(new FamilyMemberDTO("형제자매", null, null, sib.isEmpty() ? "없음" : String.join(" · ", sib), true));

		// 배우자·자녀
		if ("Y".equals(life.getMarriedYn())) {
			int sa = Math.max(16, age + f.spouseGap);
			String job = sa >= retireAge(life, stat) ? "은퇴" : f.spouseJob;
			out.add(new FamilyMemberDTO("배우자", f.spouse, sa, job, true));
		} else if (age >= 18) {
			out.add(new FamilyMemberDTO("배우자", null, null, "없음", true));
		}
		// 자녀: 한 명씩 아들·딸, 이름(성 쓰는 나라는 아버지 성), 나이 (태어난 해는 CHILD 이벤트 기록에서)
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
				out.add(new FamilyMemberDTO("M".equals(sex) ? "아들" : "딸", nm, ka, childLine(ka), true));
			}
		}
		return out;
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
		if (a < 13) {
			return "어린이";
		}
		if (a < 19) {
			return "청소년";
		}
		return a < 30 ? "청년" : "어른";
	}

	private FamilyMemberDTO parentRow(String role, String name, int at, int die, String job, boolean gone, int myAge) {
		if (gone) {
			int d = Math.min(die, at + myAge);
			return new FamilyMemberDTO(role, name, d, d + "세에 세상을 떠남", false);
		}
		return new FamilyMemberDTO(role, name, at + myAge, job, true);
	}

	// ---------- 같은 1초에 태어난 사람들 (원작 makeCompanions 4명) ----------
	private static final class PeerSeed {
		String name, cc, countryName, gender, cause;
		int dieAge;        // 이 나이를 못 넘김
		int schoolEnd;     // 학교를 마치는 나이
		int marryAge;      // 결혼하는 나이 (99면 안 함)
		int kids;
		boolean home;      // 가사·육아 전담
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
			p.schoolEnd = edu < 1 ? 0 : 6 + (int) Math.round(edu);
			p.marryAge = r.nextDouble() < 0.93 ? (int) Math.round(clamp(("F".equals(p.gender) ? 22 : 26) + r.nextGaussian() * 4, 16, 45)) : 99;
			p.kids = (int) Math.round(clamp(kidsOfEra(p.cc, c, by + 25) + r.nextGaussian() * 1.2 - 0.3, 0, 9));
			double home = by < 1965 ? 0.6 : by < 1985 ? 0.45 : 0.3;
			p.home = "F".equals(p.gender) && r.nextDouble() < home;
			list.add(p);
		}
		peerCache.put(life.getLifeId(), list);
		return list;
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
		if (age < 6) {
			return "어린 시절";
		}
		if (age < p.schoolEnd) {
			return age < 12 ? "초등학교에 다니는 중" : age < 15 ? "중학교에 다니는 중" : age < 18 ? "고등학교에 다니는 중" : "대학에 다니는 중";
		}
		if (age < 15) {
			return "집안일과 일을 돕는 중";
		}
		String s;
		if (age >= 65) {
			s = "은퇴해 노년을 보내는 중";
		} else if (p.home && age >= p.marryAge) {
			s = "가정을 돌보는 중";
		} else {
			s = "일하는 중";
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
		long startAssets = life.getAssets();
		life.setMoneyFlow(new ArrayList<>()); // [화면용] 지난 1년 순자산 내역

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

		// ⑤-2 돈 쓰기·투자 (C06-3): 모은 돈 굴리기 + 가진 물건 유지비
		investStep(life, stat);
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
		for (String note : milestones(life, eduBefore, schoolBefore, jobBefore, jobIdBefore)) {
			saveEventLog(life, lived, null, null, note);
			yearEvents.add(noteEvent(note));
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
		}

		// 4. 저장 + 그해 기록
		engineDao.updateLifeYear(life);
		engineDao.insertYearLog(life);

		closeBook(life, startAssets);
		fillRegion(life);
		return life;
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
		double med = medIncome(life.getCurrentCountry(), stat);
		for (EngineStory.Story st : EngineStory.year(life, age, already, random)) {
			String text = st.text;
			if (st.cost > 0 && age >= 18) {
				double k = clamp(Math.pow(Math.max(life.getIncome(), 1) / med, 0.3), 0.6, 1.8);
				long pay = Math.round(med * st.cost * k);
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
		} else if (wasIn && "DROPPED".equals(school)) {
			notes.add(schoolObj(edu) + " 그만두었다.");
		}

		if ("EMPLOYED".equals(jobBefore) && "RETIRED".equals(life.getJobStatus())) {
			JobDTO job = jobIdBefore == null ? null : engineDao.selectJob(jobIdBefore);
			notes.add(job == null ? "일을 그만두고 은퇴했다." : job.getJobName() + " 일을 마치고 은퇴했다.");
		}
		return notes;
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
		if (owns.contains("BIKE") && age < 70) {
			life.setHealth(life.getHealth() + 0.4); // 타고 다니면 건강 조금
		}
		if (life.getAssets() <= 0 || age < 15) {
			return;
		}
		String t = investNow(life, stat);
		double r = investReturn(t);
		double gain = life.getAssets() * r;
		if (Math.round(gain) == 0) {
			return;
		}
		life.setAssets(life.getAssets() + Math.round(gain));
		book(life, "DEPOSIT".equals(t) ? "예금 이자" : INV_NAME.get(t) + (gain >= 0 ? " 수익" : " 손실"), gain);
		String note = null;
		int pct = (int) Math.round(Math.abs(r) * 100);
		if (r <= -0.18) {
			note = "시장이 크게 떨어져 " + INV_NAME.get(t) + "에 맡긴 돈의 " + pct + "%가 사라졌다. (" + wonSigned(life, gain) + ")";
		} else if (r >= 0.25) {
			note = INV_NAME.get(t) + "이(가) 크게 올라 맡긴 돈이 " + pct + "% 불었다. (" + wonSigned(life, gain) + ")";
		}
		if (note != null) {
			saveEventLog(life, age, null, null, note);
			List<EventResultDTO> ev = life.getEvents() == null ? new ArrayList<>() : new ArrayList<>(life.getEvents());
			ev.add(noteEvent(note));
			life.setEvents(ev);
		}
	}

	// 돈 쓰기·투자 창 목록
	public ShopDTO shop(LifeDTO life) {
		CountryStatDTO stat = yearStat(life);
		double med = medIncome(life.getCurrentCountry(), stat);
		int age = life.getCurrentAge();
		int year = life.getBirthYear() + age;
		long cash = Math.max(0, life.getAssets());
		java.util.Set<String> owns = ownSet(life);
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
			long price = Math.round(med * 5 * ("Y".equals(life.getIsUrban()) ? 1.4 : 0.7));
			ShopItemDTO d = new ShopItemDTO("BUY_HOUSE", "BUY", "내 집 마련", "행복 +10 · 집값의 20%가 있으면 나머지는 대출", price);
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
			long price = it.getPrice();
			long cash = Math.max(0, life.getAssets());
			long loan = Math.max(0, price - cash);
			long interest = Math.round(loan * 0.25);
			life.setAssets(life.getAssets() - interest); // 집값은 재산으로 남고(현금 → 집) 이자만 나감
			life.setHappiness(life.getHappiness() + 10);
			owns.add("HOUSE");
			text = "내 집을 마련했다. (집값 " + won(life, price) + (loan > 0 ? ", 대출 " + won(life, loan) + " · 이자 약 " + won(life, interest) : "") + ")";
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
		// 인생 기록: 고른 직업 (선택 이벤트처럼 방금 지나간 해에 기록)
		String note = job.getJobName() + " 일을 시작했다.";
		saveEventLog(life, life.getCurrentAge() - 1, null, null, note);
		List<EventResultDTO> shown = new ArrayList<>();
		shown.add(noteEvent(note));
		life.setEvents(shown);
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

	// 나이대별로 고를 수 있는 집중 목록 (원작 기준 + 나이에 맞게 조금씩 바뀜)
	//   학생: 14세까지 집안 돕기, 15세부터는 대신 진로 준비(취업 준비)
	//   대학생·직장인·무직: 18~39세 미혼이면 연애·만남 (LOVE)
	//   무직 40세 미만: 취업 준비, 노년: 종교·봉사 (원작 faith)
	private List<String> focusOptions(String stage, LifeDTO life) {
		List<String> list = new ArrayList<>();
		int age = life.getCurrentAge();
		boolean single = !"Y".equals(life.getMarriedYn());
		boolean loveAge = age >= 18 && age < 40 && single;
		if (stage.equals("STUDENT")) {
			list.add("STUDY"); list.add("PLAY");
			if (age < 15) {
				list.add("HOUSE");
			}
			list.add("SPORT"); list.add("HOBBY");
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
			list.add("FAMILY"); list.add("HEALTH"); list.add("BRAIN"); list.add("HOBBY"); list.add("FAITH");
			if (!"EMPLOYED".equals(life.getJobStatus())) {
				list.add("REST");
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
				if ("Y".equals(life.getMarriedYn()) && age < retireAge(life, stat)) {
					double sp = med * saveRate(med, med) * 0.85;
					change += sp;
					book(life, "배우자 벌이에서 남긴 돈", sp);
				}
				// 자기계발: 배움에 소득의 2% (원작)
				if ("SELF_DEV".equals(focus)) {
					change -= income * 0.02;
					book(life, "자기계발 비용", -income * 0.02);
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
				// 상황 문장만 기록하고, 효과는 고른 선택지로 (PENDING_EVENT_ID에 저장)
				life.setPendingEventId(e.getEventId());
				life.setNeedChoice(true);
				life.setPendingEvent(e);
				life.setChoiceCosts(costsOf(life, e.getChoices(), e.getLogText()));
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
		long before = life.getAssets();
		if (e.getEffWealthRate() != 0 && life.getAssets() > 0) {
			long d = Math.round(life.getAssets() * e.getEffWealthRate());
			life.setAssets(life.getAssets() + d);
			String t = e.getLogText() == null ? "" : e.getLogText();
			book(life, t.contains("집") || t.contains("땅") ? (d > 0 ? "집값 상승" : "집값 하락")
					: d > 0 ? "뜻밖에 생긴 돈" : "사건으로 잃은 돈", d);
		}

		// 4. 상태 변경 (결혼·자녀·부모 사망·실직·조기 은퇴·이민)
		applyAction(life, e.getResultAction(), e.getMigrateTo(), age);

		// 돈이 오갔으면 기록 끝에 한국 체감 금액 (원작처럼 "(−40만 원)")
		long moved = life.getAssets() - before;
		if (moved != 0 && e.getLogText() != null) {
			e.setLogText(e.getLogText() + " (" + wonSigned(life, moved) + ")");
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

		// 큰돈이 드는 선택지(집·땅·가게·투자): 실제 비용·대출·이자로 계산 (재산 비율 대신)
		//   투자 결과(윤아님 applyChoice의 성공·실패)는 넣은 돈 × 변화율로 반영
		ChoiceCostDTO cost = costOf(life, eventDao.selectChoice(choiceId), engineDao.selectPendingText(life));
		long moneyChange = 0;
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
		}
		closeBook(life, startAssets);
		fixStats(life);
		life.setChoiceCosts(null);
		List<EventResultDTO> extra = new ArrayList<>();
		if ("LE_INVEST_BUSINESS".equals(r.getEventId()) && r.getEffIncomeRate() > 0) {
			startBusiness(life, age, extra); // 가게 차리기 성공 → 자영업 사장
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
		if (cash >= cost) {
			d.setOk(true);
		} else if (down < 1 && cash >= cost * down && age18(life)) {
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
		life.setChoiceCosts(costsOf(life, e.getChoices(), e.getLogText()));
	}

	// 장례비 (어른이고 벌이나 재산이 있을 때만, 중위 소득 × 비율 × 내 벌이 보정)
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
			if (age >= 18) {
				double med = medIncome(life.getCurrentCountry(), yearStat(life));
				double k = clamp(Math.pow(Math.max(life.getIncome(), 1) / med, 0.3), 0.6, 1.8);
				long brought = Math.round(med * (0.2 + random.nextDouble() * 0.3));
				long wedding = Math.round(med * 0.3 * k);
				life.setAssets(life.getAssets() + brought - wedding);
				book(life, "배우자가 가져온 돈", brought);
				book(life, "결혼식 비용", -wedding);
			}
			break;
		case "CHILD": // 자녀 +1
			life.setChildCount(life.getChildCount() + 1);
			break;
		case "MARRIAGE_END": // 이혼·사별 (윤아님 이벤트 값): 결혼 상태 해제 (자녀 수는 그대로)
		case "DIVORCE":
			life.setMarriedYn("N"); // 다시 연애·재혼할 수 있게, 첫째 출산은 멈춤 (STATE_MARRIED가 빠짐)
			break;
		case "SPOUSE_DEATH":
			life.setMarriedYn("N");
			payFuneral(life, 0.3, "배우자 장례비"); // 원작 중간값 −208만 원
			break;
		case "PARENT_DEATH": // 부모 사망
			life.setParentAlive("N");
			payFuneral(life, 0.06, "부모 장례비 나눠 냄"); // 원작 중간값 −40만 원
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
