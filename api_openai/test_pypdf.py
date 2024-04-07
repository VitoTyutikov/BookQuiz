import PyPDF2

def extract_chapters_text(pdf_path):

    with open(pdf_path, "rb") as f:
        pdfreader = PyPDF2.PdfReader(f, strict=False)
        content = " ".join([page.extract_text() for page in pdfreader.pages])
    return content



# pdf_path = '/home/shieldbr/projects/textSplitQuiz/Joshua_Bloch_-_Effective_Java_3rd__2018_ENG.pdf'
pdf_path = '/home/shieldbr/projects/textSplitQuiz/Joshua_Bloch_-_Effective_Java_3rd__2018_ENG.pdf'
chapters = extract_chapters_text(pdf_path)


