import pytest
from testcontainers.kafka import KafkaContainer


@pytest.fixture(scope="session")
def kafka_container() -> KafkaContainer:
    with KafkaContainer("confluentinc/cp-kafka:7.6.0") as kafka:
        yield kafka


@pytest.fixture(scope="session")
def kafka_bootstrap(kafka_container: KafkaContainer) -> str:
    return str(kafka_container.get_bootstrap_server())
