# HealthHub LangGraph Backend

LLM-powered conversational agent for the HealthHub Temi robot.

## Local Development

```bash
# Install dependencies
pip install -r requirements.txt

# Set environment variables
cp .env.example .env
# Edit .env and add your OPENAI_API_KEY

# Run server
python main.py

# Test
curl http://localhost:8000/health
```

## Deploy to Render

1. Create account at https://render.com
2. Create new "Web Service"
3. Connect this repository
4. Configure:
   - **Build Command**: `pip install -r requirements.txt`
   - **Start Command**: `uvicorn main:app --host 0.0.0.0 --port $PORT`
   - **Environment Variables**:
     - `OPENAI_API_KEY`: Your OpenAI API key

5. Deploy!

## API Endpoints

### `GET /health`
Health check

### `POST /agent/interpret`
Interpret user input

**Request**:
```json
{
  "user_text": "I'm feeling dizzy",
  "current_state": "AwaitUse",
  "session_id": "abc123",
  "context": {}
}
```

**Response**:
```json
{
  "intent": "concern",
  "next_state": null,
  "speech": "I understand you're feeling dizzy. Let's take a break and sit down.",
  "action": "pause_session",
  "confidence": 0.85
}
```

## Testing

```bash
curl -X POST http://localhost:8000/agent/interpret \
  -H "Content-Type: application/json" \
  -d '{
    "user_text": "I am done",
    "current_state": "AwaitUse",
    "session_id": "test123"
  }'
```
