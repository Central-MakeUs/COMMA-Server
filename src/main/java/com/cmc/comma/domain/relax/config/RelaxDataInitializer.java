package com.cmc.comma.domain.relax.config;

import com.cmc.comma.domain.checklist.entity.Mood;
import com.cmc.comma.domain.checklist.entity.TimeBudget;
import com.cmc.comma.domain.relax.entity.Relax;
import com.cmc.comma.domain.relax.repository.RelaxRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발자 관리 고정 데이터인 휴식 45개(9조합 × 5)를 최초 1회 seed 한다.
 * 이미 데이터가 있으면 아무것도 하지 않는다(멱등).
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class RelaxDataInitializer implements ApplicationRunner {

    private final RelaxRepository relaxRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (relaxRepository.count() > 0) {
            return;
        }

        List<Relax> relaxes = new ArrayList<>();

        // A(멍하고 싶어) - X(잠깐)
        add(relaxes, Mood.A, TimeBudget.X, "창 밖 바라보기", "환기하면서 맑은 공기를 마셔보세요");
        add(relaxes, Mood.A, TimeBudget.X, "좋아하는 음악 듣기", "눈 감고 음악 들으면서 쉬어봐요");
        add(relaxes, Mood.A, TimeBudget.X, "따뜻한 음료 마시기", "따뜻하게 마시면서 피곤을 풀어봐요");
        add(relaxes, Mood.A, TimeBudget.X, "책 한 챕터 읽기", "감명깊은 책 한 챕터만 천천히 읽어요");
        add(relaxes, Mood.A, TimeBudget.X, "향초 키고 명상하기", "눈 감고 머리를 정리해봐요");

        // A(멍하고 싶어) - Y(여유)
        add(relaxes, Mood.A, TimeBudget.Y, "공원 벤치에 앉아있기", "공원 벤치에서 지나가는 사람 구경해봐요");
        add(relaxes, Mood.A, TimeBudget.Y, "좋아하는 영화 보기", "잔잔한 영화 한편 어때요?");
        add(relaxes, Mood.A, TimeBudget.Y, "ASMR 듣기", "조용히 ASMR 들으면서 쉬어봐요");
        add(relaxes, Mood.A, TimeBudget.Y, "레고 조립하기", "블록이 맞물리는 손맛을 느껴봐요");
        add(relaxes, Mood.A, TimeBudget.Y, "거리뷰로 랜선 여행하기", "방구석에서 세계를 누벼봐요");

        // A(멍하고 싶어) - Z(넉넉)
        add(relaxes, Mood.A, TimeBudget.Z, "드라이브하기", "아무생각말고 창문열고 드라이브해봐요");
        add(relaxes, Mood.A, TimeBudget.Z, "카페에서 멍때리기", "조용한 카페가서 여유를 즐겨봐요");
        add(relaxes, Mood.A, TimeBudget.Z, "물 멍때리기", "가까운 바다나 강에 가서 물멍 어때요?");
        add(relaxes, Mood.A, TimeBudget.Z, "책 한 권 읽기", "여러번 나눠서 오늘 책 한 권 읽어봐요");
        add(relaxes, Mood.A, TimeBudget.Z, "영화 시리즈 몰아보기", "하루종일 좋아하는 시리즈물 몰아봐요");

        // B(기분 전환이 필요해) - X(잠깐)
        add(relaxes, Mood.B, TimeBudget.X, "욕조에 물 받아 목욕하기", "따뜻하게 몸을 녹여봐요");
        add(relaxes, Mood.B, TimeBudget.X, "침구류, 옷 정리하기", "집이 깨끗해지면 기분이 좋아져요");
        add(relaxes, Mood.B, TimeBudget.X, "패드나 공책에 그림 그리기", "아무 생각없이 그림 그려보면 어떨까요?");
        add(relaxes, Mood.B, TimeBudget.X, "노트에 좋아하는 구절 적기", "좋아하는 글귀나 노래 가사를 적어봐요");
        add(relaxes, Mood.B, TimeBudget.X, "방 가구 재배치하기", "가구를 옮기기만 해도 기분이 전환돼요");

        // B(기분 전환이 필요해) - Y(여유)
        add(relaxes, Mood.B, TimeBudget.Y, "동네 산책하고 사진 찍기", "동네 예쁜 골목길 사진을 찍어봐요");
        add(relaxes, Mood.B, TimeBudget.Y, "인터넷 쇼핑하기", "평소 사고 싶었던 것들을 찾아봐요");
        add(relaxes, Mood.B, TimeBudget.Y, "좋아하는 술 마시기", "평소 아끼던 술을 조금 마셔봐요");
        add(relaxes, Mood.B, TimeBudget.Y, "좋아하는 음식 요리하기", "맛있는 요리 해먹으면 좋아요");
        add(relaxes, Mood.B, TimeBudget.Y, "팟캐스트 듣기", "목소리를 들으며 교양을 쌓아봐요");

        // B(기분 전환이 필요해) - Z(넉넉)
        add(relaxes, Mood.B, TimeBudget.Z, "버스 종점까지 가기", "길거리의 사람들을 보며 힐링해봐요");
        add(relaxes, Mood.B, TimeBudget.Z, "영화관 가기", "큰 화면으로 영화 한 편 보고 와요");
        add(relaxes, Mood.B, TimeBudget.Z, "재래시장 구경하기", "시장가서 맛있는 것도 먹어봐요");
        add(relaxes, Mood.B, TimeBudget.Z, "찜질방 가기", "찜질방에서 뜨끈하게 피로를 풀어봐요");
        add(relaxes, Mood.B, TimeBudget.Z, "향수 원데이 클래스 하기", "나만의 향을 찾는 여정을 떠나요");

        // C(가볍게 해볼 수 있어) - X(잠깐)
        add(relaxes, Mood.C, TimeBudget.X, "계단으로 옥상 올라가기", "옥상에 올라가서 자유를 느껴보세요");
        add(relaxes, Mood.C, TimeBudget.X, "편의점 신상 투어", "새로운 맛을 찾아봐요");
        add(relaxes, Mood.C, TimeBudget.X, "요가하기", "굳어있던 근육을 풀어봐요");
        add(relaxes, Mood.C, TimeBudget.X, "근처 화단의 식물 물 주기", "작은 생명의 신비로움을 느껴봐요");
        add(relaxes, Mood.C, TimeBudget.X, "손 편지 쓰기", "소중한 사람에게 마음을 표현해봐요");

        // C(가볍게 해볼 수 있어) - Y(여유)
        add(relaxes, Mood.C, TimeBudget.Y, "집 근처 조깅하기", "간단하게 조깅하면서 몸을 풀어봐요");
        add(relaxes, Mood.C, TimeBudget.Y, "자전거 타기", "집 근처에서 자전거 타면서 바람 쐬어봐요");
        add(relaxes, Mood.C, TimeBudget.Y, "대형마트에서 쇼핑하기", "평소 필요했던 물건들을 사봐요");
        add(relaxes, Mood.C, TimeBudget.Y, "코인노래방 가기", "노래 부르면서 스트레스 날려요");
        add(relaxes, Mood.C, TimeBudget.Y, "동네 빵집 투어하기", "돌아다니면서 맛있는 빵 찾아봐요");

        // C(가볍게 해볼 수 있어) - Z(넉넉)
        add(relaxes, Mood.C, TimeBudget.Z, "전시 구경하기", "평소 관심있던 전시장에 가봐요");
        add(relaxes, Mood.C, TimeBudget.Z, "동네 서점 구경하기", "서점을 천천히 구경하면서 여유를 찾아요");
        add(relaxes, Mood.C, TimeBudget.Z, "스포츠 경기 보러가기", "좋아하는 스포츠 팀 응원하면서 스트레스 날려요");
        add(relaxes, Mood.C, TimeBudget.Z, "등산하기", "집 근처 산을 등산해봐요");
        add(relaxes, Mood.C, TimeBudget.Z, "다른 지역 맛집 찾아가기", "차 타고 유명한 맛집 찾아가봐요");

        relaxRepository.saveAll(relaxes);
    }

    private void add(List<Relax> list, Mood mood, TimeBudget timeBudget, String name, String description) {
        list.add(Relax.builder()
                .mood(mood)
                .timeBudget(timeBudget)
                .name(name)
                .description(description)
                .build());
    }
}