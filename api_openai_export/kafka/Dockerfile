FROM python:3.9-slim

WORKDIR /app
COPY api_openai_export/kafka/requirements.txt ./kafka/requirements.txt
RUN python3 -m venv /app/venv
RUN /app/venv/bin/pip install --no-cache-dir -r kafka/requirements.txt
COPY api_openai_export ./api_openai_export

ENV PYTHONPATH=/app
ENV VIRTUAL_ENV=/app/venv
ENV PATH="/app/venv/bin:$PATH"

CMD ["python", "api_openai_export/kafka/consumer.py"]
