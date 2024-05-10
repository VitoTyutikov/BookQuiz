import json
from confluent_kafka import Producer


def kafka_producer():
    conf = {
        'bootstrap.servers': "localhost:29092",  # Adjust as per your Kafka setup

    }
    return Producer(**conf)


def delivery_callback(err, msg):
    if err:
        print('ERROR: Message failed delivery: {}'.format(err))
    else:
        print("Produced event to topic {topic}: key = {key:12} value = {value:12}".format(
            topic=msg.topic(), key=msg.key().decode('utf-8'), value=msg.value().decode('utf-8')))


def send_questions_by_title(book_id, title, start_page, questions_by_title, current_title, total_titles):
    grouped_questions = json.dumps(
        {"book_id": book_id, "title": title, "start_page": start_page, "questions": questions_by_title[title]}, indent=4, ensure_ascii=False)

    producer = kafka_producer()
    # producer.produce('generation_progress', f"Generated questions for {title} ({current_title}/{total_titles})")
    producer.produce('generation_progress',
                     str(json.dumps(
                         {"current_title": current_title, "total_titles": total_titles})),
                     callback=delivery_callback)
    
    producer.produce('generation_response',
                     str(grouped_questions),
                     callback=delivery_callback)
    
    producer.poll(10000)
    producer.flush()  # Ensure all messages are sent

    # print(f"Sent progress for {current_title} of {total_titles}")
    # print(f"Sent questions for {title}")
