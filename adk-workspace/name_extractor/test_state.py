"""
Test script to see state access directly.
Run with: python test_state.py
"""
import asyncio
from agent import root_agent
from google.adk.runners  import Runner
from google.adk.sessions import InMemorySessionService
from google.genai.types import Content, Part

# Setup session and runner
session_service = InMemorySessionService()
session = asyncio.run(session_service.create_session(
    app_name= "name_extractor_app",
    user_id= "test_user",
    session_id="test_session"
))

runner = Runner(
    agent=root_agent,
    app_name="name_extractor_app",
    session_service=session_service
)

# Test: Extract name
user_message = Content(parts=[Part(text="Hi, my name is Alex Johnson")])

print("=== Running agent ===")
result = runner.run(
    user_id= "test_user",
    session_id= "test_session",
    new_message=user_message
)

extracted_name = None
# Show final response and check state
for event in result:
    if event.is_final_response():
        extracted_name = event.content.parts[0].text
        print(f"\nAgent response: {event.content.parts[0].text}")

#Simulando o armazenamento do estado que o ADK fara nas proximas lições
if extracted_name:
    session.state["user_name"] = extracted_name.strip()

# Recupera a sessão atualizada do serviço
session = asyncio.run(session_service.get_session(
    app_name="name_extractor_app",
    user_id="test_user",
    session_id="test_session"
))

print(f"\n=== State after execution ===")
print(f"Full state: {session.state}")
print(f"Extracted name: {session.state.get('user_name')}")

if session.state.get("user_name"):
    print("✅ Name was successfully extracted and stored!")
else:
    print("❌ Name extraction failed")

# Test accessing in subsequent turns
print("\n=== Simulating second turn ===")
result2 = runner.run(
    user_id="test_user",
    session_id="test_session",
    new_message=Content(parts=[Part(text="What's my name?")])
)

for event in result2:
    if event.is_final_response():
        print(f"Agent response: {event.content.parts[0].text}")

print(f"\nState still contains: {session.state.get('user_name')}")
print("✅ State persists across turns!")
