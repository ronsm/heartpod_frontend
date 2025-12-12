from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, Dict, Any
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="HealthHub LangGraph Agent")

# Enable CORS for Android app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Request/Response models
class InterpretRequest(BaseModel):
    user_text: str
    current_state: str
    session_id: str
    context: Dict[str, Any] = {}

class InterpretResponse(BaseModel):
    intent: str
    next_state: Optional[str]
    speech: str
    action: Optional[str]
    confidence: float

class HealthResponse(BaseModel):
    status: str

# Import agent (will create this next)
from agent import create_agent

# Initialize agent
agent = create_agent()

@app.get("/")
async def root():
    return {"message": "HealthHub LangGraph Agent API"}

@app.get("/health")
async def health() -> HealthResponse:
    """Health check endpoint"""
    return HealthResponse(status="healthy")

@app.post("/agent/interpret")
async def interpret(request: InterpretRequest) -> InterpretResponse:
    """
    Interpret user input using LangGraph agent
    """
    try:
        logger.info(f"Interpreting: {request.user_text} (state: {request.current_state})")
        
        # Run agent
        result = await agent.run(
            user_input=request.user_text,
            current_state=request.current_state,
            session_id=request.session_id,
            context=request.context
        )
        
        logger.info(f"Agent result: {result}")
        
        return InterpretResponse(**result)
        
    except Exception as e:
        logger.error(f"Error interpreting: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)



