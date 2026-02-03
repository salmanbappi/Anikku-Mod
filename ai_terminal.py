import requests
import json
import os

def call_gemini(messages, api_key, system_instruction):
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key={api_key}"
    
    contents = []
    for msg in messages:
        contents.append({
            "role": "user" if msg["role"] == "user" else "model",
            "parts": [{"text": msg["content"]}]
        })
        
    payload = {
        "contents": contents,
        "system_instruction": {
            "parts": [{"text": system_instruction}]
        }
    }
    
    headers = {"Content-Type": "application/json"}
    response = requests.post(url, json=payload, headers=headers)
    
    if response.status_code == 200:
        res_json = response.json()
        try:
            return res_json['candidates'][0]['content']['parts'][0]['text']
        except:
            return f"Error parsing response: {res_json}"
    else:
        return f"Error {response.status_code}: {response.text}"

def call_groq(messages, api_key, system_instruction):
    url = "https://api.groq.com/openai/v1/chat/completions"
    
    groq_messages = [{"role": "system", "content": system_instruction}]
    for msg in messages:
        groq_messages.append({"role": "user" if msg["role"] == "user" else "assistant", "content": msg["content"]})
        
    payload = {
        "model": "llama-3.3-70b-versatile",
        "messages": groq_messages
    }
    
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    response = requests.post(url, json=payload, headers=headers)
    
    if response.status_code == 200:
        return response.json()['choices'][0]['message']['content']
    else:
        return f"Error {response.status_code}: {response.text}"

def main():
    print("\033[95m--- AniZen Intelligence Terminal CLI ---\033[0m")
    engine = input("Select Engine (1: Gemini, 2: Groq): ")
    api_key = input("Enter API Key: ")
    
    system_instruction = """
    You are 'AniZen Intelligence', the neural core of the Anikku platform. 
    Your mission is to provide elite technical support and deep anime insights.
    TONE: Professional, efficient, futuristic.
    """
    
    history = []
    print("\n\033[92mNeural core online. Chat active. Type 'exit' to quit.\033[0m\n")
    
    while True:
        query = input("\033[94mUser: \033[0m")
        if query.lower() in ['exit', 'quit']: break
        
        history.append({"role": "user", "content": query})
        
        print("\033[93mAniZen is thinking...\033[0m", end="\r")
        
        if engine == "1":
            response = call_gemini(history, api_key, system_instruction)
        else:
            response = call_groq(history, api_key, system_instruction)
            
        print("\033[95mAniZen:\033[0m " + response + "\n")
        history.append({"role": "assistant", "content": response})

if __name__ == "__main__":
    main()
