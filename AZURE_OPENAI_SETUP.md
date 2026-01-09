# Azure OpenAI Setup for HealthHub

## Your API Key
```
FEBYAwtrgBuNOmovXErvK90kKepetB5BS7wGWdmCw3SAncpwX8QyJQQJ99BLACmepeSXJ3w3AAABACOGNPF5
```

## What I Need

### 1. Azure Endpoint URL
Format: `https://YOUR-RESOURCE-NAME.openai.azure.com`

**How to find:**
1. Go to https://portal.azure.com
2. Search for "Azure OpenAI"
3. Click your resource
4. Go to "Keys and Endpoint"
5. Copy the **Endpoint** value

### 2. Deployment Name
Usually: `gpt-4`, `gpt-35-turbo`, or `gpt-4o`

**How to find:**
1. In your Azure OpenAI resource
2. Click "Deployments" (left menu)
3. Copy the deployment name

## Once I Have These

I'll configure agent.py:
```python
from langchain_openai import AzureChatOpenAI

llm = AzureChatOpenAI(
    azure_endpoint="YOUR_ENDPOINT_HERE",
    api_key="FEBYAwtrgBuNOmovXErvK90kKepetB5BS7wGWdmCw3SAncpwX8QyJQQJ99BLACmepeSXJ3w3AAABACOGNPF5",
    deployment_name="YOUR_DEPLOYMENT_HERE",
    api_version="2024-02-15-preview"
)
```

Then test immediately!
