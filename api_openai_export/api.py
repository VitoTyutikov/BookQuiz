import json
import logging
import os
import sys
import time
from fastapi import FastAPI, Form, Header, UploadFile, File, HTTPException
from fastapi.encoders import jsonable_encoder
from fastapi.responses import FileResponse, JSONResponse
import uvicorn
from pydantic import BaseModel
from pathlib import Path

from api_openai_export.export.export_word import create_question_doc


class DocumentRequest(BaseModel):
    book: str
    
app = FastAPI()

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s\t - %(levelname)s\t : %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S')

@app.post("/api/v1/file/upload")
async def upload_pdf(file: UploadFile = File(...), bookId: str = Form(...)):
    try:
        file_location = Path("./files") / f"{bookId}_{file.filename}"
        file_location.parent.mkdir(parents=True, exist_ok=True)
        with open(file_location, "wb") as buffer:
            buffer.write(await file.read())

        return JSONResponse(status_code=200, content={"message": "File saved successfully.", "filename": file.filename})
    except Exception as e:
        return JSONResponse(status_code=500, content={"message": "An error occurred while uploading the file.", "error": str(e)})


@app.post("/api/v1/getfile/word")
def generate_document(request: DocumentRequest):

    try:
        try:
            file_path = create_question_doc(json.loads(request.book) )
        except Exception as e:
            logging.warning("Error create file or parse to json" + str(e))
        if os.path.exists(file_path):
            return FileResponse(path=file_path, filename=file_path, media_type='application/vnd.openxmlformats-officedocument.wordprocessingml.document')
        raise HTTPException(status_code=404, detail="File not found after generation.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# if __name__ == "__main__":
#     uvicorn.run(app, host="0.0.0.0", port=8000)
# uvicorn api:app --reload
