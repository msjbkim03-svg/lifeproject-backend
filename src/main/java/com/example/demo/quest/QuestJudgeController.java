package com.example.demo.quest;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quest/judge")
public class QuestJudgeController {

	private final QuestJudgeService qServ;

	public QuestJudgeController(QuestJudgeService qServ) {
		this.qServ = qServ;
	}

	// 테스트: 포인트 지급 (최종 전에 삭제)
	@GetMapping("/test/give")
	public int testGive(@RequestParam int lifeId) {
		return qServ.givePoints(lifeId);
	}

	// 테스트: 퀘스트 판정 (최종 전에 삭제)
	@GetMapping("/test/judge")
	public Map<Integer, Boolean> testJudge(@RequestParam int lifeId) {
		return qServ.judgeQuests(lifeId);
	}

	// 테스트: 판정 + 지급 (최종 전에 삭제)
	@GetMapping("/test/end")
	public Map<String, Object> testEnd(@RequestParam int lifeId) {
		return qServ.judgeAndGive(lifeId);
	}

}