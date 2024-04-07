import tiktoken
import PyPDF2
from openai import OpenAI
import time

client = OpenAI()


def split_text_to_fit_token_limit(text, encoding, max_tokens=15500):
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
    general_outlines = ['Cover', 'Title Page', 'Copyright Page', 'Contents', 'Foreword', 'Preface',
                        'Acknowledgments', 'Index', 'Table of Contents', 'List of Tables', 'List of Figures', 'References',
                        'Appendix']

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


# Main execution
encoding = tiktoken.encoding_for_model("gpt-3.5-turbo")
encoding = tiktoken.get_encoding("cl100k_base")
encoding = tiktoken.encoding_for_model("gpt-4")
encoding = tiktoken.encoding_for_model("gpt-4-turbo")

# pdf_path = '/home/shieldbr/projects/BookQuiz/api_openai/test_books/Java_CoreAdvancedFeatures.pdf'
pdf_path = '/home/shieldbr/projects/BookQuiz/api_openai/test_books/Joshua_Bloch_-_Effective_Java_3rd__2018_ENG.pdf'
chapters = extract_chapters_text(pdf_path, encoding)
for title, chapter_text in chapters:
    print(f"Title: {title}, Tokens: {len(encoding.encode(chapter_text))}")

# print(encoding.encode(chapters[1][1]))

# for title, chapter_text in chapters:
#     print(f"Title: {title}, Tokens: {len(encoding.encode(chapter_text))}")

# encoding = tiktoken.get_encoding("cl100k_base")
# encoding = tiktoken.encoding_for_model("gpt-4")
# encoding = tiktoken.encoding_for_model("gpt-4-turbo")


# for title, chapter in chapters:
#     choosed_model = "gpt-3.5-turbo"

#     response = client.chat.completions.create(
#         model=choosed_model,  # Replace with your GPT-4 model name
#         messages=[
#             {"role": "system", "content": "You are an assistant who creates questions based on provided text. \
#             You must return an array of questions and each of the questions has an array of answers. \
#             All answers should have a boolean variable named is_correct and store true if the answer is correct and false if the answer is incorrect.\
#             There should be only one correct answer for each question.\
#             Response should be JSON object. It should have fileds with names: \"questions\" for questions,\
#             \"question\" for question, \"answers\" for answers, \"answer\" for answer and \"is_correct\" for each answer."},
#             {"role": "user", "content": chapter + \
#                 "\n\nCreate 2 questions for the above text."}
#         ],
#         response_format={"type": "json_object"}
#     )
#     print("Title: ", title, "\nQuestion: \n",
#           response.choices[0].message.content, "\n\n")
    
#     time.sleep(30)

