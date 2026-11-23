package com.example.demo.costume;

import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CostumeDAO {

	private final SqlSessionTemplate mybatis;

	public CostumeDAO(SqlSessionTemplate mybatis) {
		this.mybatis = mybatis;
	}

	public CostumeLifeDTO selectLife(int lifeId) {
		return mybatis.selectOne("costume.selectLife", lifeId);
	}

	public List<CostumeDTO> selectCostumes(int lifeId) {
		return mybatis.selectList("costume.selectCostumes", lifeId);
	}

	public CostumeDTO selectCostume(int costumeId) {
		return mybatis.selectOne("costume.selectCostume", costumeId);
	}

	public int countOwned(int lifeId, int costumeId) {
		return mybatis.selectOne("costume.countOwned", Map.of("lifeId", lifeId, "costumeId", costumeId));
	}

	public int payAssets(int lifeId, long price) {
		return mybatis.update("costume.payAssets", Map.of("lifeId", lifeId, "price", price));
	}

	public int insertLifeCostume(int lifeId, int costumeId, long price, int age) {
		return mybatis.insert("costume.insertLifeCostume",
				Map.of("lifeId", lifeId, "costumeId", costumeId, "price", price, "age", age));
	}

	public int unequipPart(int lifeId, String part) {
		return mybatis.update("costume.unequipPart", Map.of("lifeId", lifeId, "part", part));
	}

	public int setEquipped(int lifeId, int costumeId, String equipped) {
		return mybatis.update("costume.setEquipped",
				Map.of("lifeId", lifeId, "costumeId", costumeId, "equipped", equipped));
	}
}
