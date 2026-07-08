package com.cmc.comma.domain.user.service;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * 랜덤 닉네임 생성기. 형용사(2~3자) + 명사(2~3자) + 숫자 4자 조합.
 * 최대 길이 = 3 + 3 + 4 = 10자로 닉네임 제한(10자)에 맞춤.
 */
@Component
public class NicknameGenerator {

    private static final String[] ADJECTIVES = {
            "행복한", "즐거운", "용감한", "씩씩한", "귀여운", "멋진", "착한", "빠른", "밝은", "조용한",
            "따뜻한", "시원한", "상냥한", "활발한", "엉뚱한", "신나는", "든든한", "포근한", "명랑한", "다정한",
            "재밌는", "깜찍한", "날쌘", "똑똑한", "친절한", "웃긴", "느긋한", "차분한", "상큼한", "발랄한"
    };

    private static final String[] NOUNS = {
            "고양이", "강아지", "토끼", "여우", "사슴", "판다", "너구리", "다람쥐", "병아리", "참새",
            "부엉이", "펭귄", "수달", "물개", "하마", "코알라", "햄스터", "오리", "거북이", "달팽이",
            "나비", "개구리", "두더지", "고래", "상어", "문어", "호랑이", "사자", "곰돌이", "돌고래"
    };

    public String generate() {
        String adjective = pick(ADJECTIVES);
        String noun = pick(NOUNS);
        int number = ThreadLocalRandom.current().nextInt(10000);
        return adjective + noun + String.format("%04d", number);
    }

    private String pick(String[] words) {
        return words[ThreadLocalRandom.current().nextInt(words.length)];
    }
}