package com.example.demo.quest;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// C01 퀘스트 설정 - 검사하고 저장하는 곳
@Service
public class QuestService {

	private QuestDAO dao;

	public QuestService(QuestDAO dao) {
		this.dao = dao;
	}

	// 퀘스트 목록 (IS_ACTIVE = 'Y' 만)
	public List<QuestDTO> list() {
		return dao.selectActiveQuests();
	}

	// 고른 퀘스트 저장: 문제가 있으면 오류 문구, 성공하면 null
	@Transactional // 1~3개를 한 번에 저장 (하나라도 실패하면 모두 취소)
	public String saveLifeQuests(int memberId, QuestSaveDTO dto) {
		List<Integer> ids = dto.getQuestIds();

		// 1. 개수: 1~3개, 같은 퀘스트 두 번은 안 됨
		if (ids == null || ids.isEmpty() || ids.size() > 3) {
			return "퀘스트는 1~3개 골라 주세요.";
		}
		if (new HashSet<>(ids).size() != ids.size()) {
			return "같은 퀘스트를 두 번 고를 수 없어요.";
		}

		// 2. 내 인생이고, 지금 진행 중이고, 태어난 직후(0살)인지
		if (dao.countMyPlayingLife(dto.getLifeId(), memberId) == 0) {
			return "퀘스트는 내 인생이 막 태어났을 때만 정할 수 있어요.";
		}

		// 3. 이미 퀘스트를 정한 인생이면 다시 못 정함 (목표는 고정)
		if (dao.countLifeQuests(dto.getLifeId()) > 0) {
			return "이 인생의 퀘스트는 이미 정해졌어요.";
		}

		// 4. 고른 퀘스트가 모두 사용 중(IS_ACTIVE='Y')인 퀘스트인지
		for (int questId : ids) {
			if (dao.countActiveQuest(questId) == 0) {
				return "없는 퀘스트예요.";
			}
		}

		// 5. LIFE_QUEST 에 한 줄씩 저장 (ACHIEVED 는 인생이 끝날 때 C12에서 판정)
		for (int questId : ids) {
			dao.insertLifeQuest(dto.getLifeId(), questId);
		}
		return null;
	}
}
