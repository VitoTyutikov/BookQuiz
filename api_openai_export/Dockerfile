FROM python:3.9-slim

WORKDIR /app
COPY api_openai_export/requirements.txt ./requirements.txt
RUN python3 -m venv /app/venv
RUN /app/venv/bin/pip install --no-cache-dir -r requirements.txt
COPY . .

ENV PYTHONPATH=/app
ENV VIRTUAL_ENV=/app/venv
ENV PATH="/app/venv/bin:$PATH"

CMD ["uvicorn", "api_openai_export.api:app", "--host", "0.0.0.0", "--port", "8000"]
