package com.example.demo.event;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 받침에 맞춰 조사 고르기: 민수은(는) → 민수는, 대한민국을(를) → 대한민국을, 서울(으)로 → 서울로
public class Josa {

	// 앞 글자 1개 + 조사 표기
	private static final Pattern P = Pattern
			.compile("(.)(은\\(는\\)|이\\(가\\)|\\(이\\)가|을\\(를\\)|과\\(와\\)|\\(으\\)로|\\(이\\)라고)");

	public static String apply(String text) {
		if (text == null)
			return null;
		Matcher m = P.matcher(text);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			String prev = m.group(1);
			String josa = pick(jong(prev.charAt(0)), m.group(2));
			m.appendReplacement(sb, Matcher.quoteReplacement(prev + josa));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	// 받침 번호: 0 = 받침 없음, 8 = ㄹ 받침, 그 외 = 다른 받침
	private static int jong(char c) {
		if (c >= '가' && c <= '힣')
			return (c - '가') % 28;
		if (c == '1' || c == '7' || c == '8')
			return 8; // 일, 칠, 팔 (ㄹ 받침)
		if (c == '0' || c == '3' || c == '6')
			return 1; // 영, 삼, 육 (받침 있음)
		return 0; // 2, 4, 5, 9, 영문 등은 받침 없음으로
	}

	private static String pick(int jong, String mark) {
		boolean has = jong != 0;
		switch (mark) {
		case "은(는)":
			return has ? "은" : "는";
		case "이(가)":
		case "(이)가":
			return has ? "이" : "가";
		case "을(를)":
			return has ? "을" : "를";
		case "과(와)":
			return has ? "과" : "와";
		case "(으)로":
			return has && jong != 8 ? "으로" : "로"; // ㄹ 받침은 로 (서울로)
		case "(이)라고":
			return has ? "이라고" : "라고";
		default:
			return mark;
		}
	}
}