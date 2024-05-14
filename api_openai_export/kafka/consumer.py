import logging
from confluent_kafka import Consumer, KafkaException, KafkaError
import sys

from api_openai_export.utils import generate_questions

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s\t - %(levelname)s\t : %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S')

def kafka_consumer():
    conf = {
        # Change this to your Kafka server configuration
        'bootstrap.servers': "localhost:29092",
        'group.id': "generate_consumer",
        'auto.offset.reset': 'earliest',
        'max.poll.interval.ms': '600000*2'
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
                  logging.error(msg.error())
            else:
                logging.info('Received message: %s\n' %
                                 (msg.value().decode('utf-8')))
                try:
                    generate_questions("../files/"+msg.value().decode('utf-8'))
                except Exception as e:
                    logging.warning(
                        "Error while generating questions: "+str(e))
                    
    except KeyboardInterrupt:
        sys.stderr.write('%% Aborted by user\n')
    finally:
        # Close down consumer to commit final offsets.
        consumer.close()


if __name__ == '__main__':
    kafka_consumer()