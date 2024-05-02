import time
from fastapi import FastAPI, Form, Header, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
import uvicorn
from pydantic import BaseModel
from pathlib import Path



app = FastAPI()


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


# uvicorn api:app --reload
