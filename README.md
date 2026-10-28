<p align="center">
  <img src="frontend/public/sana-logo.png" alt="SANA logo" width="320">
</p>

# SANA

SANA (سنا) is a web platform that turns a rough idea into a structured, visual project. Users describe an idea, and SANA helps them shape it into diagrams and workspaces for **Technology, Business, Education and Creativity** (writing and games), with an AI assistant that proposes, edits and reviews the work.

## Problem

People with a new idea usually don't know where to start: which kind of plan or diagram fits it, how to structure it, or which tool to use. They end up switching between separate tools for system design, business analysis, study notes and story planning, and generic AI chats give unstructured text that has to be copied and redrawn by hand.

## Solution

One workspace that structures any idea and keeps the AI's output usable:

- **Start with SANA AI** – write an idea; the AI picks the right domain and diagram, then draws the first version directly onto the canvas.
- **Domain diagrams** on a shared canvas:
  - **Technology:** system architecture, ERD, UML class diagram, use-case diagram.
  - **Business:** business model canvas, target audience, SWOT, competitor analysis, risk analysis, plus structured AI reports.
  - **Education:** mind maps.
  - **Creativity:** story maps (characters, worlds, locations, rules, events), branching game narratives with variables and play-through preview, character balance, moodboards, book inspiration, Yarn export.
- **AI assistant** that creates, edits or advises on a diagram. Every change arrives as a **proposal** the user accepts or rejects, and is checked against the diagram's rules before it is saved.
- **Accounts and plans** – HTTP Basic login, each user sees only their own projects, and optional paid plans (Moyasar, test mode) raise the project limit.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java , Spring Boot, Spring Data JPA (Hibernate), Spring Security (HTTP Basic), Bean Validation, Lombok |
| Database | MySQL 8.4 (deployed on AWS RDS) |
| Frontend | React, Vite |
| AI | n8n Cloud workflows; *Sana - AI Gateway* and *Sana - AI Quick Start* |
| Payments | Moyasar |
| External data | Open Library (books); Cleveland Museum of Art, The Met, Wikimedia Commons and Pixabay; World Bank (Saudi indicators) |
| Deployment | AWS EC2 + RDS, Docker, Docker Compose |

## API endpoints

**Access rules.** Every endpoint requires HTTP Basic login (email + password) unless marked **Public**. Endpoints that take a project, diagram or element ID check that it belongs to the logged-in user (admins may access everything). Endpoints marked **Admin** are for the `ADMIN` role only.

