"""Kafka SSL context 헬퍼.

MSK 등 전송 구간 TLS(security_protocol=SSL/SASL_SSL)에서는 aiokafka 클라이언트에
ssl_context가 필요하다. security_protocol만 SSL로 두고 ssl_context를 주지 않으면
클라이언트 start()가 실패해 서비스가 CrashLoop한다(#144). 로컬(PLAINTEXT)에서는 None을
반환해 영향이 없다.
"""

from ssl import SSLContext

from aiokafka.helpers import create_ssl_context

from app.core.config import settings

_SSL_PROTOCOLS = {"SSL", "SASL_SSL"}


def kafka_ssl_context() -> SSLContext | None:
    """SSL/SASL_SSL이면 ssl_context(서버 TLS, 기본 시스템 CA)를 생성하고, 그 외에는 None."""
    if settings.kafka_security_protocol.upper() in _SSL_PROTOCOLS:
        return create_ssl_context()
    return None
