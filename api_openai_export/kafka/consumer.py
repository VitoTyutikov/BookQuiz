import logging
from confluent_kafka import Consumer, KafkaException, KafkaError
import sys

from api_openai_export.utils import generate_questions


def kafka_consumer():
    conf = {
        # Change this to your Kafka server configuration
        'bootstrap.servers': "localhost:29092",
        'group.id': "generate_consumer",
        'auto.offset.reset': 'earliest',
    }

    # Create Consumer instance
    consumer = Consumer(**conf)

    try:
        # Subscribe to topic
        # Change 'your_topic_name' to the topic you are using
        consumer.subscribe(['generation_request'])

        while True:
            msg = consumer.poll(timeout=1.0)
            if msg is None:
                continue
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    # End of partition event
                    sys.stderr.write('%% %s [%d] reached end at offset %d\n' %
                                     (msg.topic(), msg.partition(), msg.offset()))
                elif msg.error():
                   raise KafkaException(msg.error())
            else:
                print("Consumed event from topic {topic}: key = {key:12} value = {value:12}".format(
                    topic=msg.topic(), key=msg.key().decode('utf-8'), value=msg.value().decode('utf-8')))
                try:
                    generate_questions("../files/"+msg.value().decode('utf-8'))
                except Exception as e:
                    sys.stdout.write(
                        "Error while generating questions: "+str(e))
    except KeyboardInterrupt:
        sys.stderr.write('%% Aborted by user\n')
    finally:
        # Close down consumer to commit final offsets.
        consumer.close()


if __name__ == '__main__':
    kafka_consumer()