### Authentication

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/auth/register` | **Public.** Create an account (role `USER`; the password is hashed) |
| GET | `/api/v1/auth/me` | The logged-in user; used by the frontend to check the login |
| POST | `/api/v1/auth/logout` | End the session |

### Projects

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/project/get` | **Admin.** All projects |
| GET | `/api/v1/project/get/my` | The logged-in user's projects |
| GET | `/api/v1/project/get/{projectId}` | One project |
| GET | `/api/v1/project/get/user/{userId}` | A user's projects |
| POST | `/api/v1/project/add/{userId}` | Create a project (checks the plan's project limit when billing is on) |
| PUT | `/api/v1/project/update/{projectId}` | Update a project |
| DELETE | `/api/v1/project/delete/{projectId}` | Delete a project |

### Diagrams (all domains)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/diagram/types` | Supported diagram types with their node and edge types and rules |
| GET | `/api/v1/diagram/get/project/{projectId}` | A project's diagrams (optionally filtered by type) |
| GET | `/api/v1/diagram/get/{outputId}` | One diagram |
| GET | `/api/v1/diagram/graph/get/{outputId}` | A diagram's full graph and current revision |
| POST | `/api/v1/diagram/add/{projectId}` | Create an empty diagram of a given type |
| PUT | `/api/v1/diagram/update/{outputId}` | Rename a diagram |
| DELETE | `/api/v1/diagram/delete/{outputId}` | Delete a diagram |
| GET | `/api/v1/diagram/node/get/{outputId}/{nodeId}` | One node |
| POST | `/api/v1/diagram/node/add/{outputId}` | Add a node |
| PUT | `/api/v1/diagram/node/update/{outputId}/{nodeId}` | Update a node |
| DELETE | `/api/v1/diagram/node/delete/{outputId}/{nodeId}` | Delete a node and its edges |
| GET | `/api/v1/diagram/edge/get/{outputId}/{edgeId}` | One edge |
| POST | `/api/v1/diagram/edge/add/{outputId}` | Add an edge |
| PUT | `/api/v1/diagram/edge/update/{outputId}/{edgeId}` | Update an edge |
| DELETE | `/api/v1/diagram/edge/delete/{outputId}/{edgeId}` | Delete an edge |

### AI assistant

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/ai/quick-start/classify` | Quick start: the AI suggests a domain, diagram type and project name for an idea |
| POST | `/api/projects/{projectId}/ai/requests` | Start a CREATE, EDIT or ADVISE request (answered later through n8n) |
| GET | `/api/projects/{projectId}/ai/requests` | Latest 50 requests; `?messages=true` also returns prompts and answers (chat history) |
| GET | `/api/projects/{projectId}/ai/requests/{requestId}` | Poll a request's status and result |
| GET | `/api/projects/{projectId}/ai/proposals/{proposalId}` | Preview a proposed diagram and its changes |
| POST | `/api/projects/{projectId}/ai/proposals/{proposalId}/accept` | Apply a proposal to the diagram |
| POST | `/api/projects/{projectId}/ai/proposals/{proposalId}/reject` | Reject a proposal |
| POST | `/api/projects/{projectId}/creativity/ai/requests` | Creativity AI request (with conversation history) |
| POST | `/api/integrations/n8n/ai/callback` | **Public** (secured by the `X-Sana-Callback-Key` header). n8n delivers an AI result |

### Business

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/business/tools` | Run a Business tool and get a structured AI report. One endpoint for all five tools, chosen by `toolType`: `BUSINESS_MODEL_BUILDER`, `TARGET_AUDIENCE_BUILDER`, `SWOT_ANALYZER`, `COMPETITOR_ANALYSIS`, `RISK_ANALYZER` |
| GET | `/api/v1/external/world-bank/saudi-indicators` | Saudi economic indicators from the World Bank (standalone; not yet fed into the AI reports) |

Business diagrams (business model canvas, target audience, SWOT, competitor analysis, risk analysis) are created and edited through the shared [Diagrams](#diagrams-all-domains) endpoints, and their AI requests and proposals go through the shared [AI assistant](#ai-assistant) endpoints.

### Technology – architecture diagrams

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/tech/architecture/get` | **Admin.** All architecture diagrams |
| GET | `/api/v1/tech/architecture/get/{outputId}` | One architecture diagram |
| GET | `/api/v1/tech/architecture/get/project/{projectId}` | A project's architecture diagrams |
| GET | `/api/v1/tech/architecture/graph/get/{outputId}` | Full architecture graph |
| POST | `/api/v1/tech/architecture/add/{projectId}` | Create an architecture diagram |
| PUT | `/api/v1/tech/architecture/update/{outputId}` | Update it |
| DELETE | `/api/v1/tech/architecture/delete/{outputId}` | Delete it |
| GET / POST / PUT / DELETE | `/api/v1/tech/architecture/node/{get\|add\|update\|delete}/{outputId}[/{nodeId}]` | Manage its components (nodes) |
| GET / POST / PUT / DELETE | `/api/v1/tech/architecture/edge/{get\|add\|update\|delete}/{outputId}[/{edgeId}]` | Manage its connections (edges) |

### Creativity – story elements

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/character-profile/get` | **Admin.** All characters |
| GET | `/api/v1/character-profile/get/by/{id}` | One character |
| POST | `/api/v1/character-profile/add/{outputId}` | Add a character to a story map |
| PUT | `/api/v1/character-profile/update/{id}` | Update a character |
| DELETE | `/api/v1/character-profile/delete/{id}` | Delete a character |
| GET | `/api/v1/world/get` | **Admin.** All worlds |
| GET | `/api/v1/world/get/{id}` | One world |
| POST | `/api/v1/world/add/{outputId}` | Add a world |
| PUT | `/api/v1/world/update/{id}` | Update a world |
| DELETE | `/api/v1/world/delete/{id}` | Delete a world |
| GET | `/api/v1/location/get` | **Admin.** All locations |
| GET | `/api/v1/location/get/{id}` | One location |
| POST | `/api/v1/location/add/{outputId}` | Add a standalone location |
| POST | `/api/v1/location/add/{outputId}/world/{worldId}` | Add a location inside a world |
| POST | `/api/v1/location/add/{outputId}/parent/{parentLocationId}` | Add a location inside another location |
| POST | `/api/v1/location/add/{outputId}/world/{worldId}/parent/{parentLocationId}` | Add a location with both a world and a parent |
| PUT | `/api/v1/location/update/{id}` | Update a location and its position |
| PUT | `/api/v1/location/update/{locationId}/world/{worldId}` | Move a location to another world |
| PUT | `/api/v1/location/update/{locationId}/parent/{parentLocationId}` | Change a location's parent (no cycles) |
| DELETE | `/api/v1/location/delete/{id}` | Delete a location (not if it has child locations) |
| GET | `/api/v1/world-rule/get` | **Admin.** All world rules |
| GET | `/api/v1/world-rule/get/{id}` | One world rule |
| POST | `/api/v1/world-rule/add/{outputId}` | Add an independent world rule |
| POST | `/api/v1/world-rule/add/{outputId}/world/{worldId}` | Add a rule to a world |
| PUT | `/api/v1/world-rule/update/{id}` | Update a world rule |
| PUT | `/api/v1/world-rule/update/{ruleId}/world/{worldId}` | Assign a rule to another world |
| DELETE | `/api/v1/world-rule/delete/{id}` | Delete a world rule |
| GET | `/api/v1/story-event/get` | **Admin.** All story events |
| GET | `/api/v1/story-event/get/{id}` | One story event |
| POST | `/api/v1/story-event/add/{outputId}` | Add a story event |
| PUT | `/api/v1/story-event/update/{id}` | Update a story event |
| DELETE | `/api/v1/story-event/delete/{id}` | Delete a story event |
| GET | `/api/v1/story-event/timeline/presentation/{outputId}` | Timeline in narrative order |
| GET | `/api/v1/story-event/timeline/chronological/{outputId}` | Timeline in chronological order |
| GET | `/api/v1/creativity/relations/get/{outputId}` | All relationships in a story map |
| GET | `/api/v1/creativity/relations/get/{outputId}/node/{nodeId}` | Relationships of one element |
| GET | `/api/v1/creativity/relations/get/types/{outputId}/{sourceNodeId}/{targetNodeId}` | Relationship types allowed between two elements |
| POST | `/api/v1/creativity/relations/add/{outputId}/{sourceNodeId}/{targetNodeId}` | Create a relationship |
| GET | `/api/v1/creativity/relations/delete/preview/{outputId}/node/{nodeId}` | What deleting an element would affect |
| GET | `/api/v1/creativity/search/{outputId}` | Search the elements of a story map |

### Creativity – branching narrative (games)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/branching/get/{outputId}/map` | Full branching map (scenes, positions, choices) |
| GET | `/api/v1/branching/get/{outputId}/nodes` | All scenes |
| GET | `/api/v1/branching/get/{outputId}/nodes/{nodeId}` | One scene |
| POST | `/api/v1/branching/add/{outputId}/nodes` | Add a scene (DIALOGUE, DECISION, EVENT, ENDING) |
| PUT | `/api/v1/branching/update/{outputId}/nodes/{nodeId}` | Update a scene |
| DELETE | `/api/v1/branching/delete/{outputId}/nodes/{nodeId}` | Delete a scene (not if it has choices) |
| PUT | `/api/v1/branching/update/{outputId}/nodes/{nodeId}/position` | Move a scene on the canvas |
| PUT | `/api/v1/branching/update/{outputId}/layout/nodes/{nodeIds}` | Move several scenes at once |
| POST | `/api/v1/branching/add/{outputId}/choices/{sourceNodeId}/{targetNodeId}` | Connect two scenes with a player choice |
| PUT | `/api/v1/branching/update/{outputId}/choices/{choiceId}/to/{targetNodeId}` | Change a choice's destination |
| DELETE | `/api/v1/branching/delete/{outputId}/choices/{choiceId}` | Delete a choice |
| GET | `/api/v1/branching/get/{outputId}/choices/{choiceId}/settings` | A choice's condition and effect |
| GET | `/api/v1/branching/get/{outputId}/variables` | Game variables |
| POST | `/api/v1/branching/add/{outputId}/variables` | Add a variable |
| PUT | `/api/v1/branching/update/{outputId}/variables/{variableId}` | Update a variable |
| DELETE | `/api/v1/branching/delete/{outputId}/variables/{variableId}` | Delete an unused variable |
| PUT | `/api/v1/branching/set/{outputId}/choices/{choiceId}/condition/{variableId}` | Set a choice's condition |
| DELETE | `/api/v1/branching/remove/{outputId}/choices/{choiceId}/condition` | Remove a choice's condition |
| PUT | `/api/v1/branching/set/{outputId}/choices/{choiceId}/effect/{variableId}` | Set a choice's effect |
| DELETE | `/api/v1/branching/remove/{outputId}/choices/{choiceId}/effect` | Remove a choice's effect |
| PUT | `/api/v1/branching/link/{outputId}/nodes/{nodeId}/speaker/{characterId}` | Link a character as a scene's speaker |
| DELETE | `/api/v1/branching/unlink/{outputId}/nodes/{nodeId}/speaker` | Remove the speaker |
| PUT | `/api/v1/branching/link/{outputId}/nodes/{nodeId}/event/{eventId}` | Link a story event to a scene |
| DELETE | `/api/v1/branching/unlink/{outputId}/nodes/{nodeId}/event` | Remove the event link |
| GET | `/api/v1/branching/get/{outputId}/nodes/{nodeId}/references` | A scene's linked character and event |
| GET | `/api/v1/branching/play/{outputId}/start/{nodeId}` | Start a play-through preview |
| POST | `/api/v1/branching/play/{outputId}/{nodeId}/choose/{choiceId}` | Make a choice in the preview |
| GET | `/api/v1/branching/export/yarn/{outputId}` | Export as Yarn (from the first scene) |
| GET | `/api/v1/branching/export/yarn/{outputId}/start/{startNodeId}` | Export as Yarn from a chosen scene |

### Creativity – character balance (games)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/character-balance/get/{outputId}/characters` | Playable characters |
| GET | `/api/v1/character-balance/{outputId}/characters/{characterId}` | One playable character and its stats |
| POST | `/api/v1/character-balance/{outputId}/characters/add` | Add a playable character |
| PUT | `/api/v1/character-balance/{outputId}/characters/update/{characterId}` | Update a playable character |
| DELETE | `/api/v1/character-balance/{outputId}/characters/delete/{characterId}` | Delete a playable character |
| PUT | `/api/v1/character-balance/{outputId}/characters/{characterId}/profile/{profileId}` | Link it to a story character |
| DELETE | `/api/v1/character-balance/{outputId}/characters/{characterId}/profile` | Unlink the story character |
| GET | `/api/v1/character-balance/{outputId}/analyze` | Analyze the balance of the characters' stats |

### Creativity – AI assist tools

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/projects/{projectId}/creativity/assist/balance` | AI suggests character stat changes (draft) |
| POST | `/api/projects/{projectId}/creativity/assist/books` | AI suggests real books for inspiration (draft) |
| GET | `/api/projects/{projectId}/creativity/assist/drafts/{draftId}` | Poll an assist draft |
| POST | `/api/projects/{projectId}/creativity/assist/drafts/{draftId}/approve-balance` | Apply the approved stat changes |

### Creativity – moodboards

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/creativity/moodboard/images/search` | Search images (Cleveland Museum, The Met, Wikimedia, and Pixabay when its key is set) |
| GET | `/api/v1/creativity/moodboard/images/pixabay-assets/{id}` | Serve a saved Pixabay image |
| GET | `/api/v1/creativity/moodboard/images/{openverseId}` | Image attribution details (legacy) |
| POST | `/api/v1/creativity/moodboard/add/{projectId}` | Create a moodboard |
| GET | `/api/v1/creativity/moodboard/get/project/{projectId}` | A project's moodboards |
| GET | `/api/v1/creativity/moodboard/get/{outputId}` | One moodboard |
| PUT | `/api/v1/creativity/moodboard/update/{outputId}/title` | Rename a moodboard |
| POST | `/api/v1/creativity/moodboard/add/{outputId}/images` | Add an image |
| POST | `/api/v1/creativity/moodboard/add/{outputId}/labels` | Add a text label |
| PUT | `/api/v1/creativity/moodboard/update/{outputId}/items/{itemId}/position` | Move an image or label |
| PUT | `/api/v1/creativity/moodboard/update/{outputId}/labels/{itemId}` | Edit a label |
| DELETE | `/api/v1/creativity/moodboard/delete/{outputId}/items/{itemId}` | Remove an item |
| GET | `/api/projects/{projectId}/creativity/moodboard-ai/capabilities` | Whether AI moodboards are available |
| POST | `/api/projects/{projectId}/creativity/moodboard-ai/drafts` | AI generates a moodboard preview |
| GET | `/api/projects/{projectId}/creativity/moodboard-ai/drafts/{draftId}` | Poll the preview |
| POST | `/api/projects/{projectId}/creativity/moodboard-ai/drafts/{draftId}/approve` | Save the preview as a moodboard |
| POST | `/api/projects/{projectId}/creativity/moodboard-ai/drafts/{draftId}/reject` | Discard the preview |
| GET | `/api/v1/creativity/books/search` | Search Open Library for books |
| GET | `/api/v1/creativity/books/get/{workId}` | Book details |

### Billing (Moyasar, test mode)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/billing/plans` | Plans, prices and project limits |
| GET | `/api/billing/me` | The user's current plan, usage and period end |
| GET | `/api/billing/orders` | The user's last 10 payment orders |
| POST | `/api/billing/checkout` | Start paying for a plan; returns the Moyasar checkout URL |
| POST | `/api/billing/reconcile` | Re-check the user's payments with Moyasar |
| POST | `/api/billing/callbacks/{orderId}/{token}` | **Public** (per-order secret token). Moyasar payment notification |

### Project context and low-level graph data

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/project-understanding/get` | **Admin.** All project understandings |
| GET | `/api/v1/project-understanding/get/{id}` | One project understanding |
| GET | `/api/v1/project-understanding/get/project/{projectId}` | A project's goal, actors, components and constraints (AI context) |
| POST | `/api/v1/project-understanding/add/{projectId}` | Add it |
| PUT | `/api/v1/project-understanding/update/{id}` | Update it |
| DELETE | `/api/v1/project-understanding/delete/{id}` | Delete it |
| GET | `/api/v1/project-output/get` | **Admin.** All project outputs |
| GET | `/api/v1/project-output/get/{outputId}` | One output (diagram, moodboard, …) |
| GET | `/api/v1/project-output/get/project/{projectId}` | A project's outputs |
| POST | `/api/v1/project-output/add/{projectId}` | Add an output |
| PUT | `/api/v1/project-output/update/{outputId}` | Update an output |
| DELETE | `/api/v1/project-output/delete/{outputId}` | Delete an output |
| GET | `/api/v1/node/get` | **Admin.** All nodes |
| POST | `/api/v1/node/add/{outputId}` | Add a node to an output |
| PUT | `/api/v1/node/update/{projectNodeId}` | Update a node |
| DELETE | `/api/v1/node/delete/{nodeId}` | Delete a node |
| GET | `/api/v1/edge/get` | **Admin.** All edges |
| POST | `/api/v1/edge/add/{outputId}/{sourceNodeId}/{targetNodeId}` | Connect two nodes |
| PUT | `/api/v1/edge/update/{projectEdgeId}` | Update an edge |
| DELETE | `/api/v1/edge/delete/{edgeId}` | Delete an edge |
| GET | `/api/v1/record/get` | **Admin.** All project records |
| POST | `/api/v1/record/add/{projectId}` | Add a project record |
| PUT | `/api/v1/record/update/{projectRecordId}` | Update a project record |
| DELETE | `/api/v1/record/delete/{projectRecordId}` | Delete a project record |

### Administration (Admin only)

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/user/get` | All users |
| POST | `/api/v1/user/add` | Create a user |
| PUT | `/api/v1/user/update/{userId}` | Update a user |
| DELETE | `/api/v1/user/delete/{userId}` | Delete a user |
| GET / POST / PUT / DELETE | `/api/v1/ai-interaction/{get\|add\|update\|delete}/…` | Raw AI request records |
| GET / POST / PUT / DELETE | `/api/v1/change-proposal/{get\|add\|update\|delete}/…` | Raw AI proposals |
| GET / POST / PUT / DELETE | `/api/v1/change-proposal-item/{get\|add\|update\|delete}/…` | Raw proposal changes |
| GET / POST / PUT / DELETE | `/api/v1/tool-run/{get\|add\|update\|delete}/…` | Raw tool-run records |


===================================khalid==================================
SANA — Backend Contributions
This document describes my backend contributions to SANA: Business domain integration, Moyasar sandbox billing, and registration welcome emails through n8n.

It is scoped to these contributions and does not document the entire team's backend or any frontend implementation.
1. Business Domain
Integrated the five Business analysis tools with the shared AI request lifecycle.
Business tool	Diagram type
BUSINESS_MODEL_BUILDER	BUSINESS_MODEL_CANVAS
TARGET_AUDIENCE_BUILDER	TARGET_AUDIENCE
SWOT_ANALYZER	SWOT
COMPETITOR_ANALYSIS	COMPETITOR_ANALYSIS
RISK_ANALYZER	RISK_ANALYSIS


Capabilities
- Submit Business requests through the backend to n8n.
- Track asynchronous execution and retrieve request status.
- Receive AI results through the shared n8n callback integration.
- Distinguish advice, clarification, and diagram proposals.
- Store structured analytical reports in execution history.
- Keep proposed diagram changes pending until user acceptance.
- Store accepted diagrams using the existing project output, node, and edge models.
- Enforce project ownership checks for project-scoped operations.
- Use graph revisions to reject changes based on an outdated diagram.
Request Flow
1. An authenticated user submits a request for their project.
2. Spring Boot prepares the request and supplied project context.
3. n8n executes the AI workflow.
4. n8n sends the result back to the backend.
5. The client retrieves the updated request status.
6. A diagram proposal requires user acceptance before its changes are applied.
AI instructions and output formatting are managed in n8n. Spring Boot owns persistence, authorization, request tracking, and proposal handling.
Main API Routes
Method	Route	Purpose
POST	/api/business/tools	Submit a Business tool request
POST	/api/projects/{projectId}/ai/requests	Submit a diagram or advice request
GET	/api/projects/{projectId}/ai/requests/{requestId}	Retrieve request status and result
POST	/api/integrations/n8n/ai/callback	Receive the protected n8n callback


These routes use their corresponding request DTOs; the Business tools endpoint and the general AI endpoint do not have interchangeable request bodies.
Independent Business Analysis Requests
Each Business analysis tool can be requested independently through the shared endpoint below. These are separate tool executions selected by toolType, not five different URL routes. They produce analytical reports rather than CREATE diagram proposals.
POST /api/business/tools
Content-Type: application/json
Authentication: use the authenticated user's registered email and password with HTTP Basic. Do not send the n8n integration keys from the client.
Analysis	Method	Endpoint	toolType
Business Model Builder	POST	/api/business/tools	BUSINESS_MODEL_BUILDER
Target Audience Builder	POST	/api/business/tools	TARGET_AUDIENCE_BUILDER
SWOT Analyzer	POST	/api/business/tools	SWOT_ANALYZER
Competitor Analysis	POST	/api/business/tools	COMPETITOR_ANALYSIS
Risk Analyzer	POST	/api/business/tools	RISK_ANALYZER


Example Request
{
  "projectId": 5,
  "toolType": "BUSINESS_MODEL_BUILDER",
  "inputJson": "{\"projectName\":\"حمل\",\"description\":\"منصة وتطبيق لتسهيل طلب نقل البضائع\",\"stage\":\"فكرة قيد الدراسة\",\"assumptions\":[\"استهداف الأفراد والمتاجر الصغيرة والمتوسطة\"],\"unknowns\":[\"التسعير\",\"تكاليف الرحلات\",\"آلية توفير الناقلين\",\"استعداد العملاء للدفع\"]}",
  "outputId": 3,
  "baseRevision": 0
}
The IDs and revision above are illustrative. Use a project and output owned by the authenticated user and the output's actual current revision. Whether outputId is required depends on the deployed BusinessToolRequestDTO; the earlier DTO required it. Independent execution means that tools do not require running the other four tools first.

To run another analysis, replace only toolType with the corresponding value from the table and supply the relevant input. Competitor analysis should include sourced competitor information when available; missing data must not be replaced with fabricated facts.

inputJson is a string containing valid JSON, not a nested JSON object. The outer request must escape quotation marks inside this string.
Retrieve the Result
The current integration returns HTTP 202 with an AIRunResponseDTO containing request metadata, rather than the final report synchronously. Use the returned statusUrl to retrieve the result:
GET /api/projects/{projectId}/ai/requests/{requestId}
- PENDING or RUNNING: execution has not finished.
- COMPLETED: inspect result. A structured report is returned as an ADVICE result with the serialized report in message; a genuine clarification may be returned instead.
- FAILED or TIMED_OUT: inspect the error and investigate the workflow before retrying.
For a structured report, preserve the complete JSON report when parsing result.message; do not keep only executiveSummary. Each tool's report follows its own schema. Analytical reports do not require accepting a graph proposal and must not modify the current graph.

This documents the current asynchronous n8n integration. It does not reintroduce the former direct ChatClient execution path or claim that obsolete routes are still available.
Example: Create a Business Model Diagram
POST /api/projects/{projectId}/ai/requests
{
  "task": "CREATE",
  "diagramType": "BUSINESS_MODEL_CANVAS",
  "prompt": "أنشئ نموذج عمل تجاري بناءً على وصف المشروع. وضّح الافتراضات ولا تخترع بيانات سوقية، واستخدم أنواع العناصر والعلاقات المسموحة في السياق."
}
Use an existing project owned by the authenticated user. A queued response confirms submission; inspect the status URL to verify completion.
2. Moyasar Sandbox Billing
Implemented backend billing for the three SANA plans.
Plan code	Display name	Price	Project limit
SPARK	شرارة	Free	1
GLOW	وهج	SAR 29 per monthly period	10
HORIZON	أفق	SAR 79 per monthly period	Unlimited


Amounts are stored and exchanged in halalas: 2900 for GLOW and 7900 for HORIZON.
Capabilities
- Retrieve the plan catalog and current user membership.
- Create a Moyasar sandbox checkout invoice.
- Retrieve the authenticated user's recent order history.
- Verify payment status server-side using Moyasar.
- Activate a paid membership only after payment verification.
- Prevent repeated verification from extending the same membership twice.
- Enforce project creation limits according to the effective membership.
- Reject a new paid checkout while a paid membership is active.
- Fall back to the free plan's effective limits when a paid period expires.
Billing API
All routes require authentication except the payment notification callback.
Method	Route	Purpose
GET	/api/billing/plans	Retrieve the plan catalog
GET	/api/billing/me	Retrieve current membership and project usage
GET	/api/billing/orders	Retrieve the user's recent orders
POST	/api/billing/checkout	Create a checkout invoice
POST	/api/billing/reconcile	Retrieve payment status from Moyasar and update membership
POST	/api/billing/callbacks/{orderId}/{token}	Process a protected payment notification


Checkout request:
{
  "planCode": "GLOW"
}
The response contains an order ID, a checkout URL, and the test-mode flag. A checkout URL alone does not mean payment has succeeded.
Payment Verification
- The callback uses an unguessable per-order token.
- The backend retrieves the invoice from Moyasar before changing membership.
- Invoice identity, amount, and currency must match the stored order.
- Payment status is not trusted from browser redirects or callback request bodies.
- Notification processing is idempotent for an already applied payment.
Main Billing Files
- Config/BillingProperties.java
- Config/BillingConfig.java
- Enums/BillingPlan.java
- Model/BillingAccount.java
- Model/BillingOrder.java
- Repository/BillingAccountRepository.java
- Repository/BillingOrderRepository.java
- DTO/Billing/BillingDTOs.java
- Service/Billing/BillingGateway.java
- Service/Billing/MoyasarGateway.java
- Service/Billing/BillingStore.java
- Service/Billing/BillingService.java
- Controller/BillingController.java
Existing security configuration, user and project repositories, and ProjectService were extended to support billing callbacks and project quotas.
Billing Limitations
- The integration is restricted to Moyasar test mode.
- Renewal is manual; automatic recurring charging is not implemented.
- Upgrades or renewals during an active paid period are rejected.
- Refund or reversal handling requires a subsequent verification; no scheduled refund polling is implemented.
- Billing verification can also be triggered through the reconcile endpoint.
3. Registration Welcome Emails
Connected successful user registration to a separate n8n email workflow.
Registration Flow
1. AuthService validates the account and hashes the password.
2. The user is saved within a database transaction.
3. A UserRegisteredEvent is published.
4. RegistrationNotificationService handles the event after the transaction commits.
5. The service sends the registration notification to n8n.
6. n8n sends an Arabic HTML welcome email using the configured SMTP account.
Files Added or Updated
File	Change
DTO/Notification/UserRegisteredEvent.java	Added a registration event containing user ID, name, and email
Service/RegistrationNotificationService.java	Added the authenticated n8n notification request
Service/AuthService.java	Added transactional registration and event publication
application.properties	Added notification environment-variable mappings


Notification Payload
{
  "eventType": "USER_REGISTERED",
  "userId": 4,
  "name": "Example User",
  "email": "user@example.com"
}
Passwords and password hashes are never included in the notification payload.
n8n Workflow
- A separate POST webhook receives registration events.
- Header authentication uses X-Sana-Notification-Key.
- The Send an Email node sends the HTML welcome message.
- The HTML template follows SANA's Arabic RTL presentation and cream, teal, and gold colors.
- Production registration uses the published production webhook URL, not the temporary test URL.
SMTP credentials, including the Gmail app password, remain inside n8n.
Email Delivery Behavior
- A failed notification does not undo successful registration.
- The backend uses bounded connection and request timeouts.
- The current listener runs synchronously after commit; the notification call may add up to five seconds.
- Automatic retries and a durable notification outbox are not implemented.
- A successful webhook response confirms n8n acceptance, not email delivery.
- This is a welcome email, not an email ownership verification mechanism.
4. Configuration
Keep secrets in environment variables and do not commit their values.
Shared n8n AI Integration
sana.n8n.business-enabled=true
sana.n8n.webhook-url=${N8N_AI_WEBHOOK_URL:}
sana.n8n.request-key=${N8N_AI_WEBHOOK_KEY:}
sana.n8n.callback-key=${N8N_AI_CALLBACK_KEY:}
The AI request and callback keys must be different.
Billing
sana.billing.enabled=${SANA_BILLING_ENABLED:false}
sana.billing.test-mode=${SANA_BILLING_TEST_MODE:true}
sana.billing.secret-key=${MOYASAR_SECRET_KEY:}
sana.billing.public-base-url=${SANA_PUBLIC_BASE_URL:}
sana.billing.frontend-base-url=${SANA_FRONTEND_BASE_URL:}
When enabled, billing requires a Moyasar test secret key and valid public HTTPS origins. The return URL setting is required by the checkout integration; no frontend implementation is included in this document.
Registration Notifications
sana.notifications.enabled=${SANA_NOTIFICATIONS_ENABLED:false}
sana.notifications.webhook-url=${N8N_NOTIFICATIONS_WEBHOOK_URL:}
sana.notifications.request-key=${N8N_NOTIFICATIONS_KEY:}
Required notification environment variables:
SANA_NOTIFICATIONS_ENABLED=true
N8N_NOTIFICATIONS_WEBHOOK_URL=https://YOUR_N8N_HOST/webhook/sana/user-registered
N8N_NOTIFICATIONS_KEY=YOUR_PRIVATE_NOTIFICATION_KEY
Use separate credentials for AI requests, AI callbacks, notifications, and Moyasar.
5. Authentication
User-facing project and billing requests use the project's existing authentication and ownership rules. For local Postman testing, use HTTP Basic with the registered email and password.

Registration remains public:
POST /api/v1/auth/register
Content-Type: application/json
{
  "name": "Example User",
  "email": "user@example.com",
  "passwordHash": "ReplaceWithYourTestPassword"
}
Despite its existing name, passwordHash accepts the registration password; AuthService hashes it before persistence. Use a new email address for each registration test.
6. Verification Completed
The following behaviors were confirmed during manual testing.
Business and Graph Persistence
- Added, retrieved, updated, and deleted diagram elements.
- Created and updated relationships between nodes.
- Rejected unsupported node types.
- Rejected stale graph revisions without applying changes.
- Rejected references to a deleted node.
- Verified unauthenticated and unauthorized project access handling.
- Retrieved completed AI advice and accepted diagram proposals.
Billing Sandbox
- Retrieved the plan catalog and free membership.
- Created and paid a sandbox GLOW invoice.
- Verified membership activation with a project limit of 10.
- Confirmed the order was stored as PAID.
- Repeated reconciliation without changing the existing subscription end time.
- Rejected another paid checkout during the active subscription.
- Confirmed that rejection did not create another order.
Welcome Email
- Tested the protected notification webhook from Postman.
- Confirmed receipt of the HTML welcome email.
- Registered a user through Spring Boot and confirmed the integrated welcome email flow.
These checks document the tested paths; they do not constitute live-payment certification or complete verification of every application feature.
7. Scope Exclusions
- Frontend components, styling, screens, and setup instructions.
