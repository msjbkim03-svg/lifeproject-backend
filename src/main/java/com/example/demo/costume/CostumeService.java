package com.example.demo.costume;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// C21 코스튬 상점: 값 = 그 나라 1인당 GDP × PRICE_RATE ÷ 100 (달러, 화면은 한국 체감 원)
//   부위(모자·머리·옷)마다 하나씩만 착용
//   IMAGE_PATH = 동현님 costumeData.js 의 상품 코드 (HAIR_01, TOP_01, HAT_01 …)
@Service
public class CostumeService {

	private static final double DEFAULT_GDP = 10000;

	// DB 부위 → 마을 그림(Village costume)의 이름
	private static final Map<String, String> WEAR_KEY = Map.of("HAIR", "hair", "CLOTHES", "top", "HAT", "hat");

	private CostumeDAO dao;

	public CostumeService(CostumeDAO dao) {
		this.dao = dao;
	}

	// 내 인생이면 인생 정보, 아니면 null (회원 인생은 회원 번호, 비회원 인생은 guestToken 으로 확인)
	public CostumeLifeDTO myLife(int lifeId, Integer memberId, String guestToken) {
		CostumeLifeDTO life = dao.selectLife(lifeId);
		if (life == null) return null;
		if (life.getMemberId() != null) {
			return life.getMemberId().equals(memberId) ? life : null;
		}
		return life.getGuestToken() != null && life.getGuestToken().equals(guestToken) ? life : null;
	}

	private long priceOf(CostumeLifeDTO life, CostumeDTO c) {
		double gdp = life.getGdpPerCapita() == null ? DEFAULT_GDP : life.getGdpPerCapita();
		return Math.max(1L, Math.round(gdp * c.getPriceRate() / 100));
	}

	// 목록 { assets, wear: {hair, top, hat}, items }  (wear 는 Village costume 에 그대로 넘기면 됨)
	public Map<String, Object> list(CostumeLifeDTO life) {
		List<CostumeDTO> items = dao.selectCostumes(life.getLifeId());
		Map<String, String> wear = new HashMap<>();
		for (CostumeDTO c : items) {
			c.setPrice(priceOf(life, c));
			if ("Y".equals(c.getEquipped())) wear.put(WEAR_KEY.get(c.getPart()), c.getLook());
		}
		Map<String, Object> result = new HashMap<>();
		result.put("assets", life.getAssets());
		result.put("wear", wear);
		result.put("items", items);
		return result;
	}

	// 사기 → 같은 부위 벗고 바로 착용. 문제 있으면 이유 문장, 성공하면 null
	@Transactional
	public String buy(CostumeLifeDTO life, int costumeId) {
		if (!"PLAYING".equals(life.getStatus())) return "진행 중인 인생에서만 살 수 있어요.";
		CostumeDTO c = dao.selectCostume(costumeId);
		if (c == null) return "없는 물건이에요.";
		if (dao.countOwned(life.getLifeId(), costumeId) > 0) return "이미 가진 물건이에요.";

		long price = priceOf(life, c);
		if (dao.payAssets(life.getLifeId(), price) == 0) return "돈이 모자라요.";

		dao.insertLifeCostume(life.getLifeId(), costumeId, price, life.getCurrentAge());
		dao.unequipPart(life.getLifeId(), c.getPart());
		dao.setEquipped(life.getLifeId(), costumeId, "Y");
		return null;
	}

	// 착용 (같은 부위에 입던 건 벗음)
	@Transactional
	public String equip(CostumeLifeDTO life, int costumeId) {
		if (!"PLAYING".equals(life.getStatus())) return "진행 중인 인생에서만 갈아입을 수 있어요.";
		CostumeDTO c = dao.selectCostume(costumeId);
		if (c == null || dao.countOwned(life.getLifeId(), costumeId) == 0) return "가진 것만 착용할 수 있어요.";
		dao.unequipPart(life.getLifeId(), c.getPart());
		dao.setEquipped(life.getLifeId(), costumeId, "Y");
		return null;
	}

	// 벗기 (그 부위는 기본 모습으로)
	public String unequip(CostumeLifeDTO life, int costumeId) {
		if (!"PLAYING".equals(life.getStatus())) return "진행 중인 인생에서만 갈아입을 수 있어요.";
		if (dao.setEquipped(life.getLifeId(), costumeId, "N") == 0) return "가진 것만 벗을 수 있어요.";
		return null;
	}
}
