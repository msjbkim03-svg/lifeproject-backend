package com.example.demo.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.engine.LifeDTO;
import com.example.demo.event.dto.DestinationDTO;
import com.example.demo.event.dto.EventResultDTO;
import com.example.demo.event.dto.EventTemplateDTO;
import com.example.demo.event.dto.GameEventDTO;

@RestController
@RequestMapping("/event")
public class EventController {

	private final EventService eServ;
	private final EventDAO eDAO;

	public EventController(EventService eServ, EventDAO eDAO) {
		this.eServ = eServ;
		this.eDAO = eDAO;
	}

	@GetMapping("/templates")
	public ResponseEntity<List<EventTemplateDTO>> getTemplates() {
		return ResponseEntity.ok(eServ.getTemplates());
	}

	// [테스트] 올해 일어난 이벤트 문장·효과
	// 예:
	// /event/test/roll?age=30&year=1960&country=IND&gender=F&wealth=3&name=프리야%20샤르마
	@GetMapping("/test/roll")
	public ResponseEntity<List<EventResultDTO>> testRoll(int age, int year, String country, String gender, int wealth,
			String name) {
		List<String> expIds = new ArrayList<>(); // 테스트: 아직 겪은 일 없음
		List<EventResultDTO> result = new ArrayList<>();

		for (EventTemplateDTO t : eServ.rollTemplates(age, year, country, gender, null)) {
			GameEventDTO e = eServ.pickEvent(t, age, gender, wealth, expIds);
			if (e != null) {
				result.add(eServ.buildResult(t, e, name, age, year, country));
			}
		}
		return ResponseEntity.ok(result);
	}

	// 테스트: 엔진 연결용 rollYear 확인 (최종 전에 삭제)
	@GetMapping("/test/year")
	public List<EventResultDTO> testYear(@RequestParam int age, @RequestParam int birthYear,
			@RequestParam String country, @RequestParam String gender, @RequestParam int wealth,
			@RequestParam String name, @RequestParam(defaultValue = "N") String married,
			@RequestParam(defaultValue = "0") int lifeId) {
		LifeDTO life = new LifeDTO();
		life.setLifeId(lifeId);
		life.setCurrentAge(age);
		life.setBirthYear(birthYear);
		life.setCurrentCountry(country);
		life.setGender(gender);
		life.setWealthClass(wealth);
		life.setName(name);
		life.setMarriedYn(married);
		return eServ.rollYear(life);
	}

	// 테스트: 선택지 고르기 (최종 전에 삭제)
	@GetMapping("/test/choice")
	public EventResultDTO testChoice(@RequestParam int choiceId, @RequestParam int age, @RequestParam int birthYear,
			@RequestParam String country, @RequestParam(defaultValue = "0") int lifeId, @RequestParam String name) {
		LifeDTO life = new LifeDTO();
		life.setCurrentAge(age);
		life.setBirthYear(birthYear);
		life.setCurrentCountry(country);
		life.setName(name);
		life.setLifeId(lifeId);
		return eServ.applyChoice(life, choiceId, age);
	}

	// 테스트: 역사 이벤트 후보 (최종 전에 삭제)
	@GetMapping("/test/hist")
	public List<GameEventDTO> testHist(@RequestParam String country, @RequestParam int year, @RequestParam int age,
			@RequestParam String gender) {
		return eDAO.selectHistCandidates(country, year, age, gender);
	}

	// 테스트: 역사 이벤트 굴리기 (최종 전에 삭제)
	@GetMapping("/test/histroll")
	public List<EventResultDTO> testHistRoll(@RequestParam String country, @RequestParam int year,
			@RequestParam int age, @RequestParam String gender, @RequestParam String name) {
		return eServ.rollHist(country, year, age, gender, name, new ArrayList<>());
	}

	// 테스트: 이민 목적지 후보 (최종 전에 삭제)
	@GetMapping("/test/dest")
	public List<DestinationDTO> testDest(@RequestParam String country, @RequestParam int year) {
		return eServ.pickDestinations(country, year);
	}

	// 테스트: 목적지 고르기 (최종 전에 삭제)
	@GetMapping("/test/destchoice")
	public EventResultDTO testDestChoice(@RequestParam String country, @RequestParam String dest, @RequestParam int age,
			@RequestParam int birthYear, @RequestParam String name, @RequestParam String eventId) {
		LifeDTO life = new LifeDTO();
		life.setCurrentCountry(country);
		life.setBirthYear(birthYear);
		life.setName(name);
		return eServ.applyDestination(life, dest, age, eventId);
	}

	// 테스트: 인생의 특성 효과 (최종 전에 삭제)
	@GetMapping("/test/trait")
	public Map<String, Double> testTrait(@RequestParam int lifeId) {
		return eServ.getTraitEffects(lifeId);
	}

	// 테스트: 템플릿별 최종 확률 (특성 적용 확인용, 최종 전에 삭제)
	@GetMapping("/test/prob")
	public Map<String, Double> testProb(@RequestParam int age, @RequestParam int year, @RequestParam String country,
			@RequestParam String gender, @RequestParam(defaultValue = "0") int lifeId) {
		Map<String, Object> stat = eDAO.selectCountryStat(country, year / 10 * 10);
		Map<String, Double> traits = eServ.getTraitEffects(lifeId);
		Map<String, Double> result = new LinkedHashMap<>();
		for (EventTemplateDTO t : eServ.getTemplates()) {
			result.put(t.getTemplateId(), eServ.finalProbability(t, age, stat, country, gender, traits));
		}
		return result;
	}
}
