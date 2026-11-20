"""
Model configuration demonstration showing factual vs creative optimization.
Demonstrates ADK's generate_content_config with different settings.
"""
from google.adk.agents import LlmAgent
from google.genai import types

# Agent 1: Optimized for Factual Data Extraction
# Uses low temperature for consistency, strict safety for accuracy
factual_agent = LlmAgent(
 	model="gemini-3.6-flash", # Flash is sufficient for extraction
 	name="data_extractor",
 	description="Extracts factual information with high consistency",
 	instruction="""You are a precise data extractor.
Extract facts exactly as stated. Do not:
- Add information not present in the input
- Make assumptions or inferences
- Use creative language

Be accurate, concise, and deterministic.""",
 	generate_content_config=types.GenerateContentConfig(
 	temperature=0.1, # Very low for consistency
 	max_output_tokens=500,
 	top_p=0.8,
 	top_k=10,
 	safety_settings=[
 		types.SafetySetting(
 			category=types.HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT,
 			threshold=types.HarmBlockThreshold.BLOCK_LOW_AND_ABOVE
 		)
 	]
 	)
)


# Agent 2: Optimized for Creative Brainstorming
# Uses high temperature for creativity, Pro model for better ideas
creative_agent = LlmAgent(
        model="gemini-3.1-pro-preview", #Brainstorm 5 innovative features for a next-generation smartphone.",  # Pro for superior creativity
    name="creative_brainstormer",
    description="Generates creative ideas and explores possibilities",
    instruction="""You are a creative brainstorming partner.

Generate innovative, diverse, and imaginative ideas. Feel free to:
- Think outside the box
- Combine unexpected concepts
- Explore unconventional approaches

Be creative, varied, and thought-provoking.""",
    generate_content_config=types.GenerateContentConfig(
        temperature=0.9,  # High for creativity
        max_output_tokens=2000,  # Allow detailed ideas
        top_p=0.95,
        top_k=40,
        safety_settings=[
            types.SafetySetting(
                category=types.HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT,
                threshold=types.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE
            )
        ]
    )
)
# For adk web, we'll use the factual agent as root_agent
# Switch to creative_agent to test different behavior

#test with factual_agent
#root_agent = factual_agent

#test with creative_agent
root_agent = creative_agent

#from google.adk.agents.llm_agent import Agent
#
#root_agent = Agent(
#    model='gemini-3.5-flash',
#    name='root_agent',
#    description='A helpful assistant for user questions.',
#    instruction='Answer user questions to the best of your knowledge',
#)
