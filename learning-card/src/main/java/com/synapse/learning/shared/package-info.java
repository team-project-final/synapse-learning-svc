/**
 * Shared kernel — 모든 모듈에서 자유롭게 참조 가능한 공통 클래스.
 * OPEN 타입으로 선언하여 하위 패키지(exception 등)도 외부 모듈에서 접근 가능.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.synapse.learning.shared;

import org.springframework.modulith.ApplicationModule;