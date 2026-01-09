import requests
import json

AZURE_ENDPOINT = "https://openai-nr-1625688.openai.azure.com"
API_KEY = "FEBYAwtrgBuNOmovXErvK90kKepetB5BS7wGWdmCw3SAncpwX8QyJQQJ99BLACmepeSXJ3w3AAABACOGNPF5"
DEPLOYMENT_NAME = "gpt-4"
API_VERSION = "2024-02-15-preview"

def test_azure_openai_connection():
    
    url = f"{AZURE_ENDPOINT}/openai/deployments/{DEPLOYMENT_NAME}/chat/completions?api-version={API_VERSION}"
    
    headers = {
        "Content-Type": "application/json",
        "api-key": API_KEY
    }
    
    payload = {
        "messages": [
            {"role": "user", "content": "test"}
        ],
        "max_tokens": 10
    }
    
    try:
        response = requests.post(url, headers=headers, json=payload)
        
        if response.status_code == 200:
            print("SUCCESS: API connection working")
            print(f"Response: {response.json()}")
            return True
        else:
            print(f"ERROR: API returned status code {response.status_code}")
            print(f"Error message: {response.text}")
            return False
            
    except Exception as e:
        print(f"EXCEPTION: Failed to connect to API")
        print(f"Error details: {str(e)}")
        return False

if __name__ == "__main__":
    print("Testing Azure OpenAI API connection...")
    print(f"Endpoint: {AZURE_ENDPOINT}")
    print(f"Deployment: {DEPLOYMENT_NAME}")
    print("-" * 50)
    
    test_azure_openai_connection()
