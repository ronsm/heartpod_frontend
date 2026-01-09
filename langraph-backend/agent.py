from langgraph.graph import StateGraph, END
from langchain_groq import ChatGroq
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

# Questionnaire-specific prompts
QUESTIONNAIRE_PROMPTS = {
    "personal_details": """Ask for the patient's first name, last name, and date of birth in a friendly way.
Example: "Hi! Let's start with some basic information. What's your first name?" """,
    
    "smoking": """Ask about smoking status in a non-judgmental way:
- "Do you currently smoke?"
- If yes: "How many cigarettes do you smoke per day?"
- If ex-smoker: "When did you stop smoking?"
Be supportive regardless of their answer.""",
    
    "alcohol": """Ask: "How many units of alcohol do you drink in a typical week?"
Explain: "One unit is about half a pint of beer, a small glass of wine, or a single measure of spirits."
Accept 0 to 50+.""",
    
    "exercise": """Ask: "How many times per week do you do moderate intensity exercise for at least 30 minutes?"
Explain: "Moderate intensity means you're breathing faster but can still hold a conversation, like brisk walking."
Accept 0 to 7.""",
    
    "height_weight": """Ask for height in centimeters and weight in kilograms.
Be encouraging and explain it's for calculating BMI."""
}

def extract_intent(state: AgentState) -> AgentState:
    """Extract user intent from input"""
    # Using Groq Llama 3.1 70B
    llm = ChatGroq(
        model="llama-3.1-70b-versatile",
        temperature=0.1,
        groq_api_key=os.getenv("GROQ_API_KEY")
    )
    
    prompt = f"""You are a healthcare assistant robot helping patients with health screenings.
    
    User input: "{state['user_input']}"
    Current system state: {state['current_state']}
    
    Analyze the user's intent. Possible intents:
    - confirm: User agrees/confirms (yes, done, ready, okay, sure)
    - cancel: User wants to stop (no, cancel, quit, stop)
    - retry: User wants to try again
    - help: User needs assistance
    - skip: User wants to skip current step
    - question: User has a question
    - answer: User is answering a question (providing information)
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
    # Using Groq Llama 3.1 70B
    llm = ChatGroq(
        model="llama-3.1-70b-versatile",
        temperature=0.7,
        groq_api_key=os.getenv("GROQ_API_KEY")
    )
    
    # Get questionnaire-specific prompt if in questionnaire mode
    questionnaire_context = ""
    if "questionnaire" in state['current_state'].lower():
        question_type = state.get('context', {}).get('current_question', '')
        questionnaire_context = QUESTIONNAIRE_PROMPTS.get(question_type, "")
    
    prompt = f"""You are a friendly healthcare assistant robot named HealthHub on a Temi robot.
    You are currently interacting with a patient.
    
    Context:
    - User said: "{state['user_input']}"
    - User's intent: {state['intent']}
    - Current system state: {state['current_state']}
    {questionnaire_context}
    
    CRITICAL SAFETY RULES:
    1. NEVER provide medical diagnoses
    2. NEVER recommend specific treatments
    3. ALWAYS suggest consulting a healthcare professional for concerns
    4. Be empathetic and non-judgmental
    5. Keep responses concise (1-2 sentences)
    
    Based on the context, provide:
    1. A natural, friendly response to say to the patient
    2. The next action the system should take
    
    Format your response as:
    SPEECH: [what to say to patient]
    ACTION: [next_state|retry|help|end]
    """
    
    response = llm.invoke(prompt)
    content = response.content.strip()
    
    # Parse response
    speech = ""
    action = "continue"
    
    for line in content.split('\n'):
        if line.startswith('SPEECH:'):
            speech = line.replace('SPEECH:', '').strip()
        elif line.startswith('ACTION:'):
            action = line.replace('ACTION:', '').strip()
    
    state['speech'] = speech if speech else "I understand. Let's continue."
    state['action'] = action
    
    logger.info(f"Response: {speech}, Action: {action}")
    
    return state

def parse_questionnaire_response(state: AgentState) -> AgentState:
    """Parse user responses to questionnaire questions"""
    llm = ChatGroq(
        model="llama-3.1-70b-versatile",
        temperature=0.1,
        groq_api_key=os.getenv("GROQ_API_KEY")
    )
    
    question_type = state.get('context', {}).get('current_question', '')
    user_input = state['user_input']
    
    prompt = f"""Extract structured data from the user's response.

Question type: {question_type}
User said: "{user_input}"

Based on the question type, extract:
- For "smoking": smoking_status (never/current/ex), cigarettes_per_day (number), quit_date (YYYY-MM-DD)
- For "alcohol": units_per_week (0-50+)
- For "exercise": times_per_week (0-7)
- For "height_weight": height_cm (number), weight_kg (number)
- For "personal_details": first_name, last_name, date_of_birth (YYYY-MM-DD)

Return ONLY a JSON object with the extracted values. Use null for missing values.
Example: {{"smoking_status": "current", "cigarettes_per_day": 10}}
"""
    
    response = llm.invoke(prompt)
    
    try:
        import json
        parsed_data = json.loads(response.content.strip())
        state['context']['parsed_response'] = parsed_data
        logger.info(f"Parsed data: {parsed_data}")
    except Exception as e:
        logger.error(f"Failed to parse response: {e}")
        state['context']['parsed_response'] = {}
    
    return state

# Build the graph
workflow = StateGraph(AgentState)

workflow.add_node("extract_intent", extract_intent)
workflow.add_node("determine_action", determine_next_action)
workflow.add_node("parse_response", parse_questionnaire_response)

workflow.set_entry_point("extract_intent")
workflow.add_edge("extract_intent", "parse_response")
workflow.add_edge("parse_response", "determine_action")
workflow.add_edge("determine_action", END)

# Compile the graph
agent = workflow.compile()

def interpret(user_text: str, current_state: str, session_id: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
    """Main entry point for agent"""
    if context is None:
        context = {}
    
    initial_state = {
        "user_input": user_text,
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
    
    result = agent.invoke(initial_state)
    
    return {
        "intent": result["intent"],
        "speech": result["speech"],
        "action": result["action"],
        "confidence": result["confidence"],
        "parsed_data": result.get("context", {}).get("parsed_response", {})
    }
