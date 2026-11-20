package com.example.demo.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.event.dto.EventChoiceDTO;
import com.example.demo.event.dto.EventTemplateDTO;
import com.example.demo.event.dto.GameEventDTO;

//A03 이벤트 관리 (쿼리는 admin-event-mapper.xml, namespace AdminEvent)
@Repository
public class AdminEventDAO {

	private final SqlSessionTemplate mybatis;

	public AdminEventDAO(SqlSessionTemplate mybatis) {
		this.mybatis = mybatis;
	}

	// 목록 (조건이 null 이면 그 조건은 안 씀)
	public List<AdminEventRowDTO> selectEventList(String type, String reviewed, String aiNarrate, String keyword) {
		Map<String, Object> param = new HashMap<>();
		param.put("type", type);
		param.put("reviewed", reviewed);
		param.put("aiNarrate", aiNarrate);
		param.put("keyword", keyword);
		return mybatis.selectList("AdminEvent.selectEventList", param);
	}

	// 목록과 같은 쿼리로 한 줄만 (상세 화면 머리 정보)
	public AdminEventRowDTO selectEventRow(String eventId) {
		Map<String, Object> param = new HashMap<>();
		param.put("eventId", eventId);
		return mybatis.selectOne("AdminEvent.selectEventList", param);
	}

	public GameEventDTO selectEvent(String eventId) {
		return mybatis.selectOne("AdminEvent.selectEvent", eventId);
	}

	public EventTemplateDTO selectTemplate(String templateId) {
		return mybatis.selectOne("AdminEvent.selectTemplate", templateId);
	}
	
	public int insertEvent(GameEventDTO e) {
		return mybatis.insert("AdminEvent.insertEvent", e);
	}

	// 바뀐 줄 수 (0 이면 없는 이벤트)
	public int updateEvent(GameEventDTO e) {
		return mybatis.update("AdminEvent.updateEvent", e);
	}

	public int updateAiNarrate(String eventId, String value) {
		Map<String, Object> param = new HashMap<>();
		param.put("eventId", eventId);
		param.put("value", value);
		return mybatis.update("AdminEvent.updateAiNarrate", param);
	}
	
	// ================= 선택지 =================

	public int selectNextChoiceOrder(String eventId) {
		return mybatis.selectOne("AdminEvent.selectNextChoiceOrder", eventId);
	}

	public int countChoiceOrder(String eventId, int choiceOrder) {
		Map<String, Object> param = new HashMap<>();
		param.put("eventId", eventId);
		param.put("choiceOrder", choiceOrder);
		return mybatis.selectOne("AdminEvent.countChoiceOrder", param);
	}

	public List<String> selectResultActions() {
		return mybatis.selectList("AdminEvent.selectResultActions");
	}

	// 실행 후 c.getChoiceId() 에 새 번호가 들어 있음 (selectKey)
	public int insertChoice(EventChoiceDTO c) {
		return mybatis.insert("AdminEvent.insertChoice", c);
	}

	public int updateChoice(EventChoiceDTO c) {
		return mybatis.update("AdminEvent.updateChoice", c);
	}
	
	// ================= 템플릿 =================
	public int updateTemplateProb(EventTemplateDTO t) {
		return mybatis.update("AdminEvent.updateTemplateProb", t);
	}
	
	// ================= 역사 문장 검수 =================

	// 바뀐 줄 수 (0 이면 없거나 역사 이벤트가 아님)
	public int updateBaseText(String eventId, String baseText) {
		Map<String, Object> param = new HashMap<>();
		param.put("eventId", eventId);
		param.put("baseText", baseText);
		return mybatis.update("AdminEvent.updateBaseText", param);
	}
	
}