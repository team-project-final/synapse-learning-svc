// 이 파일 하나가 "card 폴더는 독립 모듈입니다" 선언
// OPEN: srs 모듈에서 FlashCard, FlashCardPort 등 서브패키지 타입 참조 허용
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.synapse.learning.card;

import org.springframework.modulith.ApplicationModule;