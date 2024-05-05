import json
import os
import tiktoken
import PyPDF2
from openai import OpenAI
import time

from api_openai.kafka.producer import send_questions_by_title

client = OpenAI()


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


def extract_chapters_text(pdf_path, encoding):
    general_outlines = ['Cover', 'Title Page', 'Copyright Page', 'Copyright', 'Credits', 'www.PacktPub.com', 'About the Author', 'About the Reviewers',  'Contents', 'Foreword', 'Preface',
                        'Acknowledgments', 'Index', 'Table of Contents', 'List of Tables', 'List of Figures', 'References',
                        'Appendix', 'Об авторах', 'Об изображении на обложке', 'Предисловие', 
                        'Предметный указатель','Вступительное слово','Благодарности']

    chapters = []
    chapter_starts = []

    with open(pdf_path, 'rb') as file:
        reader = PyPDF2.PdfReader(file)
        outlines = reader.outline

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

            # chapter_text = " ".join(page.extract_text().strip() for page in reader.pages[start_page:end_page-4])
            chapter_text = ""
            for page_number in range(start_page, end_page):
                page = reader.pages[page_number - 1]
                try:
                    chapter_text += page.extract_text().strip()
                except:
                    continue

            # Split and store chapter text with titles
            chapter_parts = split_text_to_fit_token_limit(
                chapter_text, encoding)
            for part in chapter_parts:
                chapters.append((title, part))

    return chapters


# encoding = tiktoken.encoding_for_model("gpt-4")
# encoding = tiktoken.encoding_for_model("gpt-4-turbo")

# def send_questions_by_title(title, questions_by_title, current_title, total_titles):
#     grouped_questions = json.dumps(
#         {"title": title, "questions": questions_by_title[title]}, indent=4, ensure_ascii=False)

#     # TODO: change writing to file to send via kafka and add to database
#     with open(f'./{title}.json', 'w') as file:
#         file.write(grouped_questions)


def generate_questions(file_path: str):
    encoding = tiktoken.encoding_for_model("gpt-3.5-turbo")

    # pdf_path = '/home/shieldbr/projects/BookQuiz/api_openai/test_books/Joshua_Bloch_-_Effective_Java_3rd__2018_ENG.pdf'
    # pdf_path = '/home/shieldbr/projects/BookQuiz/api_openai/test_books/Java оптимизация программ.pdf'
    # chapters = extract_chapters_text(pdf_path, encoding)
    # print([x[0] for x in os.walk("./")])
    
    book_id = file_path.split('/')[-1].split('_')[0]
    chapters = extract_chapters_text(file_path, encoding)
    for title, chapter_text in chapters:
        print(f"Title: {title}, Tokens: {len(encoding.encode(chapter_text))}")

    # encoding = tiktoken.get_encoding("cl100k_base")
    # encoding = tiktoken.encoding_for_model("gpt-4")
    # encoding = tiktoken.encoding_for_model("gpt-4-turbo")

    prev_title = chapters[0][0]
    questions_by_title = {}
    total_titles = len(chapters)
    current_title = 1
    for title, chapter in chapters:
        choosed_model = "gpt-3.5-turbo"
        if prev_title != title:
            send_questions_by_title(book_id,
                prev_title, questions_by_title, current_title, total_titles)
            prev_title = title
            current_title = current_title + 1

        response = client.chat.completions.create(
            model=choosed_model,  # Replace with your GPT-4 model name
            messages=[
                {"role": "system", "content": "You are an assistant who creates questions based on provided text. \
                You must return an array of questions and each of the questions has an array of answers. Questions should be on the text of the book only.\
                All answers should have a boolean variable named is_correct and store true if the answer is correct and false if the answer is incorrect.\
                There should be only one correct answer for each question.\
                Response should be JSON object. It should have fileds with names: \"questions\" for questions,\
                \"question\" for question, \"answers\" for answers, \"answer\" for answer and \"is_correct\" for each answer."},
                {"role": "user", "content": chapter + \
                    "\n Create 2 questions on the text above in the language of the text. Don't use the word text in questions."}
            ],
            response_format={"type": "json_object"}
        )

        questions = json.loads(
            str(response.choices[0].message.content)).get('questions')

        # Store questions in the dictionary under the current title
        if title not in questions_by_title:
            questions_by_title[title] = []
        questions_by_title[title].extend(questions)
        time.sleep(30)

    send_questions_by_title(book_id,title, questions_by_title,
                            current_title, total_titles)

    # print("\n\n", questions_by_title, "\n\n")
    # final_questions = json.dumps(
    #     {"title": title, "questions": questions_by_title[title]}, indent=4, ensure_ascii=False)
    # with open(f'./{title}.json', 'w') as file:
    #     file.write(final_questions)
    # print(final_questions)


# generate_questions('/home/shieldbr/projects/BookQuiz/api_openai/files/buildind RESTful web service with spring.pdf')
