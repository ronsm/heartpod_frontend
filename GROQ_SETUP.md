# Groq AI Integration for HealthHub

## Setup Instructions

### 1. Get Groq API Key (FREE)
1. Go to https://console.groq.com
2. Sign up for free account
3. Navigate to API Keys section
4. Create new API key
5. Copy the key (starts with `gsk_...`)

### 2. Add to Environment
```bash
# On Raspberry Pi
ssh openhabian@192.168.2.150
cd ~/langraph-backend
echo "GROQ_API_KEY=your_key_here" >> .env
```

### 3. Install Groq Package
```bash
cd ~/langraph-backend
source ../langgraph-venv/bin/activate
pip install langchain-groq
```

### 4. Update agent.py
Replace ChatGoogleGenerativeAI with ChatGroq:
```python
from langchain_groq import ChatGroq

llm = ChatGroq(
    model="llama-3.1-70b-versatile",
    temperature=0.7,
    groq_api_key=os.getenv("GROQ_API_KEY")
)
```

### 5. Restart Service
```bash
sudo systemctl restart healthub-agent
```

## Models Available (FREE)

| Model | Speed | Quality | Use Case |
|:------|:------|:--------|:---------|
| llama-3.1-8b-instant | ⚡ Ultra Fast | Good | Quick responses |
| llama-3.1-70b-versatile | ⚡ Fast | Excellent | **Recommended for HealthHub** |
| mixtral-8x7b-32768 | ⚡ Fast | Very Good | Alternative |

## Free Tier Limits
- **30 requests/minute**
- **14,400 requests/day**
- More than enough for prototype/demo

## Why Groq?
- ✅ Completely FREE
- ✅ Ultra-fast inference (faster than GPT-4)
- ✅ Excellent quality (Llama 3.1 70B)
- ✅ Perfect for conversational AI
- ✅ Easy LangChain integration
- ✅ No credit card required

## Test Command
```bash
curl -X POST http://192.168.2.150:8000/agent/interpret \
  -H "Content-Type: application/json" \
  -d '{"user_text":"I smoke 10 cigarettes a day","current_state":"Idle","session_id":"test"}'
```
