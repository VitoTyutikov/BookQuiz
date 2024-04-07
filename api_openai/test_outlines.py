import PyPDF2


def extract_top_level_bookmarks_with_pages(pdf_path):
    general_outlines = ['Cover', 'Title Page',  'Copyright Page', 'Contents', 'Foreword', 'Preface',
                        'Acknowledgments', 'Index', 'Table of Contents', 'List of Tables', 'List of Figures', 'References',
                        'Appendix']
    titles_with_pages = []
    with open(pdf_path, 'rb') as file:
        reader = PyPDF2.PdfReader(file)
        outlines = reader.outline

        for item in outlines:
            if not isinstance(item, list):  # Process only top-level items
                if item.title not in general_outlines:
                    try:
                        page_number = reader.get_destination_page_number(item) + 1
                        titles_with_pages.append((item.title, page_number))
                    except:
                        titles_with_pages.append((item.title, None))

    return titles_with_pages


pdf_path = '/home/shieldbr/projects/textSplitQuiz/Joshua_Bloch_-_Effective_Java_3rd__2018_ENG.pdf'
# pdf_path = '/home/shieldbr/projects/textSplitQuiz/Horstmann C.S. - Core Java. Vol. 2. Advanced Features - 2019.pdf'
titles_with_pages = extract_top_level_bookmarks_with_pages(pdf_path)
for title, page in titles_with_pages:
    print(f"{title} - Page {page}")





# response = client.chat.completions.create(
#     model="gpt-3.5-turbo",  # Replace with your GPT-4 model name
#     messages=[
#         {"role": "system", "content": "You are an assistant who creates questions based on provided text. \
#          You must return an array of questions and each of the questions has an array of answers. \
#          All answers(same name in json) should have a boolean variable named is_correct and store true if the answer is correct and false if the answer is incorrect.\
#          There should be only one correct answer(same name in json) for each question(same name in json).\
#              Response should be JSON object."},
#         {"role": "user", "content": pdf_text + \
#             "\n\nCreate 1 question for the above text."}
#     ],
#     response_format={"type": "json_object"}
# )

# print(response.choices[0].message.content)