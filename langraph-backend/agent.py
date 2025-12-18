from langgraph.graph import StateGraph, END
from langchain_google_genai import ChatGoogleGenerativeAI
from typing import TypedDict, Optional, Dict, Any
import operator
from typing import Annotated
import os
import logging

logger = logging.getLogger(__name__)

# Agent state
class AgentState(TypedDict):
    user_input: str
    current_state: str
    session_id: str
    context: Dict[str, Any]
    intent: str
    next_state: Optional[str]
    speech: str
    action: Optional[str]
    confidence: float
    messages: Annotated[list, operator.add]

def extract_intent(state: AgentState) -> AgentState:
    """Extract user intent from input"""
    # Using Gemini 1.5 Flash
    llm = ChatGoogleGenerativeAI(model="gemini-1.5-flash", temperature=0.1)
    
    prompt = f"""
    You are a healthcare assistant robot helping patients with health screenings.
    
    User input: "{state['user_input']}"
    Current system state: {state['current_state']}
    
    Analyze the user's intent. Possible intents:
    - confirm: User agrees/confirms (yes, done, ready)
    - cancel: User wants to stop (no, cancel, quit)
    - retry: User wants to try again
    - help: User needs assistance
    - skip: User wants to skip current step
    - question: User has a question
    - answer: User is answering a question
    - concern: User has a health concern
    
    Return ONLY the intent name (one word).
    """
    
    response = llm.invoke(prompt)
    intent = response.content.strip().lower()
    
    state['intent'] = intent
    state['confidence'] = 0.85
    
    logger.info(f"Extracted intent: {intent}")
    
    return state

def determine_next_action(state: AgentState) -> AgentState:
    """Determine next FSM state and robot response"""
    # Using Gemini 1.5 Flash
    llm = ChatGoogleGenerativeAI(model="gemini-1.5-flash", temperature=0.7)
    
    prompt = f"""
    You are a friendly healthcare assistant robot named HealthHub on a Temi robot.
    You are currently interacting with a patient.
    
    Context:
    - User said: "{state['user_input']}"
    - User's intent: {state['intent']}
    - Current system state: {state['current_state']}
    - Sensor data/Context: {state['context']}
    
    CRITICAL SAFETY RULES:
    1. NEVER provide a medical diagnosis (e.g., don't say "You have hypertension").
    2. NEVER recommend specific medications.
    3. If readings are abnormal, say "Your readings are outside the typical range" and suggest consulting a professional.
    4. Be empathetic but professional. Use simple language.
    5. Always ask for consent/readiness before starting a check-up.
    
    Your task:
    1. Generate a natural, empathetic response (1-2 sentences)
    2. Determine if we should transition to a new state
    3. Specify any UI action needed
    
    Respond in this exact format:
    SPEECH: [your response]
    NEXT_STATE: [state name or NONE]
    ACTION: [action name or NONE]
    """
    
    response = llm.invoke(prompt)
    content = response.content.strip()
    
    # Parse response
    speech = "I understand."
    next_state = None
    action = None
    
    for line in content.split('\n'):
        if line.startswith('SPEECH:'):
            speech = line.replace('SPEECH:', '').strip()
        elif line.startswith('NEXT_STATE:'):
            ns = line.replace('NEXT_STATE:', '').strip()
            next_state = ns if ns != "NONE" else None
        elif line.startswith('ACTION:'):
            act = line.replace('ACTION:', '').strip()
            action = act if act != "NONE" else None
    
    state['speech'] = speech
    state['next_state'] = next_state
    state['action'] = action
    
    logger.info(f"Response: {speech}, Next: {next_state}, Action: {action}")
    
    return state

def create_agent():
    """Create the LangGraph agent"""
    workflow = StateGraph(AgentState)
    
    # Add nodes
    workflow.add_node("extract_intent", extract_intent)
    workflow.add_node("determine_action", determine_next_action)
    
    # Add edges
    workflow.set_entry_point("extract_intent")
    workflow.add_edge("extract_intent", "determine_action")
    workflow.add_edge("determine_action", END)
    
    compiled = workflow.compile()
    
    class Agent:
        async def run(self, user_input: str, current_state: str, session_id: str, context: Dict[str, Any] = {}):
            """Run the agent"""
            initial_state = {
                "user_input": user_input,
                "current_state": current_state,
                "session_id": session_id,
                "context": context,
                "intent": "",
                "next_state": None,
                "speech": "",
                "action": None,
                "confidence": 0.0,
                "messages": []
            }
            
            result = compiled.invoke(initial_state)
            
            return {
                "intent": result["intent"],
                "next_state": result["next_state"],
                "speech": result["speech"],
                "action": result["action"],
                "confidence": result["confidence"]
            }
    
    return Agent()
