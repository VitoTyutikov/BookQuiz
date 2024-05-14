import json
import logging
import os
import tiktoken
import PyPDF2
from openai import OpenAI
import time

from api_openai_export.kafka.producer import send_questions_by_title

client = OpenAI()

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s\t - %(levelname)s\t : %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S')


def split_text_to_fit_token_limit(text, encoding, max_tokens=15000):
    if len(encoding.encode(text)) <= max_tokens:
        return [text]

    middle = len(text) // 2
    part1 = text[:middle]
    part2 = text[middle:]

    parts = []
    if len(encoding.encode(part1)) > max_tokens:
        parts.extend(split_text_to_fit_token_limit(
            part1, encoding, max_tokens))
    else:
        parts.append(part1)

    if len(encoding.encode(part2)) > max_tokens:
        parts.extend(split_text_to_fit_token_limit(
            part2, encoding, max_tokens))
    else:
        parts.append(part2)

    return parts


def extract_chapters_text(pdf_path, encoding, max_tokens=15000):
    general_outlines = ['Cover', 'Title Page', 'Copyright Page', 'Copyright', 'Credits', 'www.PacktPub.com', 'About the Author', 'About the Reviewers',  'Contents', 'Foreword', 'Preface',
                        'Acknowledgments', 'Index', 'Table of Contents', 'List of Tables', 'List of Figures', 'References',
                        'Appendix', 'Об авторах', 'Об изображении на обложке', 'Предисловие',
                        'Предметный указатель', 'Вступительное слово', 'Благодарности']

    chapters = []
    chapter_starts = []

    with open(pdf_path, 'rb') as file:
        reader = PyPDF2.PdfReader(file)
        outlines = reader.outline
        if(len(outlines) == 0):
            chapter_starts.append(("Book doesn't have outlines", 0))
        else:
            for item in outlines:
                if not isinstance(item, list):
                    if item.title not in general_outlines:
                        try:
                            page_number = reader.get_destination_page_number(
                                item) + 1
                            chapter_starts.append((item.title, page_number))
                        except:
                            continue

        for i in range(len(chapter_starts)):
            title, start_page = chapter_starts[i]
            end_page = chapter_starts[i+1][1] if i + \
                1 < len(chapter_starts) else len(reader.pages)

            chapter_text = ""
            for page_number in range(start_page, end_page):
                page = reader.pages[page_number - 1]
                try:
                    chapter_text += page.extract_text().strip()
                except:
                    continue

            chapter_parts = split_text_to_fit_token_limit(
                chapter_text, encoding, max_tokens)
            for part in chapter_parts:
                chapters.append((start_page, title.replace('\n', ' '), part))

    return chapters


def generate_questions(file_path: str):

    choosed_model = "gpt-3.5-turbo"
    # choosed_model = "gpt-4"
    # choosed_model = "gpt-4o"

    encoding = tiktoken.encoding_for_model("gpt-3.5-turbo")

    max_tokens = 9200
    sleep_time = 10
    if choosed_model == "gpt-4o":
        sleep_time = 20
        encoding = tiktoken.encoding_for_model("gpt-4o")
    elif choosed_model == "gpt-4":
        sleep_time = 60
        encoding = tiktoken.encoding_for_model("gpt-4")

    book_id = file_path.split('/')[-1].split('_')[0]
    chapters = extract_chapters_text(file_path, encoding, max_tokens)
    
    for start_page, title, chapter_text in chapters:
        logging.info(
            f"Start page: {start_page}, Title: {title}, Tokens: {len(encoding.encode(chapter_text))}")

    prev_title = chapters[0][1]
    questions_by_title = {}
    total_titles = len(chapters)
    current_title = 1
    prev_start_page = chapters[0][0]
    for start_page, title, chapter in chapters:

        if prev_title != title:
            send_questions_by_title(book_id,
                                    prev_title, prev_start_page, questions_by_title, current_title, total_titles)
            prev_start_page = start_page
            prev_title = title

        response = client.chat.completions.create(
            model=choosed_model,
            messages=[
                {"role": "system", "content": "You are an assistant who creates questions based on provided text. \
                You must return an array of questions and each of the questions has an array of answers. Questions should be on the text of the book only.\
                All answers should have a boolean variable named is_correct and store true if the answer is correct and false if the answer is incorrect.\
                There should be only one correct answer for each question.\
                Response should be JSON object. It should have fileds with names: \"questions\" for questions(array),\
                \"question\" for question(string), \"answers\" for answers(array), \"answer\" for answer(string) and \"is_correct\" for each answer."
                 },
                {"role": "user", "content": chapter +
                    "\n Create 2 questions on the text above in the language of the text, language is important. Don't use the word text in questions."}
            ],
            response_format={"type": "json_object"}
        )

        questions = json.loads(
            str(response.choices[0].message.content)).get('questions')

        logging.info(
            f"Generated questions for {title}: ({current_title}/{total_titles})")
        if title not in questions_by_title:
            questions_by_title[title] = []
        questions_by_title[title].extend(questions)
        current_title = current_title + 1
        time.sleep(sleep_time)

    send_questions_by_title(book_id, title, start_page, questions_by_title,
                            current_title, total_titles)


# generate_questions('/home/shieldbr/projects/BookQuiz/Horstmann C.S. - Core Java. Vol. 2. Advanced Features - 2019.pdf')
