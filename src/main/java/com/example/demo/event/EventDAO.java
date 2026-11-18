package com.example.demo.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.event.dto.DestinationDTO;
import com.example.demo.event.dto.EventChoiceDTO;
import com.example.demo.event.dto.EventTemplateDTO;
import com.example.demo.event.dto.GameEventDTO;
import com.example.demo.event.dto.HistEventTypeDTO;

@Repository
public class EventDAO {

	private final SqlSessionTemplate mybatis;

	public EventDAO(SqlSessionTemplate mybatis) {
		this.mybatis = mybatis;
	}

	public List<EventTemplateDTO> selectTemplates() {
		return mybatis.selectList("Event.selectTemplates");
	}

	public Map<String, Object> selectCountryStat(String countryCode, int decade) {
		Map<String, Object> param = new HashMap<>();
		param.put("countryCode", countryCode);
		param.put("decade", decade);
		return mybatis.selectOne("Event.selectCountryStat", param);
	}

	public List<GameEventDTO> selectLifeCandidates(String templateId, int age, String gender, int wealth) {
		Map<String, Object> param = new HashMap<>();
		param.put("templateId", templateId);
		param.put("age", age);
		param.put("gender", gender);
		param.put("wealth", wealth);
		return mybatis.selectList("Event.selectLifeCandidates", param);
	}

	public List<String> selectExpIds(int lifeId) {
		return mybatis.selectList("Event.selectExpIds", lifeId);
	}

	public List<String> selectTexts(String templateId) {
		return mybatis.selectList("Event.selectTexts", templateId);
	}

	public String selectCountryName(String countryCode) {
		return mybatis.selectOne("Event.selectCountryName", countryCode);
	}

	// 선택 이벤트의 선택지 목록
	public List<EventChoiceDTO> selectChoices(String eventId) {
		return mybatis.selectList("Event.selectChoices", eventId);
	}

	// 선택지 하나
	public EventChoiceDTO selectChoice(int choiceId) {
		return mybatis.selectOne("Event.selectChoice", choiceId);
	}

	// 역사 이벤트 후보 (나라·연도·나이·성별)
	public List<GameEventDTO> selectHistCandidates(String countryCode, int year, int age, String gender) {
		Map<String, Object> param = new HashMap<>();
		param.put("countryCode", countryCode);
		param.put("year", year);
		param.put("age", age);
		param.put("gender", gender);
		return mybatis.selectList("Event.selectHistCandidates", param);
	}

	// 역사 사건 유형 5종
	public List<HistEventTypeDTO> selectHistTypes() {
		return mybatis.selectList("Event.selectHistTypes");
	}

	// 이민 목적지 후보 (현재 나라 제외, GDP 높은 순)
	public List<DestinationDTO> selectMigrateCandidates(String countryCode, int decade) {
		Map<String, Object> param = new HashMap<>();
		param.put("countryCode", countryCode);
		param.put("decade", decade);
		return mybatis.selectList("Event.selectMigrateCandidates", param);
	}

	// 인생의 특성 효과 (TRAIT_ID, EFFECT_VALUE)
	public List<Map<String, Object>> selectLifeTraitEffects(int lifeId) {
		return mybatis.selectList("Event.selectLifeTraitEffects", lifeId);
	}

}
