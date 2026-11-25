package com.example.demo.story;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

// C19 AI 인생 이야기 (쿼리는 resources/mappers/story-mapper.xml)
@Mapper
public interface StoryDao {
	StoryDTO selectStory(int lifeId);					// 저장된 이야기 (없으면 null)
	int insertStory(StoryDTO story);					// 새 이야기 저장
	int deleteStory(int lifeId);						// 예전 방식으로 만든 이야기 지우기 (다시 만들 때)
	List<HistoryFactDTO> selectHistoryFacts(int lifeId);	// 근거: 살았던 나라·기간의 역사 사건
	List<DecadeStatDTO> selectDecadeStats(int lifeId);	// 근거: 살았던 연대의 나라 통계
	List<SourceDTO> selectSources(List<Integer> ids);	// 저장된 이야기의 출처 다시 불러오기
}
