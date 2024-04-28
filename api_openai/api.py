import time
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
import uvicorn
from pydantic import BaseModel
from pathlib import Path
import tiktoken
import PyPDF2
from openai import OpenAI
import json
import asyncio
# from utils import split_text_to_fit_token_limit, extract_chapters_text


app = FastAPI()


# Helper function to split text


# def split_text_to_fit_token_limit(text, encoding, max_tokens=15500):
#     # Implementation as provided in your example
#     pass

# Function to extract text by chapters from a PDF


# async def extract_chapters_text(pdf_file, encoding):
#     # Implementation as provided in your example
#     pass

# Function to generate questions using OpenAI's API


@app.post("/file/upload")
async def upload_pdf(file: UploadFile = File(...)):
    if file.content_type != 'application/pdf':
        return JSONResponse(status_code=400, content={"message": "This endpoint only accepts PDF files."})

    try:
        file_location = Path("./files") / str(file.filename)
        file_location.parent.mkdir(parents=True, exist_ok=True)

        # Write the PDF to a local file
        with open(file_location, "wb") as buffer:
            buffer.write(await file.read())

        return JSONResponse(status_code=200, content={"message": "File saved successfully.", "filename": file.filename})
    except Exception as e:
        return JSONResponse(status_code=500, content={"message": "An error occurred while uploading the file.", "error": str(e)})


# if __name__ == "__main__":
#     uvicorn.run(app, host="0.0.0.0", port=8000)
